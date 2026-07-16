package com.resumescreening.controller;

import com.resumescreening.model.Candidate;
import com.resumescreening.model.ScreeningResult;
import com.resumescreening.repository.CandidateRepository;
import com.resumescreening.repository.ScreeningResultRepository;
import com.resumescreening.service.CareerDnaService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dna")
@CrossOrigin(origins = "*")
public class CareerDnaController {

    @Autowired
    private ScreeningResultRepository screeningResultRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private CareerDnaService careerDnaService;

    /**
     * Returns the Career DNA profile stored on a screening result.
     * If the result pre-dates the DNA feature (careerDnaProfile is null),
     * it computes one on-the-fly and persists it for future calls.
     */
    @GetMapping("/{resultId}")
    public ResponseEntity<?> getDnaByResult(@PathVariable Long resultId) {
        Optional<ScreeningResult> opt = screeningResultRepository.findById(resultId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ScreeningResult result = opt.get();
        String dnaJson = result.getCareerDnaProfile();

        if (dnaJson == null || dnaJson.isBlank()) {
            // Back-fill: compute and save so it's cached going forward
            CareerDnaService.CareerDnaProfile dna = careerDnaService.computeProfile(
                    result.getCandidate().getResumeText(),
                    result.getCandidate().getExtractedSkills());

            JSONObject obj = new JSONObject();
            obj.put("dominantArchetype",    dna.getDominantArchetype());
            obj.put("secondaryArchetype",   dna.getSecondaryArchetype());
            obj.put("dnaStrength",          dna.getDnaStrength());
            obj.put("archetypeDescription", dna.getArchetypeDescription());
            JSONObject scores = new JSONObject();
            dna.getArchetypeScores().forEach(scores::put);
            obj.put("archetypeScores", scores);
            dnaJson = obj.toString();

            result.setCareerDnaProfile(dnaJson);
            screeningResultRepository.save(result);
        }

        // Parse stored JSON and return as a structured map (not a raw string)
        JSONObject parsed = new JSONObject(dnaJson);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resultId",            resultId);
        response.put("candidateName",       result.getCandidate().getName());
        response.put("dominantArchetype",   parsed.optString("dominantArchetype"));
        response.put("secondaryArchetype",  parsed.optString("secondaryArchetype"));
        response.put("dnaStrength",         parsed.optInt("dnaStrength"));
        response.put("archetypeDescription", parsed.optString("archetypeDescription"));

        // Convert archetypeScores JSONObject → sorted LinkedHashMap
        JSONObject scoresObj = parsed.optJSONObject("archetypeScores");
        Map<String, Integer> scores = new LinkedHashMap<>();
        if (scoresObj != null) {
            scoresObj.keySet().stream()
                    .sorted(Comparator.comparingInt(k -> -scoresObj.optInt(k)))
                    .forEach(k -> scores.put(k, scoresObj.optInt(k)));
        }
        response.put("archetypeScores", scores);

        return ResponseEntity.ok(response);
    }

    /**
     * Computes a live Career DNA profile directly from a candidate's stored data.
     * Useful for re-computing profiles after data corrections.
     */
    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<?> getDnaByCandidate(@PathVariable Long candidateId) {
        Optional<Candidate> opt = candidateRepository.findById(candidateId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Candidate candidate = opt.get();
        CareerDnaService.CareerDnaProfile dna = careerDnaService.computeProfile(
                candidate.getResumeText(), candidate.getExtractedSkills());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("candidateId",         candidateId);
        response.put("candidateName",       candidate.getName());
        response.put("dominantArchetype",   dna.getDominantArchetype());
        response.put("secondaryArchetype",  dna.getSecondaryArchetype());
        response.put("dnaStrength",         dna.getDnaStrength());
        response.put("archetypeDescription", dna.getArchetypeDescription());
        response.put("archetypeScores",     dna.getArchetypeScores());

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the top N archetypes across all screened candidates — useful for
     * analytics: "what types of engineers have applied this month?"
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getArchetypeTrends() {
        List<ScreeningResult> all = screeningResultRepository.findAll();
        Map<String, Integer> archetypeCounts = new LinkedHashMap<>();

        for (ScreeningResult result : all) {
            String dnaJson = result.getCareerDnaProfile();
            if (dnaJson == null || dnaJson.isBlank()) continue;
            try {
                JSONObject obj = new JSONObject(dnaJson);
                String dominant = obj.optString("dominantArchetype");
                if (!dominant.isBlank()) {
                    archetypeCounts.merge(dominant, 1, Integer::sum);
                }
            } catch (Exception ignored) {}
        }

        // Sort descending by count
        Map<String, Integer> sorted = new LinkedHashMap<>();
        archetypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));

        return ResponseEntity.ok(sorted);
    }
}
