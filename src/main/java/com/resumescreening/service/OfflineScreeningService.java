package com.resumescreening.service;

import com.resumescreening.model.Candidate;
import com.resumescreening.model.JobDescription;
import com.resumescreening.model.ScreeningResult;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OfflineScreeningService {

    @Autowired
    private CareerDnaService careerDnaService;

    // Expanded taxonomy covering SDE / general software engineering roles
    private static final String[] SKILL_TAXONOMY = {
        // Languages
        "Java", "Python", "C\\+\\+", "C#", "C", "JavaScript", "TypeScript", "Go", "Rust", "Kotlin", "Swift",
        "PHP", "Ruby", "Scala", "R", "MATLAB", "Dart", "Objective-C",
        // Web frameworks
        "Spring Boot", "Spring", "Hibernate", "JPA", "Django", "Flask", "FastAPI", "Rails",
        "React", "Angular", "Vue", "Next.js", "Node.js", "Express", "Svelte", ".NET", "Laravel",
        // Databases
        "MySQL", "PostgreSQL", "SQL", "Oracle", "MongoDB", "Redis", "Cassandra", "SQLite",
        "DynamoDB", "Firebase", "Elasticsearch", "NoSQL", "JDBC",
        // Cloud & DevOps
        "AWS", "Amazon Web Services", "Azure", "GCP", "Google Cloud", "Docker", "Kubernetes",
        "Jenkins", "CI/CD", "GitHub Actions", "Terraform", "Ansible", "Linux", "Unix", "Bash",
        "Shell", "DevOps", "SRE",
        // Tools & practices
        "Git", "GitHub", "GitLab", "Bitbucket", "Maven", "Gradle", "Jira", "Confluence",
        "REST API", "GraphQL", "Microservices", "Agile", "Scrum", "Kanban", "TDD", "BDD",
        "Unit Testing", "JUnit", "Selenium", "Postman",
        // CS fundamentals — these appear in SDE resumes
        "Data Structures", "Algorithms", "OOP", "Object Oriented", "Design Patterns",
        "System Design", "Problem Solving", "Software Development", "Software Engineering",
        "Computer Science", "Operating Systems", "Networking", "OOPS", "DSA",
        // AI/ML
        "Machine Learning", "Deep Learning", "AI", "Artificial Intelligence", "TensorFlow",
        "PyTorch", "Data Science", "Data Analysis", "NLP", "Computer Vision",
        // Other
        "Agile", "Scrum", "Project Management", "Excel", "HTML", "CSS", "Android", "iOS",
        "React Native", "Flutter", "Embedded Systems", "IoT", "Cybersecurity", "Blockchain"
    };

    public ScreeningResult screen(Candidate candidate, JobDescription jobDescription) {
        String resumeText = candidate.getResumeText();
        if (resumeText == null) resumeText = "";

        // 1. Extract candidate profile
        String extractedSkills = extractSkills(resumeText);
        candidate.setExtractedSkills(extractedSkills);
        int experienceYears = parseExperienceYears(resumeText);
        candidate.setExperienceYears(experienceYears);
        String education = parseEducation(resumeText);
        candidate.setEducation(education);

        // 2. Skill score — also do a fuzzy skill match against resume text directly
        double skillScore = calculateSkillScore(extractedSkills, resumeText, jobDescription.getRequiredSkills());

        // 2b. Preferred skills bonus
        double preferredBonus = calculatePreferredSkillsBonus(extractedSkills, resumeText, jobDescription.getPreferredSkills());

        // 3. Semantic similarity — compare resume against ALL job text (title + skills + description)
        String jobFullText = buildJobFullText(jobDescription);
        double textSimilarityScore = calculateCosineSimilarity(resumeText, jobFullText);

        // 4. Experience & education
        double experienceScore = calculateExperienceScore(experienceYears, jobDescription.getMinExperience());
        double educationScore  = calculateEducationScore(education, jobDescription.getMinEducation());

        // 5. Weighted final score
        // Skill: 40%, Semantic: 30%, Experience: 20%, Education: 10%, Preferred bonus: up to +10
        double finalScore = (skillScore * 0.40) + (textSimilarityScore * 0.30)
                          + (experienceScore * 0.20) + (educationScore * 0.10);
        finalScore = Math.min(finalScore + preferredBonus, 100.0);
        finalScore = Math.round(finalScore * 10.0) / 10.0;

        String status;
        if (finalScore >= 75.0)      status = "Shortlisted";
        else if (finalScore >= 50.0) status = "Under Review";
        else                         status = "Rejected";

        // 6. Strengths & weaknesses
        List<String> strengths  = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        if (skillScore > 70) strengths.add("Excellent match of core required skills.");
        if (experienceYears >= (jobDescription.getMinExperience() != null ? jobDescription.getMinExperience() : 0)) {
            strengths.add("Meets or exceeds the required " + jobDescription.getMinExperience() + " years of experience.");
        } else if (experienceYears > 0) {
            strengths.add("Has " + experienceYears + " year(s) of practical experience.");
        }
        if (textSimilarityScore > 25) strengths.add("Good semantic alignment with the job responsibilities.");
        if (preferredBonus > 0) strengths.add("Matches preferred/bonus skills beyond core requirements.");
        if (strengths.isEmpty()) strengths.add("Basic skill competencies present in the candidate profile.");

        List<String> missingSkills = getMissingRequiredSkills(extractedSkills, resumeText, jobDescription.getRequiredSkills());
        if (!missingSkills.isEmpty()) weaknesses.add("Missing required skills: " + String.join(", ", missingSkills));
        if (jobDescription.getMinExperience() != null && experienceYears < jobDescription.getMinExperience()) {
            weaknesses.add("Experience (" + experienceYears + " yrs) is below the required " + jobDescription.getMinExperience() + " yrs.");
        }
        if (textSimilarityScore < 10) weaknesses.add("Limited overlap with the job description content.");
        if (weaknesses.isEmpty()) weaknesses.add("No significant gaps found.");

        String summary = String.format(
            "Candidate '%s' was screened offline against '%s'. Match score: %.1f%% " +
            "(skills: %.0f%%, semantic fit: %.0f%%, experience: %.0f%%, education: %.0f%%).",
            candidate.getName(), jobDescription.getTitle(), finalScore,
            skillScore, textSimilarityScore, experienceScore, educationScore);

        String questions = generateOfflineQuestions(jobDescription.getRequiredSkills(), missingSkills);

        ScreeningResult result = new ScreeningResult(
            candidate, jobDescription, finalScore, status, "OFFLINE",
            summary, String.join("\n", strengths), String.join("\n", weaknesses), questions);

        // Career DNA
        CareerDnaService.CareerDnaProfile dna = careerDnaService.computeProfile(
            candidate.getResumeText(), candidate.getExtractedSkills());
        JSONObject dnaJson = new JSONObject();
        dnaJson.put("dominantArchetype",    dna.getDominantArchetype());
        dnaJson.put("secondaryArchetype",   dna.getSecondaryArchetype());
        dnaJson.put("dnaStrength",          dna.getDnaStrength());
        dnaJson.put("archetypeDescription", dna.getArchetypeDescription());
        JSONObject scores = new JSONObject();
        dna.getArchetypeScores().forEach(scores::put);
        dnaJson.put("archetypeScores", scores);
        result.setCareerDnaProfile(dnaJson.toString());

        return result;
    }

    /**
     * Builds a single combined text string from all job description fields for
     * richer cosine similarity comparison. If description is empty, still uses
     * title + skills so the score is never 0 just because description is blank.
     */
    private String buildJobFullText(JobDescription job) {
        StringBuilder sb = new StringBuilder();
        if (job.getTitle()          != null) sb.append(job.getTitle()).append(" ");
        if (job.getDepartment()     != null) sb.append(job.getDepartment()).append(" ");
        if (job.getRequiredSkills() != null) sb.append(job.getRequiredSkills()).append(" ");
        if (job.getPreferredSkills()!= null) sb.append(job.getPreferredSkills()).append(" ");
        if (job.getDescription()    != null) sb.append(job.getDescription()).append(" ");
        // Expand abbreviations so cosine similarity picks them up
        String text = sb.toString();
        text += " software development engineer programming coding developer";
        return text;
    }

    /**
     * Extracts skills from resume text using taxonomy matching.
     */
    public String extractSkills(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        Set<String> matched = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String skillPattern : SKILL_TAXONOMY) {
            String patternStr = "\\b" + skillPattern + "\\b";
            if (skillPattern.endsWith("+") || skillPattern.endsWith("#")) {
                patternStr = "\\b" + Pattern.quote(skillPattern);
            }
            Pattern p = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            if (p.matcher(text).find()) {
                matched.add(skillPattern.replace("\\", ""));
            }
        }
        return String.join(", ", matched);
    }

    /**
     * Skill score — checks both extracted skills list AND raw resume text for each
     * required skill, so minor formatting differences don't cause misses.
     */
    private double calculateSkillScore(String extractedSkills, String resumeText, String requiredSkillsStr) {
        if (requiredSkillsStr == null || requiredSkillsStr.trim().isEmpty()) return 100.0;
        List<String> required = Arrays.stream(requiredSkillsStr.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        if (required.isEmpty()) return 100.0;

        List<String> candidateList = Arrays.stream(extractedSkills.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        String resumeLower = resumeText.toLowerCase();

        long matchCount = required.stream().filter(req -> {
            // Check extracted skills list first
            boolean inList = candidateList.stream().anyMatch(c -> c.equalsIgnoreCase(req));
            // Also check raw resume text (handles "SpringBoot", "reactjs" etc.)
            boolean inResume = resumeLower.contains(req.toLowerCase());
            return inList || inResume;
        }).count();

        return ((double) matchCount / required.size()) * 100.0;
    }

    /**
     * Preferred skills bonus — same dual-check approach.
     */
    private double calculatePreferredSkillsBonus(String extractedSkills, String resumeText, String preferredSkillsStr) {
        if (preferredSkillsStr == null || preferredSkillsStr.trim().isEmpty()) return 0.0;
        List<String> preferred = Arrays.stream(preferredSkillsStr.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        if (preferred.isEmpty()) return 0.0;

        List<String> candidateList = Arrays.stream(extractedSkills.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        String resumeLower = resumeText.toLowerCase();

        long matchCount = preferred.stream().filter(req -> {
            boolean inList   = candidateList.stream().anyMatch(c -> c.equalsIgnoreCase(req));
            boolean inResume = resumeLower.contains(req.toLowerCase());
            return inList || inResume;
        }).count();

        return Math.min(((double) matchCount / preferred.size()) * 10.0, 10.0);
    }

    /**
     * Returns required skills that appear in neither the extracted list nor the raw resume.
     */
    private List<String> getMissingRequiredSkills(String extractedSkills, String resumeText, String requiredSkillsStr) {
        if (requiredSkillsStr == null || requiredSkillsStr.trim().isEmpty()) return Collections.emptyList();
        List<String> required = Arrays.stream(requiredSkillsStr.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        List<String> candidateList = Arrays.stream(extractedSkills.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        String resumeLower = resumeText.toLowerCase();

        return required.stream().filter(req -> {
            boolean inList   = candidateList.stream().anyMatch(c -> c.equalsIgnoreCase(req));
            boolean inResume = resumeLower.contains(req.toLowerCase());
            return !inList && !inResume;
        }).collect(Collectors.toList());
    }

    public int parseExperienceYears(String text) {
        if (text == null || text.isEmpty()) return 0;
        Pattern expPattern = Pattern.compile(
            "(\\b\\d{1,2}\\b)\\+?\\s*(?:years? of experience|years? experience|years? in|yrs? exp|years? work)",
            Pattern.CASE_INSENSITIVE);
        Matcher expMatcher = expPattern.matcher(text);
        int maxFound = 0;
        while (expMatcher.find()) {
            try {
                int val = Integer.parseInt(expMatcher.group(1));
                if (val > maxFound && val < 50) maxFound = val;
            } catch (NumberFormatException ignored) {}
        }
        if (maxFound > 0) return maxFound;

        Pattern yearRange = Pattern.compile(
            "(\\b20\\d{2}\\b)\\s*(?:-|–|to|until)\\s*(\\b20\\d{2}\\b|present|current|now)",
            Pattern.CASE_INSENSITIVE);
        Matcher rangeMatcher = yearRange.matcher(text);
        int total = 0;
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        Set<String> seen = new HashSet<>();
        while (rangeMatcher.find()) {
            String key = rangeMatcher.group(1) + "-" + rangeMatcher.group(2);
            if (seen.contains(key)) continue;
            seen.add(key);
            try {
                int start = Integer.parseInt(rangeMatcher.group(1));
                int end;
                String endVal = rangeMatcher.group(2).toLowerCase();
                end = (endVal.contains("present") || endVal.contains("current") || endVal.contains("now"))
                    ? currentYear : Integer.parseInt(endVal);
                int diff = end - start;
                if (diff > 0 && diff < 40) total += diff;
            } catch (NumberFormatException ignored) {}
        }
        return Math.min(Math.max(total, 0), 40);
    }

    public String parseEducation(String text) {
        if (text == null || text.isEmpty()) return "Not Specified";
        String lower = text.toLowerCase();
        if (lower.contains("phd") || lower.contains("ph.d") || lower.contains("doctor")) return "PhD";
        if (lower.contains("master") || lower.contains("m.tech") || lower.contains("m.e") ||
            lower.contains("mba") || lower.contains("msc") || lower.contains("m.s")) return "Master's Degree";
        if (lower.contains("bachelor") || lower.contains("b.tech") || lower.contains("b.e") ||
            lower.contains("b.s") || lower.contains("bca") || lower.contains("bsc") ||
            lower.contains("b.sc") || lower.contains("be ") || lower.contains("engineering")) return "Bachelor's Degree";
        if (lower.contains("diploma")) return "Diploma";
        return "High School / Certificate";
    }

    private double calculateExperienceScore(int candidateExp, Integer requiredExp) {
        if (requiredExp == null || requiredExp == 0) return 100.0;
        if (candidateExp >= requiredExp) return 100.0;
        return ((double) candidateExp / requiredExp) * 100.0;
    }

    private double calculateEducationScore(String candidateEd, String requiredEd) {
        if (requiredEd == null || requiredEd.trim().isEmpty() || requiredEd.equalsIgnoreCase("Any")) return 100.0;
        int reqRank  = getEducationRank(requiredEd);
        int candRank = getEducationRank(candidateEd);
        if (candRank >= reqRank) return 100.0;
        return (candRank == 0) ? 30.0 : 70.0;
    }

    private int getEducationRank(String edu) {
        String lower = edu.toLowerCase();
        if (lower.contains("phd") || lower.contains("doctor")) return 4;
        if (lower.contains("master"))   return 3;
        if (lower.contains("bachelor")) return 2;
        if (lower.contains("diploma"))  return 1;
        return 0;
    }

    private double calculateCosineSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) return 0.0;
        Map<String, Integer> v1 = getWordFrequencyVector(text1);
        Map<String, Integer> v2 = getWordFrequencyVector(text2);
        Set<String> all = new HashSet<>();
        all.addAll(v1.keySet()); all.addAll(v2.keySet());
        double dot = 0, n1 = 0, n2 = 0;
        for (String w : all) {
            int f1 = v1.getOrDefault(w, 0), f2 = v2.getOrDefault(w, 0);
            dot += f1 * f2; n1 += f1 * f1; n2 += f2 * f2;
        }
        if (n1 == 0 || n2 == 0) return 0.0;
        return Math.min((dot / (Math.sqrt(n1) * Math.sqrt(n2))) * 100.0, 100.0);
    }

    private Map<String, Integer> getWordFrequencyVector(String text) {
        Map<String, Integer> vector = new HashMap<>();
        Set<String> stopwords = new HashSet<>(Arrays.asList(
            "the","a","an","and","or","but","if","to","for","in","on","at","by","from","with",
            "as","into","after","over","of","is","are","was","were","be","been","have","has",
            "had","do","does","did","we","they","he","she","it","you","i","my","our","your","its"
        ));
        for (String word : text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+")) {
            if (word.length() > 2 && !stopwords.contains(word)) {
                vector.put(word, vector.getOrDefault(word, 0) + 1);
            }
        }
        return vector;
    }

    private String generateOfflineQuestions(String reqSkills, List<String> missingSkills) {
        StringBuilder sb = new StringBuilder();
        sb.append("1. Describe your experience with ").append(
            reqSkills != null && !reqSkills.isEmpty() ? reqSkills : "your core tech stack").append(".\n");
        if (!missingSkills.isEmpty()) {
            sb.append("2. Your resume doesn't explicitly mention ").append(String.join(", ", missingSkills))
              .append(". Have you worked with these? Explain your exposure.\n");
        } else {
            sb.append("2. How do you approach designing scalable, maintainable systems?\n");
        }
        sb.append("3. Walk us through a challenging technical problem you solved. What was your approach?\n");
        sb.append("4. How do you ensure code quality — testing, reviews, CI/CD?");
        return sb.toString();
    }
}
