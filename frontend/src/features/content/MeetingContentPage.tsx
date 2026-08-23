import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  contentApi,
  meetingsApi,
  resolveErrorMessage,
  type MeetingResponse,
  type NoticeResponse,
  type PostResponse,
} from '@/api';
import { useAuth } from '@/auth/useAuth';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { validateAttachment } from './attachmentValidation';

/** Format a byte count for display (KB/MB). */
function formatSize(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  }
  return `${Math.max(1, Math.round(bytes / 1024))}KB`;
}

/**
 * 자료실 / 공지 page for a meeting (U6). Participants (owning mentor, applied mentee, admin) view
 * week posts with downloadable attachments and notices; the owning mentor additionally authors
 * posts, uploads attachments and posts notices. The server is authoritative on every access
 * (non-participants get 403, non-owning mentors get 403 on write).
 */
export function MeetingContentPage() {
  const { id } = useParams<{ id: string }>();
  const meetingId = Number(id);
  const { role } = useAuth();
  const canAuthor = role === 'MENTOR';

  const [meeting, setMeeting] = useState<MeetingResponse | null>(null);
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [notices, setNotices] = useState<NoticeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!Number.isFinite(meetingId)) {
      setError('잘못된 모임입니다.');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [meetingData, postData, noticeData] = await Promise.all([
        meetingsApi.get(meetingId),
        contentApi.listPosts(meetingId),
        contentApi.listNotices(meetingId),
      ]);
      setMeeting(meetingData);
      setPosts(postData);
      setNotices(noticeData);
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [meetingId]);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground" data-testid="content-loading">
        불러오는 중...
      </p>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col gap-2">
        <h2 className="text-xl font-bold">자료실</h2>
        <p role="alert" className="text-sm text-destructive" data-testid="content-error">
          {error}
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-xl font-bold" data-testid="content-title">
          {meeting?.title ?? `모임 #${meetingId}`} · 자료실
        </h2>
        <p className="text-sm text-muted-foreground">참여자만 열람할 수 있습니다.</p>
      </div>

      <NoticeSection
        meetingId={meetingId}
        notices={notices}
        canAuthor={canAuthor}
        onChanged={load}
      />

      <PostSection
        meetingId={meetingId}
        weeks={meeting?.weeks ?? 1}
        posts={posts}
        canAuthor={canAuthor}
        onChanged={load}
      />
    </div>
  );
}

// --- Notices ---

