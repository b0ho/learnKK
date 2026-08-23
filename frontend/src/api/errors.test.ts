import { describe, expect, it } from 'vitest';
import { ApiError, ERROR_CODE_MESSAGES, isApiErrorCode, resolveErrorMessage } from './errors';

describe('ApiError', () => {
  it('prefers the server-provided message', () => {
    const err = new ApiError(409, { code: 'DUPLICATE_NICKNAME', message: '이미 사용 중입니다.' });
    expect(err.message).toBe('이미 사용 중입니다.');
    expect(err.code).toBe('DUPLICATE_NICKNAME');
    expect(err.status).toBe(409);
  });

  it('falls back to the code map when message is blank', () => {
    const err = new ApiError(401, { code: 'AUTH_INVALID_CREDENTIALS', message: '   ' });
    expect(err.message).toBe(ERROR_CODE_MESSAGES.AUTH_INVALID_CREDENTIALS);
  });

  it('uses a generic message for unknown codes without message', () => {
    const err = new ApiError(500, {});
    expect(err.code).toBe('UNKNOWN');
    expect(err.message.length).toBeGreaterThan(0);
  });
});

describe('resolveErrorMessage', () => {
  it('reads ApiError messages', () => {
    expect(resolveErrorMessage(new ApiError(404, { code: 'MEETING_NOT_FOUND' }))).toBe(
      ERROR_CODE_MESSAGES.MEETING_NOT_FOUND,
    );
  });

  it('reads plain Error messages', () => {
    expect(resolveErrorMessage(new Error('네트워크 오류'))).toBe('네트워크 오류');
  });

  it('handles non-error values', () => {
    expect(resolveErrorMessage('nope')).toMatch(/문제가 발생/);
  });
});

describe('isApiErrorCode', () => {
  it('matches by code', () => {
    const err = new ApiError(409, { code: 'MEETING_INVALID_TRANSITION' });
    expect(isApiErrorCode(err, 'MEETING_INVALID_TRANSITION')).toBe(true);
    expect(isApiErrorCode(err, 'DUPLICATE_NICKNAME')).toBe(false);
    expect(isApiErrorCode(new Error('x'), 'ANY')).toBe(false);
  });
});
