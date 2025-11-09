import { CommonModule } from '@angular/common';
import { Component, Directive, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { GroupAddUserComponent } from './group-add-user.component';
import { SecurityService } from '../../security/security.service';

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

@Directive({
  selector: '[pInputText]',
  standalone: true,
})
class PInputTextStub {}

@Directive({
  selector: '[pButton]',
  standalone: true,
})
class PButtonStub {
  @Input() severity?: string;
  @Input() outlined?: any;
  @Input() type?: string;
}

class RefStub {
  close = jasmine.createSpy('close');
}
class SecurityServiceStub {}

describe('GroupAddUserComponent', () => {
  let fixture: ComponentFixture<GroupAddUserComponent> | undefined;
  let component: GroupAddUserComponent;
  let ref: RefStub;

  function create() {
    ref = new RefStub();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        ReactiveFormsModule,
        PInputGroupStub,
        PInputGroupAddonStub,
        PMessageStub,
        PInputTextStub,
        PButtonStub,
      ],
      declarations: [GroupAddUserComponent],
      providers: [
        FormBuilder,
        { provide: DynamicDialogRef, useValue: ref },
        { provide: DynamicDialogConfig, useValue: {} },
        { provide: SecurityService, useClass: SecurityServiceStub },
      ],
    });
    fixture = TestBed.createComponent(GroupAddUserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    if (fixture) {
      fixture.destroy();
      fixture = undefined;
    }
  });

  it('shows required and email format validation messages', () => {
    create();
    const email = component.form.get('email')!;
    email.markAsTouched();
    fixture!.detectChanges();

    let msgs = fixture!.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Email is required/i.test(t))).toBeTrue();

    email.setValue('not-an-email');
    fixture!.detectChanges();
    msgs = fixture!.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((d) => (d.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Not a valid email/i.test(t))).toBeTrue();

    const input = fixture!.debugElement.query(By.css('input[formControlName="email"]'))
      .nativeElement as HTMLInputElement;
    expect(input.classList).toContain('p-invalid');
    expect(input.getAttribute('aria-invalid')).toBe('true');
  });

  it('does not close dialog when form is invalid', () => {
    create();
    component.form.get('email')!.setValue('');
    fixture!.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    expect(ref.close).not.toHaveBeenCalled();
  });

  it('closes dialog with email payload when form is valid', () => {
    create();
    component.form.get('email')!.setValue('user@example.com');
    fixture!.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    expect(ref.close).toHaveBeenCalledTimes(1);
    const arg = ref.close.calls.mostRecent().args[0];
    expect(arg).toEqual({ email: 'user@example.com' });
  });

  it('Cancel button closes dialog without payload', () => {
    create();
    const cancelBtn = fixture!.debugElement
      .queryAll(By.css('button[type="button"]'))
      .map((d) => d.nativeElement as HTMLButtonElement)
      .find((el) => el.textContent?.trim() === 'Cancel')!;
    cancelBtn.click();
    fixture!.detectChanges();
    expect(ref.close).toHaveBeenCalledWith();
  });
});
