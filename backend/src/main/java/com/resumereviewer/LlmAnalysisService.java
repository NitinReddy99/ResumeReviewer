package com.resumereviewer;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LlmAnalysisService {

    private final RestClient client = RestClient.create();

    @Value("${ollama.model:llama3.2}")
    private String model;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;


    public Optional<String> analyze(String resume, String job) {

        System.out.println("========== OLLAMA LLM ANALYSIS ==========");

        System.out.println("Model: " + model);
        System.out.println("Ollama URL: " + ollamaUrl);


        String prompt = """
You are an expert technical recruiter.

Compare the candidate RESUME with the JOB DESCRIPTION.

Analyze the candidate based on:
- Technical skills
- Relevant experience
- Education
- Projects
- Job requirements

Return ONLY valid JSON.

Use exactly this structure:

{
  "score": 0,
  "recommendation": "",
  "summary": "",
  "matchedSkills": [],
  "missingSkills": [],
  "extractedSkills": [],
  "strengths": [],
  "concerns": []
}

Rules:
- score must be an integer from 0 to 100

- recommendation must be exactly one of:
  "Strong shortlist"
  "Consider after review"
  "Needs deeper review"

- matchedSkills must contain skills found in BOTH
  the resume and job description

- missingSkills must contain important skills required
  in the job description but NOT found in the resume

- extractedSkills must contain ALL important technical
  skills detected in the candidate's resume, even if they
  are not required by the job description

- strengths must contain the candidate's main strengths

- concerns must contain possible weaknesses or concerns

- summary should be a short description of the
  candidate's overall suitability

- Do not use markdown
- Do not use ```json
- Do not add explanations outside JSON
- Return valid JSON only

RESUME:
%s

JOB DESCRIPTION:
%s
""".formatted(resume, job);


        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "format", "json"
        );


        try {

            System.out.println("Sending request to Ollama...");


            Map<?, ?> response = client.post()
                    .uri(ollamaUrl + "/api/generate")

                    .contentType(MediaType.APPLICATION_JSON)

                    .body(body)

                    .retrieve()

                    .body(Map.class);


            System.out.println("Ollama response received");


            String result = extractText(response);


            System.out.println("========== LLM OUTPUT ==========");

            System.out.println(result);

            System.out.println("================================");


            return Optional.ofNullable(result);


        } catch (Exception e) {

            System.out.println(
                    "========== OLLAMA API ERROR =========="
            );

            e.printStackTrace();

            System.out.println(
                    "======================================"
            );

            return Optional.empty();
        }
    }


    private String extractText(Map<?, ?> response) {

        if (response == null) {

            System.out.println("Ollama response is null");

            return null;
        }


        Object result = response.get("response");


        if (result == null) {

            System.out.println(
                    "Could not find 'response' in Ollama output"
            );

            System.out.println(response);

            return null;
        }


        return String.valueOf(result).trim();
    }
}