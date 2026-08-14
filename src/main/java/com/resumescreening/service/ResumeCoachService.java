package com.resumescreening.service;

import com.resumescreening.model.Candidate;
import com.resumescreening.model.JobDescription;
import com.resumescreening.model.ScreeningResult;
import com.resumescreening.repository.ScreeningResultRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 *  NOVELTY FEATURE — AI Resume Coach (Conversational)
 * ============================================================
 *
 * What makes this unique:
 *   Every other ATS gives you a static one-shot report.
 *   KIT AI Resume Coach lets recruiters and candidates have
 *   a live multi-turn conversation with Gemini AI that is
 *   fully GROUNDED in a specific candidate's resume AND a
 *   specific job description.
 *
 *   Examples of what you can ask:
 *   - "How can Sri Hari improve his resume for this role?"
 *   - "What projects should he add to get shortlisted?"
 *   - "Write a cover letter for this candidate."
 *   - "What salary range is he likely to get?"
 *   - "Compare his skills with the top requirement gaps."
 *
 *   The conversation history is maintained per session so
 *   follow-up questions work correctly (multi-turn chat).
 *
 *   No existing open-source ATS product ships this feature.
 * ============================================================
 */
@Service
public class ResumeCoachService {

    @Autowired
    private ScreeningResultRepository screeningResultRepository;

    @Value("${gemini.api.key:}")
    private String defaultApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .proxy(ProxySelector.getDefault())
            .build();

    /**
     * Sends a user message to the AI Resume Coach and gets a response.
     * The conversation is grounded in the candidate's resume and job description.
     *
     * @param resultId       The screening result ID to ground the conversation in
     * @param userMessage    The recruiter/user's question or request
     * @param chatHistory    Previous turns: list of {role: "user"|"model", text: "..."}
     * @param apiKey         Gemini API key (optional — falls back to config)
     * @return AI coach response text
     */
    public String chat(Long resultId, String userMessage, List<Map<String, String>> chatHistory, String apiKey) throws Exception {
        String activeKey = (defaultApiKey != null && !defaultApiKey.trim().isEmpty()) ? defaultApiKey : apiKey;
        if (activeKey == null || activeKey.trim().isEmpty()) {
            return "Gemini API key is not configured. Please set it in Settings to use the AI Resume Coach.";
        }

        // Load context from the screening result
        ScreeningResult result = screeningResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Screening result not found: " + resultId));

        Candidate candidate = result.getCandidate();
        JobDescription job = result.getJobDescription();

        // Build the system instruction (grounds the AI in this specific candidate+job)
        String systemContext = buildSystemContext(candidate, job, result);

        // Build the contents array with full conversation history
        JSONArray contents = new JSONArray();

        // First turn: inject system context as the first user message + model acknowledgement
        // This grounds the conversation without using a system instruction field
        JSONArray systemParts = new JSONArray();
        systemParts.put(new JSONObject().put("text", systemContext));
        contents.put(new JSONObject()
                .put("role", "user")
                .put("parts", systemParts));

        JSONArray ackParts = new JSONArray();
        ackParts.put(new JSONObject().put("text",
                "Understood. I am the AI Resume Coach for " + candidate.getName() +
                " applying to " + job.getTitle() + ". I have full context of their resume, " +
                "skills, screening score (" + Math.round(result.getMatchScore()) + "%), and the job requirements. " +
                "I am ready to provide personalized coaching advice. How can I help?"));
        contents.put(new JSONObject()
                .put("role", "model")
                .put("parts", ackParts));

        // Add previous conversation history
        if (chatHistory != null) {
            for (Map<String, String> turn : chatHistory) {
                String role = turn.getOrDefault("role", "user");
                String text = turn.getOrDefault("text", "");
                if (!text.trim().isEmpty()) {
                    JSONArray parts = new JSONArray();
                    parts.put(new JSONObject().put("text", text));
                    contents.put(new JSONObject().put("role", role).put("parts", parts));
                }
            }
        }

        // Add current user message
        JSONArray userParts = new JSONArray();
        userParts.put(new JSONObject().put("text", userMessage));
        contents.put(new JSONObject().put("role", "user").put("parts", userParts));

        // Build request payload
        JSONObject payload = new JSONObject();
        payload.put("contents", contents);

        // Generation config — keep responses focused and concise
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("maxOutputTokens", 1024);
        generationConfig.put("temperature", 0.7);
        payload.put("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", activeKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return "AI Coach is temporarily unavailable. Error " + response.statusCode() +
                   ". Please check your Gemini API key in Settings.";
        }

        // Parse the response
        try {
            JSONObject jsonResponse = new JSONObject(response.body());
            String responseText = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
            return responseText.trim();
        } catch (Exception e) {
            return "Sorry, I couldn't parse the AI response. Please try again.";
        }
    }

    /**
     * Builds the grounding context that gives the AI full knowledge of
     * the candidate, their resume, the job requirements, and the screening results.
     */
    private String buildSystemContext(Candidate candidate, JobDescription job, ScreeningResult result) {
        return "You are an expert AI Resume Coach embedded inside KIT AI, an intelligent resume screening system. " +
               "You have complete knowledge of the following candidate and job. " +
               "Your role is to give highly personalized, actionable coaching advice. " +
               "Be conversational, supportive, and specific. Use bullet points when listing multiple items.\n\n" +

               "=== CANDIDATE PROFILE ===\n" +
               "Name: " + candidate.getName() + "\n" +
               "Email: " + candidate.getEmail() + "\n" +
               "Experience: " + (candidate.getExperienceYears() != null ? candidate.getExperienceYears() + " years" : "Fresher") + "\n" +
               "Education: " + (candidate.getEducation() != null ? candidate.getEducation() : "Not specified") + "\n" +
               "Extracted Skills: " + (candidate.getExtractedSkills() != null ? candidate.getExtractedSkills() : "None extracted") + "\n\n" +

               "=== RESUME TEXT ===\n" +
               (candidate.getResumeText() != null ?
                       candidate.getResumeText().substring(0, Math.min(candidate.getResumeText().length(), 2000)) : "Not available") + "\n\n" +

               "=== TARGET JOB ===\n" +
               "Title: " + job.getTitle() + "\n" +
               "Department: " + job.getDepartment() + "\n" +
               "Required Skills: " + job.getRequiredSkills() + "\n" +
               "Preferred Skills: " + (job.getPreferredSkills() != null ? job.getPreferredSkills() : "None") + "\n" +
               "Min Experience: " + (job.getMinExperience() != null ? job.getMinExperience() + " years" : "Any") + "\n" +
               "Min Education: " + (job.getMinEducation() != null ? job.getMinEducation() : "Any") + "\n" +
               "Job Description: " + (job.getDescription() != null ? job.getDescription() : "Not provided") + "\n\n" +

               "=== SCREENING RESULT ===\n" +
               "Match Score: " + Math.round(result.getMatchScore()) + "%\n" +
               "Status: " + result.getMatchStatus() + "\n" +
               "Screening Mode: " + result.getScreeningMode() + "\n" +
               "AI Summary: " + (result.getAiSummary() != null ? result.getAiSummary() : "N/A") + "\n" +
               "Strengths: " + (result.getAiStrengths() != null ? result.getAiStrengths() : "N/A") + "\n" +
               "Weaknesses/Gaps: " + (result.getAiWeaknesses() != null ? result.getAiWeaknesses() : "N/A") + "\n\n" +

               "Always give specific, actionable advice based on THIS candidate's actual resume content. " +
               "Do not give generic advice. Reference specific skills, projects, or gaps from the data above.";
    }
}
