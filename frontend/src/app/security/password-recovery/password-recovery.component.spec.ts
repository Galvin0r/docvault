import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, forwardRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
} from '@angular/forms';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { PasswordRecoveryComponent } from './password-recovery.component';
import { SecurityService } from '../security.service';

@Component({
  selector: 'app-auth-card',
  standalone: true,
  template: `
    <ng-content select="[card-body]"></ng-content>
    <ng-content select="[card-footer]"></ng-content>
  `,
})
class AuthCardStub {
  @Input() title!: string;
  @Input() subtitle?: string;
  @Input() showGoogle?: boolean;
  @Output() googleContinue = new EventEmitter<void>();
}

@Component({
  selector: 'app-form-error',
  standalone: true,
  template: `
    <div class="form-error" (click)="onClose.emit()"></div>
  `,
})
class FormErrorStub {
  @Input() error: any;
  @Output() onClose = new EventEmitter<void>();
}

@Component({
  selector: 'p-input-group',
  standalone: true,
  template: `
    <ng-content></ng-content>
  `,
})
class PInputGroupStub { }

@Component({
  selector: 'p-inputgroup-addon',
  standalone: true,
  template: `
    <ng-content></ng-content>
  `,
})
class PInputGroupAddonStub { }

@Component({
  selector: 'p-message',
  standalone: true,
  template: `
    <ng-content></ng-content>
  `,
})
class PMessageStub {
  @Input() severity?: string;
  @Input() variant?: string;
  @Input() size?: string;
}

@Component({
  selector: 'p-password',
  standalone: true,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => PPasswordStub), multi: true },
  ],
  template: `
    <input class="p-password" [class]="inputStyleClass" (input)="onInput($event)" />
  `,
})
class PPasswordStub implements ControlValueAccessor {
  @Input() feedback?: boolean;
  @Input() toggleMask?: boolean;
  @Input() inputStyleClass?: string;
  private onChange = (_: any) => { };
  private onTouched = () => { };
  writeValue(_: any): void { }
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
  setDisabledState(_: boolean): void { }
  onInput(e: Event) {
    this.onChange((e.target as HTMLInputElement).value);
    this.onTouched();
  }
}

class MessageServiceStub {
  add = jasmine.createSpy('add');
}
class RouterStub {
  navigate = jasmine.createSpy('navigate');
}

