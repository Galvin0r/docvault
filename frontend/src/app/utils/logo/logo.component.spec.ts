import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LogoComponent } from './logo.component';

describe('LogoComponent', () => {
  let fixture: ComponentFixture<LogoComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ declarations: [LogoComponent] });
    fixture = TestBed.createComponent(LogoComponent);
  });

  it('sets default size, role, and aria', () => {
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.style.width).toBe('64px');
    expect(host.style.height).toBe('64px');
    expect(host.getAttribute('role')).toBe('img');
    expect(host.getAttribute('aria-label')).toBe('DocVault Logo');
  });

  it('applies size input to width and height', () => {
    fixture.componentRef.setInput('size', 120);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.style.width).toBe('120px');
    expect(host.style.height).toBe('120px');
  });
});
