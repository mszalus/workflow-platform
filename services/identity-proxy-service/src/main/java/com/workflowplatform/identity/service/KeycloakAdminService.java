package com.workflowplatform.identity.service;

import com.workflowplatform.identity.dto.GroupDto;
import com.workflowplatform.identity.dto.PagedResponse;
import com.workflowplatform.identity.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final RestTemplate restTemplate;
    private final KeycloakTokenService tokenService;

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    // ---- Helpers ----

    private String adminUrl() {
        return serverUrl + "/admin/realms/" + realm;
    }

    private HttpEntity<Void> authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenService.getAccessToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }

    // ---- User operations ----

    @Cacheable(value = "users", key = "#tenantId + ':search:' + #search + ':' + #page + ':' + #size")
    public PagedResponse<UserDto> searchUsers(String tenantId, String search, int page, int size) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(adminUrl() + "/users")
                .queryParam("first", page * size)
                .queryParam("max", size)
                .queryParam("briefRepresentation", false);

            if (search != null && !search.isBlank()) {
                builder.queryParam("search", search);
            }

            String url = builder.build().toUriString();

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                authHeaders(),
                new ParameterizedTypeReference<>() {}
            );

            List<UserDto> users = mapUsers(response.getBody());

            // Fetch total count for pagination metadata
            long totalElements = countUsers(search);
            int totalPages = (int) Math.ceil((double) totalElements / size);

            return PagedResponse.<UserDto>builder()
                .content(users)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();

        } catch (HttpClientErrorException.NotFound ex) {
            return PagedResponse.<UserDto>builder()
                .content(List.of()).page(page).size(size).totalElements(0).totalPages(0)
                .build();
        } catch (RestClientException ex) {
            log.error("Keycloak error searching users: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak unavailable", ex);
        }
    }

    private long countUsers(String search) {
        try {
            UriComponentsBuilder countBuilder = UriComponentsBuilder
                .fromHttpUrl(adminUrl() + "/users/count");
            if (search != null && !search.isBlank()) {
                countBuilder.queryParam("search", search);
            }
            ResponseEntity<Integer> countResponse = restTemplate.exchange(
                countBuilder.build().toUriString(),
                HttpMethod.GET,
                authHeaders(),
                Integer.class
            );
            Integer count = countResponse.getBody();
            return count != null ? count.longValue() : 0L;
        } catch (RestClientException ex) {
            log.warn("Could not fetch user count from Keycloak: {}", ex.getMessage());
            return 0L;
        }
    }

    @Cacheable(value = "user", key = "#userId")
    public UserDto getUser(String userId) {
        try {
            String url = adminUrl() + "/users/" + userId;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                authHeaders(),
                new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
            }

            UserDto user = mapUser(body);
            user.setGroups(fetchUserGroupNames(userId));
            user.setRoles(fetchUserRoleNames(userId));
            return user;

        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId, ex);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("Keycloak error fetching user {}: {}", userId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak unavailable", ex);
        }
    }

    private List<String> fetchUserGroupNames(String userId) {
        try {
            String url = adminUrl() + "/users/" + userId + "/groups";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, authHeaders(), new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> body = response.getBody();
            if (body == null) return List.of();
            return body.stream()
                .map(g -> (String) g.get("name"))
                .filter(name -> name != null)
                .toList();
        } catch (RestClientException ex) {
            log.warn("Could not fetch groups for user {}: {}", userId, ex.getMessage());
            return List.of();
        }
    }

    private List<String> fetchUserRoleNames(String userId) {
        try {
            String url = adminUrl() + "/users/" + userId + "/role-mappings/realm";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, authHeaders(), new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> body = response.getBody();
            if (body == null) return List.of();
            return body.stream()
                .map(r -> (String) r.get("name"))
                .filter(name -> name != null)
                .toList();
        } catch (RestClientException ex) {
            log.warn("Could not fetch roles for user {}: {}", userId, ex.getMessage());
            return List.of();
        }
    }

    // ---- Group operations ----

    @Cacheable(value = "groups", key = "#tenantId + ':' + #search")
    public List<GroupDto> listGroups(String tenantId, String search) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(adminUrl() + "/groups")
                .queryParam("max", 100);

            if (search != null && !search.isBlank()) {
                builder.queryParam("search", search);
            }

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                builder.build().toUriString(),
                HttpMethod.GET,
                authHeaders(),
                new ParameterizedTypeReference<>() {}
            );

            return mapGroups(response.getBody());

        } catch (RestClientException ex) {
            log.error("Keycloak error listing groups: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak unavailable", ex);
        }
    }

    @Cacheable(value = "groupMembers", key = "#groupId + ':' + #page + ':' + #size")
    public PagedResponse<UserDto> getGroupMembers(String groupId, int page, int size) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(adminUrl() + "/groups/" + groupId + "/members")
                .queryParam("first", page * size)
                .queryParam("max", size)
                .build()
                .toUriString();

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                authHeaders(),
                new ParameterizedTypeReference<>() {}
            );

            List<UserDto> members = mapUsers(response.getBody());

            return PagedResponse.<UserDto>builder()
                .content(members)
                .page(page)
                .size(size)
                .totalElements(members.size())   // Keycloak doesn't return total count for group members
                .totalPages(members.size() < size ? page + 1 : page + 2)
                .build();

        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found: " + groupId, ex);
        } catch (RestClientException ex) {
            log.error("Keycloak error fetching group members for {}: {}", groupId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak unavailable", ex);
        }
    }

    // ---- Role operations ----

    @Cacheable(value = "userRoles", key = "#userId")
    public List<String> getUserRoles(String userId) {
        try {
            String url = adminUrl() + "/users/" + userId + "/role-mappings/realm";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                authHeaders(),
                new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> body = response.getBody();
            if (body == null) return List.of();
            return body.stream()
                .map(r -> (String) r.get("name"))
                .filter(name -> name != null)
                .toList();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId, ex);
        } catch (RestClientException ex) {
            log.error("Keycloak error fetching roles for user {}: {}", userId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak unavailable", ex);
        }
    }

    // ---- Cache eviction ----

    @CacheEvict(value = {"users", "user", "userRoles"}, allEntries = true)
    public void invalidateUserCache() {
        log.info("Invalidated all user/userRoles caches");
    }

    @CacheEvict(value = "groups", allEntries = true)
    public void invalidateGroupCache() {
        log.info("Invalidated all group caches");
    }

    @CacheEvict(value = {"users", "user", "userRoles", "groups", "groupMembers"}, allEntries = true)
    public void invalidateAllCaches() {
        log.info("Invalidated all identity caches");
    }

    // ---- Mapping helpers ----

    private List<UserDto> mapUsers(List<Map<String, Object>> raw) {
        if (raw == null) return List.of();
        return raw.stream().map(this::mapUser).toList();
    }

    private UserDto mapUser(Map<String, Object> raw) {
        return UserDto.builder()
            .id((String) raw.get("id"))
            .username((String) raw.get("username"))
            .email((String) raw.get("email"))
            .firstName((String) raw.get("firstName"))
            .lastName((String) raw.get("lastName"))
            .enabled(Boolean.TRUE.equals(raw.get("enabled")))
            .build();
    }

    private List<GroupDto> mapGroups(List<Map<String, Object>> raw) {
        if (raw == null) return List.of();
        return raw.stream().map(this::mapGroup).toList();
    }

    @SuppressWarnings("unchecked")
    private GroupDto mapGroup(Map<String, Object> raw) {
        List<Map<String, Object>> subGroupsRaw = (List<Map<String, Object>>) raw.get("subGroups");
        List<String> subGroupNames = new ArrayList<>();
        if (subGroupsRaw != null) {
            for (Map<String, Object> sg : subGroupsRaw) {
                String name = (String) sg.get("name");
                if (name != null) subGroupNames.add(name);
            }
        }

        return GroupDto.builder()
            .id((String) raw.get("id"))
            .name((String) raw.get("name"))
            .path((String) raw.get("path"))
            .subGroups(subGroupNames)
            .build();
    }
}
