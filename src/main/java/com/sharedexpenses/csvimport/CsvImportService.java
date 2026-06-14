package com.sharedexpenses.csvimport;

import com.sharedexpenses.common.ResourceNotFoundException;
import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.csvimport.dto.*;
import com.sharedexpenses.expense.ExpenseService;
import com.sharedexpenses.expense.SplitType;
import com.sharedexpenses.expense.dto.CreateExpenseRequest;
import com.sharedexpenses.expense.dto.ParticipantRequest;
import com.sharedexpenses.csvimport.anomaly.AnomalyEngine;
import com.sharedexpenses.group.GroupMember;
import com.sharedexpenses.group.GroupMemberRepository;
import com.sharedexpenses.group.GroupService;
import com.sharedexpenses.user.User;
import com.sharedexpenses.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CsvImportService {

    private final ImportJobRepository jobRepository;
    private final ImportRowRepository rowRepository;
    private final ImportIssueRepository issueRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final ExpenseService expenseService;
    private final CsvParserService csvParserService;
    private final AnomalyEngine anomalyEngine;

    public CsvImportService(ImportJobRepository jobRepository,
                            ImportRowRepository rowRepository,
                            ImportIssueRepository issueRepository,
                            GroupMemberRepository groupMemberRepository,
                            UserRepository userRepository,
                            GroupService groupService,
                            ExpenseService expenseService,
                            CsvParserService csvParserService,
                            AnomalyEngine anomalyEngine) {
        this.jobRepository = jobRepository;
        this.rowRepository = rowRepository;
        this.issueRepository = issueRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
        this.expenseService = expenseService;
        this.csvParserService = csvParserService;
        this.anomalyEngine = anomalyEngine;
    }

    
    @Transactional
    public ImportJobResponse uploadCsv(Long groupId, MultipartFile file, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);

        if (file.isEmpty()) {
            throw new ValidationException("The uploaded file is empty.");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.csv";

        ImportJob job = new ImportJob(groupId, filename, currentUserId);
        job = jobRepository.save(job);

        
        List<GroupMember> allMembers = groupMemberRepository.findByGroupId(groupId);
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
            csvParserService.parseAndStore(job, file.getInputStream(), membersByName, activeMemberIds);
        } catch (IOException e) {
            job.markFailed("Could not read the uploaded file: " + e.getMessage());
        }

        
        if (job.getStatus() != ImportJobStatus.FAILED) {
            List<ImportRow> parsedRows = rowRepository.findByImportJobIdOrderByRowNumber(job.getId());
            anomalyEngine.analyseAllRows(job, parsedRows, groupId);
        }

        ImportJob saved = jobRepository.save(job);
        return ImportJobResponse.from(saved);
    }

    public List<ImportJobResponse> listImportJobs(Long groupId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);
        return jobRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(ImportJobResponse::from)
                .collect(Collectors.toList());
    }

    public ImportJobResponse getImportJob(Long groupId, Long jobId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);
        ImportJob job = findJobOrThrow(groupId, jobId);
        return ImportJobResponse.from(job);
    }

    
    public ImportReportResponse getImportReport(Long groupId, Long jobId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);
        ImportJob job = findJobOrThrow(groupId, jobId);

        List<ImportRow> rows = rowRepository.findByImportJobIdOrderByRowNumber(jobId);
        List<Long> rowIds = rows.stream().map(ImportRow::getId).collect(Collectors.toList());

        
        Map<Long, List<ImportIssue>> issuesByRowId = issueRepository
                .findByImportRowIdIn(rowIds).stream()
                .collect(Collectors.groupingBy(ImportIssue::getImportRowId));

        
        List<ImportRowDetail> rowDetails = rows.stream()
                .map(row -> ImportRowDetail.from(
                        row,
                        issuesByRowId.getOrDefault(row.getId(), List.of()).stream()
                                .map(ImportIssueDetail::from)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        
        Map<ImportIssueType, Long> issueSummary = issuesByRowId.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(ImportIssue::getIssueType, Collectors.counting()));

        return ImportReportResponse.of(job, issueSummary, rowDetails);
    }

    
    @Transactional
    public ImportJobResponse confirmImport(Long groupId, Long jobId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);
        ImportJob job = findJobOrThrow(groupId, jobId);

        if (job.getStatus() == ImportJobStatus.FAILED) {
            throw new ValidationException("Cannot confirm a FAILED import job. Re-upload the file after fixing it.");
        }
        if (job.getStatus() == ImportJobStatus.IMPORTED) {
            throw new ValidationException("This import job has already been confirmed.");
        }
        if (job.getStatus() == ImportJobStatus.PROCESSING) {
            throw new ValidationException("The import is still processing. Refresh and try again.");
        }

        List<ImportRow> validRows = rowRepository.findByImportJobIdAndStatusOrderByRowNumber(
                jobId, ImportRowStatus.VALID);

        for (ImportRow row : validRows) {
            try {
                CreateExpenseRequest request = buildExpenseRequest(row);
                var response = expenseService.createExpense(groupId, request, currentUserId);
                row.markImported(response.getId());
                rowRepository.save(row);
                job.recordImportedRow();
            } catch (Exception e) {
                
                
                issueRepository.save(new ImportIssue(
                        row.getId(), ImportIssueType.MISSING_REQUIRED_FIELD, IssueSeverity.ERROR,
                        null, null,
                        "Failed to create expense during import: " + e.getMessage()
                ));
                row.setStatus(ImportRowStatus.INVALID);
                rowRepository.save(row);
            }
        }

        job.finaliseConfirm();
        return ImportJobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public void deleteImportJob(Long groupId, Long jobId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);
        ImportJob job = findJobOrThrow(groupId, jobId);
        jobRepository.delete(job); 
    }

    

    private ImportJob findJobOrThrow(Long groupId, Long jobId) {
        return jobRepository.findByIdAndGroupId(jobId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Import job " + jobId + " not found in this group"));
    }

    private CreateExpenseRequest buildExpenseRequest(ImportRow row) {
        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription(row.getDescription());
        request.setAmount(row.getAmount());
        request.setCurrency(row.getCurrency());
        request.setExpenseDate(row.getExpenseDate());
        request.setPaidBy(row.getPaidByUserId());
        request.setSplitType(SplitType.valueOf(row.getSplitType()));
        request.setCategory(row.getCategory());
        request.setParticipants(buildParticipantRequests(row));
        return request;
    }

    private List<ParticipantRequest> buildParticipantRequests(ImportRow row) {
        String[] ids = row.getParticipantIds().split("\\|");
        String[] values = row.getParticipantValues() != null
                ? row.getParticipantValues().split("\\|")
                : null;

        List<ParticipantRequest> participants = new ArrayList<>();
        SplitType splitType = SplitType.valueOf(row.getSplitType());

        for (int i = 0; i < ids.length; i++) {
            ParticipantRequest p = new ParticipantRequest();
            p.setUserId(Long.parseLong(ids[i].trim()));

            if (values != null && i < values.length) {
                BigDecimal value = new BigDecimal(values[i].trim());
                switch (splitType) {
                    case EXACT      -> p.setShareAmount(value);
                    case PERCENTAGE -> p.setPercentage(value);
                    case SHARES     -> p.setShares(value);
                    default         -> {}
                }
            }
            participants.add(p);
        }
        return participants;
    }
}
