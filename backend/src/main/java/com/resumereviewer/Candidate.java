package com.resumereviewer;

import java.util.List;

public record Candidate(
    String id,
    String name,
    int score,
    String recommendation,
    List<String> matchedSkills,
    List<String> missingSkills,
    String experience,
    String educationDetected
) {}
