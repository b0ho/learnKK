import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi, resolveErrorMessage } from '@/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { FieldError } from '@/components/FieldError';
import { PATHS } from '@/routes/paths';
import { validateSignup, type FieldErrors, type SignupFormValues } from './validation';

export function SignupPage() {
  const navigate = useNavigate();

  const [values, setValues] = useState<SignupFormValues>({
    employeeNo: '',
    nickname: '',
    password: '',
    role: 'MENTEE',
  });
  const [errors, setErrors] = useState<FieldErrors<SignupFormValues>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    const nextErrors = validateSignup(values);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    setSubmitting(true);
    try {
      await authApi.signup(values);
      setDone(true);
    } catch (error) {
      // 409 DUPLICATE_EMPLOYEE_NO / DUPLICATE_NICKNAME etc. -> server Korean message.
      setFormError(resolveErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  if (done) {
    return (
      <div className="flex flex-col gap-6 pt-8">
        <Card>
          <CardHeader>
            <CardTitle>가입 완료</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <p className="text-sm text-muted-foreground" data-testid="signup-success">
              가입이 완료되었습니다. 로그인 후 이용해 주세요.
            </p>
            <Button data-testid="go-login" onClick={() => navigate(PATHS.login)}>
              로그인하러 가기
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 pt-8">
      <Card>
        <CardHeader>
          <CardTitle>회원가입</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
            {formError && (
              <p role="alert" className="text-sm text-destructive" data-testid="signup-error">
                {formError}
              </p>
            )}

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="signup-employeeNo">사번</Label>
              <Input
                id="signup-employeeNo"
                data-testid="signup-employeeNo"
                value={values.employeeNo}
                aria-invalid={Boolean(errors.employeeNo)}
                aria-describedby={errors.employeeNo ? 'signup-employeeNo-error' : undefined}
                onChange={(e) => setValues((v) => ({ ...v, employeeNo: e.target.value }))}
              />
              <FieldError id="signup-employeeNo-error" message={errors.employeeNo} />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="signup-nickname">닉네임</Label>
              <Input
                id="signup-nickname"
                data-testid="signup-nickname"
                value={values.nickname}
                aria-invalid={Boolean(errors.nickname)}
                aria-describedby={errors.nickname ? 'signup-nickname-error' : undefined}
                onChange={(e) => setValues((v) => ({ ...v, nickname: e.target.value }))}
              />
              <FieldError id="signup-nickname-error" message={errors.nickname} />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="signup-password">비밀번호</Label>
              <Input
                id="signup-password"
                data-testid="signup-password"
                type="password"
                value={values.password}
                autoComplete="new-password"
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? 'signup-password-error' : undefined}
                onChange={(e) => setValues((v) => ({ ...v, password: e.target.value }))}
              />
              <FieldError id="signup-password-error" message={errors.password} />
            </div>

            <fieldset className="flex flex-col gap-2">
              <legend className="text-sm font-medium">역할</legend>
              <RadioGroup
                value={values.role}
                onValueChange={(role) =>
                  setValues((v) => ({ ...v, role: role as SignupFormValues['role'] }))
                }
                className="flex gap-6"
              >
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="MENTEE" id="role-mentee" data-testid="role-mentee" />
                  <Label htmlFor="role-mentee">멘티</Label>
                </div>
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="MENTOR" id="role-mentor" data-testid="role-mentor" />
                  <Label htmlFor="role-mentor">멘토</Label>
                </div>
              </RadioGroup>
            </fieldset>

            <Button type="submit" data-testid="signup-submit" disabled={submitting}>
              {submitting ? '가입 중...' : '가입하기'}
            </Button>
          </form>
        </CardContent>
      </Card>

      <p className="text-center text-sm text-muted-foreground">
        이미 계정이 있으신가요?{' '}
        <Link to={PATHS.login} className="font-medium text-primary underline-offset-4 hover:underline" data-testid="link-login">
          로그인
        </Link>
      </p>
    </div>
  );
}
