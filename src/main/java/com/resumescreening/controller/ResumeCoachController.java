package com.resumescreening.controller;

import com.resumescreening.service.ResumeCoachService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * REST Controller for the AI Resume Coach — novelty feature.
 *
 * POST /api/coach/chat
 * Body: {
 *   "resultId": 5,
 *   "message": "How can this candidate improve their resume?",
 *   "history": [ {"role":"user","text":"..."}, {"role":"model","text":"..."} ]
 * }
 */
@RestController
@RequestMapping("/api/coach")
@CrossOrigin(origins = "*")
public class ResumeCoachController {

    @Autowired
    private ResumeCoachService coachService;

    /**
     * Main chat endpoint — sends a message and gets an AI coaching response.
     * The conversation is grounded in a specific screening result (candidate + job).
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Gemini-Key", required = false) String apiKey) {

        try {
            Long resultId = Long.valueOf(body.get("resultId").toString());
            String message = (String) body.get("message");

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Message cannot be empty.");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("history", List.of());

            String reply = coachService.chat(resultId, message.trim(), history, apiKey);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("reply", reply);
            response.put("resultId", resultId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Coach error: " + e.getMessage());
        }
    }
}
