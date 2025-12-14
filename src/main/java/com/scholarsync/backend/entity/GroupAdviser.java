package com.scholarsync.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "group_advisers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"group_id"})
)
@Getter
@Setter
public class GroupAdviser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "adviser_id", nullable = false)
    private User adviser;

    private LocalDateTime assignedAt = LocalDateTime.now();
}
