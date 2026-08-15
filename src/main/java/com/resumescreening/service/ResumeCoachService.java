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
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;
    
    @Value("${groq.api.key:}")
    private String groqApiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    
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
        // Load context first
        ScreeningResult result = screeningResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Screening result not found: " + resultId));

        Candidate candidate = result.getCandidate();
        JobDescription job = result.getJobDescription();
        
        // Try Groq first (FREE and super fast!)
        if (groqApiKey != null && !groqApiKey.trim().isEmpty()) {
            try {
                System.out.println("[AI Coach] Trying Groq AI (Llama 3)...");
                return chatWithGroq(resultId, userMessage, chatHistory);
            } catch (Exception e) {
                System.err.println("[AI Coach] Groq failed: " + e.getMessage());
                // Fall through to next option
            }
        }
        
        // Try Anthropic Claude if key is available
        if (anthropicApiKey != null && !anthropicApiKey.trim().isEmpty()) {
            try {
                System.out.println("[AI Coach] Trying Anthropic Claude...");
                return chatWithAnthropic(resultId, userMessage, chatHistory);
            } catch (Exception e) {
                System.err.println("[AI Coach] Anthropic failed: " + e.getMessage());
                // Fall through to next option
            }
        }
        
        // Try OpenAI if key is available
        if (openaiApiKey != null && !openaiApiKey.trim().isEmpty()) {
            try {
                System.out.println("[AI Coach] Trying OpenAI...");
                return chatWithOpenAI(resultId, userMessage, chatHistory);
            } catch (Exception e) {
                System.err.println("[AI Coach] OpenAI failed: " + e.getMessage());
                // Fall through to next option
            }
        }
        
        // Try Gemini if key is available
        String activeKey = (defaultApiKey != null && !defaultApiKey.trim().isEmpty()) ? defaultApiKey : apiKey;
        if (activeKey != null && !activeKey.trim().isEmpty()) {
            try {
                System.out.println("[AI Coach] Trying Gemini...");
                return chatWithGemini(resultId, userMessage, chatHistory, activeKey);
            } catch (Exception e) {
                System.err.println("[AI Coach] Gemini failed: " + e.getMessage());
                // Fall through to simple coach
            }
        }
        
        // Simple rule-based coach (always works - no API needed!)
        System.out.println("[AI Coach] All APIs unavailable, using simple rule-based coach");
        return generateSimpleCoachResponse(candidate, job, result, userMessage);
    }
    
    /**
     * Chat using Groq AI (FREE Llama 3!)
     */
    private String chatWithGroq(Long resultId, String userMessage, List<Map<String, String>> chatHistory) throws Exception {
        // Load context
        ScreeningResult result = screeningResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Screening result not found: " + resultId));

        Candidate candidate = result.getCandidate();
        JobDescription job = result.getJobDescription();
        String systemContext = buildSystemContext(candidate, job, result);

        // Build messages array
        JSONArray messages = new JSONArray();
        
        // System message
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", systemContext));
        
        // Add conversation history
        if (chatHistory != null) {
            for (Map<String, String> turn : chatHistory) {
                String role = turn.getOrDefault("role", "user");
                String text = turn.getOrDefault("text", "");
                if (!text.trim().isEmpty()) {
                    // Convert Gemini's "model" role to "assistant" role
                    String chatRole = role.equals("model") ? "assistant" : role;
                    messages.put(new JSONObject()
                            .put("role", chatRole)
                            .put("content", text));
                }
            }
        }
        
        // Add current user message
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userMessage));

        // Build Groq request
        JSONObject payload = new JSONObject();
        payload.put("model", "llama-3.3-70b-versatile"); // FREE and fast!
        payload.put("messages", messages);
        payload.put("max_tokens", 1024);
        payload.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + groqApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();

        System.out.println("[AI Coach] Using Groq Llama 3...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[AI Coach] Groq response status: " + response.statusCode());

        if (response.statusCode() != 200) {
            String errorBody = response.body();
            System.err.println("[AI Coach] Groq error: " + errorBody);
            throw new Exception("Groq API error " + response.statusCode());
        }

        // Parse response (same format as OpenAI)
        try {
            JSONObject jsonResponse = new JSONObject(response.body());
            String responseText = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
            return responseText.trim();
        } catch (Exception e) {
            throw new Exception("Failed to parse Groq response");
        }
    }
    
    /**
     * Simple rule-based coach - works without any API!
     */
    private String generateSimpleCoachResponse(Candidate candidate, JobDescription job, ScreeningResult result, String message) {
        String lowerMsg = message.toLowerCase();
        
        // Parse what the user is asking about
        if (lowerMsg.contains("improve") || lowerMsg.contains("better") || lowerMsg.contains("enhance")) {
            return String.format("**How %s can improve for %s:**\n\n" +
                    "Based on the screening (score: %.0f%%), here are specific recommendations:\n\n" +
                    "**Skill Gaps to Address:**\n%s\n\n" +
                    "**Actionable Steps:**\n" +
                    "1. Add projects demonstrating the missing skills\n" +
                    "2. Get certifications in key areas: %s\n" +
                    "3. Update resume to highlight relevant experience\n" +
                    "4. Add quantifiable achievements (e.g., 'Improved performance by 30%%')\n\n" +
                    "*This is a simple AI coach response. For advanced AI coaching, please add credits to OpenAI or configure a valid Gemini API key.*",
                    candidate.getName(), job.getTitle(),
                    result.getMatchScore(),
                    result.getAiWeaknesses() != null ? result.getAiWeaknesses() : "No major gaps identified",
                    job.getRequiredSkills());
        }
        
        if (lowerMsg.contains("skill") || lowerMsg.contains("learn")) {
            String[] requiredSkills = job.getRequiredSkills().split(",");
            String[] candidateSkills = (candidate.getExtractedSkills() != null ? candidate.getExtractedSkills() : "").split(",");
            
            StringBuilder missing = new StringBuilder();
            for (String req : requiredSkills) {
                boolean found = false;
                for (String has : candidateSkills) {
                    if (has.trim().equalsIgnoreCase(req.trim())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    missing.append("• ").append(req.trim()).append("\n");
                }
            }
            
            return String.format("**Skills Analysis for %s:**\n\n" +
                    "**Required Skills:** %s\n\n" +
                    "**Current Skills:** %s\n\n" +
                    "**Skills to Learn:**\n%s\n" +
                    "**Learning Resources:**\n" +
                    "• Online courses: Udemy, Coursera, Pluralsight\n" +
                    "• Practice projects on GitHub\n" +
                    "• Join relevant communities and forums\n\n" +
                    "*This is a simple AI coach response.*",
                    candidate.getName(),
                    job.getRequiredSkills(),
                    candidate.getExtractedSkills() != null ? candidate.getExtractedSkills() : "None extracted",
                    missing.length() > 0 ? missing.toString() : "All required skills present!");
        }
        
        if (lowerMsg.contains("cover letter") || lowerMsg.contains("letter")) {
            return String.format("**Cover Letter Template for %s:**\n\n" +
                    "Dear Hiring Manager,\n\n" +
                    "I am writing to express my interest in the %s position at your organization. " +
                    "With %s years of experience and expertise in %s, I am confident I can contribute significantly to your team.\n\n" +
                    "**Key Qualifications:**\n%s\n\n" +
                    "I am particularly excited about this opportunity because [add specific reason]. " +
                    "My background in [add relevant experience] aligns perfectly with your requirements.\n\n" +
                    "I would welcome the opportunity to discuss how my skills can benefit your team.\n\n" +
                    "Best regards,\n%s\n\n" +
                    "*This is a template. Customize it with specific details.*",
                    job.getTitle(),
                    job.getTitle(),
                    candidate.getExperienceYears() != null ? candidate.getExperienceYears() : 0,
                    candidate.getExtractedSkills() != null ? candidate.getExtractedSkills() : "relevant skills",
                    result.getAiStrengths() != null ? result.getAiStrengths() : "Strong technical background",
                    candidate.getName());
        }
        
        // Default response
        return String.format("**AI Coach for %s applying to %s**\n\n" +
                "**Current Match:** %.0f%% (%s)\n\n" +
                "**Strengths:**\n%s\n\n" +
                "**Areas for Improvement:**\n%s\n\n" +
                "**Quick Tips:**\n" +
                "• Ask me 'How can I improve?' for detailed recommendations\n" +
                "• Ask 'What skills should I learn?' for skill gap analysis\n" +
                "• Ask 'Write a cover letter' for a template\n\n" +
                "*This is a simple AI coach. For advanced AI responses, add OpenAI credits or configure Gemini API.*",
                candidate.getName(), job.getTitle(),
                result.getMatchScore(), result.getMatchStatus(),
                result.getAiStrengths() != null ? result.getAiStrengths() : "Good overall profile",
                result.getAiWeaknesses() != null ? result.getAiWeaknesses() : "Minor skill gaps");
    }
    
    /**
     * Chat using Anthropic Claude (best free tier!)
     */
    private String chatWithAnthropic(Long resultId, String userMessage, List<Map<String, String>> chatHistory) throws Exception {
        // Load context
        ScreeningResult result = screeningResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Screening result not found: " + resultId));

        Candidate candidate = result.getCandidate();
        JobDescription job = result.getJobDescription();
        String systemContext = buildSystemContext(candidate, job, result);

        // Build messages array for Anthropic
        JSONArray messages = new JSONArray();
        
        // Add conversation history
        if (chatHistory != null) {
            for (Map<String, String> turn : chatHistory) {
                String role = turn.getOrDefault("role", "user");
                String text = turn.getOrDefault("text", "");
                if (!text.trim().isEmpty()) {
                    // Convert Gemini's "model" role to Anthropic's "assistant" role
                    String anthropicRole = role.equals("model") ? "assistant" : role;
                    messages.put(new JSONObject()
                            .put("role", anthropicRole)
                            .put("content", text));
                }
            }
        }
        
        // Add current user message
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userMessage));

        // Build Anthropic request
        JSONObject payload = new JSONObject();
        payload.put("model", "claude-3-5-sonnet-20241022"); // Latest Claude model
        payload.put("system", systemContext); // Anthropic uses system parameter
        payload.put("messages", messages);
        payload.put("max_tokens", 1024);
        payload.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();

        System.out.println("[AI Coach] Using Anthropic Claude...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[AI Coach] Anthropic response status: " + response.statusCode());

        if (response.statusCode() != 200) {
            String errorBody = response.body();
            System.err.println("[AI Coach] Anthropic error: " + errorBody);
            throw new Exception("Anthropic API error " + response.statusCode());
        }

        // Parse Anthropic response
        try {
            JSONObject jsonResponse = new JSONObject(response.body());
            String responseText = jsonResponse
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text");
            return responseText.trim();
        } catch (Exception e) {
            throw new Exception("Failed to parse Anthropic response");
        }
    }
    
    /**
     * Chat using OpenAI GPT-4 (more reliable!)
     */
    private String chatWithOpenAI(Long resultId, String userMessage, List<Map<String, String>> chatHistory) throws Exception {
        // Load context
        ScreeningResult result = screeningResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Screening result not found: " + resultId));

        Candidate candidate = result.getCandidate();
        JobDescription job = result.getJobDescription();
        String systemContext = buildSystemContext(candidate, job, result);

        // Build messages array for OpenAI
        JSONArray messages = new JSONArray();
        
        // System message
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", systemContext));
        
        // Add conversation history
        if (chatHistory != null) {
            for (Map<String, String> turn : chatHistory) {
                String role = turn.getOrDefault("role", "user");
                String text = turn.getOrDefault("text", "");
                if (!text.trim().isEmpty()) {
                    // Convert Gemini's "model" role to OpenAI's "assistant" role
                    String openaiRole = role.equals("model") ? "assistant" : role;
                    messages.put(new JSONObject()
                            .put("role", openaiRole)
                            .put("content", text));
                }
            }
        }
        
        // Add current user message
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userMessage));

        // Build OpenAI request
        JSONObject payload = new JSONObject();
        payload.put("model", "gpt-4o-mini"); // Fast and affordable
        payload.put("messages", messages);
        payload.put("max_tokens", 1024);
        payload.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();

        System.out.println("[AI Coach] Using OpenAI GPT-4...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[AI Coach] OpenAI response status: " + response.statusCode());

        if (response.statusCode() != 200) {
            System.err.println("[AI Coach] OpenAI error: " + response.body());
            return "⚠️ AI Coach temporarily unavailable (OpenAI error " + response.statusCode() + "). Please try again.";
        }

        // Parse OpenAI response
        try {
            JSONObject jsonResponse = new JSONObject(response.body());
            String responseText = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
            return responseText.trim();
        } catch (Exception e) {
            return "Sorry, I couldn't parse the AI response. Please try again.";
        }
    }
    
    /**
     * Chat using Gemini (original implementation)
     */
    private String chatWithGemini(Long resultId, String userMessage, List<Map<String, String>> chatHistory, String activeKey) throws Exception {

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

        System.out.println("[AI Coach] Calling Gemini API...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[AI Coach] Response status: " + response.statusCode());

        if (response.statusCode() != 200) {
            // Throw exception so fallback coach can activate
            String errorBody = response.body();
            System.err.println("[AI Coach] Gemini error " + response.statusCode() + ": " + errorBody);
            throw new Exception("Gemini API error " + response.statusCode());
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
            throw new Exception("Failed to parse Gemini response: " + e.getMessage());
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
