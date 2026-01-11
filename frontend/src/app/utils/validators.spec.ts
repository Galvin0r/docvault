import { FormBuilder } from '@angular/forms';
import { passwordsMatchValidator } from './validators';
import { FormControl } from '@angular/forms';
import { fileSizeValidator } from './validators';

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

describe('fileSizeValidator', () => {
  const maxSize = 1000;
  const validator = fileSizeValidator(maxSize);

  it('returns null if no file is present', () => {
    const control = new FormControl(null);
    expect(validator(control)).toBeNull();
  });

  it('returns null if file size is within limit', () => {
    const file = { size: 500 } as File;
    const control = new FormControl(file);
    expect(validator(control)).toBeNull();
  });

  it('returns error if file size exceeds limit', () => {
    const file = { size: 1500 } as File;
    const control = new FormControl(file);
    expect(validator(control)).toEqual({
      maxSize: { requiredLength: maxSize, actualLength: 1500 }
    });
  });
});
