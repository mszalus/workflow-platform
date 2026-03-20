package com.workflowplatform.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "schedule_execution",
    indexes = {
        @Index(name = "idx_schedule_execution_process", columnList = "scheduled_process_id"),
        @Index(name = "idx_schedule_execution_fired_at", columnList = "fired_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_process_id", nullable = false)
    private ScheduledProcess scheduledProcess;

    @Column(name = "fired_at", nullable = false)
    private Instant firedAt;

    /**
     * RUNNING, SUCCESS, FAILED
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "process_instance_id", length = 255)
    private String processInstanceId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    void prePersist() {
        if (firedAt == null) {
            firedAt = Instant.now();
        }
    }
}