function NoticeSection({
  meetingId,
  notices,
  canAuthor,
  onChanged,
}: {
  meetingId: number;
  notices: NoticeResponse[];
  canAuthor: boolean;
  onChanged: () => Promise<void>;
}) {
  const [body, setBody] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!body.trim()) {
      setFormError('공지 내용을 입력해 주세요.');
      return;
    }
    setSubmitting(true);
    setFormError(null);
    try {
      await contentApi.createNotice(meetingId, { body: body.trim() });
      setBody('');
      await onChanged();
    } catch (err) {
      setFormError(resolveErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="flex flex-col gap-3" data-testid="notice-section">
      <h3 className="text-lg font-semibold">공지</h3>

      {canAuthor && (
        <form onSubmit={handleSubmit} className="flex flex-col gap-2" data-testid="notice-form">
          <Label htmlFor="notice-body">새 공지</Label>
          <Textarea
            id="notice-body"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="공지 내용을 입력하세요"
            data-testid="notice-body-input"
          />
          {formError && (
            <p role="alert" className="text-sm text-destructive" data-testid="notice-form-error">
              {formError}
            </p>
          )}
          <Button type="submit" size="sm" className="self-start" disabled={submitting}>
            {submitting ? '등록 중...' : '공지 등록'}
          </Button>
        </form>
      )}

      {notices.length === 0 ? (
        <p className="text-sm text-muted-foreground" data-testid="notice-empty">
          등록된 공지가 없습니다.
        </p>
      ) : (
        <ul className="flex flex-col gap-2" data-testid="notice-list">
          {notices.map((n) => (
            <li key={n.id}>
              <Card data-testid={`notice-${n.id}`}>
                <CardContent className="flex flex-col gap-1 pt-4">
                  <p className="whitespace-pre-wrap text-sm">{n.body}</p>
                  <span className="text-xs text-muted-foreground">
                    {new Date(n.createdAt).toLocaleString('ko-KR')}
                  </span>
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

// --- Posts + attachments ---

function PostSection({
  meetingId,
  weeks,
  posts,
  canAuthor,
  onChanged,
}: {
  meetingId: number;
  weeks: number;
  posts: PostResponse[];
  canAuthor: boolean;
  onChanged: () => Promise<void>;
}) {
  const [week, setWeek] = useState('1');
  const [body, setBody] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const weekNum = Number(week);
    if (!Number.isInteger(weekNum) || weekNum < 1 || weekNum > weeks) {
      setFormError(`주차는 1 ~ ${weeks} 사이여야 합니다.`);
      return;
    }
    if (!body.trim()) {
      setFormError('본문을 입력해 주세요.');
      return;
    }
    setSubmitting(true);
    setFormError(null);
    try {
      await contentApi.createPost(meetingId, { week: weekNum, body: body.trim() });
      setBody('');
      await onChanged();
    } catch (err) {
      setFormError(resolveErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="flex flex-col gap-3" data-testid="post-section">
      <h3 className="text-lg font-semibold">주차별 게시글</h3>

      {canAuthor && (
        <form onSubmit={handleSubmit} className="flex flex-col gap-2" data-testid="post-form">
          <Label htmlFor="post-week">주차</Label>
          <Input
            id="post-week"
            type="number"
            min={1}
            max={weeks}
            value={week}
            onChange={(e) => setWeek(e.target.value)}
            data-testid="post-week-input"
          />
          <Label htmlFor="post-body">본문</Label>
          <Textarea
            id="post-body"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="게시글 본문 (첨부 없이 글만 작성할 수 있습니다)"
            data-testid="post-body-input"
          />
          {formError && (
            <p role="alert" className="text-sm text-destructive" data-testid="post-form-error">
              {formError}
            </p>
          )}
          <Button type="submit" size="sm" className="self-start" disabled={submitting}>
            {submitting ? '작성 중...' : '게시글 작성'}
          </Button>
        </form>
      )}

      {posts.length === 0 ? (
        <p className="text-sm text-muted-foreground" data-testid="post-empty">
          등록된 게시글이 없습니다.
        </p>
      ) : (
        <ul className="flex flex-col gap-2" data-testid="post-list">
          {posts.map((post) => (
            <li key={post.id}>
              <PostCard post={post} canAuthor={canAuthor} onChanged={onChanged} />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function PostCard({
  post,
  canAuthor,
  onChanged,
}: {
  post: PostResponse;
  canAuthor: boolean;
  onChanged: () => Promise<void>;
}) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) {
      return;
    }
    const validationError = validateAttachment(file);
    if (validationError) {
      setUploadError(validationError);
      e.target.value = '';
      return;
    }
    setUploading(true);
    setUploadError(null);
    try {
      await contentApi.uploadAttachment(post.id, file);
      await onChanged();
    } catch (err) {
      setUploadError(resolveErrorMessage(err));
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  }

  async function handleDownload(attachmentId: number, fileName: string) {
    setDownloadingId(attachmentId);
    try {
      const { blob, fileName: served } = await contentApi.downloadAttachment(
        attachmentId,
        fileName,
      );
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = served || fileName;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } catch {
      // Surface nothing intrusive; the row stays as-is on a failed download.
    } finally {
      setDownloadingId(null);
    }
  }

  return (
    <Card data-testid={`post-${post.id}`}>
      <CardHeader className="pb-2">
        <CardTitle className="text-base">{post.week}주차</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        <p className="whitespace-pre-wrap text-sm">{post.body}</p>

        {post.attachments.length > 0 && (
          <ul className="flex flex-col gap-1" data-testid={`post-attachments-${post.id}`}>
            {post.attachments.map((a) => (
              <li key={a.id} className="flex items-center gap-2 text-sm">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={downloadingId === a.id}
                  onClick={() => handleDownload(a.id, a.fileName)}
                  data-testid={`attachment-download-${a.id}`}
                >
                  {downloadingId === a.id ? '다운로드 중...' : `⬇ ${a.fileName}`}
                </Button>
                <span className="text-xs text-muted-foreground">{formatSize(a.sizeBytes)}</span>
              </li>
            ))}
          </ul>
        )}

        {canAuthor && (
          <div className="flex flex-col gap-1">
            <Label htmlFor={`upload-${post.id}`} className="text-xs text-muted-foreground">
              첨부 추가 (PDF·이미지·오피스·txt, 최대 20MB)
            </Label>
            <Input
              id={`upload-${post.id}`}
              ref={fileInputRef}
              type="file"
              disabled={uploading}
              onChange={handleUpload}
              data-testid={`attachment-upload-${post.id}`}
            />
            {uploading && <span className="text-xs text-muted-foreground">업로드 중...</span>}
            {uploadError && (
              <p
                role="alert"
                className="text-sm text-destructive"
                data-testid={`attachment-upload-error-${post.id}`}
              >
                {uploadError}
              </p>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
