/**
 * Client-side attachment pre-validation mirroring the backend whitelist / 20MB cap (BR-U6-2).
 * The server re-validates on upload — this only gives fast feedback before the request.
 */
export const MAX_ATTACHMENT_BYTES = 20 * 1024 * 1024;

const ALLOWED_CONTENT_TYPES = new Set<string>([
  'application/pdf',
  'image/png',
  'image/jpeg',
  'image/gif',
  'image/webp',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'text/plain',
]);

/** Returns a Korean error message if the file is invalid, or null if it passes. */
export function validateAttachment(file: File): string | null {
  const baseType = file.type.split(';')[0]?.trim().toLowerCase() ?? '';
  if (!ALLOWED_CONTENT_TYPES.has(baseType)) {
    return '허용되지 않는 파일 형식입니다. (PDF·이미지·오피스 문서·txt만 가능)';
  }
  if (file.size > MAX_ATTACHMENT_BYTES) {
    return '첨부 파일은 20MB를 초과할 수 없습니다.';
  }
  if (file.size === 0) {
    return '빈 파일은 업로드할 수 없습니다.';
  }
  return null;
}
