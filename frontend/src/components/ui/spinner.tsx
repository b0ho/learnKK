import { Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

interface SpinnerProps {
  className?: string;
  /** 접근성 라벨 및 시각적으로 숨긴 텍스트. */
  label?: string;
  'data-testid'?: string;
}

/**
 * 로딩 표시용 스피너. 로딩 상태에서 텍스트("불러오는 중...") 대신 사용해
 * 텍스트 렌더/제거로 인한 레이아웃 깜빡임(높이 점프)을 줄인다.
 */
export function Spinner({ className, label = '불러오는 중', ...props }: SpinnerProps) {
  return (
    <div
      role="status"
      aria-label={label}
      className={cn('flex items-center justify-center py-4 text-muted-foreground', className)}
      {...props}
    >
      <Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" />
      <span className="sr-only">{label}</span>
    </div>
  );
}
