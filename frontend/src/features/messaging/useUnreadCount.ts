import { useCallback, useEffect, useState } from 'react';
import { messagesApi } from '@/api';

/** Default poll interval for the unread badge (ms). */
const POLL_INTERVAL_MS = 30_000;

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
    return () => window.clearInterval(timer);
  }, [refresh, intervalMs]);

  return { count, refresh };
}
