package com.resumescreening.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String department;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills; // Mandatory skills — heavily weighted in scoring

    @Column(name = "preferred_skills", columnDefinition = "TEXT")
    private String preferredSkills; // Nice-to-have skills — bonus weight in scoring

    @Column(name = "min_experience")
    private Integer minExperience; // in years

    @Column(name = "min_education")
    private String minEducation; // e.g., "Bachelor's", "Master's", "PhD"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public JobDescription() {
    }

    public JobDescription(String title, String department, String description, String requiredSkills, Integer minExperience, String minEducation) {
        this.title = title;
        this.department = department;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.minExperience = minExperience;
        this.minEducation = minEducation;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getPreferredSkills() {
        return preferredSkills;
    }

    public void setPreferredSkills(String preferredSkills) {
        this.preferredSkills = preferredSkills;
    }

    public Integer getMinExperience() {
        return minExperience;
    }

    public void setMinExperience(Integer minExperience) {
        this.minExperience = minExperience;
    }

    public String getMinEducation() {
        return minEducation;
    }

    public void setMinEducation(String minEducation) {
        this.minEducation = minEducation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
