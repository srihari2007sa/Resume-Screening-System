package com.resumescreening.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Career DNA Service — the unique feature of this platform.
 *
 * Classifies a candidate's skill set into a multi-dimensional "DNA Profile"
 * across 8 engineering/professional archetypes. The result is a radar/spider
 * chart payload (0-100 score per axis) that gives recruiters an instant visual
 * fingerprint of who the candidate really is — beyond a single match score.
 *
 * No other mainstream ATS product ships this as a built-in feature.
 */
@Service
public class CareerDnaService {

    // -----------------------------------------------------------------------
    // Archetype definitions — each archetype has a weighted keyword list.
    // Format: keyword → weight (1=mentioned, 2=strong signal, 3=very strong)
    // -----------------------------------------------------------------------
    private static final Map<String, Map<String, Integer>> ARCHETYPES = new LinkedHashMap<>();

    static {
        // 1. Backend Architect
        Map<String, Integer> backend = new LinkedHashMap<>();
        backend.put("java", 3); backend.put("spring boot", 3); backend.put("spring", 2);
        backend.put("hibernate", 2); backend.put("jpa", 2); backend.put("microservices", 3);
        backend.put("rest api", 2); backend.put("graphql", 2); backend.put("kafka", 3);
        backend.put("rabbitmq", 2); backend.put("grpc", 2); backend.put("python", 2);
        backend.put("node.js", 2); backend.put("go", 2); backend.put("rust", 2);
        backend.put("c#", 2); backend.put(".net", 2); backend.put("php", 1);
        backend.put("design patterns", 2); backend.put("system design", 3);
        backend.put("scalability", 2); backend.put("distributed systems", 3);
        ARCHETYPES.put("Backend Architect", backend);

        // 2. Frontend Craftsman
        Map<String, Integer> frontend = new LinkedHashMap<>();
        frontend.put("react", 3); frontend.put("angular", 3); frontend.put("vue", 3);
        frontend.put("next.js", 2); frontend.put("typescript", 2); frontend.put("javascript", 3);
        frontend.put("html", 2); frontend.put("css", 2); frontend.put("sass", 1);
        frontend.put("webpack", 1); frontend.put("figma", 2); frontend.put("ux", 2);
        frontend.put("ui", 1); frontend.put("accessibility", 1); frontend.put("responsive design", 2);
        frontend.put("redux", 2); frontend.put("tailwind", 1); frontend.put("svelte", 2);
        ARCHETYPES.put("Frontend Craftsman", frontend);

        // 3. Data & ML Engineer
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("python", 3); data.put("machine learning", 3); data.put("deep learning", 3);
        data.put("tensorflow", 3); data.put("pytorch", 3); data.put("scikit-learn", 2);
        data.put("pandas", 2); data.put("numpy", 2); data.put("data science", 3);
        data.put("data analysis", 2); data.put("nlp", 2); data.put("computer vision", 2);
        data.put("spark", 2); data.put("hadoop", 2); data.put("airflow", 2);
        data.put("sql", 2); data.put("statistics", 2); data.put("r", 1);
        data.put("jupyter", 1); data.put("etl", 2); data.put("data pipeline", 2);
        ARCHETYPES.put("Data & ML Engineer", data);

        // 4. Cloud & DevOps
        Map<String, Integer> devops = new LinkedHashMap<>();
        devops.put("aws", 3); devops.put("azure", 3); devops.put("gcp", 3);
        devops.put("docker", 3); devops.put("kubernetes", 3); devops.put("terraform", 3);
        devops.put("jenkins", 2); devops.put("ci/cd", 3); devops.put("github actions", 2);
        devops.put("ansible", 2); devops.put("prometheus", 2); devops.put("grafana", 2);
        devops.put("linux", 2); devops.put("bash", 2); devops.put("shell", 1);
        devops.put("devops", 3); devops.put("sre", 2); devops.put("observability", 2);
        devops.put("nginx", 1); devops.put("load balancing", 2);
        ARCHETYPES.put("Cloud & DevOps", devops);

        // 5. Database Specialist
        Map<String, Integer> db = new LinkedHashMap<>();
        db.put("sql", 3); db.put("mysql", 3); db.put("postgresql", 3); db.put("oracle", 2);
        db.put("mongodb", 3); db.put("redis", 2); db.put("cassandra", 2); db.put("elasticsearch", 2);
        db.put("nosql", 2); db.put("database design", 3); db.put("data modeling", 3);
        db.put("query optimization", 2); db.put("indexing", 2); db.put("stored procedures", 2);
        db.put("dba", 3); db.put("sqlite", 1); db.put("dynamodb", 2); db.put("firebase", 1);
        ARCHETYPES.put("Database Specialist", db);

        // 6. Security Engineer
        Map<String, Integer> security = new LinkedHashMap<>();
        security.put("cybersecurity", 3); security.put("security", 2); security.put("penetration testing", 3);
        security.put("owasp", 3); security.put("vulnerability", 2); security.put("encryption", 2);
        security.put("oauth", 2); security.put("jwt", 2); security.put("ssl", 1);
        security.put("firewall", 2); security.put("siem", 2); security.put("soc", 2);
        security.put("compliance", 2); security.put("gdpr", 2); security.put("iso 27001", 2);
        security.put("network security", 2); security.put("cryptography", 3);
        ARCHETYPES.put("Security Engineer", security);

        // 7. Technical Leader
        Map<String, Integer> leader = new LinkedHashMap<>();
        leader.put("agile", 3); leader.put("scrum", 3); leader.put("kanban", 2);
        leader.put("project management", 3); leader.put("team lead", 3); leader.put("mentoring", 2);
        leader.put("architecture", 3); leader.put("roadmap", 2); leader.put("stakeholder", 2);
        leader.put("jira", 2); leader.put("confluence", 1); leader.put("okr", 2);
        leader.put("sprint", 2); leader.put("code review", 2); leader.put("technical strategy", 3);
        leader.put("cross-functional", 2); leader.put("product management", 2);
        ARCHETYPES.put("Technical Leader", leader);

        // 8. Mobile Developer
        Map<String, Integer> mobile = new LinkedHashMap<>();
        mobile.put("android", 3); mobile.put("ios", 3); mobile.put("swift", 3);
        mobile.put("kotlin", 3); mobile.put("react native", 3); mobile.put("flutter", 3);
        mobile.put("dart", 2); mobile.put("xcode", 2); mobile.put("android studio", 2);
        mobile.put("firebase", 2); mobile.put("push notifications", 1); mobile.put("mobile ui", 2);
        mobile.put("app store", 2); mobile.put("play store", 2); mobile.put("objective-c", 2);
        ARCHETYPES.put("Mobile Developer", mobile);
    }

