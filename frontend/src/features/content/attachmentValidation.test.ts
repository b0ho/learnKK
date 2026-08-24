import { describe, expect, it } from 'vitest';
import { MAX_ATTACHMENT_BYTES, validateAttachment } from './attachmentValidation';

function fileOf(type: string, size: number, name = 'f'): File {
  const file = new File(['x'], name, { type });
  // Override the size getter since File in jsdom derives size from content.
  Object.defineProperty(file, 'size', { value: size });
  return file;
}

describe('validateAttachment', () => {
  it('accepts a PDF within the size cap', () => {
    expect(validateAttachment(fileOf('application/pdf', 1024))).toBeNull();
  });

  it('accepts an office document (docx)', () => {
    const docx = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
    expect(validateAttachment(fileOf(docx, 1024))).toBeNull();
  });

  it('accepts text/plain with a charset parameter', () => {
    expect(validateAttachment(fileOf('text/plain; charset=utf-8', 10))).toBeNull();
  });

  it('rejects a disallowed type', () => {
    expect(validateAttachment(fileOf('application/x-msdownload', 10))).toMatch(/형식/);
  });

  it('rejects SVG (stored-XSS vector)', () => {
    expect(validateAttachment(fileOf('image/svg+xml', 10))).toMatch(/형식/);
  });

  it('rejects files over the 20MB cap', () => {
    expect(validateAttachment(fileOf('application/pdf', MAX_ATTACHMENT_BYTES + 1))).toMatch(/20MB/);
  });

  it('rejects an empty file', () => {
    expect(validateAttachment(fileOf('application/pdf', 0))).toMatch(/빈 파일/);
  });
});
