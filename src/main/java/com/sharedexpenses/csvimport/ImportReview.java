package com.sharedexpenses.csvimport;

import com.sharedexpenses.common.BaseEntity;
import com.sharedexpenses.AppException;
import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "import_reviews")
public class ImportReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_job_id", nullable = false, unique = true)
    private Long importJobId;

    @Column(name = "review_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReviewStatus reviewStatus;

    
    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    
    @Column
    private String note;

    protected ImportReview() {}

    
    public ImportReview(Long importJobId, Long submittedBy) {
        this.importJobId = importJobId;
        this.submittedBy = submittedBy;
        this.submittedAt = LocalDateTime.now();
        this.reviewStatus = ReviewStatus.PENDING;
    }

    

    
    public void approve(Long reviewerId, String note) {
        if (this.reviewStatus == ReviewStatus.APPROVED) {
            throw new AppException(
                    "This import review is already APPROVED. No action needed.");
        }
        if (this.reviewStatus == ReviewStatus.REJECTED) {
            throw new AppException(
                    "Cannot directly approve a REJECTED review. "
                            + "Call resubmit() first, then approve.");
        }
        this.reviewStatus = ReviewStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.note = note;
    }

    
    public void reject(Long reviewerId, String note) {
        if (this.reviewStatus == ReviewStatus.REJECTED) {
            throw new AppException(
                    "This import review is already REJECTED.");
        }
        this.reviewStatus = ReviewStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.note = note;
    }

    
    public void resubmit(Long userId) {
        if (this.reviewStatus == ReviewStatus.PENDING) {
            throw new AppException(
                    "This review is already PENDING. No resubmission needed.");
        }
        if (this.reviewStatus == ReviewStatus.APPROVED) {
            throw new AppException(
                    "Cannot resubmit an already APPROVED review. "
                            + "Reject it first if you want to restart the review process.");
        }
        this.reviewStatus = ReviewStatus.PENDING;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.note = null;
        this.submittedBy = userId;
        this.submittedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getImportJobId() { return importJobId; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public Long getSubmittedBy() { return submittedBy; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public Long getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getNote() { return note; }
}
