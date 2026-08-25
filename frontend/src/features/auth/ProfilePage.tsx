import { useEffect, useState, type FormEvent, type KeyboardEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { usersApi, resolveErrorMessage, type ProfileResponse } from '@/api';
import { useAuth } from '@/auth/useAuth';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Spinner } from '@/components/ui/spinner';
import { FieldError } from '@/components/FieldError';
import { PATHS } from '@/routes/paths';
import { roleLabel } from '@/features/shared/roleLabel';
import { INTRO_MAX, TAGS_MAX, validateProfile, type FieldErrors, type ProfileFormValues } from './validation';

export function ProfilePage() {
  const navigate = useNavigate();
  const { role, logout } = useAuth();

  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [tags, setTags] = useState<string[]>([]);
  const [intro, setIntro] = useState('');
  const [tagDraft, setTagDraft] = useState('');
  const [errors, setErrors] = useState<FieldErrors<ProfileFormValues>>({});
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [savedMessage, setSavedMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    usersApi
      .getMyProfile()
      .then((data) => {
        if (!active) return;
        setProfile(data);
        setTags(data.tags ?? []);
        setIntro(data.intro ?? '');
      })
      .catch((error) => {
        if (active) setLoadError(resolveErrorMessage(error));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  function addTag() {
    const value = tagDraft.trim();
    if (!value || tags.includes(value)) {
      setTagDraft('');
      return;
    }
    setTags((prev) => [...prev, value]);
    setTagDraft('');
  }

  function handleTagKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      event.preventDefault();
      addTag();
    }
  }

  function removeTag(tag: string) {
    setTags((prev) => prev.filter((t) => t !== tag));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    setSavedMessage(null);
    const nextErrors = validateProfile({ tags, intro });
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    setSaving(true);
    try {
      const updated = await usersApi.updateMyProfile({ tags, intro: intro || null });
      setProfile(updated);
      setTags(updated.tags ?? []);
      setIntro(updated.intro ?? '');
      setSavedMessage('프로필이 저장되었습니다.');
    } catch (error) {
      setFormError(resolveErrorMessage(error));
    } finally {
      setSaving(false);
    }
  }

  async function handleLogout() {
    await logout();
    navigate(PATHS.login, { replace: true });
  }

  if (loading) {
    return <Spinner data-testid="profile-loading" label="프로필을 불러오는 중" />;
  }

  if (loadError) {
    return (
      <p role="alert" className="text-destructive" data-testid="profile-load-error">
        {loadError}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <CardTitle>내정보</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">닉네임</span>
            <span className="font-medium" data-testid="profile-nickname">
              {profile?.nickname}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">사번</span>
            <span className="font-medium" data-testid="profile-employeeNo">
              {profile?.employeeNo}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">역할</span>
            <Badge variant="secondary" data-testid="profile-role">
              {role ? roleLabel(role) : '-'}
            </Badge>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>프로필 수정</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
            {formError && (
              <p role="alert" className="text-sm text-destructive" data-testid="profile-error">
                {formError}
              </p>
            )}
            {savedMessage && (
              <p className="text-sm text-primary" data-testid="profile-saved">
                {savedMessage}
              </p>
            )}

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="profile-tag-input">관심사 태그 (최대 {TAGS_MAX}개)</Label>
              <div className="flex gap-2">
                <Input
                  id="profile-tag-input"
                  data-testid="profile-tag-input"
                  value={tagDraft}
                  placeholder="태그 입력 후 추가"
                  onChange={(e) => setTagDraft(e.target.value)}
                  onKeyDown={handleTagKeyDown}
                />
                <Button type="button" variant="outline" data-testid="profile-tag-add" onClick={addTag}>
                  추가
                </Button>
              </div>
              <div className="flex flex-wrap gap-2" data-testid="profile-tags">
                {tags.map((tag) => (
                  <Badge key={tag} variant="secondary" className="gap-1">
                    {tag}
                    <button
                      type="button"
                      aria-label={`${tag} 삭제`}
                      data-testid={`profile-tag-remove-${tag}`}
                      className="ml-1 text-muted-foreground hover:text-foreground"
                      onClick={() => removeTag(tag)}
                    >
                      ×
                    </button>
                  </Badge>
                ))}
              </div>
              <FieldError id="profile-tags-error" message={errors.tags} />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="profile-intro">소개 (최대 {INTRO_MAX}자)</Label>
              <Textarea
                id="profile-intro"
                data-testid="profile-intro"
                value={intro}
                maxLength={INTRO_MAX + 50}
                aria-invalid={Boolean(errors.intro)}
                aria-describedby={errors.intro ? 'profile-intro-error' : undefined}
                onChange={(e) => setIntro(e.target.value)}
              />
              <FieldError id="profile-intro-error" message={errors.intro} />
            </div>

            <Button type="submit" data-testid="profile-save" disabled={saving}>
              {saving ? '저장 중...' : '저장'}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Button variant="outline" data-testid="logout-button" onClick={handleLogout}>
        로그아웃
      </Button>
    </div>
  );
}
