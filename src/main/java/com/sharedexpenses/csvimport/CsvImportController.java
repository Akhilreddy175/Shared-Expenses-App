package com.sharedexpenses.csvimport;

import com.sharedexpenses.common.ApiResponse;
import com.sharedexpenses.csvimport.dto.ImportJobResponse;
import com.sharedexpenses.csvimport.dto.ImportReportResponse;
import com.sharedexpenses.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/imports")
public class CsvImportController {

    private final CsvImportService importService;

    public CsvImportController(CsvImportService importService) {
        this.importService = importService;
    }

    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportJobResponse>> uploadCsv(
            @PathVariable Long groupId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {

        ImportJobResponse job = importService.uploadCsv(groupId, file, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(job, "CSV uploaded and parsed. Review the report before confirming."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ImportJobResponse>>> listImportJobs(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok(
                importService.listImportJobs(groupId, principal.getId())));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<ImportJobResponse>> getImportJob(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok(
                importService.getImportJob(groupId, jobId, principal.getId())));
    }

    
    @GetMapping("/{jobId}/report")
    public ResponseEntity<ApiResponse<ImportReportResponse>> getImportReport(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok(
                importService.getImportReport(groupId, jobId, principal.getId())));
    }

    
    @PostMapping("/{jobId}/confirm")
    public ResponseEntity<ApiResponse<ImportJobResponse>> confirmImport(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ImportJobResponse result = importService.confirmImport(groupId, jobId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(result,
                "Import confirmed. " + result.getImportedRows() + " expenses created."));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Void>> deleteImportJob(
            @PathVariable Long groupId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal) {

        importService.deleteImportJob(groupId, jobId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Import job deleted."));
    }
}
