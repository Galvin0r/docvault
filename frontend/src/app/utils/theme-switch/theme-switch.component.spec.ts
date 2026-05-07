import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { OverlayModule } from '@angular/cdk/overlay';
import { ThemeSwitchComponent } from './theme-switch.component';
import { ThemeService } from '../theme.service';

describe('ThemeSwitchComponent', () => {
  let fixture: ComponentFixture<ThemeSwitchComponent>;
  let component: ThemeSwitchComponent;
  let theme: jasmine.SpyObj<ThemeService>;

  beforeEach(() => {
    theme = jasmine.createSpyObj<ThemeService>('ThemeService', ['getMode', 'setMode']);
    theme.getMode.and.returnValue('system');

    TestBed.configureTestingModule({
      imports: [OverlayModule],
      declarations: [ThemeSwitchComponent],
      providers: [{ provide: ThemeService, useValue: theme }],
      schemas: [NO_ERRORS_SCHEMA],
    });

    fixture = TestBed.createComponent(ThemeSwitchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('initializes mode from ThemeService', () => {
    expect(theme.getMode).toHaveBeenCalled();
    expect(component.mode).toBe('system');
    const btn = fixture.debugElement.query(By.css('button.theme-trigger'))
      .nativeElement as HTMLButtonElement;
    expect(btn.getAttribute('aria-label')).toBe('Theme: system');
  });

  it('toggle/close control "open"', () => {
    expect(component.open).toBeFalse();
    component.toggle();
    expect(component.open).toBeTrue();
    component.toggle();
    expect(component.open).toBeFalse();
    component.open = true;
    component.close();
    expect(component.open).toBeFalse();
  });

  it('triggerIcon reflects current mode', () => {
    component.mode = 'dark';
    expect(component.triggerIcon).toBe('pi pi-moon');
    component.mode = 'light';
    expect(component.triggerIcon).toBe('pi pi-sun');
    component.mode = 'system';
    expect(component.triggerIcon).toBe('pi pi-desktop');
  });

  it('triggerAria reflects current mode', () => {
    component.mode = 'dark';
    expect(component.triggerAria).toBe('Theme: dark');
    component.mode = 'light';
    expect(component.triggerAria).toBe('Theme: light');
    component.mode = 'system';
    expect(component.triggerAria).toBe('Theme: system');
  });

  it('choose sets mode, persists via ThemeService, and closes', () => {
    component.open = true;
    component.choose('light');
    expect(component.mode).toBe('light');
    expect(theme.setMode).toHaveBeenCalledWith('light');
    expect(component.open).toBeFalse();
  });

  it('clicking the trigger button toggles the panel', () => {
    const btn = fixture.debugElement.query(By.css('button.theme-trigger'))
      .nativeElement as HTMLButtonElement;
    btn.click();
    fixture.detectChanges();
    expect(component.open).toBeTrue();
    btn.click();
    fixture.detectChanges();
    expect(component.open).toBeFalse();
  });
});