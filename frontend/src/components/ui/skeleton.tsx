import * as React from 'react';
import { cn } from '@/lib/utils';

/** Loading placeholder block. Additive polish — does not replace any existing
 *  text-based loading state that tests assert on. */
function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('animate-pulse rounded-md bg-muted', className)} {...props} />;
}

export { Skeleton };
