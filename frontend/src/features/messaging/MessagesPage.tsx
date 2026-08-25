import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { messagesApi, resolveErrorMessage, type ThreadSummaryResponse } from '@/api';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { PATHS } from '@/routes/paths';
import { formatTime } from './formatTime';
import { NewMessageDialog } from './NewMessageDialog';

/** "쪽지" tab: the caller's conversations with unread badges, plus a new-message composer. */
export function MessagesPage() {
  const navigate = useNavigate();
  const [threads, setThreads] = useState<ThreadSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setThreads(await messagesApi.listThreads());
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  function openThread(thread: ThreadSummaryResponse) {
    navigate(PATHS.messageThread(thread.threadId), {
      state: { partnerId: thread.partnerId, partnerNickname: thread.partnerNickname },
    });
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">쪽지</h2>
        <NewMessageDialog onSent={() => void load()} />
      </div>

      {loading && (
<Spinner data-testid="messages-loading" />
      )}

      {error && (
        <p role="alert" className="text-sm text-destructive" data-testid="messages-error">
          {error}
        </p>
      )}

      {!loading && !error && threads.length === 0 && (
        <p className="text-sm text-muted-foreground" data-testid="messages-empty">
          아직 주고받은 쪽지가 없습니다.
        </p>
      )}

      <ul className="flex flex-col gap-2" data-testid="thread-list">
        {threads.map((thread) => (
          <li key={thread.threadId}>
            <Card
              role="button"
              tabIndex={0}
              data-testid={`thread-${thread.threadId}`}
              className="cursor-pointer transition-colors hover:bg-accent"
              onClick={() => openThread(thread)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  openThread(thread);
                }
              }}
            >
              <CardContent className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium">{thread.partnerNickname ?? '알 수 없음'}</span>
                    {thread.unreadCount > 0 && (
                      <Badge variant="destructive" data-testid={`thread-unread-${thread.threadId}`}>
                        {thread.unreadCount}
                      </Badge>
                    )}
                  </div>
                  <p className="truncate text-sm text-muted-foreground">
                    {thread.lastMessageBody ?? ''}
                  </p>
                </div>
                <span className="shrink-0 text-xs text-muted-foreground">
                  {formatTime(thread.lastMessageAt)}
                </span>
              </CardContent>
            </Card>
          </li>
        ))}
      </ul>
    </div>
  );
}