    /**
     * Computes a Career DNA profile for a candidate based on their resume text.
     *
     * @param resumeText  raw resume text
     * @param skills      comma-separated extracted skills string
     * @return CareerDnaProfile containing radar scores and dominant archetype
     */
    public CareerDnaProfile computeProfile(String resumeText, String skills) {
        String combined = ((resumeText != null ? resumeText : "") + " " + (skills != null ? skills : "")).toLowerCase();

        Map<String, Integer> rawScores = new LinkedHashMap<>();
        int grandMaxPossible = 0;

        for (Map.Entry<String, Map<String, Integer>> archetype : ARCHETYPES.entrySet()) {
            String name = archetype.getKey();
            Map<String, Integer> keywords = archetype.getValue();

            int score = 0;
            int maxPossible = keywords.values().stream().mapToInt(Integer::intValue).sum();
            grandMaxPossible = Math.max(grandMaxPossible, maxPossible);

            for (Map.Entry<String, Integer> kw : keywords.entrySet()) {
                if (combined.contains(kw.getKey())) {
                    score += kw.getValue();
                }
            }

            // Normalize to 0-100 per archetype
            int normalizedScore = (int) Math.min(Math.round(((double) score / maxPossible) * 100.0), 100);
            rawScores.put(name, normalizedScore);
        }

        // Find dominant archetype (highest score)
        String dominantArchetype = rawScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("General Engineer");

        // Find secondary archetype (second highest)
        String secondaryArchetype = rawScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .skip(1)
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse("Technical Leader");

        // Overall DNA strength (average of all non-zero scores)
        double avgStrength = rawScores.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        int dnaStrength = (int) Math.round(avgStrength);
        int dominantScore = rawScores.getOrDefault(dominantArchetype, 0);

        // Generate a short archetype description
        String archetypeDescription = buildArchetypeDescription(dominantArchetype, secondaryArchetype, dominantScore);

        return new CareerDnaProfile(rawScores, dominantArchetype, secondaryArchetype, dnaStrength, archetypeDescription);
    }

    private String buildArchetypeDescription(String dominant, String secondary, int score) {
        if (score < 15) {
            return "Generalist profile — broad exposure without a deep specialization spike. Versatile and adaptable.";
        }
        return String.format(
            "Primarily a %s with strong secondary capabilities in %s. " +
            "This candidate brings focused depth in their core domain while maintaining cross-functional versatility.",
            dominant, secondary
        );
    }

    // -----------------------------------------------------------------------
    // Inner record: the DNA profile payload sent to the frontend
    // -----------------------------------------------------------------------
    public static class CareerDnaProfile {
        private final Map<String, Integer> archetypeScores;  // { "Backend Architect": 78, ... }
        private final String dominantArchetype;
        private final String secondaryArchetype;
        private final int dnaStrength;                        // 0-100 overall
        private final String archetypeDescription;

        public CareerDnaProfile(Map<String, Integer> archetypeScores, String dominantArchetype,
                                String secondaryArchetype, int dnaStrength, String archetypeDescription) {
            this.archetypeScores = archetypeScores;
            this.dominantArchetype = dominantArchetype;
            this.secondaryArchetype = secondaryArchetype;
            this.dnaStrength = dnaStrength;
            this.archetypeDescription = archetypeDescription;
        }

        public Map<String, Integer> getArchetypeScores() { return archetypeScores; }
        public String getDominantArchetype() { return dominantArchetype; }
        public String getSecondaryArchetype() { return secondaryArchetype; }
        public int getDnaStrength() { return dnaStrength; }
        public String getArchetypeDescription() { return archetypeDescription; }
    }
}
