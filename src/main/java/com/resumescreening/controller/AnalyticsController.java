package com.resumescreening.controller;

import com.resumescreening.model.ScreeningResult;
import com.resumescreening.repository.ScreeningResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private ScreeningResultRepository screeningResultRepository;

    /**
     * REST Endpoint to aggregate stats: counts of shortlisted/rejected candidates and average score.
     */
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        List<ScreeningResult> results = screeningResultRepository.findAll();
        
        long total = results.size();
        long shortlisted = results.stream().filter(r -> "Shortlisted".equalsIgnoreCase(r.getMatchStatus())).count();
        long underReview = results.stream().filter(r -> "Under Review".equalsIgnoreCase(r.getMatchStatus())).count();
        long rejected = results.stream().filter(r -> "Rejected".equalsIgnoreCase(r.getMatchStatus())).count();
        
        double avgScore = 0.0;
        if (total > 0) {
            avgScore = results.stream().mapToDouble(ScreeningResult::getMatchScore).average().orElse(0.0);
            avgScore = Math.round(avgScore * 10.0) / 10.0;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalScreened", total);
        stats.put("shortlisted", shortlisted);
        stats.put("underReview", underReview);
        stats.put("rejected", rejected);
        stats.put("averageScore", avgScore);

        return stats;
    }

    /**
     * REST Endpoint mapping frequencies of parsed candidate skills.
     */
    @GetMapping("/skills")
    public Map<String, Integer> getSkillDistribution() {
        List<ScreeningResult> results = screeningResultRepository.findAll();
        Map<String, Integer> skillCounts = new HashMap<>();

        for (ScreeningResult result : results) {
            String skills = result.getCandidate().getExtractedSkills();
            if (skills != null && !skills.trim().isEmpty()) {
                String[] split = skills.split(",");
                for (String skill : split) {
                    String trimmedSkill = skill.trim();
                    if (!trimmedSkill.isEmpty()) {
                        skillCounts.put(trimmedSkill, skillCounts.getOrDefault(trimmedSkill, 0) + 1);
                    }
                }
            }
        }
        return skillCounts;
    }
}
