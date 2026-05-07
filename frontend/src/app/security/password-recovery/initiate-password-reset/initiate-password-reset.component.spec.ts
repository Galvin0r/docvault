import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { InitiatePasswordResetComponent } from './initiate-password-reset.component';
import { SecurityService } from '../../security.service';

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
class PInputGroupStub {}

@Component({
  selector: 'p-inputgroup-addon',
  standalone: true,
  template: `
    <ng-content></ng-content>
  `,
})
class PInputGroupAddonStub {}

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

class MessageServiceStub {
  add = jasmine.createSpy('add');
}

describe('InitiatePasswordResetComponent', () => {
  let fixture: ComponentFixture<InitiatePasswordResetComponent>;
  let component: InitiatePasswordResetComponent;
  let security: jasmine.SpyObj<SecurityService>;
  let router: Router;
  let messages: MessageServiceStub;

  function create() {
    security = jasmine.createSpyObj<SecurityService>('SecurityService', ['resetPassword']);

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
      ],
      declarations: [InitiatePasswordResetComponent],
      providers: [
        FormBuilder,
        { provide: SecurityService, useValue: security },
        { provide: MessageService, useClass: MessageServiceStub },
        provideRouter([]),
      ],
    });

    fixture = TestBed.createComponent(InitiatePasswordResetComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    messages = TestBed.inject(MessageService) as any;
    fixture.detectChanges();
  }

  afterEach(() => fixture.destroy());

  it('shows required and email format validation messages', () => {
    create();
    const inputEl = fixture.debugElement.query(By.css('input[formControlName="email"]'));
    component.form.get('email')?.markAsTouched();
    fixture.detectChanges();
    let msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Email is required/i.test(t))).toBeTrue();

    component.form.get('email')?.setValue('not-an-email');
    fixture.detectChanges();
    msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Not a valid email/i.test(t))).toBeTrue();

    component.form.get('email')?.setValue('user@example.com');
    fixture.detectChanges();
    const cls = (inputEl.nativeElement as HTMLInputElement).classList;
    expect(cls).toContain('p-valid');
    expect(fixture.debugElement.query(By.directive(PMessageStub))).toBeNull();
    expect(inputEl.attributes['aria-invalid']).toBe('false');
  });

  it('does not submit when form is invalid', () => {
    create();
    component.form.get('email')?.setValue('');
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    expect(security.resetPassword).not.toHaveBeenCalled();
  });

  it('submits successfully, shows toast and navigates', () => {
    create();
    const navigateSpy = spyOn(router, 'navigate').and.stub();
    security.resetPassword.and.returnValue(of(void 0));
    component.form.get('email')?.setValue('john@site.com');
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    expect(security.resetPassword).toHaveBeenCalledWith('john@site.com');
    expect(messages.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('sets error on failure and passes it to form error component', () => {
    create();
    security.resetPassword.and.returnValue(
      throwError(() => ({ appCode: 'USER_NOT_FOUND' } as any))
    );
    component.form.get('email')?.setValue('ghost@site.com');
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    fixture.detectChanges();
    const errComp = fixture.debugElement.query(By.directive(FormErrorStub))
      .componentInstance as FormErrorStub;
    expect(errComp.error).toBe('USER_NOT_FOUND');
  });

  it('applies p-invalid and aria-invalid when invalid', () => {
    create();
    const inputDe = fixture.debugElement.query(By.css('input[formControlName="email"]'));
    component.form.get('email')?.markAsTouched();
    fixture.detectChanges();
    expect(inputDe.nativeElement.classList).toContain('p-invalid');
    expect(inputDe.attributes['aria-invalid']).toBe('true');
  });
});