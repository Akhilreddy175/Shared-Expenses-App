package com.sharedexpenses.csvimport;

import com.sharedexpenses.AppException;
import com.sharedexpenses.expense.*;
import com.sharedexpenses.group.GroupController;
import com.sharedexpenses.group.GroupMember;
import com.sharedexpenses.group.GroupMemberRepository;
import com.sharedexpenses.group.GroupService;
import com.sharedexpenses.user.User;
import com.sharedexpenses.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CsvImportService {

    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("50000.00");
    private static final int MAX_ROWS = 1000;
    private static final Set<String> REQUIRED_HEADERS = Set.of("date", "description", "amount", "paid_by");
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD", "AED",
            "CHF", "SEK", "NOK", "DKK", "MYR", "THB", "HKD", "NZD", "ZAR",
            "SAR", "QAR", "KWD", "BHD", "OMR", "BDT", "LKR", "NPR", "PKR"
    );
    private static final Set<String> SETTLEMENT_KEYWORDS = Set.of(
            "paid back", "payback", "pay back", "reimburse", "reimbursed",
            "reimbursement", "settlement", "settled", "transfer", "transferred",
            "repay", "repaid", "repayment", "lent", "borrowed", "returned",
            "gave back", "giving back", "owes me", "owe you", "owed me"
    );

    private final ImportReportRepository jobRepository;
    private final ImportRowRepository rowRepository;
    private final AnomalyRepository issueRepository;
    private final ImportReviewRepository reviewRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final GroupService groupService;

    public CsvImportService(ImportReportRepository jobRepository,
                            ImportRowRepository rowRepository,
                            AnomalyRepository issueRepository,
                            ImportReviewRepository reviewRepository,
                            GroupMemberRepository memberRepository,
                            UserRepository userRepository,
                            ExpenseRepository expenseRepository,
                            ExpenseSplitRepository splitRepository,
                            GroupService groupService) {
        this.jobRepository = jobRepository;
        this.rowRepository = rowRepository;
        this.issueRepository = issueRepository;
        this.reviewRepository = reviewRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
        this.groupService = groupService;
    }

    @Transactional
    public ImportReport uploadCsv(Long groupId, MultipartFile file, Long userId) {
        groupService.requireMember(groupId, userId);

        if (file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "The uploaded file is empty.");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.csv";

        ImportReport job = new ImportReport(groupId, filename, userId);
        job = jobRepository.save(job);

        List<GroupMember> allMembers = memberRepository.findByGroupId(groupId);
        List<Long> memberUserIds = allMembers.stream()
                .map(GroupMember::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> usersById = userRepository.findAllById(memberUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<String, Long> membersByName = new LinkedHashMap<>();
        for (GroupMember m : allMembers) {
            User u = usersById.get(m.getUserId());
            if (u != null) {
                membersByName.put(u.getDisplayName().toLowerCase(Locale.ROOT), u.getId());
            }
        }

        Set<Long> activeMemberIds = allMembers.stream()
                .filter(m -> m.getLeftAt() == null)
                .map(GroupMember::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        try {
            parseAndStore(job, file.getInputStream(), membersByName, activeMemberIds);
        } catch (IOException e) {
            job.markFailed("Could not read the uploaded file: " + e.getMessage());
            return jobRepository.save(job);
        }

        if (job.getStatus() != ImportJobStatus.FAILED) {
            List<ImportRow> parsedRows = rowRepository.findByImportJobIdOrderByRowNumber(job.getId());
            analyseAllRows(job, parsedRows, groupId);
        }

        return jobRepository.save(job);
    }

    public List<ImportReport> listImportJobs(Long groupId, Long userId) {
        groupService.requireMember(groupId, userId);
        return jobRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    public ImportReport getImportJob(Long groupId, Long jobId, Long userId) {
        groupService.requireMember(groupId, userId);
        return findJobOrThrow(groupId, jobId);
    }

    @Transactional
    public void deleteImportJob(Long groupId, Long jobId, Long userId) {
        groupService.requireMember(groupId, userId);
        ImportReport job = findJobOrThrow(groupId, jobId);
        jobRepository.delete(job);
    }

    @Transactional
    public ImportReview submitForReview(Long groupId, Long jobId, Long userId) {
        groupService.requireMember(groupId, userId);
        ImportReport job = findJobOrThrow(groupId, jobId);

        if (job.getStatus() == ImportJobStatus.FAILED) {
            throw new AppException("Cannot review a FAILED import job. Fix the file and re-upload.");
        }
        if (job.getStatus() == ImportJobStatus.IMPORTED) {
            throw new AppException("This import has already been confirmed. Nothing to review.");
        }
        if (reviewRepository.existsByImportJobId(jobId)) {
            ImportReview existing = reviewRepository.findByImportJobId(jobId).orElseThrow();
            throw new AppException("A review already exists for this import job (status: "
                    + existing.getReviewStatus() + "). Use approve or reject endpoints instead.");
        }

        ImportReview review = new ImportReview(jobId, userId);
        review = reviewRepository.save(review);

        return review;
    }

    public ImportReview getReview(Long groupId, Long jobId, Long userId) {
        groupService.requireMember(groupId, userId);
        findJobOrThrow(groupId, jobId);
        return reviewRepository.findByImportJobId(jobId)
                .orElseThrow(() -> AppException.notFound("Review not found"));
    }

    @Transactional
    public ImportReview approve(Long groupId, Long jobId, String note, Long userId) {
        groupService.requireMember(groupId, userId);
        findJobOrThrow(groupId, jobId);

        ImportReview review = reviewRepository.findByImportJobId(jobId)
                .orElseThrow(() -> AppException.notFound("No review found for job " + jobId));

        review.approve(userId, note);
        reviewRepository.save(review);

        return review;
    }

    @Transactional
    public ImportReview reject(Long groupId, Long jobId, String note, Long userId) {
        groupService.requireMember(groupId, userId);
        findJobOrThrow(groupId, jobId);

        ImportReview review = reviewRepository.findByImportJobId(jobId)
                .orElseThrow(() -> AppException.notFound("No review found for job " + jobId));

        review.reject(userId, note);
        reviewRepository.save(review);

        return review;
    }

    @Transactional
    public ImportReview resubmit(Long groupId, Long jobId, Long userId) {
        groupService.requireMember(groupId, userId);
        findJobOrThrow(groupId, jobId);

        ImportReview review = reviewRepository.findByImportJobId(jobId)
                .orElseThrow(() -> AppException.notFound("No review found for job " + jobId));

        review.resubmit(userId);
        reviewRepository.save(review);

        return review;
    }

    @Transactional
    public ImportReport confirmImport(Long groupId, Long jobId, Long userId) {
        groupService.requireMember(groupId, userId);
        ImportReport job = findJobOrThrow(groupId, jobId);

        if (job.getStatus() == ImportJobStatus.FAILED) {
            throw new AppException("Cannot confirm a FAILED import job. Re-upload the file after fixing it.");
        }
        if (job.getStatus() == ImportJobStatus.IMPORTED) {
            throw new AppException("This import job has already been confirmed.");
        }

        reviewRepository.findByImportJobId(jobId).ifPresentOrElse(
                review -> {
                    if (review.getReviewStatus() != ReviewStatus.APPROVED) {
                        throw new AppException("This import must be APPROVED before confirming.");
                    }
                },
                () -> {
                    throw new AppException("This import has not been submitted for review.");
                });

        List<ImportRow> validRows = rowRepository.findByImportJobIdAndStatusOrderByRowNumber(
                jobId, ImportRowStatus.VALID);

        for (ImportRow row : validRows) {
            try {
                Expense expense = expenseRepository.save(new Expense(
                        groupId,
                        row.getDescription().trim(),
                        row.getAmount(),
                        row.getCurrency().toUpperCase(),
                        row.getExpenseDate(),
                        row.getPaidByUserId(),
                        SplitType.valueOf(row.getSplitType()),
                        row.getCategory() != null ? row.getCategory().trim() : null,
                        userId
                ));

                List<ExpenseSplit> splits = buildParticipantsForImport(expense.getId(), row);
                splitRepository.saveAll(splits);

                row.markImported(expense.getId());
                rowRepository.save(row);
                job.recordImportedRow();
            } catch (Exception e) {
                issueRepository.save(new Anomaly(
                        row.getId(), ImportIssueType.MISSING_REQUIRED_FIELD, IssueSeverity.ERROR,
                        null, null, "Failed to create expense during import: " + e.getMessage()
                ));
                row.setStatus(ImportRowStatus.INVALID);
                rowRepository.save(row);
            }
        }

        job.finaliseConfirm();
        return jobRepository.save(job);
    }

    public Map<String, Object> generateImportReport(Long groupId, Long jobId, Long userId) {
        groupService.requireMember(groupId, userId);
        ImportReport job = findJobOrThrow(groupId, jobId);

        List<ImportRow> rows = rowRepository.findByImportJobIdOrderByRowNumber(jobId);
        List<Long> rowIds = rows.stream().map(ImportRow::getId).collect(Collectors.toList());

        Map<Long, List<Anomaly>> issuesByRowId = rowIds.isEmpty() ? Map.of() :
                issueRepository.findByImportRowIdIn(rowIds).stream()
                        .collect(Collectors.groupingBy(Anomaly::getImportRowId));

        Map<String, Long> issueSummary = issuesByRowId.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(i -> i.getIssueType().name(), Collectors.counting()));

        List<Map<String, Object>> rowDetails = rows.stream().map(row -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", row.getId());
            r.put("rowNumber", row.getRowNumber());
            r.put("rawData", row.getRawData());
            r.put("status", row.getStatus());
            r.put("description", row.getDescription());
            r.put("amount", row.getAmount());
            r.put("currency", row.getCurrency());
            r.put("expenseDate", row.getExpenseDate());
            r.put("paidByName", row.getPaidByName());
            r.put("paidByUserId", row.getPaidByUserId());
            r.put("splitType", row.getSplitType());
            r.put("category", row.getCategory());
            r.put("participantsRaw", row.getParticipantsRaw());
            r.put("participantIds", row.getParticipantIds());
            r.put("participantValues", row.getParticipantValues());
            r.put("createdExpenseId", row.getCreatedExpenseId());
            r.put("issues", issuesByRowId.getOrDefault(row.getId(), List.of()).stream().map(i -> {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("id", i.getId());
                im.put("issueType", i.getIssueType());
                im.put("severity", i.getSeverity());
                im.put("fieldName", i.getFieldName());
                im.put("rawValue", i.getRawValue());
                im.put("message", i.getMessage());
                im.put("recommendedAction", i.getRecommendedAction());
                return im;
            }).collect(Collectors.toList()));
            return r;
        }).collect(Collectors.toList());

        Map<String, Object> rep = new LinkedHashMap<>();
        rep.put("jobId", job.getId());
        rep.put("filename", job.getFilename());
        rep.put("status", job.getStatus());
        rep.put("totalRows", job.getTotalRows());
        rep.put("validRows", job.getValidRows());
        rep.put("invalidRows", job.getInvalidRows());
        rep.put("importedRows", job.getImportedRows());
        rep.put("issueSummary", issueSummary);
        rep.put("rows", rowDetails);
        return rep;
    }

    public Map<String, Object> generateFullReport(Long groupId, Long jobId, Long userId) {
        groupService.requireMember(groupId, userId);
        ImportReport job = findJobOrThrow(groupId, jobId);

        List<ImportRow> rows = rowRepository.findByImportJobIdOrderByRowNumber(jobId);
        List<Long> rowIds = rows.stream().map(ImportRow::getId).collect(Collectors.toList());

        Map<Long, List<Anomaly>> issuesByRowId = rowIds.isEmpty() ? Map.of() :
                issueRepository.findByImportRowIdIn(rowIds).stream()
                        .collect(Collectors.groupingBy(Anomaly::getImportRowId));

        Optional<ImportReview> review = reviewRepository.findByImportJobId(jobId);

        // Summary
        Map<String, Object> sum = new LinkedHashMap<>();
        sum.put("jobId", job.getId());
        sum.put("filename", job.getFilename());
        sum.put("jobStatus", job.getStatus());
        sum.put("uploadedBy", job.getUploadedBy());
        sum.put("uploadedAt", job.getCreatedAt());
        sum.put("totalRows", job.getTotalRows());
        sum.put("validRows", job.getValidRows());
        sum.put("invalidRows", job.getInvalidRows());
        sum.put("importedRows", job.getImportedRows());
        sum.put("jobErrorMessage", job.getErrorMessage());
        sum.put("reviewStatus", review.map(ImportReview::getReviewStatus).orElse(null));
        sum.put("submittedBy", review.map(ImportReview::getSubmittedBy).orElse(null));
        sum.put("submittedAt", review.map(ImportReview::getSubmittedAt).orElse(null));
        sum.put("reviewedBy", review.map(ImportReview::getReviewedBy).orElse(null));
        sum.put("reviewedAt", review.map(ImportReview::getReviewedAt).orElse(null));
        sum.put("reviewNote", review.map(ImportReview::getNote).orElse(null));

        // Issue Stats
        List<Anomaly> allIssues = issuesByRowId.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        int totalIssues = allIssues.size();
        int errorCount = (int) allIssues.stream().filter(i -> i.getSeverity() == IssueSeverity.ERROR).count();
        int warningCount = totalIssues - errorCount;

        Map<String, Long> byType = allIssues.stream().collect(Collectors.groupingBy(i -> i.getIssueType().name(), Collectors.counting()));
        Map<String, Long> bySeverity = allIssues.stream().collect(Collectors.groupingBy(i -> i.getSeverity().name(), Collectors.counting()));

        int rowsWithErrors = 0;
        int rowsWithWarningsOnly = 0;
        for (ImportRow r : rows) {
            List<Anomaly> ri = issuesByRowId.getOrDefault(r.getId(), List.of());
            if (ri.isEmpty()) continue;
            boolean err = ri.stream().anyMatch(i -> i.getSeverity() == IssueSeverity.ERROR);
            if (err) rowsWithErrors++;
            else rowsWithWarningsOnly++;
        }

        Map<String, Object> issueStats = new LinkedHashMap<>();
        issueStats.put("totalIssues", totalIssues);
        issueStats.put("errors", errorCount);
        issueStats.put("warnings", warningCount);
        issueStats.put("rowsWithErrors", rowsWithErrors);
        issueStats.put("rowsWithWarningsOnly", rowsWithWarningsOnly);
        issueStats.put("byType", byType);
        issueStats.put("bySeverity", bySeverity);

        // Issues Found List
        List<Map<String, Object>> issuesFound = new ArrayList<>();
        for (ImportRow r : rows) {
            List<Anomaly> ri = issuesByRowId.getOrDefault(r.getId(), List.of());
            for (Anomaly i : ri) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("rowNumber", r.getRowNumber());
                im.put("description", r.getDescription());
                im.put("issueType", i.getIssueType());
                im.put("severity", i.getSeverity());
                im.put("fieldName", i.getFieldName());
                im.put("rawValue", i.getRawValue());
                im.put("message", i.getMessage());
                im.put("recommendedAction", i.getRecommendedAction());
                issuesFound.add(im);
            }
        }

        // Actions Taken
        List<Map<String, Object>> actions = List.of();

        // Imported records
        List<Map<String, Object>> imported = rows.stream()
                .filter(r -> r.getStatus() == ImportRowStatus.IMPORTED && r.getCreatedExpenseId() != null)
                .map(r -> {
                    Map<String, Object> im = new LinkedHashMap<>();
                    im.put("rowNumber", r.getRowNumber());
                    im.put("expenseId", r.getCreatedExpenseId());
                    im.put("description", r.getDescription());
                    im.put("amount", r.getAmount());
                    im.put("currency", r.getCurrency());
                    im.put("date", r.getExpenseDate());
                    im.put("paidByUserId", r.getPaidByUserId());
                    return im;
                }).collect(Collectors.toList());

        // Skipped records
        List<Map<String, Object>> skipped = rows.stream()
                .filter(r -> r.getStatus() != ImportRowStatus.IMPORTED)
                .map(r -> {
                    Map<String, Object> sm = new LinkedHashMap<>();
                    sm.put("rowNumber", r.getRowNumber());
                    sm.put("description", r.getDescription());
                    sm.put("amount", r.getAmount());
                    sm.put("currency", r.getCurrency());
                    sm.put("status", r.getStatus());
                    sm.put("issuesCount", issuesByRowId.getOrDefault(r.getId(), List.of()).size());
                    return sm;
                }).collect(Collectors.toList());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", LocalDateTime.now());
        report.put("summary", sum);
        report.put("issueStats", issueStats);
        report.put("issuesFound", issuesFound);
        report.put("actionsTaken", actions);
        report.put("importedRecords", imported);
        report.put("skippedRecords", skipped);
        return report;
    }

    // ── PRIVATE HELPERS / INLINED SERVICES ──────────────────────────────────────

    private ImportReport findJobOrThrow(Long groupId, Long jobId) {
        return jobRepository.findByIdAndGroupId(jobId, groupId)
                .orElseThrow(() -> AppException.notFound("Import job not found"));
    }

    private void parseAndStore(ImportReport job, java.io.InputStream stream,
                               Map<String, Long> membersByName, Set<Long> activeMemberIds) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                job.markFailed("The uploaded file is empty.");
                return;
            }

            Map<String, Integer> colIndex = parseHeader(headerLine);
            List<String> missing = REQUIRED_HEADERS.stream()
                    .filter(h -> !colIndex.containsKey(h))
                    .sorted()
                    .collect(Collectors.toList());

            if (!missing.isEmpty()) {
                job.markFailed("Missing required header columns: " + missing);
                return;
            }

            Set<String> seenDuplicates = new HashSet<>();
            int rowNumber = 1;
            int valid = 0, invalid = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                rowNumber++;

                if (rowNumber > MAX_ROWS + 1) {
                    job.markFailed("File exceeds the maximum of " + MAX_ROWS + " data rows.");
                    return;
                }

                ParseRowResult res = parseRow(line, rowNumber, colIndex, membersByName, activeMemberIds, seenDuplicates, job.getId());
                ImportRow row = rowRepository.save(res.row);

                List<Anomaly> boundIssues = res.issues.stream()
                        .map(i -> new Anomaly(row.getId(), i.getIssueType(), i.getSeverity(),
                                i.getFieldName(), i.getRawValue(), i.getMessage()))
                        .collect(Collectors.toList());
                issueRepository.saveAll(boundIssues);

                if (row.getStatus() == ImportRowStatus.VALID) valid++;
                else invalid++;
            }

            job.markCompleted(valid + invalid, valid, invalid);
        } catch (IOException e) {
            job.markFailed("Could not read file: " + e.getMessage());
        }
    }

    record ParseRowResult(ImportRow row, List<Anomaly> issues) {}

    private ParseRowResult parseRow(String rawLine, int rowNumber, Map<String, Integer> colIndex,
                                   Map<String, Long> membersByName, Set<Long> activeMemberIds,
                                   Set<String> seenDuplicates, Long jobId) {

        ImportRow row = new ImportRow(jobId, rowNumber, rawLine);
        List<Anomaly> issues = new ArrayList<>();
        String[] fields = parseCsvLine(rawLine);

        // Date
        String dateRaw = safeGet(fields, colIndex.get("date"));
        LocalDate expDate = null;
        if (dateRaw == null || dateRaw.isBlank()) {
            issues.add(new Anomaly(0L, ImportIssueType.MISSING_REQUIRED_FIELD, IssueSeverity.ERROR, "date", dateRaw, "Date is required."));
        } else {
            try {
                expDate = LocalDate.parse(dateRaw.trim());
                row.setExpenseDate(expDate);
                if (expDate.isAfter(LocalDate.now())) {
                    issues.add(new Anomaly(0L, ImportIssueType.FUTURE_DATE, IssueSeverity.WARNING, "date", dateRaw, "Expense date is in the future."));
                }
            } catch (DateTimeParseException e) {
                issues.add(new Anomaly(0L, ImportIssueType.INVALID_DATE, IssueSeverity.ERROR, "date", dateRaw, "Cannot parse date. Expected YYYY-MM-DD"));
            }
        }

        // Description
        String descRaw = safeGet(fields, colIndex.get("description"));
        if (descRaw == null || descRaw.isBlank()) {
            issues.add(new Anomaly(0L, ImportIssueType.MISSING_REQUIRED_FIELD, IssueSeverity.ERROR, "description", descRaw, "Description is required."));
        } else {
            row.setDescription(descRaw.trim());
        }

        // Amount
        String amtRaw = safeGet(fields, colIndex.get("amount"));
        BigDecimal amount = null;
        if (amtRaw == null || amtRaw.isBlank()) {
            issues.add(new Anomaly(0L, ImportIssueType.MISSING_REQUIRED_FIELD, IssueSeverity.ERROR, "amount", amtRaw, "Amount is required."));
        } else {
            try {
                amount = new BigDecimal(amtRaw.trim());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    issues.add(new Anomaly(0L, ImportIssueType.INVALID_AMOUNT, IssueSeverity.ERROR, "amount", amtRaw, "Amount must be positive."));
                } else {
                    row.setAmount(amount);
                    if (amount.compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
                        issues.add(new Anomaly(0L, ImportIssueType.AMOUNT_TOO_LARGE, IssueSeverity.WARNING, "amount", amtRaw, "Large amount."));
                    }
                }
            } catch (NumberFormatException e) {
                issues.add(new Anomaly(0L, ImportIssueType.INVALID_AMOUNT, IssueSeverity.ERROR, "amount", amtRaw, "Cannot parse amount."));
            }
        }

        // Currency
        String currRaw = safeGet(fields, colIndex.get("currency"));
        if (currRaw == null || currRaw.isBlank()) {
            row.setCurrency("INR");
            issues.add(new Anomaly(0L, ImportIssueType.DEFAULT_APPLIED, IssueSeverity.WARNING, "currency", null, "Currency defaulted to INR."));
        } else {
            String curr = currRaw.trim().toUpperCase();
            if (!curr.matches("[A-Z]{3}")) {
                issues.add(new Anomaly(0L, ImportIssueType.INVALID_CURRENCY, IssueSeverity.ERROR, "currency", currRaw, "Invalid currency."));
            } else {
                row.setCurrency(curr);
            }
        }

        // Paid By
        String paidRaw = safeGet(fields, colIndex.get("paid_by"));
        if (paidRaw == null || paidRaw.isBlank()) {
            issues.add(new Anomaly(0L, ImportIssueType.MISSING_REQUIRED_FIELD, IssueSeverity.ERROR, "paid_by", paidRaw, "Paid by is required."));
        } else {
            Long paidByUserId = membersByName.get(paidRaw.trim().toLowerCase(Locale.ROOT));
            if (paidByUserId == null) {
                issues.add(new Anomaly(0L, ImportIssueType.UNKNOWN_USER, IssueSeverity.ERROR, "paid_by", paidRaw, "Payer not found."));
            } else {
                row.setPaidByName(paidRaw.trim());
                row.setPaidByUserId(paidByUserId);
            }
        }

        // Split Type
        String splitRaw = safeGet(fields, colIndex.get("split_type"));
        SplitType splitType = SplitType.EQUAL;
        if (splitRaw == null || splitRaw.isBlank()) {
            row.setSplitType(SplitType.EQUAL.name());
            issues.add(new Anomaly(0L, ImportIssueType.DEFAULT_APPLIED, IssueSeverity.WARNING, "split_type", null, "Split type defaulted to EQUAL."));
        } else {
            try {
                splitType = SplitType.valueOf(splitRaw.trim().toUpperCase(Locale.ROOT));
                row.setSplitType(splitType.name());
            } catch (IllegalArgumentException e) {
                issues.add(new Anomaly(0L, ImportIssueType.INVALID_SPLIT_TYPE, IssueSeverity.ERROR, "split_type", splitRaw, "Invalid split type."));
            }
        }

        // Category
        String catRaw = safeGet(fields, colIndex.get("category"));
        if (catRaw != null && !catRaw.isBlank()) {
            row.setCategory(catRaw.trim());
        }

        // Participants
        String partRaw = safeGet(fields, colIndex.get("participants"));
        parseParticipantsForImport(partRaw, splitType, membersByName, activeMemberIds, row, issues);

        // Duplicates within CSV
        if (expDate != null && descRaw != null && !descRaw.isBlank() && amount != null) {
            String key = expDate + "|" + descRaw.trim().toLowerCase(Locale.ROOT) + "|" + amount;
            if (!seenDuplicates.add(key)) {
                issues.add(new Anomaly(0L, ImportIssueType.DUPLICATE_ROW, IssueSeverity.WARNING, null, null, "Row appears to be a duplicate within the CSV."));
            }
        }

        boolean hasErrors = issues.stream().anyMatch(i -> i.getSeverity() == IssueSeverity.ERROR);
        row.setStatus(hasErrors ? ImportRowStatus.INVALID : ImportRowStatus.VALID);

        return new ParseRowResult(row, issues);
    }

    private void parseParticipantsForImport(String raw, SplitType splitType, Map<String, Long> membersByName,
                                            Set<Long> activeMemberIds, ImportRow row, List<Anomaly> issues) {
        if (raw == null || raw.isBlank()) {
            if (splitType == SplitType.EQUAL) {
                String ids = activeMemberIds.stream().map(String::valueOf).collect(Collectors.joining("|"));
                row.setParticipantsRaw("(all active members)");
                row.setParticipantIds(ids);
                issues.add(new Anomaly(0L, ImportIssueType.DEFAULT_APPLIED, IssueSeverity.WARNING, "participants", null, "Defaulted participants to active members."));
            } else {
                issues.add(new Anomaly(0L, ImportIssueType.PARTICIPANT_VALUE_MISSING, IssueSeverity.ERROR, "participants", null, "Participants required."));
            }
            return;
        }

        row.setParticipantsRaw(raw.trim());
        String[] parts = raw.split("\\|");
        List<Long> ids = new ArrayList<>();
        List<String> values = new ArrayList<>();
        boolean error = false;

        for (String p : parts) {
            String trimmed = p.trim();
            if (splitType == SplitType.EQUAL) {
                Long uid = membersByName.get(trimmed.toLowerCase(Locale.ROOT));
                if (uid == null) {
                    issues.add(new Anomaly(0L, ImportIssueType.UNKNOWN_USER, IssueSeverity.ERROR, "participants", trimmed, "Member not found."));
                    error = true;
                } else {
                    ids.add(uid);
                }
            } else {
                int col = trimmed.lastIndexOf(':');
                if (col < 0) {
                    issues.add(new Anomaly(0L, ImportIssueType.PARTICIPANT_VALUE_MISSING, IssueSeverity.ERROR, "participants", trimmed, "Format should be Name:value"));
                    error = true;
                } else {
                    String name = trimmed.substring(0, col).trim();
                    String val = trimmed.substring(col + 1).trim();
                    Long uid = membersByName.get(name.toLowerCase(Locale.ROOT));
                    if (uid == null) {
                        issues.add(new Anomaly(0L, ImportIssueType.UNKNOWN_USER, IssueSeverity.ERROR, "participants", name, "Member not found."));
                        error = true;
                    } else {
                        try {
                            new BigDecimal(val);
                            ids.add(uid);
                            values.add(val);
                        } catch (NumberFormatException e) {
                            issues.add(new Anomaly(0L, ImportIssueType.INVALID_AMOUNT, IssueSeverity.ERROR, "participants", val, "Cannot parse value."));
                            error = true;
                        }
                    }
                }
            }
        }

        if (!error) {
            row.setParticipantIds(ids.stream().map(String::valueOf).collect(Collectors.joining("|")));
            if (!values.isEmpty()) {
                row.setParticipantValues(String.join("|", values));
            }
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder curr = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    curr.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(curr.toString().trim());
                curr = new StringBuilder();
            } else {
                curr.append(c);
            }
        }
        fields.add(curr.toString().trim());
        return fields.toArray(new String[0]);
    }

    private Map<String, Integer> parseHeader(String header) {
        String[] headers = parseCsvLine(header);
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].toLowerCase(Locale.ROOT).trim(), i);
        }
        return index;
    }

    private String safeGet(String[] fields, Integer idx) {
        if (idx == null || idx >= fields.length) return null;
        return fields[idx];
    }

    private void analyseAllRows(ImportReport job, List<ImportRow> rows, Long groupId) {
        if (rows.isEmpty()) return;

        List<Expense> existing = expenseRepository.findByGroupId(groupId);
        List<GroupMember> members = memberRepository.findByGroupId(groupId);
        Set<Long> memberIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toSet());

        for (ImportRow row : rows) {
            List<Anomaly> anomalies = new ArrayList<>();

            // 1. Negative amount
            if (row.getAmount() == null) {
                anomalies.add(new Anomaly(row.getId(), ImportIssueType.INVALID_AMOUNT, IssueSeverity.ERROR, "amount", null, "Amount is missing."));
            } else if (row.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                anomalies.add(new Anomaly(row.getId(), ImportIssueType.INVALID_AMOUNT, IssueSeverity.ERROR, "amount", row.getAmount().toPlainString(), "Amount must be positive."));
            }

            // 2. Duplicate expense
            if (row.getExpenseDate() != null && row.getAmount() != null && row.getDescription() != null && row.getPaidByUserId() != null) {
                for (Expense ex : existing) {
                    if (ex.getExpenseDate().equals(row.getExpenseDate())
                            && ex.getAmount().compareTo(row.getAmount()) == 0
                            && ex.getPaidBy().equals(row.getPaidByUserId())
                            && ex.getDescription().equalsIgnoreCase(row.getDescription().trim())) {
                        anomalies.add(new Anomaly(row.getId(), ImportIssueType.DUPLICATE_ROW, IssueSeverity.WARNING, null, null,
                                "Expense already exists in group (ID: " + ex.getId() + ")."));
                        break;
                    }
                }
            }

            // 3. Invalid currency
            if (row.getCurrency() != null && !SUPPORTED_CURRENCIES.contains(row.getCurrency().toUpperCase())) {
                anomalies.add(new Anomaly(row.getId(), ImportIssueType.INVALID_CURRENCY, IssueSeverity.WARNING, "currency", row.getCurrency(), "Currency not recognized."));
            }

            // 4. Unknown member
            if (row.getPaidByUserId() != null && !memberIds.contains(row.getPaidByUserId())) {
                anomalies.add(new Anomaly(row.getId(), ImportIssueType.UNKNOWN_USER, IssueSeverity.ERROR, "paid_by", row.getPaidByName(), "Payer not a group member."));
            }
            if (row.getParticipantIds() != null && !row.getParticipantIds().isBlank()) {
                for (String idStr : row.getParticipantIds().split("\\|")) {
                    try {
                        Long uid = Long.parseLong(idStr.trim());
                        if (!memberIds.contains(uid)) {
                            anomalies.add(new Anomaly(row.getId(), ImportIssueType.UNKNOWN_USER, IssueSeverity.ERROR, "participants", idStr, "Participant not a group member."));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            // 5. Settlement logged as expense
            if (row.getDescription() != null) {
                String desc = row.getDescription().toLowerCase(Locale.ROOT);
                boolean keyword = SETTLEMENT_KEYWORDS.stream().anyMatch(desc::contains);
                int count = (row.getParticipantIds() != null && !row.getParticipantIds().isBlank()) ? row.getParticipantIds().split("\\|").length : 0;
                if (keyword && count == 2) {
                    anomalies.add(new Anomaly(row.getId(), ImportIssueType.DUPLICATE_ROW, IssueSeverity.WARNING, "description", row.getDescription(), "Looks like a settlement transfer."));
                }
            }

            // 6. Member not active on date
            if (row.getExpenseDate() != null) {
                if (row.getPaidByUserId() != null && !isMemberActiveOnDate(row.getPaidByUserId(), row.getExpenseDate(), members)) {
                    anomalies.add(new Anomaly(row.getId(), ImportIssueType.UNKNOWN_USER, IssueSeverity.ERROR, "paid_by", row.getPaidByName(), "Payer not active on expense date."));
                }
                if (row.getParticipantIds() != null && !row.getParticipantIds().isBlank()) {
                    for (String idStr : row.getParticipantIds().split("\\|")) {
                        try {
                            Long uid = Long.parseLong(idStr.trim());
                            if (!isMemberActiveOnDate(uid, row.getExpenseDate(), members)) {
                                anomalies.add(new Anomaly(row.getId(), ImportIssueType.UNKNOWN_USER, IssueSeverity.ERROR, "participants", idStr, "Participant not active on expense date."));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // 7. Invalid split
            if (row.getSplitType() != null) {
                try {
                    SplitType splitType = SplitType.valueOf(row.getSplitType());
                    if (splitType != SplitType.EQUAL) {
                        if (row.getParticipantValues() == null || row.getParticipantValues().isBlank()) {
                            anomalies.add(new Anomaly(row.getId(), ImportIssueType.PARTICIPANT_VALUE_MISSING, IssueSeverity.ERROR, "participants", null, "Split values missing."));
                        } else {
                            String[] vals = row.getParticipantValues().split("\\|");
                            List<BigDecimal> valList = new ArrayList<>();
                            boolean parseError = false;
                            for (String v : vals) {
                                try {
                                    valList.add(new BigDecimal(v.trim()));
                                } catch (NumberFormatException e) {
                                    parseError = true;
                                }
                            }

                            if (parseError) {
                                anomalies.add(new Anomaly(row.getId(), ImportIssueType.INVALID_AMOUNT, IssueSeverity.ERROR, "participants", null, "Could not parse split values."));
                            } else {
                                BigDecimal sum = valList.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                if (splitType == SplitType.PERCENTAGE) {
                                    if (sum.subtract(new BigDecimal("100.00")).abs().compareTo(new BigDecimal("0.01")) > 0) {
                                        anomalies.add(new Anomaly(row.getId(), ImportIssueType.INVALID_AMOUNT, IssueSeverity.ERROR, "participants", sum.toPlainString() + "%", "Percentages must sum to 100%."));
                                    }
                                } else if (splitType == SplitType.EXACT) {
                                    if (row.getAmount() != null) {
                                        if (sum.subtract(row.getAmount()).abs().compareTo(new BigDecimal("0.01")) > 0) {
                                            anomalies.add(new Anomaly(row.getId(), ImportIssueType.INVALID_AMOUNT, IssueSeverity.ERROR, "participants", sum.toPlainString(), "Exact amounts must sum to expense total."));
                                        }
                                    }
                                } else if (splitType == SplitType.SHARES) {
                                    for (BigDecimal sh : valList) {
                                        if (sh.compareTo(BigDecimal.ZERO) <= 0) {
                                            anomalies.add(new Anomaly(row.getId(), ImportIssueType.INVALID_AMOUNT, IssueSeverity.ERROR, "participants", sh.toPlainString(), "Shares must be positive."));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IllegalArgumentException ignored) {}
            }

            if (!anomalies.isEmpty()) {
                issueRepository.saveAll(anomalies);
                boolean hasErrors = anomalies.stream().anyMatch(a -> a.getSeverity() == IssueSeverity.ERROR);
                if (hasErrors && row.getStatus() == ImportRowStatus.VALID) {
                    row.setStatus(ImportRowStatus.INVALID);
                    rowRepository.save(row);
                }
            }
        }

        long valid = rows.stream().filter(r -> r.getStatus() == ImportRowStatus.VALID).count();
        long invalid = rows.stream().filter(r -> r.getStatus() == ImportRowStatus.INVALID).count();
        job.updateCounts((int) (valid + invalid), (int) valid, (int) invalid);
    }

    private boolean isMemberActiveOnDate(Long userId, LocalDate date, List<GroupMember> members) {
        if (date == null) return true;
        return members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .anyMatch(m -> m.isActiveOn(date));
    }

    private List<ExpenseSplit> buildParticipantsForImport(Long expenseId, ImportRow row) {
        String[] ids = row.getParticipantIds().split("\\|");
        String[] values = row.getParticipantValues() != null ? row.getParticipantValues().split("\\|") : null;

        SplitType splitType = SplitType.valueOf(row.getSplitType());
        int n = ids.length;
        BigDecimal total = row.getAmount();

        List<ExpenseSplit> result = new ArrayList<>();

        switch (splitType) {
            case EQUAL -> {
                BigDecimal share = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
                BigDecimal remainder = total.subtract(share.multiply(BigDecimal.valueOf(n)));
                for (int i = 0; i < n; i++) {
                    BigDecimal s = (i == n - 1) ? share.add(remainder) : share;
                    result.add(new ExpenseSplit(expenseId, Long.parseLong(ids[i].trim()), s));
                }
            }
            case EXACT -> {
                for (int i = 0; i < n; i++) {
                    result.add(new ExpenseSplit(expenseId, Long.parseLong(ids[i].trim()), new BigDecimal(values[i].trim())));
                }
            }
            case PERCENTAGE -> {
                for (int i = 0; i < n; i++) {
                    BigDecimal pct = new BigDecimal(values[i].trim());
                    BigDecimal s = total.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    result.add(new ExpenseSplit(expenseId, Long.parseLong(ids[i].trim()), s));
                }
            }
            case SHARES -> {
                BigDecimal totalShares = BigDecimal.ZERO;
                for (String v : values) {
                    totalShares = totalShares.add(new BigDecimal(v.trim()));
                }
                for (int i = 0; i < n; i++) {
                    BigDecimal sh = new BigDecimal(values[i].trim());
                    BigDecimal s = total.multiply(sh).divide(totalShares, 2, RoundingMode.HALF_UP);
                    result.add(new ExpenseSplit(expenseId, Long.parseLong(ids[i].trim()), s));
                }
            }
        }
        return result;
    }
}
