package com.resumereviewer;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidates")
@CrossOrigin(origins = "*")
public class BatchResumeController {

    private static final List<String> SKILLS = List.of(
            "java",
            "spring boot",
            "react",
            "javascript",
            "python",
            "sql",
            "mysql",
            "postgresql",
            "aws",
            "docker",
            "kubernetes",
            "git",
            "machine learning",
            "artificial intelligence",
            "html",
            "css",
            "node.js",
            "mongodb",
            "rest api",
            "data structures"
    );

    private final LlmAnalysisService llmAnalysisService;
    private final ObjectMapper objectMapper;

    public BatchResumeController(
            LlmAnalysisService llmAnalysisService,
            ObjectMapper objectMapper
    ) {
        this.llmAnalysisService = llmAnalysisService;
        this.objectMapper = objectMapper;
    }


    @PostMapping(
            value = "/batch",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> batch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("jobDescription") String job
    ) {

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Upload at least one resume."
                    ));
        }

        if (job == null || job.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Job description is required."
                    ));
        }


        List<String> requiredSkills =
                skills(job.toLowerCase());

        List<Candidate> candidates =
                new ArrayList<>();


        for (MultipartFile file : files) {

            try {

                String resumeText =
                        read(file);


                Candidate candidate =
                        analyzeCandidate(
                                resumeText,
                                job,
                                file.getOriginalFilename(),
                                requiredSkills
                        );


                candidates.add(candidate);


            } catch (Exception e) {

                e.printStackTrace();

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message",
                                "Could not analyze "
                                        + file.getOriginalFilename()
                        ));
            }
        }


        candidates.sort(
                Comparator.comparingInt(
                        Candidate::score
                ).reversed()
        );


        long shortlisted =
                candidates.stream()
                        .filter(
                                candidate ->
                                        candidate.score() >= 75
                        )
                        .count();


        return ResponseEntity.ok(
                Map.of(
                        "total", candidates.size(),
                        "shortlisted", shortlisted,
                        "candidates", candidates
                )
        );
    }


    private Candidate analyzeCandidate(
            String resumeText,
            String job,
            String filename,
            List<String> requiredSkills
    ) {

        /*
         * Extract ALL skills from resume.
         * This is used for Resume Inventory.
         */

        List<String> extractedSkills =
                skills(resumeText.toLowerCase());


        Optional<String> aiResponse =
                llmAnalysisService.analyze(
                        resumeText,
                        job
                );


        /*
         * FIRST TRY LLM ANALYSIS
         */

        if (aiResponse.isPresent()) {

            try {

                System.out.println(
                        "Using Ollama LLM analysis"
                );


                JsonNode json =
                        objectMapper.readTree(
                                aiResponse.get()
                        );


                int score =
                        json.path("score")
                                .asInt(0);


                score = Math.max(
                        0,
                        Math.min(
                                100,
                                score
                        )
                );


                String recommendation =
                        json.path("recommendation")
                                .asText(
                                        "Consider after review"
                                );


                List<String> matchedSkills =
                        jsonArrayToList(
                                json.path(
                                        "matchedSkills"
                                )
                        );


                List<String> missingSkills =
                        jsonArrayToList(
                                json.path(
                                        "missingSkills"
                                )
                        );


                /*
                 * Safety:
                 * If Ollama does not return matched skills,
                 * calculate them locally.
                 */

                if (matchedSkills.isEmpty()) {

                    Set<String> matched =
                            new LinkedHashSet<>(
                                    extractedSkills
                            );

                    matched.retainAll(
                            requiredSkills
                    );

                    matchedSkills =
                            new ArrayList<>(
                                    matched
                            );
                }


                /*
                 * Safety:
                 * If Ollama does not return missing skills,
                 * calculate them locally.
                 */

                if (missingSkills.isEmpty()) {

                    Set<String> gaps =
                            new LinkedHashSet<>(
                                    requiredSkills
                            );

                    gaps.removeAll(
                            extractedSkills
                    );

                    missingSkills =
                            new ArrayList<>(
                                    gaps
                            );
                }


                String name =
                        extractName(
                                resumeText,
                                filename
                        );


                return new Candidate(
                        UUID.randomUUID()
                                .toString(),

                        name,

                        score,

                        recommendation,

                        matchedSkills,

                        missingSkills,

                        extractedSkills,

                        experience(
                                resumeText
                        ),

                        education(
                                resumeText
                        )
                );


            } catch (Exception e) {

                System.out.println(
                        "LLM response could not be parsed."
                );

                e.printStackTrace();
            }
        }


        /*
         * FALLBACK ANALYSIS
         */

        System.out.println(
                "Using fallback keyword analysis"
        );


        Set<String> matched =
                new LinkedHashSet<>(
                        extractedSkills
                );

        matched.retainAll(
                requiredSkills
        );


        Set<String> gaps =
                new LinkedHashSet<>(
                        requiredSkills
                );

        gaps.removeAll(
                extractedSkills
        );


        int score;


        if (requiredSkills.isEmpty()) {

            score = Math.min(
                    100,
                    40 + extractedSkills.size() * 5
            );

        } else {

            score = (int) Math.round(
                    100.0
                            * matched.size()
                            / requiredSkills.size()
            );
        }


        score = Math.max(
                15,
                Math.min(
                        98,
                        score
                )
        );


        String name =
                extractName(
                        resumeText,
                        filename
                );


        String recommendation;

        if (score >= 75) {

            recommendation =
                    "Strong shortlist";

        } else if (score >= 50) {

            recommendation =
                    "Consider after review";

        } else {

            recommendation =
                    "Needs deeper review";
        }


        return new Candidate(
                UUID.randomUUID()
                        .toString(),

                name,

                score,

                recommendation,

                new ArrayList<>(
                        matched
                ),

                new ArrayList<>(
                        gaps
                ),

                extractedSkills,

                experience(
                        resumeText
                ),

                education(
                        resumeText
                )
        );
    }


    private List<String> jsonArrayToList(
            JsonNode node
    ) {

        List<String> result =
                new ArrayList<>();


        if (node != null && node.isArray()) {

            for (JsonNode item : node) {

                String value =
                        item.asText()
                                .trim();

                if (!value.isBlank()) {

                    result.add(value);
                }
            }
        }


        return result;
    }


    private String extractName(
            String text,
            String filename
    ) {

        if (text == null || text.isBlank()) {

            return filename;
        }


        String firstLine =
                text.strip()
                        .split("\\R")[0]
                        .trim();


        return firstLine.isBlank()
                ? filename
                : firstLine;
    }


    private String read(
            MultipartFile file
    ) throws Exception {

        String filename =
                Optional.ofNullable(
                        file.getOriginalFilename()
                )
                .orElse("")
                .toLowerCase();


        if (filename.endsWith(".pdf")) {

            try (
                    var pdf =
                            Loader.loadPDF(
                                    file.getBytes()
                            )
            ) {

                return new PDFTextStripper()
                        .getText(pdf);
            }
        }


        return new String(
                file.getBytes(),
                StandardCharsets.UTF_8
        );
    }


    private List<String> skills(
            String text
    ) {

        if (text == null) {
            return new ArrayList<>();
        }


        return SKILLS.stream()

                .filter(
                        text.toLowerCase()::contains
                )

                .collect(
                        Collectors.toList()
                );
    }


    private String experience(
            String text
    ) {

        var matcher =
                java.util.regex.Pattern.compile(
                        "(\\d+)\\+?\\s*(years|yrs)",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                )
                .matcher(text);


        if (matcher.find()) {

            return matcher.group(1)
                    + "+ years";
        }


        return "Not clearly specified";
    }


    private String education(
            String text
    ) {

        String lowerText =
                text.toLowerCase();


        if (
                lowerText.contains("master")
                        || lowerText.contains("m.tech")
                        || lowerText.contains("mba")
        ) {

            return "Postgraduate degree detected";
        }


        if (
                lowerText.contains("b.tech")
                        || lowerText.contains("bachelor")
        ) {

            return "Bachelor's degree detected";
        }


        return "Not clearly detected";
    }


    /*
     * IMPORTANT:
     * extractedSkills is added here.
     * Your frontend needs this field.
     */

    public record Candidate(

            String id,

            String name,

            int score,

            String recommendation,

            List<String> matchedSkills,

            List<String> missingSkills,

            List<String> extractedSkills,

            String experience,

            String educationDetected

    ) {}
}