package com.wfp.workflow.controller;

import com.wfp.workflow.dto.CommentDto;
import com.wfp.workflow.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/processes/{processId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> list(@PathVariable String processId) {
        return commentService.getComments(processId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto add(@PathVariable String processId, @RequestBody Map<String, String> body,
                           @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("preferred_username");
        return commentService.addComment(processId, null, userId, body.get("content"));
    }
}
