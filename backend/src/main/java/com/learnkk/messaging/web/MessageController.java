package com.learnkk.messaging.web;

import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageRequestFactory;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.messaging.dto.MessageResponse;
import com.learnkk.messaging.dto.RecipientResponse;
import com.learnkk.messaging.dto.SendMessageRequest;
import com.learnkk.messaging.dto.ThreadSummaryResponse;
import com.learnkk.messaging.dto.UnreadCountResponse;
import com.learnkk.messaging.service.MessageService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Messaging endpoints: send / thread list / thread transcript / unread count / recipient picker.
 */
@RestController
public class MessageController {

  private static final Set<String> ALLOWED_SORT = Set.of("createdAt");

  private final MessageService messageService;

  public MessageController(MessageService messageService) {
    this.messageService = messageService;
  }

  @PostMapping("/api/messages")
  public ResponseEntity<MessageResponse> send(
      @AuthPrincipal Principal principal, @Valid @RequestBody SendMessageRequest request) {
    MessageResponse response =
        messageService.send(principal, request.recipientId(), request.body());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/api/messages/threads")
  public ResponseEntity<List<ThreadSummaryResponse>> listThreads(
      @AuthPrincipal Principal principal) {
    return ResponseEntity.ok(messageService.listThreads(principal));
  }

  @GetMapping("/api/messages/threads/{id}")
  public ResponseEntity<PageResponse<MessageResponse>> getThread(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sort) {
    Pageable pageable = PageRequestFactory.of(page, size, sort, ALLOWED_SORT);
    return ResponseEntity.ok(messageService.getThread(principal, id, pageable));
  }

  @GetMapping("/api/messages/unread-count")
  public ResponseEntity<UnreadCountResponse> unreadCount(@AuthPrincipal Principal principal) {
    return ResponseEntity.ok(messageService.unreadCount(principal));
  }

  @GetMapping("/api/messages/recipients")
  public ResponseEntity<List<RecipientResponse>> listRecipients(
      @AuthPrincipal Principal principal) {
    return ResponseEntity.ok(messageService.listRecipients(principal));
  }
}
