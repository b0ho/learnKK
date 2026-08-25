import { Toaster as Sonner, type ToasterProps } from 'sonner';

/**
 * App toast surface (additive). Mounted once in AppShell. Existing
 * `role="alert"` error flows are NOT converted to toasts — this is purely
 * additive polish for new notifications. Light-only for now (theme scaffold
 * lives in index.css `.dark`).
 */
function Toaster(props: ToasterProps) {
  return (
    <Sonner
      theme="light"
      className="toaster group"
      toastOptions={{
        classNames: {
          toast:
            'group toast group-[.toaster]:bg-popover group-[.toaster]:text-popover-foreground group-[.toaster]:border-border group-[.toaster]:shadow-lg group-[.toaster]:rounded-md',
          description: 'group-[.toast]:text-muted-foreground',
          actionButton:
            'group-[.toast]:bg-primary group-[.toast]:text-primary-foreground',
          cancelButton:
            'group-[.toast]:bg-muted group-[.toast]:text-muted-foreground',
        },
      }}
      {...props}
    />
  );
}

export { Toaster };
