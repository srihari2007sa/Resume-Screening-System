package com.resumescreening.service;

import com.resumescreening.model.Candidate;
import com.resumescreening.model.JobDescription;
import com.resumescreening.model.ScreeningResult;
import com.resumescreening.repository.CandidateRepository;
import com.resumescreening.repository.JobDescriptionRepository;
import com.resumescreening.repository.ScreeningResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class ScreeningService {

    @Autowired
    private ResumeParserService parserService;

    @Autowired
    private OfflineScreeningService offlineScreeningService;

    @Autowired
    private GeminiAiService geminiAiService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @Autowired
    private ScreeningResultRepository screeningResultRepository;

    @org.springframework.beans.factory.annotation.Value("${gemini.api.key:}")
    private String defaultGeminiApiKey;

    /**
     * Executes the main screening workflow.
     * 1. Parses raw resume text from file
     * 2. Identifies or registers Candidate
     * 3. Selects Offline or Online (Gemini) screening based on API Key presence
     * 4. Saves and returns the final screening transaction
     */
    @Transactional
    public ScreeningResult screenResume(MultipartFile file, Long jobId, String geminiApiKey) throws Exception {
        // Resolve API Key: prioritize application.properties config if set, otherwise fallback to browser header key
        String activeApiKey = (defaultGeminiApiKey != null && !defaultGeminiApiKey.trim().isEmpty()) ? defaultGeminiApiKey : geminiApiKey;

        // Fetch the corresponding Job Description
        JobDescription job = jobDescriptionRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job Description not found for ID: " + jobId));

        // 1. Extract text from resume using Tika
        String rawText = parserService.parseText(file);
        String filename = file.getOriginalFilename();
        if (filename == null) {
            filename = "resume.txt";
        }

        // Extract basic fields offline as a standard initial step/fallback
        String email = parserService.extractEmail(rawText);
        String phone = parserService.extractPhone(rawText);
        String name = parserService.extractName(rawText, filename);

        // 2. Fetch existing or create new Candidate
        Candidate candidate;
        if (!email.isEmpty() && !email.equalsIgnoreCase("Not Provided")) {
            Optional<Candidate> existingCandidate = candidateRepository.findByEmail(email);
            if (existingCandidate.isPresent()) {
                candidate = existingCandidate.get();
                // Update resume fields
                candidate.setResumeText(rawText);
                candidate.setResumeFilename(filename);
            } else {
                candidate = new Candidate(name, email, phone, "", 0, "", filename, rawText);
            }
        } else {
            // No email found, generate anonymous name format
            candidate = new Candidate(name, "no-email-" + System.currentTimeMillis() + "@example.com", phone, "", 0, "", filename, rawText);
        }

        // Save candidate initially to generate database ID
        candidate = candidateRepository.save(candidate);

        // 3. Perform screening
        ScreeningResult result;
        if (activeApiKey != null && !activeApiKey.trim().isEmpty()) {
            try {
                // Generative AI Screening
                result = geminiAiService.screenWithGemini(candidate, job, activeApiKey);
            } catch (Exception e) {
                // Log and Fallback to offline scoring if Gemini fails (e.g. invalid key or network issue)
                e.printStackTrace();
                System.err.println("Gemini AI failed: " + e.getMessage() + ". Falling back to high-performance Offline Engine.");
                result = offlineScreeningService.screen(candidate, job);
                result.setAiSummary(result.getAiSummary() + " (Note: Gemini API failed, offline engine was used as fallback. Error: " + e.getMessage() + ")");
            }
        } else {
            // Core Offline NLP Scoring
            result = offlineScreeningService.screen(candidate, job);
        }

        // 4. Update the candidate entity with parsed/updated metrics
        candidateRepository.save(candidate);

        // Associate relationship and save results
        result.setCandidate(candidate);
        result.setJobDescription(job);
        
        return screeningResultRepository.save(result);
    }

    /**
     * Re-screens a candidate already stored in the database against any job.
     * No file upload required — uses the resume text stored from the original screening.
     */
    @Transactional
    public ScreeningResult rescreenCandidate(Long candidateId, Long jobId, String geminiApiKey) throws Exception {
        String activeApiKey = (defaultGeminiApiKey != null && !defaultGeminiApiKey.trim().isEmpty()) ? defaultGeminiApiKey : geminiApiKey;

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found for ID: " + candidateId));

        if (candidate.getResumeText() == null || candidate.getResumeText().isBlank()) {
            throw new IllegalStateException("No stored resume text for this candidate. Please upload the resume again.");
        }

        JobDescription job = jobDescriptionRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job Description not found for ID: " + jobId));

        ScreeningResult result;
        if (activeApiKey != null && !activeApiKey.trim().isEmpty()) {
            try {
                result = geminiAiService.screenWithGemini(candidate, job, activeApiKey);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Gemini AI failed on re-screen: " + e.getMessage() + ". Falling back to Offline Engine.");
                result = offlineScreeningService.screen(candidate, job);
                result.setAiSummary(result.getAiSummary() + " (Gemini fallback — re-screen. Error: " + e.getMessage() + ")");
            }
        } else {
            result = offlineScreeningService.screen(candidate, job);
        }

        candidateRepository.save(candidate);
        result.setCandidate(candidate);
        result.setJobDescription(job);

        return screeningResultRepository.save(result);
    }
}
