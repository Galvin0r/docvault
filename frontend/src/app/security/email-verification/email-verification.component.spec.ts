import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, forwardRef } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
} from '@angular/forms';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { EmailVerificationComponent } from './email-verification.component';
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
  selector: 'p-input-otp',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PInputOtpStub),
      multi: true,
    },
  ],
  template: `
    <input (input)="handle($event)" />
  `,
})
class PInputOtpStub implements ControlValueAccessor {
  @Input() integerOnly?: boolean;
  @Input() length?: number;

  private onChange = (_: any) => {};
  private onTouched = () => {};
  writeValue(_: any): void {}
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
  setDisabledState(_: boolean): void {}
  handle(e: Event) {
    this.onChange((e.target as HTMLInputElement).value);
    this.onTouched();
  }
}

class RouterStub {
  navigate = jasmine.createSpy('navigate');
}
class MessageServiceStub {
  add = jasmine.createSpy('add');
}

describe('EmailVerificationComponent', () => {
  let fixture: ComponentFixture<EmailVerificationComponent>;
  let component: EmailVerificationComponent;
  let security: jasmine.SpyObj<SecurityService>;
  let router: RouterStub;
  let messages: MessageServiceStub;

  function create(routeEmail: string | null = 'user@example.com') {
    security = jasmine.createSpyObj<SecurityService>('SecurityService', [
      'activateAccount',
      'resendActivationCode',
    ]);
    security.activateAccount.and.returnValue(of(void 0));
    security.resendActivationCode.and.returnValue(of(void 0));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CommonModule, ReactiveFormsModule, AuthCardStub, FormErrorStub, PInputOtpStub],
      declarations: [EmailVerificationComponent],
      providers: [
        FormBuilder,
        { provide: Router, useClass: RouterStub },
        { provide: MessageService, useClass: MessageServiceStub },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap(routeEmail ? { email: routeEmail } : {}) },
          },
        },
        { provide: SecurityService, useValue: security },
      ],
    });

    fixture = TestBed.createComponent(EmailVerificationComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as any;
    messages = TestBed.inject(MessageService) as any;
    fixture.detectChanges();
  }

  afterEach(() => localStorage.clear());

  it('shows email from query params', () => {
    create('hello@site.com');
    const label = fixture.debugElement.query(By.css('label.font-medium'))
      .nativeElement as HTMLElement;
    expect(label.textContent?.trim()).toBe('hello@site.com');
  });

  it('submits code and navigates on success', () => {
    create();
    component.form.get('code')?.setValue('123456');
    fixture.detectChanges();
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    expect(security.activateAccount).toHaveBeenCalledWith(123456);
    expect(messages.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('shows error on submit failure (propagates to form error input)', () => {
    create();
    security.activateAccount.and.returnValue(throwError(() => ({ error: { code: 'INVALID' } })));
    component.form.get('code')?.setValue('000000');
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    fixture.detectChanges();
    const errComp = fixture.debugElement.query(By.directive(FormErrorStub))
      .componentInstance as FormErrorStub;
    expect(errComp.error).toBe('INVALID');
  });

  it('shows resend link when cooldown is 0 and countdown when cooldown > 0', () => {
    create();
    component.cooldown = 0;
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('a.doc-link'))).toBeTruthy();
    component.cooldown = 5;
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('a.doc-link'))).toBeNull();
    const countdown = fixture.debugElement.query(By.css('span.text-gray-400'))
      .nativeElement as HTMLElement;
    expect(countdown.textContent).toContain('Try again in 5s');
  });

  it('resends code, starts cooldown, and shows success toast', fakeAsync(() => {
    create('again@site.com');
    const link = fixture.debugElement.query(By.css('a.doc-link'));
    link.triggerEventHandler('click', {});
    fixture.detectChanges();
    expect(security.resendActivationCode).toHaveBeenCalledWith('again@site.com');
    expect(messages.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    expect(component.cooldown).toBeGreaterThan(0);
  }));

  it('shows error toast when resend fails', () => {
    create('err@site.com');
    security.resendActivationCode.and.returnValue(throwError(() => new Error('x')));
    fixture.debugElement.query(By.css('a.doc-link')).triggerEventHandler('click', {});
    expect(messages.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  it('does not call resend when email is null', () => {
    create(null);
    fixture.debugElement.query(By.css('a.doc-link')).triggerEventHandler('click', {});
    expect(security.resendActivationCode).not.toHaveBeenCalled();
  });

  it('resumes cooldown from lastResendTimestamp using 60s window', fakeAsync(() => {
    const now = 1_000_000_000_000;
    spyOn(Date, 'now').and.returnValue(now);
    localStorage.setItem('lastResendTimestamp', String(now - 59_000));
    create('cold@site.com');
    expect(component.cooldown).toBe(1);
    tick(1000);
    expect(component.cooldown).toBe(0);
  }));

  it('startCooldown counts down and clears interval', fakeAsync(() => {
    create();
    component.startCooldown(2);
    expect(component.cooldown).toBe(2);
    tick(1000);
    expect(component.cooldown).toBe(1);
    tick(1000);
    expect(component.cooldown).toBe(0);
  }));
});