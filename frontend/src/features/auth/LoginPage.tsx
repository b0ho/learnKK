import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { authApi, resolveErrorMessage } from '@/api';
import { useAuth } from '@/auth/useAuth';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { FieldError } from '@/components/FieldError';
import { PATHS } from '@/routes/paths';
import { validateLogin, type FieldErrors, type LoginFormValues } from './validation';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [values, setValues] = useState<LoginFormValues>({ nickname: '', password: '' });
  const [errors, setErrors] = useState<FieldErrors<LoginFormValues>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const redirectTo = (location.state as { from?: string } | null)?.from ?? PATHS.meetings;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    const nextErrors = validateLogin(values);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    setSubmitting(true);
    try {
      const session = await authApi.login(values);
      login(session.token, session.role);
      navigate(redirectTo, { replace: true });
    } catch (error) {
      // 401 -> AUTH_INVALID_CREDENTIALS Korean message from the server.
      setFormError(resolveErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex flex-col gap-6 pt-8">
      <Card>
        <CardHeader>
          <CardTitle>로그인</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
            {formError && (
              <p role="alert" className="text-sm text-destructive" data-testid="login-error">
                {formError}
              </p>
            )}

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="login-nickname">닉네임</Label>
              <Input
                id="login-nickname"
                data-testid="login-nickname"
                value={values.nickname}
                autoComplete="username"
                aria-invalid={Boolean(errors.nickname)}
                aria-describedby={errors.nickname ? 'login-nickname-error' : undefined}
                onChange={(e) => setValues((v) => ({ ...v, nickname: e.target.value }))}
              />
              <FieldError id="login-nickname-error" message={errors.nickname} />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="login-password">비밀번호</Label>
              <Input
                id="login-password"
                data-testid="login-password"
                type="password"
                value={values.password}
                autoComplete="current-password"
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? 'login-password-error' : undefined}
                onChange={(e) => setValues((v) => ({ ...v, password: e.target.value }))}
              />
              <FieldError id="login-password-error" message={errors.password} />
            </div>

            <Button type="submit" data-testid="login-submit" disabled={submitting}>
              {submitting ? '로그인 중...' : '로그인'}
            </Button>
          </form>
        </CardContent>
      </Card>

      <p className="text-center text-sm text-muted-foreground">
        아직 계정이 없으신가요?{' '}
        <Link to={PATHS.signup} className="font-medium text-primary underline-offset-4 hover:underline" data-testid="link-signup">
          가입하기
        </Link>
      </p>
    </div>
  );
}
