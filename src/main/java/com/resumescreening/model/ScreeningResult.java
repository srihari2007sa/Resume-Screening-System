package com.resumescreening.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "screening_results")
public class ScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    @Column(name = "match_score")
    private Double matchScore; // Score from 0 to 100

    @Column(name = "match_status")
    private String matchStatus; // e.g. "Shortlisted", "Under Review", "Rejected"

    @Column(name = "screening_mode")
    private String screeningMode; // "OFFLINE" or "GEMINI_AI"

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_strengths", columnDefinition = "TEXT")
    private String aiStrengths; // comma-separated or newline list of strengths

    @Column(name = "ai_weaknesses", columnDefinition = "TEXT")
    private String aiWeaknesses; // comma-separated or newline list of weaknesses

    @Column(name = "suggested_questions", columnDefinition = "TEXT")
    private String suggestedQuestions; // newline list of interview questions

    /**
     * Career DNA JSON payload — stores archetype radar scores computed at screening time.
     * Example: {"dominantArchetype":"Backend Architect","archetypeScores":{"Backend Architect":78,...}}
     */
    @Column(name = "career_dna_profile", columnDefinition = "TEXT")
    private String careerDnaProfile;

    /**
     * Recruiter notes — free-text annotations added manually after screening.
     * e.g. "Spoke on phone, strong communication, fast learner."
     */
    @Column(name = "recruiter_notes", columnDefinition = "TEXT")
    private String recruiterNotes;

    @Column(name = "screened_at")
    private LocalDateTime screenedAt;

    public ScreeningResult() {
    }

    public ScreeningResult(Candidate candidate, JobDescription jobDescription, Double matchScore, String matchStatus, String screeningMode, String aiSummary, String aiStrengths, String aiWeaknesses, String suggestedQuestions) {
        this.candidate = candidate;
        this.jobDescription = jobDescription;
        this.matchScore = matchScore;
        this.matchStatus = matchStatus;
        this.screeningMode = screeningMode;
        this.aiSummary = aiSummary;
        this.aiStrengths = aiStrengths;
        this.aiWeaknesses = aiWeaknesses;
        this.suggestedQuestions = suggestedQuestions;
    }

    @PrePersist
    protected void onCreate() {
        screenedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public JobDescription getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(JobDescription jobDescription) {
        this.jobDescription = jobDescription;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getScreeningMode() {
        return screeningMode;
    }

    public void setScreeningMode(String screeningMode) {
        this.screeningMode = screeningMode;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiStrengths() {
        return aiStrengths;
    }

    public void setAiStrengths(String aiStrengths) {
        this.aiStrengths = aiStrengths;
    }

    public String getAiWeaknesses() {
        return aiWeaknesses;
    }

    public void setAiWeaknesses(String aiWeaknesses) {
        this.aiWeaknesses = aiWeaknesses;
    }

    public String getSuggestedQuestions() {
        return suggestedQuestions;
    }

    public void setSuggestedQuestions(String suggestedQuestions) {
        this.suggestedQuestions = suggestedQuestions;
    }

    public String getCareerDnaProfile() {
        return careerDnaProfile;
    }

    public void setCareerDnaProfile(String careerDnaProfile) {
        this.careerDnaProfile = careerDnaProfile;
    }

    public String getRecruiterNotes() {
        return recruiterNotes;
    }

    public void setRecruiterNotes(String recruiterNotes) {
        this.recruiterNotes = recruiterNotes;
    }

    public LocalDateTime getScreenedAt() {
        return screenedAt;
    }

    public void setScreenedAt(LocalDateTime screenedAt) {
        this.screenedAt = screenedAt;
    }
}