describe('PasswordRecoveryComponent', () => {
  let fixture: ComponentFixture<PasswordRecoveryComponent>;
  let component: PasswordRecoveryComponent;
  let security: jasmine.SpyObj<SecurityService>;
  let router: RouterStub;
  let messages: MessageServiceStub;

  function create(token: string | null = 'tok123') {
    security = jasmine.createSpyObj<SecurityService>('SecurityService', ['setNewPassword']);

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        ReactiveFormsModule,
        AuthCardStub,
        FormErrorStub,
        PInputGroupStub,
        PInputGroupAddonStub,
        PMessageStub,
        PPasswordStub,
      ],
      declarations: [PasswordRecoveryComponent],
      providers: [
        FormBuilder,
        { provide: Router, useClass: RouterStub },
        { provide: MessageService, useClass: MessageServiceStub },
        { provide: SecurityService, useValue: security },
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(token ? { token } : {}) } },
        },
      ],
    });

    fixture = TestBed.createComponent(PasswordRecoveryComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as any;
    messages = TestBed.inject(MessageService) as any;
    fixture.detectChanges();
  }

  afterEach(() => fixture.destroy());

  it('reads token from query params', () => {
    create('abcXYZ');
    expect(component.token).toBe('abcXYZ');
  });

  it('shows required and minlength errors for password', () => {
    create();
    const pwdCtrl = component.form.get('password')!;
    pwdCtrl.markAsTouched();
    fixture.detectChanges();
    let msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Password is required/i.test(t))).toBeTrue();

    pwdCtrl.setValue('short');
    fixture.detectChanges();
    msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /at least 8/i.test(t))).toBeTrue();
  });

  it('shows mismatch error when passwords differ', () => {
    create();
    component.form.get('password')?.setValue('password123');
    component.form.get('confirmPassword')?.setValue('password124');
    component.form.get('confirmPassword')?.markAsTouched();
    fixture.detectChanges();
    const msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Passwords don't match/i.test(t))).toBeTrue();
  });

  it('adds invalid class to confirm password on mismatch', () => {
    create();
    component.form.get('password')?.setValue('password123');
    component.form.get('confirmPassword')?.markAsTouched();
    component.form.get('confirmPassword')?.setValue('mismatch1');
    fixture.detectChanges();
    const confirmPwdDe = fixture.debugElement.queryAll(By.css('p-password'))[1];
    const confirmInput = confirmPwdDe.query(By.css('input')).nativeElement as HTMLInputElement;
    expect(confirmInput.className).toContain('p-invalid');
  });

  it('submits successfully, shows toast and navigates', () => {
    create('ZzT0k');
    security.setNewPassword.and.returnValue(of(void 0));
    component.form.setValue({ password: 'password123', confirmPassword: 'password123' });
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    expect(security.setNewPassword).toHaveBeenCalledWith('ZzT0k', 'password123');
    expect(messages.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    expect(router.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('onSubmit branch: success subscribe path', () => {
    create('tok');
    security.setNewPassword.and.returnValue(of(void 0));
    component.form.setValue({ password: 'password123', confirmPassword: 'password123' });
    component.onSubmit();
    expect(messages.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    expect((router as any).navigate).toHaveBeenCalledWith(['/home']);
  });

  it('onSubmit branch: error subscribe path', () => {
    create('tok');
    security.setNewPassword.and.returnValue(throwError(() => ({ error: { code: 'ERR' } })));
    component.form.setValue({ password: 'password123', confirmPassword: 'password123' });
    component.onSubmit();
    expect((component as any).error).toBe('ERR');
  });

  it('onSubmit branch: invalid form (early return)', () => {
    create('tok');
    component.form.setValue({ password: '', confirmPassword: '' });
    component.onSubmit();
    expect(security.setNewPassword).not.toHaveBeenCalled();
    expect(component.submitted).toBeTrue();
  });

  describe('Template and Logic branches', () => {
    beforeEach(() => create());

    it('shows and hides password validation message and nested @if branches', () => {
      const pwd = component.form.get('password')!;

      expect(fixture.debugElement.queryAll(By.directive(PMessageStub)).length).toBe(0);

      pwd.markAsTouched();
      pwd.setValue('');
      fixture.detectChanges();
      let msgs = fixture.debugElement.queryAll(By.directive(PMessageStub));
      expect(msgs.length).toBeGreaterThan(0);
      expect(msgs[0].nativeElement.textContent).toContain('Password is required');

      pwd.setValue('short');
      fixture.detectChanges();
      msgs = fixture.debugElement.queryAll(By.directive(PMessageStub));
      expect(msgs[0].nativeElement.textContent).toContain('at least 8');

      pwd.setValue('validPassword123');
      fixture.detectChanges();
      expect(fixture.debugElement.queryAll(By.directive(PMessageStub)).length).toBe(0);
    });

    it('shows and hides isMismatched() validation @if', () => {
      component.form.get('password')?.setValue('p1');
      component.form.get('confirmPassword')?.setValue('p2');

      fixture.detectChanges();
      expect(fixture.debugElement.queryAll(By.directive(PMessageStub)).length).toBe(0);

      component.form.get('confirmPassword')?.markAsTouched();
      fixture.detectChanges();
      expect(fixture.debugElement.queryAll(By.directive(PMessageStub)).length).toBeGreaterThan(0);
      expect(fixture.debugElement.query(By.directive(PMessageStub)).nativeElement.textContent).toContain('match');
    });

    it('isMismatched coverage (exhaustive logic permutations)', () => {
      const pwd = component.form.get('password')!;
      const confirm = component.form.get('confirmPassword')!;

      pwd.setValue('p1');
      confirm.setValue('p1');
      expect(component.isMismatched()).toBeFalse();

      confirm.setValue('p2');
      expect(component.form.hasError('passwordsMismatch')).toBeTrue();
      expect(component.isMismatched()).toBeFalse();

      confirm.markAsTouched();
      expect(component.isMismatched()).toBeTrue();

      create();
      component.form.get('password')?.setValue('p1');
      component.form.get('confirmPassword')?.setValue('p2');
      component.submitted = true;
      expect(component.isMismatched()).toBeTrue();
    });
  });

  it('sets error on submit failure and passes it to form error', () => {
    create('badtoken');
    security.setNewPassword.and.returnValue(
      throwError(() => ({ error: { code: 'INVALID_TOKEN' } }))
    );
    component.form.setValue({ password: 'password123', confirmPassword: 'password123' });
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    fixture.detectChanges();
    const err = fixture.debugElement.query(By.directive(FormErrorStub))
      .componentInstance as FormErrorStub;
    expect(err.error).toBe('INVALID_TOKEN');
  });

  it('reflects password validity classes', () => {
    create();
    const pwdCmpDe = fixture.debugElement.queryAll(By.directive(PPasswordStub))[0];
    component.form.get('password')?.markAsTouched();
    fixture.detectChanges();
    let input = pwdCmpDe.query(By.css('input')).nativeElement as HTMLInputElement;
    expect(input.className).toContain('p-invalid');
    component.form.get('password')?.setValue('password123');
    fixture.detectChanges();
    input = pwdCmpDe.query(By.css('input')).nativeElement as HTMLInputElement;
    expect(input.className).toContain('p-valid');
  });
});
