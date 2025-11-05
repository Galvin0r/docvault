import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from './base-form.component';
import { TestBed } from '@angular/core/testing';
import { SecurityService } from './security.service';
import { Router } from '@angular/router';

class TestFormCmp extends BaseFormComponent {
  protected buildForm(): FormGroup {
    return this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }
}

describe('BaseFormComponent', () => {
  let cmp: TestFormCmp;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        FormBuilder,
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate'), url: '/home' } },
        { provide: SecurityService, useValue: {} },
      ],
    });

    cmp = TestBed.runInInjectionContext(() => new TestFormCmp());
  });

  it('creates form when constructing (with fields email and password)', () => {
    expect(cmp.form).toBeTruthy();
    expect(cmp.ctrl('email')).toBeTruthy();
    expect(cmp.ctrl('password')).toBeTruthy();
  });

  it('ctrl() returns null for not existing control', () => {
    expect(cmp.ctrl('nope')).toBeNull();
  });

  it('isValid()/isInvalid() – requires valid && (touched || submitted)', () => {
    expect(cmp.isValid('email')).toBeFalse();
    expect(cmp.isInvalid('email')).toBeFalse();

    cmp.ctrl('email')!.setValue('a@b.com');
    cmp.ctrl('password')!.setValue('secret1');
    expect(cmp.isValid('email')).toBeFalse();

    cmp.ctrl('email')!.markAsTouched();
    expect(cmp.isValid('email')).toBeTrue();

    cmp.ctrl('email')!.setValue('');
    cmp.ctrl('email')!.markAsTouched();
    cmp.form.updateValueAndValidity();
    expect(cmp.isInvalid('email')).toBeTrue();
  });

  it('hasError() – proxy for control', () => {
    cmp.ctrl('email')!.setValue('');
    cmp.form.updateValueAndValidity();
    expect(cmp.hasError('email', 'required')).toBeTrue();

    expect(cmp.hasError('nope', 'required')).toBeUndefined();
  });

  it('guardSubmit(): for invalid → sets submitted, markAllAsTouched and returns false', () => {
    expect(cmp.form.valid).toBeFalse();

    const res = cmp['guardSubmit']();
    expect(res).toBeFalse();
    expect(cmp.submitted).toBeTrue();
    expect(cmp.ctrl('email')!.touched).toBeTrue();
    expect(cmp.ctrl('password')!.touched).toBeTrue();
  });

  it('guardSubmit(): for valid → sets submitted and returns true, without markAllAsTouched', () => {
    cmp = TestBed.runInInjectionContext(() => new TestFormCmp());
    cmp.ctrl('email')!.setValue('a@b.com');
    cmp.ctrl('password')!.setValue('secret1');
    cmp.form.updateValueAndValidity();
    expect(cmp.form.valid).toBeTrue();

    const res = cmp['guardSubmit']();
    expect(res).toBeTrue();
    expect(cmp.submitted).toBeTrue();
    expect(cmp.ctrl('email')!.touched).toBeFalse();
    expect(cmp.ctrl('password')!.touched).toBeFalse();
  });

  it('onErrorClose() clears error', () => {
    (cmp as any).error = 'SOME_CODE';
    cmp.onErrorClose();
    expect((cmp as any).error).toBeNull();
  });
});
