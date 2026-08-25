import { useCallback, useEffect, useState } from 'react';
import { messagesApi } from '@/api';

/** Default poll interval for the unread badge (ms). */
const POLL_INTERVAL_MS = 30_000;

/** Event name broadcast when the caller reads messages, so the unread badge refreshes at once. */
const MESSAGES_READ_EVENT = 'learnkk:messages-read';

/**
 * Notify listeners (the unread badge) that messages were just read, so the count refetches
 * immediately instead of waiting for the next poll (FR-8).
 */
export function notifyMessagesRead(): void {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(MESSAGES_READ_EVENT));
  }
}

/**
 * Polls the caller's total unread message count on an interval, exposing the current value and a
 * manual refresh. Failures are swallowed (the badge is best-effort, never blocks the UI).
 */
export function useUnreadCount(intervalMs: number = POLL_INTERVAL_MS) {
  const [count, setCount] = useState(0);

  const refresh = useCallback(async () => {
    try {
      const { count: next } = await messagesApi.unreadCount();
      setCount(next);
    } catch {
      // Best-effort: leave the last known count in place on a transient failure.
    }
  }, []);

  useEffect(() => {
    void refresh();
    const timer = window.setInterval(() => void refresh(), intervalMs);
    // FR-8: 쪽지 열람(읽음 처리) 직후 즉시 뱃지를 갱신한다.
    const onRead = () => void refresh();
    window.addEventListener(MESSAGES_READ_EVENT, onRead);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener(MESSAGES_READ_EVENT, onRead);
    };
  }, [refresh, intervalMs]);

  return { count, refresh };
}
