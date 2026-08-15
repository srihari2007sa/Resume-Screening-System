package com.resumescreening.service;

import com.resumescreening.model.Candidate;
import com.resumescreening.model.JobDescription;
import com.resumescreening.model.ScreeningResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class GroqAiService {

    @Autowired
    private CareerDnaService careerDnaService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .proxy(ProxySelector.getDefault())
            .build();

    /**
     * Integrates with Groq AI API using Llama 3.3 70B model.
     * Evaluates the candidate resume semantically and returns a structured AI report
     * including a Career DNA archetype profile.
     */
    public ScreeningResult screenWithGroq(Candidate candidate, JobDescription jobDescription, String apiKey) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Groq API Key is missing. Please configure it in your Settings.");
        }

        String prompt = buildPrompt(candidate, jobDescription);
        String url = "https://api.groq.com/openai/v1/chat/completions";

        // Build messages array (Groq uses OpenAI-compatible format)
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "You are an expert AI recruiter and ATS screening parser. Return ONLY valid JSON. No markdown, no code blocks."));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", prompt));

        JSONObject payload = new JSONObject();
        payload.put("model", "llama-3.3-70b-versatile");
        payload.put("messages", messages);
        payload.put("max_tokens", 2048);
        payload.put("temperature", 0.3);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();

        System.out.println("Groq HTTP Request Details:");
        System.out.println("  URL: " + url);
        System.out.println("  API Key Length: " + apiKey.length());
        System.out.println("  Payload Length: " + payload.toString().length());

        long start = System.currentTimeMillis();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Groq response received in " + (System.currentTimeMillis() - start) + " ms. Status: " + response.statusCode());
        } catch (Exception ex) {
            System.err.println("Groq HTTP Request failed after " + (System.currentTimeMillis() - start) + " ms.");
            throw ex;
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq API call failed with status: " + response.statusCode() + ". Body: " + response.body());
        }

        // Parse Groq response (OpenAI-compatible format)
        JSONObject jsonResponse = new JSONObject(response.body());
        String responseText = jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        return parseAiResponse(responseText, candidate, jobDescription);
    }

    /**
     * Parses the AI text response (JSON string) into a ScreeningResult.
     */
    private ScreeningResult parseAiResponse(String responseText, Candidate candidate, JobDescription jobDescription) throws Exception {
        // Strip markdown code fences if present
        String cleanJson = responseText.trim();
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.replaceAll("^```(?:json)?\\s*", "")
                                 .replaceAll("\\s*```$", "");
        }

        // Find the JSON object within the text (handles extra text around it)
        int braceStart = cleanJson.indexOf('{');
        int braceEnd   = cleanJson.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            cleanJson = cleanJson.substring(braceStart, braceEnd + 1);
        }

        JSONObject ai = new JSONObject(cleanJson);

        // Update candidate fields with AI-parsed values
        if (ai.has("candidateName") && !ai.getString("candidateName").trim().isEmpty()) {
            candidate.setName(ai.getString("candidateName"));
        }
        if (ai.has("candidateEmail") && !ai.getString("candidateEmail").trim().isEmpty()) {
            candidate.setEmail(ai.getString("candidateEmail"));
        }
        if (ai.has("candidatePhone") && !ai.getString("candidatePhone").trim().isEmpty()) {
            candidate.setPhone(ai.getString("candidatePhone"));
        }
        if (ai.has("extractedSkills")) {
            candidate.setExtractedSkills(ai.getString("extractedSkills"));
        }
        if (ai.has("experienceYears")) {
            candidate.setExperienceYears(ai.getInt("experienceYears"));
        }
        if (ai.has("education")) {
            candidate.setEducation(ai.getString("education"));
        }

        double matchScore  = ai.optDouble("matchScore", 50.0);
        String matchStatus = ai.optString("matchStatus", "Under Review");
        String aiSummary   = ai.optString("aiSummary", "Screened successfully via Groq AI.");

        List<String> strengths = new ArrayList<>();
        if (ai.has("strengths")) {
            JSONArray arr = ai.getJSONArray("strengths");
            for (int i = 0; i < arr.length(); i++) strengths.add(arr.getString(i));
        }

        List<String> weaknesses = new ArrayList<>();
        if (ai.has("weaknesses")) {
            JSONArray arr = ai.getJSONArray("weaknesses");
            for (int i = 0; i < arr.length(); i++) weaknesses.add(arr.getString(i));
        }

        List<String> questions = new ArrayList<>();
        if (ai.has("suggestedQuestions")) {
            JSONArray arr = ai.getJSONArray("suggestedQuestions");
            for (int i = 0; i < arr.length(); i++) questions.add(arr.getString(i));
        }

        ScreeningResult result = new ScreeningResult(
                candidate,
                jobDescription,
                matchScore,
                matchStatus,
                "GROQ_AI",
                aiSummary,
                String.join("\n", strengths),
                String.join("\n", weaknesses),
                String.join("\n", questions)
        );

        // Compute and attach Career DNA profile
        CareerDnaService.CareerDnaProfile dna = careerDnaService.computeProfile(
                candidate.getResumeText(), candidate.getExtractedSkills());
        result.setCareerDnaProfile(buildDnaJson(dna));

        return result;
    }

    /**
     * Serialises a CareerDnaProfile into a compact JSON string for DB storage.
     */
    private String buildDnaJson(CareerDnaService.CareerDnaProfile dna) {
        JSONObject obj = new JSONObject();
        obj.put("dominantArchetype",   dna.getDominantArchetype());
        obj.put("secondaryArchetype",  dna.getSecondaryArchetype());
        obj.put("dnaStrength",         dna.getDnaStrength());
        obj.put("archetypeDescription", dna.getArchetypeDescription());
        JSONObject scores = new JSONObject();
        dna.getArchetypeScores().forEach(scores::put);
        obj.put("archetypeScores", scores);
        return obj.toString();
    }

    /**
     * Builds the AI prompt.
     */
    private String buildPrompt(Candidate candidate, JobDescription jobDescription) {
        return "You are an expert AI recruiter and ATS screening parser. " +
                "Evaluate the following CANDIDATE RESUME against the JOB DESCRIPTION. " +
                "Extract candidate credentials and compute suitability metrics. " +
                "Be highly objective. Return ONLY a plain JSON object. Do not wrap in markdown or code blocks.\n\n" +
                "=== JOB DESCRIPTION ===\n" +
                "Title: " + jobDescription.getTitle() + "\n" +
                "Department: " + jobDescription.getDepartment() + "\n" +
                "Required Skills: " + jobDescription.getRequiredSkills() + "\n" +
                "Required Min Experience: " + jobDescription.getMinExperience() + " years\n" +
                "Required Min Education: " + jobDescription.getMinEducation() + "\n" +
                "Job Details: " + jobDescription.getDescription() + "\n\n" +
                "=== CANDIDATE RESUME TEXT ===\n" +
                candidate.getResumeText() + "\n\n" +
                "=== EXPECTED JSON RESPONSE FORMAT ===\n" +
                "{\n" +
                "  \"candidateName\": \"Full Name\",\n" +
                "  \"candidateEmail\": \"email@example.com\",\n" +
                "  \"candidatePhone\": \"phone number\",\n" +
                "  \"extractedSkills\": \"Comma-separated skills e.g. Java, Docker, Git\",\n" +
                "  \"experienceYears\": 5,\n" +
                "  \"education\": \"Highest degree e.g. Bachelor of Technology in CS\",\n" +
                "  \"matchScore\": 85.5,\n" +
                "  \"matchStatus\": \"Shortlisted | Under Review | Rejected (>=75 Shortlisted, 50-74 Under Review, <50 Rejected)\",\n" +
                "  \"aiSummary\": \"2-3 sentence professional summary of suitability.\",\n" +
                "  \"strengths\": [\"Strength 1\", \"Strength 2\"],\n" +
                "  \"weaknesses\": [\"Gap 1\", \"Gap 2\"],\n" +
                "  \"suggestedQuestions\": [\"Interview question 1\", \"Interview question 2\"]\n" +
                "}";
    }
}
