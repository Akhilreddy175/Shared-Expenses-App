package com.sharedexpenses.group;

import com.sharedexpenses.common.BaseEntity;
import jakarta.persistence.*;

/**
 * A group is the container for shared expenses — a household, a trip, etc.
 * It tracks who created it and when. Members are tracked separately in GroupMember.
 *
 * Table name is expense_groups because "groups" is a reserved SQL keyword.
 */
@Entity
@Table(name = "expense_groups")
public class Group extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    // Storing just the ID, not a @ManyToOne relation, to keep the entity simple.
    // We load the creator's User when we need their display name.
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    protected Group() {}

    public Group(String name, String description, Long createdBy) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Long getCreatedBy() { return createdBy; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
}
