import { downloadFile, request, type DownloadedFile } from './client';
import type {
  AttachmentResponse,
  NoticeCreateRequest,
  NoticeResponse,
  PostCreateRequest,
  PostResponse,
} from './types';

/** Content endpoints (U6): week posts, attachments (BLOB) and notices. */
export const contentApi = {
  /** Create a week post (owning mentor). Body required; attachments uploaded separately. */
  createPost(meetingId: number, payload: PostCreateRequest): Promise<PostResponse> {
    return request<PostResponse>(`/api/meetings/${meetingId}/posts`, {
      method: 'POST',
      body: payload,
    });
  },

  /** List a meeting's posts with attachment metadata (participants only). */
  listPosts(meetingId: number): Promise<PostResponse[]> {
    return request<PostResponse[]>(`/api/meetings/${meetingId}/posts`);
  },

  /** Upload a file to a post (owning mentor). Format/size are re-validated server-side. */
  uploadAttachment(postId: number, file: File): Promise<AttachmentResponse> {
    const form = new FormData();
    form.append('file', file);
    return request<AttachmentResponse>(`/api/posts/${postId}/attachments`, {
      method: 'POST',
      body: form,
    });
  },

  /** Download an attachment by id (participants only). */
  downloadAttachment(attachmentId: number, fallbackName?: string): Promise<DownloadedFile> {
    return downloadFile(`/api/attachments/${attachmentId}`, fallbackName);
  },

  /** Post a notice (owning mentor). */
  createNotice(meetingId: number, payload: NoticeCreateRequest): Promise<NoticeResponse> {
    return request<NoticeResponse>(`/api/meetings/${meetingId}/notices`, {
      method: 'POST',
      body: payload,
    });
  },

  /** List a meeting's notices, newest first (participants only). */
  listNotices(meetingId: number): Promise<NoticeResponse[]> {
    return request<NoticeResponse[]>(`/api/meetings/${meetingId}/notices`);
  },
};
