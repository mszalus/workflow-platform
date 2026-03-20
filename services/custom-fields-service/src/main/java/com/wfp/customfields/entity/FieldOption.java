package com.wfp.customfields.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "field_option")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FieldOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_schema_id", nullable = false)
    private FieldSchema fieldSchema;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String value;

    @Column(name = "sort_order")
    private int sortOrder;
}
