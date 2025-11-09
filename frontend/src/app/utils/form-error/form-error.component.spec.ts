import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { By } from '@angular/platform-browser';
import { FormErrorComponent } from './form-error.component';

@Pipe({ name: 'code', standalone: true })
class CodePipeStub implements PipeTransform {
  transform(value: any): any {
    return String(value);
  }
}

describe('FormErrorComponent', () => {
  let fixture: ComponentFixture<FormErrorComponent>;
  let component: FormErrorComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [FormErrorComponent],
      imports: [CodePipeStub],
      schemas: [NO_ERRORS_SCHEMA],
    });

    fixture = TestBed.createComponent(FormErrorComponent);
    component = fixture.componentInstance;
  });

  it('does not render message when error is null/undefined', () => {
    fixture.componentRef.setInput('error', null);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('p-message'))).toBeNull();

    fixture.componentRef.setInput('error', undefined);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('p-message'))).toBeNull();
  });

  it('renders message when error is set', () => {
    fixture.componentRef.setInput('error', 'ANY_ERROR' as any);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('p-message'))).toBeTruthy();
  });

  it('emits onClose when the message is closed', () => {
    const closeSpy = jasmine.createSpy('close');
    component.onClose.subscribe(closeSpy);

    fixture.componentRef.setInput('error', 'ANY_ERROR' as any);
    fixture.detectChanges();

    const msgEl = fixture.debugElement.query(By.css('p-message')).nativeElement as HTMLElement;
    msgEl.dispatchEvent(new Event('onClose'));
    fixture.detectChanges();

    expect(closeSpy).toHaveBeenCalled();
  });
});
