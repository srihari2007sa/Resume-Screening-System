package com.resumescreening.controller;

import com.resumescreening.model.JobDescription;
import com.resumescreening.repository.JobDescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @GetMapping
    public List<JobDescription> getAllJobs() {
        return jobDescriptionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDescription> getJobById(@PathVariable Long id) {
        return jobDescriptionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public JobDescription createJob(@RequestBody JobDescription jobDescription) {
        // Trim comma-separated skill strings
        if (jobDescription.getRequiredSkills() != null) {
            jobDescription.setRequiredSkills(jobDescription.getRequiredSkills().trim());
        }
        if (jobDescription.getPreferredSkills() != null) {
            jobDescription.setPreferredSkills(jobDescription.getPreferredSkills().trim());
        }
        return jobDescriptionRepository.save(jobDescription);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        if (!jobDescriptionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        jobDescriptionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
