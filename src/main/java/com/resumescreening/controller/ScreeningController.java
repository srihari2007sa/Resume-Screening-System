package com.resumescreening.controller;

import com.resumescreening.model.ScreeningResult;
import com.resumescreening.repository.ScreeningResultRepository;
import com.resumescreening.service.ScreeningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/screen")
@CrossOrigin(origins = "*")
public class ScreeningController {

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private ScreeningResultRepository screeningResultRepository;

    /**
     * Single resume screening.
     */
    @PostMapping
    public ResponseEntity<?> screenResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobId") Long jobId,
            @RequestHeader(value = "X-Gemini-Key", required = false) String geminiApiKey) {
        try {
            ScreeningResult result = screeningService.screenResume(file, jobId, geminiApiKey);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error screening resume: " + e.getMessage());
        }
    }

    /**
     * Batch resume screening — accepts multiple files against one job.
     * Returns a list of results in the same order as submitted files.
     * Failed individual files are reported inline rather than aborting the whole batch.
     */
    @PostMapping("/batch")
    public ResponseEntity<?> screenBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("jobId") Long jobId,
            @RequestHeader(value = "X-Gemini-Key", required = false) String geminiApiKey) {

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("No files provided for batch screening.");
        }
        if (files.length > 20) {
            return ResponseEntity.badRequest().body("Batch limit is 20 files per request.");
        }

        List<Map<String, Object>> batchResults = new ArrayList<>();

        for (MultipartFile file : files) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("filename", file.getOriginalFilename());
            try {
                ScreeningResult result = screeningService.screenResume(file, jobId, geminiApiKey);
                entry.put("success", true);
                entry.put("result", result);
            } catch (Exception e) {
                entry.put("success", false);
                entry.put("error", e.getMessage());
            }
            batchResults.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalSubmitted", files.length);
        response.put("totalSucceeded", batchResults.stream().filter(r -> Boolean.TRUE.equals(r.get("success"))).count());
        response.put("totalFailed", batchResults.stream().filter(r -> !Boolean.TRUE.equals(r.get("success"))).count());
        response.put("results", batchResults);

        return ResponseEntity.ok(response);
    }

    /**
     * Candidate comparison — returns structured side-by-side data for 2-5 result IDs.
     */
    @GetMapping("/compare")
    public ResponseEntity<?> compareResults(@RequestParam("ids") String ids) {
        List<Long> idList;
        try {
            idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid IDs format. Use comma-separated numbers.");
        }

        if (idList.size() < 2 || idList.size() > 5) {
            return ResponseEntity.badRequest().body("Compare requires 2 to 5 result IDs.");
        }

        List<ScreeningResult> results = screeningResultRepository.findAllById(idList);
        if (results.size() < 2) {
            return ResponseEntity.badRequest().body("Could not find enough results to compare.");
        }

        // Build a structured comparison payload the frontend can render as a table
        List<Map<String, Object>> comparison = new ArrayList<>();
        for (ScreeningResult r : results) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("resultId", r.getId());
            entry.put("candidateName", r.getCandidate().getName());
            entry.put("candidateEmail", r.getCandidate().getEmail());
            entry.put("matchScore", r.getMatchScore());
            entry.put("matchStatus", r.getMatchStatus());
            entry.put("screeningMode", r.getScreeningMode());
            entry.put("experienceYears", r.getCandidate().getExperienceYears());
            entry.put("education", r.getCandidate().getEducation());
            entry.put("extractedSkills", r.getCandidate().getExtractedSkills());
            entry.put("aiSummary", r.getAiSummary());
            entry.put("strengths", r.getAiStrengths());
            entry.put("weaknesses", r.getAiWeaknesses());
            entry.put("careerDnaProfile", r.getCareerDnaProfile());
            entry.put("screenedAt", r.getScreenedAt());
            comparison.add(entry);
        }

        return ResponseEntity.ok(comparison);
    }

    /**
     * Score history for a single candidate across all their screenings over time.
     */
    @GetMapping("/history/{candidateId}")
    public ResponseEntity<?> getCandidateHistory(@PathVariable Long candidateId) {
        List<ScreeningResult> history = screeningResultRepository.findByCandidateId(candidateId);
        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Sort ascending by screenedAt for a timeline view
        history.sort(Comparator.comparing(ScreeningResult::getScreenedAt));

        List<Map<String, Object>> timeline = history.stream().map(r -> {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("resultId", r.getId());
            point.put("jobTitle", r.getJobDescription().getTitle());
            point.put("matchScore", r.getMatchScore());
            point.put("matchStatus", r.getMatchStatus());
            point.put("screeningMode", r.getScreeningMode());
            point.put("screenedAt", r.getScreenedAt());
            return point;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("candidateId", candidateId);
        response.put("candidateName", history.get(0).getCandidate().getName());
        response.put("totalScreenings", history.size());
        response.put("bestScore", history.stream().mapToDouble(ScreeningResult::getMatchScore).max().orElse(0));
        response.put("latestScore", history.get(history.size() - 1).getMatchScore());
        response.put("timeline", timeline);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all results.
     */
    @GetMapping("/results")
    public List<ScreeningResult> getAllResults() {
        return screeningResultRepository.findAll();
    }

    /**
     * Get results filtered by job.
     */
    @GetMapping("/results/job/{jobId}")
    public List<ScreeningResult> getResultsByJob(@PathVariable Long jobId) {
        return screeningResultRepository.findByJobDescriptionId(jobId);
    }

    /**
     * Update candidate screening status.
     */
    @PutMapping("/results/{id}/status")
    public ResponseEntity<ScreeningResult> updateStatus(
            @PathVariable Long id,
            @RequestParam("status") String status) {
        return screeningResultRepository.findById(id)
                .map(result -> {
                    result.setMatchStatus(status);
                    return ResponseEntity.ok(screeningResultRepository.save(result));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update recruiter notes on a screening result.
     */
    @PutMapping("/results/{id}/notes")
    public ResponseEntity<ScreeningResult> updateNotes(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return screeningResultRepository.findById(id)
                .map(result -> {
                    result.setRecruiterNotes(body.getOrDefault("notes", ""));
                    return ResponseEntity.ok(screeningResultRepository.save(result));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Re-screen a candidate who is already in the database against a (possibly different) job.
     * Uses stored resume text — no file upload needed.
     */
    @PostMapping("/rescreen")
    public ResponseEntity<?> rescreenCandidate(
            @RequestParam("candidateId") Long candidateId,
            @RequestParam("jobId") Long jobId,
            @RequestHeader(value = "X-Gemini-Key", required = false) String geminiApiKey) {
        try {
            ScreeningResult result = screeningService.rescreenCandidate(candidateId, jobId, geminiApiKey);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Re-screen failed: " + e.getMessage());
        }
    }

    /**
     * Delete a screening result.
     */
    @DeleteMapping("/results/{id}")
    public ResponseEntity<Void> deleteResult(@PathVariable Long id) {
        if (!screeningResultRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        screeningResultRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Clear all old results to start fresh.
     */
    @DeleteMapping("/results/clear-all")
    public ResponseEntity<?> clearAllResults() {
        try {
            screeningResultRepository.deleteAll();
            Map<String, String> response = new HashMap<>();
            response.put("message", "All screening results cleared successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error clearing results: " + e.getMessage());
        }
    }
}
