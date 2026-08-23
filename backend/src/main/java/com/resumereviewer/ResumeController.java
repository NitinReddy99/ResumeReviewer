package com.resumereviewer;

import java.util.*;
import java.util.regex.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ResumeController {

    private static final List<String> SKILLS = List.of("java","spring boot","react","javascript","python","sql","mysql","postgresql","aws","docker","kubernetes","git","machine learning","artificial intelligence","html","css","node.js","mongodb","rest api","data structures");

    @PostMapping("/analyze")
    public ResponseEntity<Map<String,Object>> analyze(@RequestBody AnalysisRequest request) {
        String resume = Optional.ofNullable(request.resume()).orElse("").toLowerCase();
        String job = Optional.ofNullable(request.jobDescription()).orElse("").toLowerCase();
        List<String> candidateSkills = extractSkills(resume);
        List<String> requiredSkills = extractSkills(job);
        Set<String> matched = new LinkedHashSet<>(candidateSkills);
        matched.retainAll(requiredSkills);
        Set<String> gaps = new LinkedHashSet<>(requiredSkills);
        gaps.removeAll(candidateSkills);
        int score = requiredSkills.isEmpty() ? Math.min(100, 45 + candidateSkills.size() * 4) : (int)Math.round((matched.size() * 100.0) / requiredSkills.size());
        score = Math.max(18, Math.min(98, score));
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("candidate", extractName(request.resume()));
        response.put("score", score);
        response.put("matchedSkills", matched);
        response.put("missingSkills", gaps);
        response.put("extractedSkills", candidateSkills);
        response.put("recommendation", score >= 75 ? "Strong shortlist" : score >= 50 ? "Consider after review" : "Not currently recommended");
        response.put("summary", buildSummary(score, matched.size(), requiredSkills.size()));
        response.put("experience", estimateExperience(request.resume()));
        response.put("educationDetected", detectEducation(request.resume()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/demo")
    public Map<String,Object> demo() {
        return Map.of("status", "ready", "message", "ResumeReviewer API is running");
    }

    private List<String> extractSkills(String text) {
        List<String> found = new ArrayList<>();
        for (String skill : SKILLS) if (text.contains(skill)) found.add(skill);
        return found;
    }
    private String extractName(String text) {
        if (text == null || text.isBlank()) return "Candidate";
        String first = text.strip().split("\\R")[0].trim();
        return first.length() > 50 ? "Candidate" : first;
    }
    private String estimateExperience(String text) {
        if (text == null) return "Not detected";
        Matcher m = Pattern.compile("(\\d+)\\+?\\s*(years|yrs)", Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group(1) + "+ years" : "Not clearly specified";
    }
    private String detectEducation(String text) {
        String lower = Optional.ofNullable(text).orElse("").toLowerCase();
        if (lower.contains("b.tech") || lower.contains("bachelor")) return "Bachelor's degree detected";
        if (lower.contains("master") || lower.contains("m.tech") || lower.contains("mba")) return "Postgraduate degree detected";
        return "Not clearly detected";
    }
    private String buildSummary(int score, int matched, int required) {
        return "Matched " + matched + " of " + required + " job-relevant skills. Overall compatibility score: " + score + "%.";
    }

    public record AnalysisRequest(String resume, String jobDescription) {}
}
