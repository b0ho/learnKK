// Client-side validation mirroring the backend business rules (BR-U2-*).
// The server remains the source of truth; these checks give fast feedback.

export const EMPLOYEE_NO_PATTERN = /^[A-Za-z0-9]{4,20}$/;
export const PASSWORD_MIN_LENGTH = 8;
export const TAGS_MAX = 10;
export const INTRO_MAX = 500;

export interface SignupFormValues {
  employeeNo: string;
  nickname: string;
  password: string;
  role: 'MENTOR' | 'MENTEE';
}

export type FieldErrors<T> = Partial<Record<keyof T, string>>;

export function validateSignup(values: SignupFormValues): FieldErrors<SignupFormValues> {
  const errors: FieldErrors<SignupFormValues> = {};

  if (!values.employeeNo.trim()) {
    errors.employeeNo = '사번을 입력해 주세요.';
  } else if (!EMPLOYEE_NO_PATTERN.test(values.employeeNo)) {
    errors.employeeNo = '사번은 영문·숫자 4~20자여야 합니다.';
  }

  if (!values.nickname.trim()) {
    errors.nickname = '닉네임을 입력해 주세요.';
  }

  if (!values.password) {
    errors.password = '비밀번호를 입력해 주세요.';
  } else if (values.password.length < PASSWORD_MIN_LENGTH) {
    errors.password = `비밀번호는 최소 ${PASSWORD_MIN_LENGTH}자 이상이어야 합니다.`;
  }

  return errors;
}

export interface LoginFormValues {
  nickname: string;
  password: string;
}

export function validateLogin(values: LoginFormValues): FieldErrors<LoginFormValues> {
  const errors: FieldErrors<LoginFormValues> = {};
  if (!values.nickname.trim()) {
    errors.nickname = '닉네임을 입력해 주세요.';
  }
  if (!values.password) {
    errors.password = '비밀번호를 입력해 주세요.';
  }
  return errors;
}

export interface ProfileFormValues {
  tags: string[];
  intro: string;
}

export function validateProfile(values: ProfileFormValues): FieldErrors<ProfileFormValues> {
  const errors: FieldErrors<ProfileFormValues> = {};
  if (values.tags.length > TAGS_MAX) {
    errors.tags = `관심사 태그는 최대 ${TAGS_MAX}개까지 등록할 수 있습니다.`;
  }
  if (values.intro.length > INTRO_MAX) {
    errors.intro = `소개는 최대 ${INTRO_MAX}자까지 입력할 수 있습니다.`;
  }
  return errors;
}
