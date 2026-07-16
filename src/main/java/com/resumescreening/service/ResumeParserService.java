package com.resumescreening.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeParserService {

    private final Tika tika = new Tika();

    /**
     * Parses text out of any resume format (PDF, DOCX, TXT, RTF) using Apache Tika.
     */
    public String parseText(MultipartFile file) throws Exception {
        try (InputStream stream = file.getInputStream()) {
            return tika.parseToString(stream);
        }
    }

    /**
     * Extracts email using email regex patterns.
     */
    public String extractEmail(String text) {
        Pattern pattern = Pattern.compile("([a-zA-Z0-9_\\.\\+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-\\.]+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Not Provided";
    }

    /**
     * Extracts common phone number patterns.
     */
    public String extractPhone(String text) {
        // Matches common patterns e.g., +1 234 567 8900, +91-9876543210, (123) 456-7890 etc.
        Pattern pattern = Pattern.compile("(\\+?\\d{1,4}[-\\s\\.]?)?\\(?[0-9]{3,4}\\)?[-\\s\\.]?[0-9]{3,4}[-\\s\\.]?[0-9]{4}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }
        return "Not Provided";
    }

    /**
     * Extracts a candidate's name based on a simple heuristic (e.g. first valid line of text),
     * defaulting to the clean filename if not found.
     */
    public String extractName(String text, String filename) {
        // Remove file extension and replace separators
        String fallbackName = filename.replaceAll("(?i)\\.(pdf|docx|txt|doc|rtf)$", "")
                .replaceAll("[_-]", " ").trim();
        
        // Capitalize words in the fallback name
        String[] words = fallbackName.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        fallbackName = sb.toString().trim();

        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String line = lines[i].trim();
            // Look for a short line near the top that contains only letters and spaces, and is not "resume"
            if (line.length() > 2 && line.length() < 30 
                    && line.matches("^[a-zA-Z\\s]+$")
                    && !line.equalsIgnoreCase("resume") 
                    && !line.equalsIgnoreCase("curriculum vitae")
                    && !line.equalsIgnoreCase("cv")) {
                return line;
            }
        }
        return fallbackName;
    }
}
