import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { messagesApi, resolveErrorMessage, type MessageResponse } from '@/api';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Spinner } from '@/components/ui/spinner';
import { PATHS } from '@/routes/paths';
import { formatTime } from './formatTime';
import { notifyMessagesRead } from './useUnreadCount';

interface ThreadState {
  partnerId?: number;
  partnerNickname?: string | null;
}

/** A single conversation: transcript (mine vs partner) plus a reply composer. */
export function ThreadView() {
  const { id } = useParams();
  const threadId = Number(id);
  const navigate = useNavigate();
  const location = useLocation();
  const initial = (location.state ?? {}) as ThreadState;

  const [partnerId, setPartnerId] = useState<number | null>(initial.partnerId ?? null);
  const [partnerNickname, setPartnerNickname] = useState<string | null>(
    initial.partnerNickname ?? null,
  );
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reply, setReply] = useState('');
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await messagesApi.getThread(threadId, { size: 100 });
      setMessages(page.content);
      // FR-8: 스레드를 열람하면 서버가 읽음 처리하므로, 안읽음 뱃지를 즉시 갱신한다.
      notifyMessagesRead();
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [threadId]);

  useEffect(() => {
    void load();
  }, [load]);

  // Resolve the partner (needed to tell "mine" from "theirs") if we arrived without router state.
  useEffect(() => {
    if (partnerId != null) {
      return;
    }
    let active = true;
    messagesApi
      .listThreads()
      .then((threads) => {
        if (!active) {
          return;
        }
        const match = threads.find((t) => t.threadId === threadId);
        if (match) {
          setPartnerId(match.partnerId);
          setPartnerNickname(match.partnerNickname ?? null);
        }
      })
      .catch(() => {
        // Best-effort: without the partner, messages still render (styling defaults to partner).
      });
    return () => {
      active = false;
    };
  }, [threadId, partnerId]);

  async function handleSend() {
    if (partnerId == null || !reply.trim()) {
      return;
    }
    setSending(true);
    setSendError(null);
    try {
      await messagesApi.send(partnerId, reply.trim());
      setReply('');
      await load();
    } catch (err) {
      setSendError(resolveErrorMessage(err));
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2">
        <Button
          variant="ghost"
          size="sm"
          data-testid="thread-back"
          onClick={() => navigate(PATHS.messages)}
          aria-label="목록으로"
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h2 className="text-lg font-bold">{partnerNickname ?? '대화'}</h2>
      </div>

      {loading && (
<Spinner data-testid="thread-loading" />
      )}

      {error && (
        <p role="alert" className="text-sm text-destructive" data-testid="thread-error">
          {error}
        </p>
      )}

      {!loading && !error && (
        <ul className="flex flex-col gap-2" data-testid="message-list">
          {messages.length === 0 && (
            <li className="text-sm text-muted-foreground" data-testid="thread-empty">
              첫 쪽지를 보내보세요.
            </li>
          )}
          {messages.map((m) => {
            const mine = partnerId != null && m.senderId !== partnerId;
            return (
              <li
                key={m.id}
                data-testid={`message-${m.id}`}
                data-mine={mine}
                className={cn('flex flex-col', mine ? 'items-end' : 'items-start')}
              >
                <span
                  className={cn(
                    'max-w-[80%] rounded-lg px-3 py-2 text-sm',
                    mine ? 'bg-primary text-primary-foreground' : 'bg-muted',
                  )}
                >
                  {m.body}
                </span>
                <span className="text-[10px] text-muted-foreground">{formatTime(m.createdAt)}</span>
              </li>
            );
          })}
        </ul>
      )}

      <div className="flex flex-col gap-2">
        <Textarea
          data-testid="reply-input"
          placeholder="답장을 입력하세요"
          value={reply}
          onChange={(e) => setReply(e.target.value)}
          rows={2}
        />
        {sendError && (
          <p role="alert" className="text-sm text-destructive" data-testid="reply-error">
            {sendError}
          </p>
        )}
        <Button
          data-testid="reply-send"
          onClick={handleSend}
          disabled={sending || partnerId == null || !reply.trim()}
        >
          {sending ? '보내는 중...' : '보내기'}
        </Button>
      </div>
    </div>
  );
}
