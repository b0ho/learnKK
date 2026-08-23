import { NavLink, Outlet } from 'react-router-dom';
import { BookOpen, GraduationCap, User } from 'lucide-react';
import { cn } from '@/lib/utils';
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
  { to: PATHS.profile, label: '내정보', icon: User, testId: 'tab-profile' },
];

/** Mobile-first app frame: scrollable content + fixed bottom 3-tab navigation. */
export function AppShell() {
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
                'flex flex-1 flex-col items-center gap-1 py-2 text-xs font-medium transition-colors',
                isActive ? 'text-primary' : 'text-muted-foreground hover:text-foreground',
              )
            }
          >
            <Icon className="h-5 w-5" aria-hidden="true" />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
