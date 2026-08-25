import { useCallback, useState } from 'react';
import { messagesApi, resolveErrorMessage, type RecipientResponse } from '@/api';
import { roleLabel } from '@/features/shared/roleLabel';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { Spinner } from '@/components/ui/spinner';

interface NewMessageDialogProps {
  /** Called after a message is sent so the caller can refresh its thread list. */
  onSent: (recipientId: number) => void;
}

/** "새 쪽지" composer: pick a permitted recipient and send. The server re-validates the boundary. */
export function NewMessageDialog({ onSent }: NewMessageDialogProps) {
  const [open, setOpen] = useState(false);
  const [recipients, setRecipients] = useState<RecipientResponse[]>([]);
  const [recipientId, setRecipientId] = useState<number | ''>('');
  const [body, setBody] = useState('');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadRecipients = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setRecipients(await messagesApi.listRecipients());
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  function handleOpenChange(next: boolean) {
    setOpen(next);
    if (next) {
      setRecipientId('');
      setBody('');
      setError(null);
      void loadRecipients();
    }
  }

  async function handleSend() {
    if (recipientId === '' || !body.trim()) {
      setError('받는 사람과 내용을 모두 입력해 주세요.');
      return;
    }
    setSending(true);
    setError(null);
    try {
      await messagesApi.send(recipientId, body.trim());
      setOpen(false);
      onSent(recipientId);
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setSending(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button data-testid="new-message-button">새 쪽지</Button>
      </DialogTrigger>
      <DialogContent data-testid="new-message-dialog">
        <DialogHeader>
          <DialogTitle>새 쪽지 보내기</DialogTitle>
        </DialogHeader>

        {loading ? (
<Spinner data-testid="recipients-loading" label="받는 사람 목록을 불러오는 중" />
        ) : recipients.length === 0 ? (
          <p className="text-sm text-muted-foreground" data-testid="recipients-empty">
            쪽지를 보낼 수 있는 상대가 없습니다.
          </p>
        ) : (
          <div className="flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-sm">
              <span className="font-medium">받는 사람</span>
              <select
                data-testid="recipient-select"
                className="rounded-md border px-3 py-2 text-sm"
                value={recipientId}
                onChange={(e) => setRecipientId(e.target.value ? Number(e.target.value) : '')}
              >
                <option value="">선택하세요</option>
                {recipients.map((r) => (
                  <option key={r.userId} value={r.userId}>
                    {r.nickname} ({roleLabel(r.role)})
                  </option>
                ))}
              </select>
            </label>

            <Textarea
              data-testid="message-body-input"
              placeholder="내용을 입력하세요"
              value={body}
              onChange={(e) => setBody(e.target.value)}
              rows={4}
            />
          </div>
        )}

        {error && (
          <p role="alert" className="text-sm text-destructive" data-testid="new-message-error">
            {error}
          </p>
        )}

        <Button
          data-testid="send-message-button"
          onClick={handleSend}
          disabled={sending || loading || recipients.length === 0}
        >
          {sending ? '보내는 중...' : '보내기'}
        </Button>
      </DialogContent>
    </Dialog>
  );
}
