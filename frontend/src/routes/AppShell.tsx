import { NavLink, Outlet } from 'react-router-dom';
import { BookOpen, GraduationCap, MessageSquare, Shield, User } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth } from '@/auth/useAuth';
import { useUnreadCount } from '@/features/messaging/useUnreadCount';
import { Toaster } from '@/components/ui/sonner';
import { PATHS } from './paths';

interface TabDef {
  to: string;
  label: string;
  icon: typeof BookOpen;
  testId: string;
}

/** Mobile-first app frame: scrollable content + fixed bottom navigation with an unread badge. */
export function AppShell() {
  const { count: unread } = useUnreadCount();
  const { role } = useAuth();

  // 관리자는 '내 러닝'이 없으므로 그 자리에 '관리'(승인 큐) 진입점을 노출한다(FR-4).
  const secondTab: TabDef =
    role === 'ADMIN'
      ? { to: PATHS.adminApproval, label: '관리', icon: Shield, testId: 'tab-admin' }
      : { to: PATHS.myLearning, label: '내 러닝', icon: GraduationCap, testId: 'tab-my-learning' };

  const TABS: TabDef[] = [
    { to: PATHS.meetings, label: '모임', icon: BookOpen, testId: 'tab-meetings' },
    secondTab,
    { to: PATHS.messages, label: '쪽지', icon: MessageSquare, testId: 'tab-messages' },
    { to: PATHS.profile, label: '내정보', icon: User, testId: 'tab-profile' },
  ];

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col bg-background">
      <header className="sticky top-0 z-30 border-b border-border/80 bg-background/95 px-4 py-3 backdrop-blur supports-[backdrop-filter]:bg-background/75">
        <h1 className="text-lg font-extrabold tracking-tight text-primary">런크크</h1>
      </header>

      <main className="flex-1 px-4 py-4 pb-24" data-testid="app-content">
        <Outlet />
      </main>

      <Toaster position="top-center" richColors closeButton />

      <nav
        className="fixed inset-x-0 bottom-0 z-30 mx-auto flex max-w-md border-t border-border/80 bg-background/95 shadow-[0_-1px_3px_0_hsl(155_25%_12%_/_0.06)] backdrop-blur supports-[backdrop-filter]:bg-background/85"
        aria-label="주요 메뉴"
      >
        {TABS.map(({ to, label, icon: Icon, testId }) => (
          <NavLink
            key={to}
            to={to}
            data-testid={testId}
            className={({ isActive }) =>
              cn(
                'relative flex flex-1 flex-col items-center gap-1 py-2.5 text-xs font-medium transition-colors',
                isActive
                  ? 'text-primary after:absolute after:inset-x-5 after:top-0 after:h-0.5 after:rounded-full after:bg-primary'
                  : 'text-muted-foreground hover:text-foreground',
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
