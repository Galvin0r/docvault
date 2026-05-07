import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { GoogleButtonComponent } from './google-button.component';

describe('GoogleButtonComponent', () => {
  let fixture: ComponentFixture<GoogleButtonComponent>;
  let component: GoogleButtonComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [GoogleButtonComponent],
      schemas: [NO_ERRORS_SCHEMA],
    });
    fixture = TestBed.createComponent(GoogleButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders the button', () => {
    const btn = fixture.debugElement.query(By.css('button.google-btn'));
    expect(btn).toBeTruthy();
  });

  it('emits "continue" when clicked', () => {
    const spy = jasmine.createSpy('continue');
    component.continue.subscribe(spy);

    const btn = fixture.debugElement.query(By.css('button.google-btn'))
      .nativeElement as HTMLButtonElement;
    btn.click();
    fixture.detectChanges();

    expect(spy).toHaveBeenCalledTimes(1);
  });
});