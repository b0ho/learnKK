import { NavLink, Outlet } from 'react-router-dom';
import { BookOpen, GraduationCap, MessageSquare, User } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useUnreadCount } from '@/features/messaging/useUnreadCount';
import { PATHS } from './paths';

interface TabDef {
  to: string;
  label: string;
  icon: typeof BookOpen;
  testId: string;
}

const TABS: TabDef[] = [
  { to: PATHS.meetings, label: '모임', icon: BookOpen, testId: 'tab-meetings' },
  { to: PATHS.myLearning, label: '내 러닝', icon: GraduationCap, testId: 'tab-my-learning' },
  { to: PATHS.messages, label: '쪽지', icon: MessageSquare, testId: 'tab-messages' },
  { to: PATHS.profile, label: '내정보', icon: User, testId: 'tab-profile' },
];

/** Mobile-first app frame: scrollable content + fixed bottom navigation with an unread badge. */
export function AppShell() {
  const { count: unread } = useUnreadCount();

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col bg-background">
      <header className="border-b px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">런크크</h1>
      </header>

      <main className="flex-1 px-4 py-4 pb-24" data-testid="app-content">
        <Outlet />
      </main>

      <nav
        className="fixed inset-x-0 bottom-0 mx-auto flex max-w-md border-t bg-background"
        aria-label="주요 메뉴"
      >
        {TABS.map(({ to, label, icon: Icon, testId }) => (
          <NavLink
            key={to}
            to={to}
            data-testid={testId}
            className={({ isActive }) =>
              cn(
                'relative flex flex-1 flex-col items-center gap-1 py-2 text-xs font-medium transition-colors',
                isActive ? 'text-primary' : 'text-muted-foreground hover:text-foreground',
              )
            }
          >
            <span className="relative">
              <Icon className="h-5 w-5" aria-hidden="true" />
              {testId === 'tab-messages' && unread > 0 && (
                <span
                  data-testid="unread-badge"
                  aria-label={`읽지 않은 쪽지 ${unread}건`}
                  className="absolute -right-2 -top-1 inline-flex min-w-[1rem] items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-bold leading-none text-destructive-foreground"
                >
                  {unread > 99 ? '99+' : unread}
                </span>
              )}
            </span>
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
