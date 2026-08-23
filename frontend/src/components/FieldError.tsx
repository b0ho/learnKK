interface FieldErrorProps {
  id: string;
  message?: string;
}

/** Accessible inline field error, linked to an input via aria-describedby. */
export function FieldError({ id, message }: FieldErrorProps) {
  if (!message) {
    return null;
  }
  return (
    <p id={id} role="alert" className="text-sm text-destructive" data-testid={id}>
      {message}
    </p>
  );
}
