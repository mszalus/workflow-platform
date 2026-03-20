package com.workflowplatform.identity.web;

import com.workflowplatform.identity.dto.GroupDto;
import com.workflowplatform.identity.dto.PagedResponse;
import com.workflowplatform.identity.dto.UserDto;
import com.workflowplatform.identity.service.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final KeycloakAdminService keycloakAdminService;

    // ---- User endpoints ----

    /**
     * GET /api/v1/users?search=&page=0&size=50
     * Searches users in the caller's tenant. The tenantId is extracted from the JWT "tenant_id" claim.
     */
    @GetMapping("/api/v1/users")
    public ResponseEntity<PagedResponse<UserDto>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(keycloakAdminService.searchUsers(tenantId, search, page, size));
    }

    /**
     * GET /api/v1/users/{userId}
     * Returns full user details including groups and roles.
     */
    @GetMapping("/api/v1/users/{userId}")
    public ResponseEntity<UserDto> getUser(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(keycloakAdminService.getUser(userId));
    }

    /**
     * GET /api/v1/users/{userId}/roles
     * Returns the list of realm role names assigned to the user.
     */
    @GetMapping("/api/v1/users/{userId}/roles")
    public ResponseEntity<List<String>> getUserRoles(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(keycloakAdminService.getUserRoles(userId));
    }

    // ---- Group endpoints ----

    /**
     * GET /api/v1/groups?search=
     * Lists groups optionally filtered by name. tenantId from JWT.
     */
    @GetMapping("/api/v1/groups")
    public ResponseEntity<List<GroupDto>> listGroups(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(keycloakAdminService.listGroups(tenantId, search));
    }

    /**
     * GET /api/v1/groups/{groupId}/members?page=0&size=50
     */
    @GetMapping("/api/v1/groups/{groupId}/members")
    public ResponseEntity<PagedResponse<UserDto>> getGroupMembers(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(keycloakAdminService.getGroupMembers(groupId, page, size));
    }

    // ---- Cache management ----

    /**
     * POST /api/v1/cache/invalidate
     * Body: {"target": "users"|"groups"|"all"}
     * Requires WORKFLOW_ADMIN or SYSTEM role.
     */
    @PostMapping("/api/v1/cache/invalidate")
    @PreAuthorize("hasRole('WORKFLOW_ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<Void> invalidateCache(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {

        String target = body != null ? body.getOrDefault("target", "all") : "all";

        switch (target) {
            case "users" -> keycloakAdminService.invalidateUserCache();
            case "groups" -> keycloakAdminService.invalidateGroupCache();
            default -> keycloakAdminService.invalidateAllCaches();
        }

        log.info("Cache invalidated for target='{}' by subject='{}'", target, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    // ---- Private helpers ----

    private String extractTenantId(Jwt jwt) {
        return jwt.getClaimAsString("tenant_id");
    }
}
