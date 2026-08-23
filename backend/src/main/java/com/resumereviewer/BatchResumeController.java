package com.resumereviewer;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidates")
@CrossOrigin(origins = "*")
public class BatchResumeController {
  private static final List<String> SKILLS = List.of("java","spring boot","react","javascript","python","sql","mysql","postgresql","aws","docker","kubernetes","git","machine learning","artificial intelligence","html","css","node.js","mongodb","rest api","data structures");

  @PostMapping(value="/batch", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> batch(@RequestParam("files") MultipartFile[] files, @RequestParam("jobDescription") String job) {
    if (files == null || files.length == 0) return ResponseEntity.badRequest().body(Map.of("message","Upload at least one resume."));
    List<String> required = skills(job.toLowerCase());
    List<Candidate> candidates = new ArrayList<>();
    for (MultipartFile file : files) {
      try {
        String text = read(file);
        List<String> candidateSkills = skills(text.toLowerCase());
        Set<String> matched = new LinkedHashSet<>(candidateSkills); matched.retainAll(required);
        Set<String> gaps = new LinkedHashSet<>(required); gaps.removeAll(candidateSkills);
        int score = required.isEmpty() ? Math.min(100, 40 + candidateSkills.size()*5) : (int)Math.round(100.0*matched.size()/required.size());
        score = Math.max(15, Math.min(98, score));
        String name = text.isBlank() ? file.getOriginalFilename() : text.strip().split("\\R")[0].trim();
        candidates.add(new Candidate(UUID.randomUUID().toString(), name, score, score>=75?"Strong shortlist":score>=50?"Consider after review":"Needs deeper review", new ArrayList<>(matched), new ArrayList<>(gaps), experience(text), education(text)));
      } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message","Could not read " + file.getOriginalFilename())); }
    }
    candidates.sort(Comparator.comparingInt(Candidate::score).reversed());
    return ResponseEntity.ok(Map.of("total", candidates.size(), "shortlisted", candidates.stream().filter(c->c.score()>=75).count(), "candidates", candidates));
  }

  private String read(MultipartFile file) throws Exception {
    String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
    if (name.endsWith(".pdf")) {
      try (var pdf = Loader.loadPDF(file.getBytes())) { return new PDFTextStripper().getText(pdf); }
    }
    return new String(file.getBytes());
  }
  private List<String> skills(String text) { return SKILLS.stream().filter(text::contains).collect(Collectors.toList()); }
  private String experience(String text) { var m=java.util.regex.Pattern.compile("(\\d+)\\+?\\s*(years|yrs)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text); return m.find()?m.group(1)+"+ years":"Not clearly specified"; }
  private String education(String text) { String t=text.toLowerCase(); if(t.contains("master")||t.contains("m.tech")||t.contains("mba"))return"Postgraduate degree detected"; if(t.contains("b.tech")||t.contains("bachelor"))return"Bachelor's degree detected"; return"Not clearly detected"; }
}
