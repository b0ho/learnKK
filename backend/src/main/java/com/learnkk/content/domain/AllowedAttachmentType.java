package com.learnkk.content.domain;

import java.util.Set;

/**
 * Attachment content-type whitelist (BR-U6-2, TD-U6-2). Only documents (PDF), raster images and
 * office formats plus plain text are accepted; script-embeddable formats (SVG, HTML) are excluded
 * to prevent stored XSS. Validation is by declared content type — magic-number verification is a
 * recommended [assumption] hardening step, not enforced in the pilot.
 */
public final class AllowedAttachmentType {

  /** Per-file size cap: 20MB (A1). */
  public static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;

  /** Max attachments per post ([assumption], BR-U6-2). */
  public static final int MAX_ATTACHMENTS_PER_POST = 10;

  private static final Set<String> WHITELIST =
      Set.of(
          "application/pdf",
          "image/png",
          "image/jpeg",
          "image/gif",
          "image/webp",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // docx
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // xlsx
          "application/vnd.openxmlformats-officedocument.presentationml.presentation", // pptx
          "text/plain");

  private AllowedAttachmentType() {}

  public static boolean isAllowed(String contentType) {
    if (contentType == null) {
      return false;
    }
    // Strip any parameters (e.g. "text/plain; charset=utf-8") before matching.
    int semicolon = contentType.indexOf(';');
    String base = (semicolon >= 0 ? contentType.substring(0, semicolon) : contentType).trim();
    return WHITELIST.contains(base.toLowerCase());
  }
}
