package com.resumereviewer;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LlmAnalysisService {
  private final RestClient client = RestClient.create();
  @Value("${openai.api.key:}") private String apiKey;
  @Value("${openai.model:gpt-5.6-luna}") private String model;

  public Optional<String> analyze(String resume, String job) {
    if (apiKey == null || apiKey.isBlank()) return Optional.empty();
    String prompt = "You are an expert technical recruiter. Compare the RESUME and JOB DESCRIPTION. Return ONLY valid JSON with keys: score (integer 0-100), recommendation (string), summary (string), matchedSkills (array of strings), missingSkills (array of strings), strengths (array of strings), concerns (array of strings). Do not include markdown.\nRESUME:\n" + resume + "\nJOB DESCRIPTION:\n" + job;
    Map<String,Object> body = Map.of("model", model, "input", prompt);
    try {
      Map<?,?> response = client.post().uri("https://api.openai.com/v1/responses")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
      return Optional.ofNullable(extractText(response));
    } catch (Exception e) { return Optional.empty(); }
  }

  @SuppressWarnings("unchecked")
  private String extractText(Map<?,?> response) {
    Object output = response.get("output");
    if (!(output instanceof List<?> list)) return null;
    for (Object item : list) if (item instanceof Map<?,?> message) {
      Object content = message.get("content");
      if (content instanceof List<?> blocks) for (Object block : blocks) if (block instanceof Map<?,?> b && b.get("text") != null) return String.valueOf(b.get("text"));
    }
    return null;
  }
}
