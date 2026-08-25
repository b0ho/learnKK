import { useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft, BookOpen, GraduationCap, MessageSquare, Shield, User } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
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

/**
 * 탭의 스코프 루트. '관리' 탭(→ /admin/meetings)은 승인 큐·운영 현황 등 /admin 하위 전체를 하나의 탭으로 묶는다.
 */
function tabScope(to: string): string {
  return to === PATHS.adminApproval ? '/admin' : to;
}

/** A tab "owns" its scope root and any deeper path under it (segment-aware, so /me ≠ /meetings). */
function isWithinTab(pathname: string, to: string): boolean {
  const scope = tabScope(to);
  return pathname === scope || pathname.startsWith(`${scope}/`);
}

/** Tab root paths — the back button is hidden on these (they are tab tops, not deep views). */
const TAB_ROOTS = [
  PATHS.meetings,
  PATHS.myLearning,
  PATHS.adminApproval,
  PATHS.messages,
  PATHS.profile,
];

/** Mobile-first app frame: scrollable content + fixed bottom navigation with an unread badge. */
export function AppShell() {
  const { count: unread } = useUnreadCount();
  const { role } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  // FR-1: 활성 탭 재클릭 시 현재 탭 콘텐츠를 강제 리마운트(재조회)하기 위한 키.
  const [reloadKey, setReloadKey] = useState(0);
  const pathname = location.pathname;
  // FR-3: 탭 루트가 아닌 하위(뎁스) 화면에서만 '이전' 버튼을 노출한다.
  const showBack = pathname !== '/' && !TAB_ROOTS.includes(pathname);

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
        {/* FR-1: key = pathname + reloadKey → 활성 탭 재클릭 시 리마운트되어 데이터가 재조회된다. */}
        <div key={`${pathname}-${reloadKey}`}>
          <Outlet />
        </div>
      </main>

      {/* FR-3: 뎁스 화면에서 이전 화면으로 돌아가는 우하단 플로팅 버튼. */}
      {showBack && (
        <Button
          type="button"
          variant="secondary"
          size="sm"
          data-testid="floating-back"
          aria-label="이전 화면으로"
          onClick={() => navigate(-1)}
          className="fixed bottom-20 right-4 z-40 gap-1 rounded-full shadow-md"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          이전
        </Button>
      )}

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
            onClick={(e) => {
              // FR-1: 이미 이 탭 안(루트 또는 하위)에 있으면, 탭 루트로 이동 + 강제 재조회.
              if (isWithinTab(pathname, to)) {
                e.preventDefault();
                if (pathname !== to) {
                  navigate(to);
                }
                setReloadKey((k) => k + 1);
              }
            }}
            className={() =>
              cn(
                'relative flex flex-1 flex-col items-center gap-1 py-2.5 text-xs font-medium transition-colors',
                isWithinTab(pathname, to)
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
