import { CommonModule } from '@angular/common';
import { Component, Directive, Input, forwardRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
} from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { GroupAddComponent } from './group-add.component';
import { SecurityService } from '../../security/security.service';

@Component({
  selector: 'p-floatlabel',
  standalone: true,
  template: `
    <ng-content></ng-content>
  `,
})
class PFloatLabelStub {
  @Input() variant?: string;
}

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

@Directive({ selector: '[pTextarea]', standalone: true })
class PTextareaStub {}

@Directive({ selector: '[pInputText]', standalone: true })
class PInputTextStub {}

@Directive({ selector: '[pButton]', standalone: true })
class PButtonStub {
  @Input() severity?: string;
  @Input() outlined?: any;
  @Input() type?: string;
  @Input() icon?: string;
}

@Component({
  selector: 'p-selectButton',
  standalone: true,
  imports: [CommonModule],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => PSelectButtonStub), multi: true },
  ],
  template: `
    <div class="selectbutton">
      <button class="opt" *ngFor="let o of options" type="button" (click)="choose(o)">
        {{ optionLabel ? o[optionLabel] : o?.label ?? o }}
      </button>
    </div>
  `,
})
class PSelectButtonStub implements ControlValueAccessor {
  @Input() options: any[] = [];
  @Input() optionLabel?: string;
  @Input() optionValue?: string;
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
  choose(o: any) {
    const val = this.optionValue ? o?.[this.optionValue] : o?.value ?? o;
    this.onChange(val);
    this.onTouched();
  }
}

class RefStub {
  close = jasmine.createSpy('close');
}
class SecurityServiceStub {}

describe('GroupAddComponent', () => {
  let fixture: ComponentFixture<GroupAddComponent>;
  let component: GroupAddComponent;
  let ref: RefStub;

  function create(initial?: any) {
    ref = new RefStub();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        ReactiveFormsModule,
        PFloatLabelStub,
        PMessageStub,
        PTextareaStub,
        PInputTextStub,
        PButtonStub,
        PSelectButtonStub,
      ],
      declarations: [GroupAddComponent],
      providers: [
        FormBuilder,
        { provide: DynamicDialogRef, useValue: ref },
        { provide: DynamicDialogConfig, useValue: { data: initial ? { initial } : {} } },
        { provide: SecurityService, useClass: SecurityServiceStub },
      ],
    });
    fixture = TestBed.createComponent(GroupAddComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    fixture?.destroy();
  });

  it('shows required error for name when touched', () => {
    create();
    component.form.get('name')!.markAsTouched();
    fixture.detectChanges();

    const msgs = fixture.debugElement
      .queryAll(By.directive(PMessageStub))
      .map((de) => (de.nativeElement as HTMLElement).textContent?.trim() || '');
    expect(msgs.some((t) => /Group name is required/i.test(t))).toBeTrue();

    const input = fixture.debugElement.query(By.css('#name')).nativeElement as HTMLInputElement;
    expect(input.classList).toContain('p-invalid');
    expect(input.getAttribute('aria-invalid')).toBe('true');
  });

  it('toggles Create/Save text based on presence of id', () => {
    create();
    const btn = () =>
      fixture.debugElement.queryAll(By.css('button[type="submit"]'))[0]
        .nativeElement as HTMLButtonElement;

    expect(btn().textContent?.trim()).toBe('Create');

    component.form.patchValue({ id: 7 });
    fixture.detectChanges();
    expect(btn().textContent?.trim()).toBe('Save');
  });

  it('binds visibility via select button (form -> view and view -> form)', () => {
    create();
    expect(component.form.get('visibility')!.value).toBe('PUBLIC');

    const opts = fixture.debugElement.queryAll(By.css('.selectbutton .opt'));
    expect(opts.length).toBeGreaterThan(1);

    (opts[1].nativeElement as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(component.form.get('visibility')!.value).not.toBe('PUBLIC');
  });

  it('does not close dialog when form is invalid', () => {
    create();
    component.form.get('name')!.setValue('');
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    expect(ref.close).not.toHaveBeenCalled();
  });

  it('closes dialog with group payload when form is valid (create mode)', () => {
    create();
    component.form.setValue({
      name: 'New Group',
      description: 'Hello',
      visibility: 'PRIVATE',
      id: null,
    });
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', {});
    expect(ref.close).toHaveBeenCalledTimes(1);
    expect(ref.close.calls.mostRecent().args[0]).toEqual({
      name: 'New Group',
      description: 'Hello',
      visibility: 'PRIVATE',
      id: null,
    });
  });

  it('patches initial values on init and shows Save', () => {
    create({ id: 42, name: 'Init', description: 'D', visibility: 'PUBLIC' });
    expect(component.form.value).toEqual({
      id: 42,
      name: 'Init',
      description: 'D',
      visibility: 'PUBLIC',
    });
    const submitBtn = fixture.debugElement.queryAll(By.css('button[type="submit"]'))[0]
      .nativeElement as HTMLButtonElement;
    expect(submitBtn.textContent?.trim()).toBe('Save');
  });

  it('Cancel button closes dialog without payload', () => {
    create();
    const cancel = fixture.debugElement
      .queryAll(By.css('button[type="button"]'))
      .map((de) => de.nativeElement as HTMLButtonElement)
      .find((el) => el.textContent?.trim() === 'Cancel')!;
    cancel.click();
    fixture.detectChanges();
    expect(ref.close).toHaveBeenCalledWith();
  });
});
