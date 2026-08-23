import { describe, expect, it } from 'vitest';
import { validateLogin, validateProfile, validateSignup } from './validation';

describe('validateSignup', () => {
  it('accepts a valid payload', () => {
    expect(
      validateSignup({ employeeNo: 'EMP1234', nickname: '길동', password: 'password1', role: 'MENTEE' }),
    ).toEqual({});
  });

  it('rejects short / non-alphanumeric employeeNo', () => {
    expect(validateSignup({ employeeNo: 'ab', nickname: 'n', password: 'password1', role: 'MENTEE' }).employeeNo).toBeDefined();
    expect(validateSignup({ employeeNo: 'emp!!', nickname: 'n', password: 'password1', role: 'MENTEE' }).employeeNo).toBeDefined();
  });

  it('rejects blank required fields and short password', () => {
    const errors = validateSignup({ employeeNo: '', nickname: '', password: '123', role: 'MENTOR' });
    expect(errors.employeeNo).toBeDefined();
    expect(errors.nickname).toBeDefined();
    expect(errors.password).toBeDefined();
  });
});

describe('validateLogin', () => {
  it('requires both fields', () => {
    const errors = validateLogin({ nickname: '', password: '' });
    expect(errors.nickname).toBeDefined();
    expect(errors.password).toBeDefined();
  });

  it('passes with values', () => {
    expect(validateLogin({ nickname: 'a', password: 'b' })).toEqual({});
  });
});

describe('validateProfile', () => {
  it('enforces tag and intro limits', () => {
    const tags = Array.from({ length: 11 }, (_, i) => `t${i}`);
    const errors = validateProfile({ tags, intro: 'x'.repeat(501) });
    expect(errors.tags).toBeDefined();
    expect(errors.intro).toBeDefined();
  });

  it('passes within limits', () => {
    expect(validateProfile({ tags: ['a'], intro: '소개' })).toEqual({});
  });
});
