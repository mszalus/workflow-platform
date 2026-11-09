package com.wfp.workflow.dto;

public record UpgradeSummary(String workflowKey, int openItems, int sameStatus, int walkedBack, int reset, int failed) {
}
