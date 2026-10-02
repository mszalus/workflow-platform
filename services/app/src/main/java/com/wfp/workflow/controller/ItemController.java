package com.wfp.workflow.controller;

import com.wfp.common.dto.PagedResponse;
import com.wfp.workflow.dto.CreateItemRequest;
import com.wfp.workflow.dto.ItemDto;
import com.wfp.workflow.dto.TransitionItemRequest;
import com.wfp.workflow.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto create(@Valid @RequestBody CreateItemRequest request, @AuthenticationPrincipal Jwt jwt) {
        return itemService.create(request, username(jwt));
    }

    @GetMapping
    public PagedResponse<ItemDto> list(@RequestParam(required = false) String project,
                                       @RequestParam(required = false) String assignee,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "25") int size) {
        return itemService.list(project, assignee, page, size);
    }

    @GetMapping("/{key}")
    public ItemDto get(@PathVariable String key) {
        return itemService.get(key);
    }

    @PatchMapping("/{key}")
    public ItemDto update(@PathVariable String key, @RequestBody Map<String, Object> changes,
                          @AuthenticationPrincipal Jwt jwt) {
        return itemService.update(key, changes, username(jwt));
    }

    @PostMapping("/{key}/transitions")
    public ItemDto transition(@PathVariable String key, @Valid @RequestBody TransitionItemRequest request,
                              @AuthenticationPrincipal Jwt jwt) {
        return itemService.transition(key, request.getTransitionId(), request.getReason(), username(jwt));
    }

    private static String username(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username");
    }
}
