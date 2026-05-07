import { CommonModule } from '@angular/common';
import { Component, Directive, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { GroupComponent } from './group.component';

@Component({
  selector: 'p-card',
  standalone: true,
  template: `
    <ng-content></ng-content>
  `,
})
class PCardStub {}

@Directive({
  selector: '[pButton]',
  standalone: true,
})
class PButtonStub {
  @Input() icon?: string;
}

@Directive({
  selector: '[pTooltip]',
  standalone: true,
})
class PTooltipStub {
  @Input('pTooltip') text?: string;
}

@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <div class="logo"></div>
  `,
})
class LogoStub {
  @Input() size?: number | string;
}

class RouterStub {
  navigate = jasmine.createSpy('navigate');
}

type TestGroup = {
  id: number;
  name: string;
  visibility: 'PUBLIC' | 'PRIVATE' | 'REQUEST_ONLY' | string;
  membersNumber: number;
  allowedToAccess: boolean;
};

describe('GroupComponent', () => {
  let fixture: ComponentFixture<GroupComponent>;
  let component: GroupComponent;
  let router: RouterStub;

  function render(group: TestGroup) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CommonModule, PCardStub, PButtonStub, PTooltipStub, LogoStub],
      declarations: [GroupComponent],
      providers: [{ provide: Router, useClass: RouterStub }, provideRouter([])],
    });
    fixture = TestBed.createComponent(GroupComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as any;
    fixture.componentRef.setInput('group', group);
    fixture.detectChanges();
  }

  it('shows name and members chip for non-private groups', () => {
    render({
      id: 1,
      name: 'Alpha Squad',
      visibility: 'PUBLIC',
      membersNumber: 7,
      allowedToAccess: false,
    });
    const nameEl = fixture.debugElement.query(By.css('.font-medium')).nativeElement as HTMLElement;
    expect(nameEl.textContent?.trim()).toBe('Alpha Squad');
    const chip = fixture.debugElement
      .queryAll(By.css('span'))
      .map((d) => d.nativeElement as HTMLElement)
      .find((el) => /members/.test(el.textContent || ''));
    expect(chip?.textContent?.trim()).toBe('7 members');
  });

  it('hides members chip for private groups', () => {
    render({
      id: 2,
      name: 'Stealth Ops',
      visibility: 'PRIVATE',
      membersNumber: 12,
      allowedToAccess: false,
    });
    const chip = fixture.debugElement
      .queryAll(By.css('span'))
      .map((d) => d.nativeElement as HTMLElement)
      .find((el) => /members/.test(el.textContent || ''));
    expect(chip).toBeUndefined();
  });

  it('renders Public status with proper icon', () => {
    render({
      id: 3,
      name: 'Open Team',
      visibility: 'PUBLIC',
      membersNumber: 3,
      allowedToAccess: false,
    });
    const statusText = fixture.debugElement
      .queryAll(By.css('.text-xs'))
      .map((d) => d.nativeElement as HTMLElement)
      .find((el) => /Public|Private|Request only/.test(el.textContent || ''));
    expect(statusText?.textContent).toContain('Public');
    const icon = fixture.debugElement.query(By.css('i.pi.pi-lock-open'));
    expect(icon).toBeTruthy();
  });

  it('renders Private status with proper icon', () => {
    render({
      id: 4,
      name: 'Vault',
      visibility: 'PRIVATE',
      membersNumber: 2,
      allowedToAccess: false,
    });
    const statusText = fixture.debugElement
      .queryAll(By.css('.text-xs'))
      .map((d) => d.nativeElement as HTMLElement)
      .find((el) => /Public|Private|Request only/.test(el.textContent || ''));
    expect(statusText?.textContent).toContain('Private');
    const icon = fixture.debugElement.query(By.css('i.pi.pi-shield'));
    expect(icon).toBeTruthy();
  });

  it('renders Request only status with proper icon', () => {
    render({
      id: 5,
      name: 'Applicants',
      visibility: 'REQUEST_ONLY',
      membersNumber: 10,
      allowedToAccess: false,
    });
    const statusText = fixture.debugElement
      .queryAll(By.css('.text-xs'))
      .map((d) => d.nativeElement as HTMLElement)
      .find((el) => /Public|Private|Request only/.test(el.textContent || ''));
    expect(statusText?.textContent).toContain('Request only');
    const icon = fixture.debugElement.query(By.css('i.pi.pi-user-plus'));
    expect(icon).toBeTruthy();
  });

  it('navigates to edit page on open button click', () => {
    render({
      id: 9,
      name: 'Gamma',
      visibility: 'PUBLIC',
      membersNumber: 8,
      allowedToAccess: true,
    });
    const btn = fixture.debugElement.query(By.css('button'));
    btn.triggerEventHandler('click', {});
    expect(router.navigate).toHaveBeenCalledWith(['/groups/edit', 9]);
  });
});