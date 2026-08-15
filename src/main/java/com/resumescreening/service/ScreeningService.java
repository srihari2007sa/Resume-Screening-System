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
    private GroqAiService groqAiService;

    @Autowired
    private GeminiAiService geminiAiService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @Autowired
    private ScreeningResultRepository screeningResultRepository;

    @org.springframework.beans.factory.annotation.Value("${groq.api.key:}")
    private String defaultGroqApiKey;
    
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
        // Try Groq first (primary AI), then Gemini as fallback
        String activeGroqKey = defaultGroqApiKey;
        String activeGeminiKey = (defaultGeminiApiKey != null && !defaultGeminiApiKey.trim().isEmpty()) ? defaultGeminiApiKey : geminiApiKey;

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

        // 3. Perform screening - try Groq first, then Gemini, then offline
        ScreeningResult result;
        
        // Try Groq AI first
        if (activeGroqKey != null && !activeGroqKey.trim().isEmpty()) {
            try {
                System.out.println("[Screening] Using Groq AI (Llama 3.3)...");
                result = groqAiService.screenWithGroq(candidate, job, activeGroqKey);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("[Screening] Groq AI failed: " + e.getMessage() + ". Trying Gemini...");
                
                // Try Gemini as fallback
                if (activeGeminiKey != null && !activeGeminiKey.trim().isEmpty()) {
                    try {
                        System.out.println("[Screening] Using Gemini AI...");
                        result = geminiAiService.screenWithGemini(candidate, job, activeGeminiKey);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        System.err.println("[Screening] Gemini also failed: " + e2.getMessage() + ". Using offline engine.");
                        result = offlineScreeningService.screen(candidate, job);
                        result.setAiSummary(result.getAiSummary() + " (Note: Both Groq and Gemini failed, offline engine used as fallback.)");
                    }
                } else {
                    // No Gemini key, use offline
                    result = offlineScreeningService.screen(candidate, job);
                    result.setAiSummary(result.getAiSummary() + " (Note: Groq API failed, offline engine used as fallback. Error: " + e.getMessage() + ")");
                }
            }
        } else if (activeGeminiKey != null && !activeGeminiKey.trim().isEmpty()) {
            // No Groq key, try Gemini
            try {
                System.out.println("[Screening] Using Gemini AI...");
                result = geminiAiService.screenWithGemini(candidate, job, activeGeminiKey);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("[Screening] Gemini AI failed: " + e.getMessage() + ". Using offline engine.");
                result = offlineScreeningService.screen(candidate, job);
                result.setAiSummary(result.getAiSummary() + " (Note: Gemini API failed, offline engine was used as fallback. Error: " + e.getMessage() + ")");
            }
        } else {
            // No AI keys available, use offline
            System.out.println("[Screening] No AI keys available, using offline heuristics engine.");
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
        String activeGroqKey = defaultGroqApiKey;
        String activeGeminiKey = (defaultGeminiApiKey != null && !defaultGeminiApiKey.trim().isEmpty()) ? defaultGeminiApiKey : geminiApiKey;

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found for ID: " + candidateId));

        if (candidate.getResumeText() == null || candidate.getResumeText().isBlank()) {
            throw new IllegalStateException("No stored resume text for this candidate. Please upload the resume again.");
        }

        JobDescription job = jobDescriptionRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job Description not found for ID: " + jobId));

        ScreeningResult result;
        
        // Try Groq first
        if (activeGroqKey != null && !activeGroqKey.trim().isEmpty()) {
            try {
                System.out.println("[Re-Screening] Using Groq AI (Llama 3.3)...");
                result = groqAiService.screenWithGroq(candidate, job, activeGroqKey);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("[Re-Screening] Groq failed: " + e.getMessage() + ". Trying Gemini...");
                
                if (activeGeminiKey != null && !activeGeminiKey.trim().isEmpty()) {
                    try {
                        result = geminiAiService.screenWithGemini(candidate, job, activeGeminiKey);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        System.err.println("[Re-Screening] Gemini also failed. Using offline engine.");
                        result = offlineScreeningService.screen(candidate, job);
                        result.setAiSummary(result.getAiSummary() + " (Both Groq and Gemini failed on re-screen.)");
                    }
                } else {
                    result = offlineScreeningService.screen(candidate, job);
                    result.setAiSummary(result.getAiSummary() + " (Groq fallback — re-screen. Error: " + e.getMessage() + ")");
                }
            }
        } else if (activeGeminiKey != null && !activeGeminiKey.trim().isEmpty()) {
            try {
                result = geminiAiService.screenWithGemini(candidate, job, activeGeminiKey);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("[Re-Screening] Gemini failed: " + e.getMessage() + ". Using offline engine.");
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
