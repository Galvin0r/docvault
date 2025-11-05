import { FormBuilder } from '@angular/forms';
import { passwordsMatchValidator } from './validators';

describe('passwordsMatchValidator', () => {
  const fb = new FormBuilder();

  it('returns null when passwords match', () => {
    const form = fb.group({ password: 'abc123', confirmPassword: 'abc123' });
    expect(passwordsMatchValidator(form)).toBeNull();
  });

  it('returns error when passwords differ', () => {
    const form = fb.group({ password: 'abc123', confirmPassword: 'xyz' });
    expect(passwordsMatchValidator(form)).toEqual({ passwordsMismatch: true });
  });

  it('returns null when one or both values are empty', () => {
    expect(passwordsMatchValidator(fb.group({ password: '', confirmPassword: '' }))).toBeNull();
    expect(passwordsMatchValidator(fb.group({ password: 'abc', confirmPassword: '' }))).toBeNull();
    expect(passwordsMatchValidator(fb.group({ password: '', confirmPassword: 'abc' }))).toBeNull();
  });

  it('returns null when controls are missing', () => {
    const form = fb.group({});
    expect(passwordsMatchValidator(form)).toBeNull();
  });

  it('handles non-string values', () => {
    const form = fb.group({ password: 123, confirmPassword: 123 });
    expect(passwordsMatchValidator(form)).toBeNull();
  });
});
