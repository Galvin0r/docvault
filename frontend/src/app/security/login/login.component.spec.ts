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
import { Router, RouterLink, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { LoginComponent } from './login.component';
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
    <input class="p-password" [class]="inputStyleClass" (input)="handle($event)" />
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
  handle(e: Event) {
    this.onChange((e.target as HTMLInputElement).value);
    this.onTouched();
  }
}

@Component({
  selector: 'p-check-box',
  standalone: true,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => PCheckBoxStub), multi: true },
  ],
  template: `
    <input type="checkbox" [checked]="model" (change)="toggle($event)" />
  `,
})
class PCheckBoxStub implements ControlValueAccessor {
  @Input() binary?: boolean;
  @Input() size?: string;
  model = false;
  private onChange = (_: any) => { };
  private onTouched = () => { };
  writeValue(val: any): void {
    this.model = !!val;
  }
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
  setDisabledState(_: boolean): void { }
  toggle(e: Event) {
    this.model = (e.target as HTMLInputElement).checked;
    this.onChange(this.model);
    this.onTouched();
  }
}

class MessageServiceStub {
  add = jasmine.createSpy('add');
}

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let security: jasmine.SpyObj<SecurityService>;
  let router: Router;

  function create() {
    security = jasmine.createSpyObj<SecurityService>('SecurityService', ['login']);

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink,
        AuthCardStub,
        FormErrorStub,
        PInputGroupStub,
        PInputGroupAddonStub,
        PMessageStub,
        PPasswordStub,
        PCheckBoxStub,
      ],
      declarations: [LoginComponent],
      providers: [
        FormBuilder,
        { provide: MessageService, useClass: MessageServiceStub },
        { provide: SecurityService, useValue: security },
        provideRouter([]),
      ],
    });

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  afterEach(() => fixture.destroy());

  it('shows field errors when controls are touched and invalid', () => {
    create();
    component.form.get('identifier')?.markAsTouched();
    component.form.get('password')?.markAsTouched();
    fixture.detectChanges();
    const msgs = fixture.debugElement.queryAll(By.directive(PMessageStub));
    expect(msgs.length).toBe(2);
  });

  it('applies invalid/valid classes and aria-invalid on identifier input', () => {
    create();
    const inputDe = fixture.debugElement.query(By.css('input[formControlName="identifier"]'));
    component.form.get('identifier')?.markAsTouched();
    fixture.detectChanges();
    expect(inputDe.nativeElement.classList).toContain('p-invalid');
    expect(inputDe.attributes['aria-invalid']).toBe('true');
    component.form.get('identifier')?.setValue('john');
    fixture.detectChanges();
    expect(inputDe.nativeElement.classList).toContain('p-valid');
  });

  it('submits with valid form, calls service and navigates home', () => {
    create();
    const navSpy = spyOn(router, 'navigate').and.stub();
    security.login.and.returnValue(of(void 0));
    component.form.setValue({ identifier: 'john', password: 'secret', rememberMe: true });
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    expect(security.login).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/home']);
    const req = security.login.calls.mostRecent().args[0];
    expect(req.login).toBe('john');
    expect(req.email).toBe('john');
    expect(req.password).toBe('secret');
    expect(req.rememberMe).toBeTrue();
    expect(typeof req.deviceInfo).toBe('string');
    expect((req.deviceInfo as string).length).toBeGreaterThan(0);
  });

  it('onSubmit branch: successful login (next path)', () => {
    create();
    const navSpy = spyOn(router, 'navigate').and.stub();
    security.login.and.returnValue(of(void 0));
    component.form.setValue({ identifier: 'john', password: 'secret', rememberMe: true });
    component.onSubmit();
    expect(navSpy).toHaveBeenCalledWith(['/home']);
  });

  it('onSubmit branch: login failure (error path)', () => {
    create();
    security.login.and.returnValue(throwError(() => ({ error: { code: 'INVALID' } })));
    component.form.setValue({ identifier: 'john', password: 'bad', rememberMe: false });
    component.onSubmit();
    expect((component as any).error).toBe('INVALID');
  });

  it('onSubmit branch: invalid form (early return)', () => {
    create();
    component.form.setValue({ identifier: '', password: '', rememberMe: false });
    component.onSubmit();
    expect(security.login).not.toHaveBeenCalled();
    expect(component.submitted).toBeTrue();
  });

  it('toggles rememberMe via checkbox CVA', () => {
    create();
    const box = fixture.debugElement.query(By.directive(PCheckBoxStub));
    const input = box.query(By.css('input'));
    expect(component.form.get('rememberMe')?.value).toBeFalse();
    input.nativeElement.click();
    fixture.detectChanges();
    expect(component.form.get('rememberMe')?.value).toBeTrue();
  });

  it('continues with Google by setting cookie and redirecting', () => {
    create();

    let lastCookie = '';
    const assign = jasmine.createSpy('assign');
    const fakeDoc = {
      get cookie() {
        return '';
      },
      set cookie(value: string) {
        lastCookie = value;
      },
      location: { assign },
    } as any;

    (component as any).doc = fakeDoc;

    const card = fixture.debugElement.query(By.directive(AuthCardStub))
      .componentInstance as AuthCardStub;
    card.googleContinue.emit();

    expect(lastCookie.startsWith('deviceId=')).toBeTrue();
    expect(lastCookie.endsWith('; path=/')).toBeTrue();
    expect(assign).toHaveBeenCalledWith('/api/oauth2/authorization/google');
  });

  describe('Template branches', () => {
    beforeEach(() => create());

    it('shows and hides identifier validation message (@if)', () => {
      expect(fixture.debugElement.query(By.directive(PMessageStub))).toBeNull();

      component.form.get('identifier')?.markAsTouched();
      component.form.get('identifier')?.setValue('');
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.directive(PMessageStub))).not.toBeNull();

      component.form.get('identifier')?.setValue('user');
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.directive(PMessageStub))).toBeNull();
    });

    it('covers password inputStyleClass ternary branches', () => {
      const pwdDe = fixture.debugElement.query(By.directive(PPasswordStub));
      const getStyle = () => (pwdDe.componentInstance as PPasswordStub).inputStyleClass;

      expect(getStyle()).toBe('');

      component.form.get('password')?.markAsTouched();
      component.form.get('password')?.setValue('');
      fixture.detectChanges();
      expect(getStyle()).toBe('p-invalid');

      component.form.get('password')?.setValue('pass1234');
      fixture.detectChanges();
      expect(getStyle()).toBe('p-valid');
    });

    it('covers identifier input ngClass/aria-invalid branches', () => {
      const inputDe = fixture.debugElement.query(By.css('input[formControlName="identifier"]'));

      component.form.get('identifier')?.markAsTouched();
      fixture.detectChanges();
      expect(inputDe.nativeElement.classList).toContain('p-invalid');

      component.form.get('identifier')?.setValue('val');
      fixture.detectChanges();
      expect(inputDe.nativeElement.classList).toContain('p-valid');
    });
  });
});
