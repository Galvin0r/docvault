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
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SignupComponent } from './signup.component';
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

class RouterStub {
  navigate = jasmine.createSpy('navigate');
}

describe('SignupComponent', () => {
  let fixture: ComponentFixture<SignupComponent>;
  let component: SignupComponent;
  let security: jasmine.SpyObj<SecurityService>;
  let router: RouterStub;

  function create() {
    security = jasmine.createSpyObj<SecurityService>('SecurityService', ['register']);

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
      declarations: [SignupComponent],
      providers: [
        FormBuilder,
        { provide: SecurityService, useValue: security },
        { provide: Router, useClass: RouterStub },
        provideRouter([]),
      ],
    });

    fixture = TestBed.createComponent(SignupComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as any;
    fixture.detectChanges();
  }

  afterEach(() => fixture.destroy());

  it('shows required validation for username', () => {
    create();
    component.form.get('username')?.markAsTouched();
    fixture.detectChanges();
    const msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Username is required/i.test(t))).toBeTrue();
  });

  it('validates email: required and format', () => {
    create();
    const emailInput = fixture.debugElement.query(By.css('input[formControlName="email"]'));
    const email = component.form.get('email')!;
    email.markAsTouched();
    fixture.detectChanges();
    let msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Email is required/i.test(t))).toBeTrue();

    email.setValue('bad');
    fixture.detectChanges();
    msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Not a valid email/i.test(t))).toBeTrue();

    email.setValue('user@example.com');
    fixture.detectChanges();
    expect(emailInput.nativeElement.classList).toContain('p-valid');
  });

  it('validates password required and minlength', () => {
    create();
    const pwd = component.form.get('passwords.password')!;
    pwd.markAsTouched();
    fixture.detectChanges();
    let msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Password is required/i.test(t))).toBeTrue();

    pwd.setValue('short');
    fixture.detectChanges();
    msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /at least 8/i.test(t))).toBeTrue();
  });

  it('shows mismatch error when passwords differ', () => {
    create();
    component.form.get('passwords.password')?.setValue('password123');
    component.form.get('passwords.confirmPassword')?.setValue('password124');
    component.form.get('passwords.confirmPassword')?.markAsTouched();
    fixture.detectChanges();
    const msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Passwords don't match/i.test(t))).toBeTrue();
  });

  it('applies invalid/valid classes to p-password inputs', () => {
    create();
    const pwdDe = fixture.debugElement.queryAll(By.directive(PPasswordStub))[0];
    component.form.get('passwords.password')?.markAsTouched();
    fixture.detectChanges();
    let input = pwdDe.query(By.css('input')).nativeElement as HTMLInputElement;
    expect(input.className).toContain('p-invalid');

    component.form.get('passwords.password')?.setValue('password123');
    fixture.detectChanges();
    input = pwdDe.query(By.css('input')).nativeElement as HTMLInputElement;
    expect(input.className).toContain('p-valid');
  });

  it('submits valid form, calls register and navigates with email query param', () => {
    create();
    security.register.and.returnValue(of(void 0));
    component.form.setValue({
      username: 'alice',
      email: 'alice@example.com',
      passwords: { password: 'password123', confirmPassword: 'password123' },
    });
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    expect(security.register).toHaveBeenCalled();
    const req = security.register.calls.mostRecent().args[0];
    expect(req.login).toBe('alice');
    expect(req.password).toBe('password123');
    expect(req.email).toBe('alice@example.com');
    expect((router as any).navigate).toHaveBeenCalledWith(['/emailVerification'], {
      queryParams: { email: 'alice@example.com' },
    });
  });

  it('onSubmit branch: success subscribe path', () => {
    create();
    security.register.and.returnValue(of(void 0));
    component.form.setValue({
      username: 'alice',
      email: 'p1@site.com',
      passwords: { password: 'password123', confirmPassword: 'password123' },
    });
    component.onSubmit();
    expect((router as any).navigate).toHaveBeenCalledWith(['/emailVerification'], {
      queryParams: { email: 'p1@site.com' },
    });
  });

  it('onSubmit branch: error subscribe path', () => {
    create();
    security.register.and.returnValue(throwError(() => ({ error: { code: 'ERR' } })));
    component.form.setValue({
      username: 'alice',
      email: 'p1@site.com',
      passwords: { password: 'password123', confirmPassword: 'password123' },
    });
    component.onSubmit();
    expect((component as any).error).toBe('ERR');
  });

  it('onSubmit branch: invalid form (early return)', () => {
    create();
    component.form.setValue({
      username: '',
      email: '',
      passwords: { password: '', confirmPassword: '' },
    });
    component.onSubmit();
    expect(security.register).not.toHaveBeenCalled();
    expect(component.submitted).toBeTrue();
  });

  describe('Template and Logic branches', () => {
    beforeEach(() => create());

    it('shows and hides username validation message (@if)', () => {
      expect(fixture.debugElement.query(By.css('p-message[severity="error"]'))).toBeNull();
      component.form.get('username')?.markAsTouched();
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('p-message[severity="error"]'))).not.toBeNull();
    });

    it('covers email validation @if and @else if branches', () => {
      const email = component.form.get('email')!;
      email.markAsTouched();

      email.setValue('');
      fixture.detectChanges();
      expect(fixture.debugElement.nativeElement.textContent).toContain('Email is required');

      email.setValue('invalid');
      fixture.detectChanges();
      expect(fixture.debugElement.nativeElement.textContent).toContain('Not a valid email');
    });

    it('covers password validation @if and @else if branches', () => {
      const pwd = component.form.get('passwords.password')!;
      pwd.markAsTouched();

      pwd.setValue('');
      fixture.detectChanges();
      expect(fixture.debugElement.nativeElement.textContent).toContain('Password is required');

      pwd.setValue('short');
      fixture.detectChanges();
      expect(fixture.debugElement.nativeElement.textContent).toContain('at least 8');
    });

    it('isMismatched coverage (exhaustive logic permutations)', () => {
      const pwd = component.form.get('passwords.password')!;
      const confirm = component.form.get('passwords.confirmPassword')!;

      pwd.setValue('p1');
      confirm.setValue('p1');
      expect(component.isMismatched()).toBeFalse();

      confirm.setValue('p2');
      expect(component.isMismatched()).toBeFalse();

      confirm.markAsTouched();
      expect(component.isMismatched()).toBeTrue();

      create();
      component.form.get('passwords.password')?.setValue('p1');
      component.form.get('passwords.confirmPassword')?.setValue('p2');
      component.submitted = true;
      expect(component.isMismatched()).toBeTrue();
    });
  });

  it('continueWithGoogle sets cookie and redirects', () => {
    create();
    const fakeDoc: any = {
      cookie: '',
      defaultView: { location: { assign: jasmine.createSpy('assign') } },
    };
    (component as any).doc = fakeDoc;
    component.continueWithGoogle();
    expect(fakeDoc.cookie).toContain('deviceId=');
    expect(fakeDoc.cookie).toContain('path=/');
    expect(fakeDoc.defaultView.location.assign).toHaveBeenCalledWith(
      '/api/oauth2/authorization/google'
    );
  });
});
