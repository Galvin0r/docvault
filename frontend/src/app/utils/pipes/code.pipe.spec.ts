import { CodePipe } from './code.pipe';

describe('CodePipe', () => {
  let pipe: CodePipe;

  const terms = [
    { code: 'OWNER', name: 'Owner' },
    { code: 'ADMIN', name: 'Admin' },
    { code: 'USER', name: 'User' },
  ];

  beforeEach(() => {
    pipe = new CodePipe();
  });

  it('maps a known code to its display name', () => {
    expect(pipe.transform('ADMIN', terms as any)).toBe('Admin');
  });

  it('returns empty string when code not found', () => {
    expect(pipe.transform('UNKNOWN', terms as any)).toBe('');
  });

  it('eturns empty string for null/undefined/empty code', () => {
    expect((pipe as any).transform(null, terms)).toBe('');
    expect((pipe as any).transform(undefined, terms)).toBe('');
    expect(pipe.transform('', terms as any)).toBe('');
  });

  it('returns empty string if terms is null/undefined/empty', () => {
    expect((pipe as any).transform('ADMIN', null)).toBe('');
    expect((pipe as any).transform('ADMIN', undefined)).toBe('');
    expect(pipe.transform('ADMIN', [] as any)).toBe('');
  });

  it('prefers the first match when duplicates exist', () => {
    const dup = [
      { code: 'ADMIN', name: 'Admin A' },
      { code: 'ADMIN', name: 'Admin B' },
    ];
    expect(pipe.transform('ADMIN', dup as any)).toBe('Admin A');
  });
});
