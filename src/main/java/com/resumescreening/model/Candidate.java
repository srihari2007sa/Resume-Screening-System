package com.resumescreening.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;
    private String phone;

    @Column(name = "extracted_skills", columnDefinition = "TEXT")
    private String extractedSkills; // Comma-separated list of extracted skills, e.g. "Java, Spring, Git"

    @Column(name = "experience_years")
    private Integer experienceYears; // parsed experience in years

    private String education; // parsed education level (e.g. B.Tech, Master, PhD)

    @Column(name = "resume_filename")
    private String resumeFilename;

    @Lob
    @Column(name = "resume_text", columnDefinition = "LONGTEXT")
    private String resumeText; // raw resume text

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Candidate() {
    }

    public Candidate(String name, String email, String phone, String extractedSkills, Integer experienceYears, String education, String resumeFilename, String resumeText) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.extractedSkills = extractedSkills;
        this.experienceYears = experienceYears;
        this.education = education;
        this.resumeFilename = resumeFilename;
        this.resumeText = resumeText;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getExtractedSkills() {
        return extractedSkills;
    }

    public void setExtractedSkills(String extractedSkills) {
        this.extractedSkills = extractedSkills;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getResumeFilename() {
        return resumeFilename;
    }

    public void setResumeFilename(String resumeFilename) {
        this.resumeFilename = resumeFilename;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
