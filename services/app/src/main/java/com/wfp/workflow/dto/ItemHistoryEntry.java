package com.wfp.workflow.dto;

import java.time.Instant;

public record ItemHistoryEntry(Instant timestamp, String actor, String kind, String field, Object from, Object to,
                               String transition, String reason) {
}
