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
public class GeminiAiService {

    @Autowired
    private CareerDnaService careerDnaService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .proxy(ProxySelector.getDefault())
            .build();

    /**
     * Integrates with Google Gemini 2.5 Flash API using the developer's API key.
     * Evaluates the candidate resume semantically and returns a structured AI report
     * including a Career DNA archetype profile.
     */
    public ScreeningResult screenWithGemini(Candidate candidate, JobDescription jobDescription, String apiKey) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini API Key is missing. Please configure it in your Settings.");
        }

        String prompt = buildPrompt(candidate, jobDescription);

        // gemini-2.5-flash is the current stable model
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

        JSONObject payload = new JSONObject();
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", prompt));
        JSONArray contents = new JSONArray();
        contents.put(new JSONObject().put("parts", parts));
        payload.put("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();

        System.out.println("Gemini HTTP Request Details:");
        System.out.println("  URL: " + url);
        System.out.println("  API Key Length: " + (apiKey.length()));
        if (apiKey.length() > 10) {
            System.out.println("  API Key Masked: " + apiKey.substring(0, 5) + "..." + apiKey.substring(apiKey.length() - 5));
        }
        System.out.println("  Payload Length: " + payload.toString().length());

        long start = System.currentTimeMillis();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Gemini response received in " + (System.currentTimeMillis() - start) + " ms. Status: " + response.statusCode());
        } catch (Exception ex) {
            System.err.println("Gemini HTTP Request failed after " + (System.currentTimeMillis() - start) + " ms.");
            throw ex;
        }

        if (response.statusCode() == 403 || response.statusCode() == 404) {
            // AQ. auth keys require the newer Interactions API endpoint
            System.out.println("Retrying with Interactions API endpoint for AQ. auth key...");
            String interactionsUrl = "https://generativelanguage.googleapis.com/v1beta/interactions";
            JSONObject interactionsPayload = new JSONObject();
            interactionsPayload.put("model", "gemini-2.5-flash");
            interactionsPayload.put("input", prompt);

            HttpRequest retryRequest = HttpRequest.newBuilder()
                    .uri(URI.create(interactionsUrl))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(interactionsPayload.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            response = httpClient.send(retryRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Interactions API response: " + response.statusCode());

            if (response.statusCode() == 200) {
                // Parse interactions API response format
                JSONObject interactionsResponse = new JSONObject(response.body());
                String outputText = extractTextFromInteractionsResponse(interactionsResponse);
                return parseGeminiResponse(outputText, candidate, jobDescription);
            }
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API call failed with status: " + response.statusCode() + ". Body: " + response.body());
        }

        // Parse generateContent response
        JSONObject jsonResponse = new JSONObject(response.body());
        String responseText = jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        return parseGeminiResponse(responseText, candidate, jobDescription);
    }

    /**
     * Extracts plain text from the Interactions API response format.
     */
    private String extractTextFromInteractionsResponse(JSONObject response) {
        // Interactions API: response.steps[].modelOutput.content[].text.text
        try {
            JSONArray steps = response.optJSONArray("steps");
            if (steps != null) {
                for (int i = 0; i < steps.length(); i++) {
                    JSONObject step = steps.getJSONObject(i);
                    JSONObject modelOutput = step.optJSONObject("modelOutput");
                    if (modelOutput != null) {
                        JSONArray content = modelOutput.optJSONArray("content");
                        if (content != null) {
                            for (int j = 0; j < content.length(); j++) {
                                JSONObject part = content.getJSONObject(j);
                                JSONObject textObj = part.optJSONObject("text");
                                if (textObj != null && textObj.has("text")) {
                                    return textObj.getString("text");
                                }
                            }
                        }
                    }
                }
            }
            // Fallback: try outputText field
            if (response.has("outputText")) return response.getString("outputText");
        } catch (Exception e) {
            System.err.println("Could not parse Interactions API response: " + e.getMessage());
        }
        return "";
    }

    /**
     * Parses the Gemini text response (JSON string) into a ScreeningResult.
     */
    private ScreeningResult parseGeminiResponse(String responseText, Candidate candidate, JobDescription jobDescription) throws Exception {
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

        // Update candidate fields with Gemini-parsed values
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
        String aiSummary   = ai.optString("aiSummary", "Screened successfully via Gemini AI.");

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
                "GEMINI_AI",
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
    } // end parseGeminiResponse

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
     * Builds the Gemini prompt.
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
