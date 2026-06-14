package com.sharedexpenses.csvimport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sharedexpenses.security.UserPrincipal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups/{groupId}/imports")
public class ImportController {

    private final CsvImportService importService;
    private final ObjectMapper objectMapper;

    public ImportController(CsvImportService importService) {
        this.importService = importService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    record ReviewRequest(@Size(max = 500) String note) {}

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadCsv(
            @PathVariable Long groupId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal me) {
        ImportReport report = importService.uploadCsv(groupId, file, me.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(report));
    }

    @GetMapping
    public List<Map<String, Object>> listImportJobs(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal me) {
        return importService.listImportJobs(groupId, me.getId()).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @GetMapping("/{jobId}")
    public Map<String, Object> getImportJob(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal me) {
        ImportReport report = importService.getImportJob(groupId, jobId, me.getId());
        return toMap(report);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteImportJob(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal me) {
        importService.deleteImportJob(groupId, jobId, me.getId());
        return ResponseEntity.noContent().build();
    }

    // ── REVIEW ENDPOINTS ───────────────────────────────────────────────────────

    @PostMapping("/{jobId}/review/submit")
    public ResponseEntity<Map<String, Object>> submitForReview(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal me) {
        ImportReview review = importService.submitForReview(groupId, jobId, me.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(review));
    }

    @GetMapping("/{jobId}/review")
    public Map<String, Object> getReview(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal me) {
        ImportReview review = importService.getReview(groupId, jobId, me.getId());
        return toMap(review);
    }

    @PostMapping("/{jobId}/review/approve")
    public Map<String, Object> approve(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @RequestBody(required = false) @Valid ReviewRequest req,
            @AuthenticationPrincipal UserPrincipal me) {
        String note = req != null ? req.note() : null;
        ImportReview review = importService.approve(groupId, jobId, note, me.getId());
        return toMap(review);
    }

    @PostMapping("/{jobId}/review/reject")
    public Map<String, Object> reject(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @RequestBody(required = false) @Valid ReviewRequest req,
            @AuthenticationPrincipal UserPrincipal me) {
        String note = req != null ? req.note() : null;
        ImportReview review = importService.reject(groupId, jobId, note, me.getId());
        return toMap(review);
    }

    @PostMapping("/{jobId}/review/resubmit")
    public Map<String, Object> resubmit(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal me) {
        ImportReview review = importService.resubmit(groupId, jobId, me.getId());
        return toMap(review);
    }

    // ── REPORT ENDPOINTS ───────────────────────────────────────────────────────

    @GetMapping("/{jobId}/report")
    public Map<String, Object> getImportReport(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal me) {
        return importService.generateImportReport(groupId, jobId, me.getId());
    }

    @GetMapping("/{jobId}/full-report")
    public Map<String, Object> getFullReport(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal me) {
        return importService.generateFullReport(groupId, jobId, me.getId());
    }

    @GetMapping({"/report/download", "/full-report/download", "/{jobId}/report/download", "/{jobId}/full-report/download"})
    public ResponseEntity<byte[]> downloadFullReport(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal me) {
        Map<String, Object> report = importService.generateFullReport(groupId, jobId, me.getId());

        byte[] json;
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise report to JSON", e);
        }

        String filename = "import-report-" + jobId + ".json";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentLength(json.length);

        return ResponseEntity.ok().headers(headers).body(json);
    }

    @PostMapping("/{jobId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmImport(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal me) {
        ImportReport report = importService.confirmImport(groupId, jobId, me.getId());
        return ResponseEntity.ok(toMap(report));
    }

    private Map<String, Object> toMap(ImportReport j) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", j.getId());
        map.put("groupId", j.getGroupId());
        map.put("filename", j.getFilename());
        map.put("status", j.getStatus());
        map.put("totalRows", j.getTotalRows());
        map.put("validRows", j.getValidRows());
        map.put("invalidRows", j.getInvalidRows());
        map.put("importedRows", j.getImportedRows());
        map.put("errorMessage", j.getErrorMessage());
        map.put("uploadedBy", j.getUploadedBy());
        return map;
    }

    private Map<String, Object> toMap(ImportReview r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("importJobId", r.getImportJobId());
        map.put("reviewStatus", r.getReviewStatus());
        map.put("submittedBy", r.getSubmittedBy());
        map.put("submittedAt", r.getSubmittedAt());
        map.put("reviewedBy", r.getReviewedBy());
        map.put("reviewedAt", r.getReviewedAt());
        map.put("note", r.getNote());
        return map;
    }
}
