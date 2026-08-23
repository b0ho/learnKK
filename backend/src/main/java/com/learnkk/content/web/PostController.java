package com.learnkk.content.web;

import com.learnkk.content.dto.PostCreateRequest;
import com.learnkk.content.dto.PostResponse;
import com.learnkk.content.service.PostService;
import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Week-post endpoints: create (owning mentor) / list (participants). */
@RestController
public class PostController {

  private final PostService postService;

  public PostController(PostService postService) {
    this.postService = postService;
  }

  @PostMapping("/api/meetings/{id}/posts")
  public ResponseEntity<PostResponse> create(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @Valid @RequestBody PostCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(postService.createPost(principal, id, request));
  }

  @GetMapping("/api/meetings/{id}/posts")
  public ResponseEntity<List<PostResponse>> list(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(postService.listPosts(principal, id));
  }
}
