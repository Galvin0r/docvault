import { CommonModule, formatDate } from '@angular/common';
import { Component, Directive, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { GroupJoinRequestComponent } from './group-join-request.component';

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
  @Input() severity?: string;
  @Input() class?: string;
  @Input() type?: string;
}

describe('GroupJoinRequestComponent', () => {
  let fixture: ComponentFixture<GroupJoinRequestComponent>;
  let component: GroupJoinRequestComponent;

  function create() {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CommonModule, PCardStub, PButtonStub],
      declarations: [GroupJoinRequestComponent],
    });
    fixture = TestBed.createComponent(GroupJoinRequestComponent);
    component = fixture.componentInstance;
  }

  it('renders login, Pending badge, and formatted date', () => {
    create();
    const req = { userLogin: 'alice', created: new Date(2025, 0, 2, 3, 4) } as any;
    fixture.componentRef.setInput('request', req);
    fixture.detectChanges();

    const loginEl = fixture.debugElement.query(By.css('.font-medium')).nativeElement as HTMLElement;
    expect((loginEl.textContent || '').trim()).toBe('alice');

    const badgeEl = fixture.debugElement.query(By.css('.px-2.py-0\\.5'))
      .nativeElement as HTMLElement;
    expect((badgeEl.textContent || '').trim()).toContain('Pending');

    const dateEl = fixture.debugElement.query(By.css('.text-xs span')).nativeElement as HTMLElement;
    const expected = formatDate(req.created, 'yyyy-MM-dd HH:mm', 'en-US');
    expect((dateEl.textContent || '').trim()).toBe(expected);
  });

  it('emits accept with payload', () => {
    create();
    const req = { userLogin: 'bob', created: new Date() } as any;
    fixture.componentRef.setInput('request', req);
    const emitSpy = spyOn(component.accept, 'emit');
    fixture.detectChanges();

    const acceptBtn = fixture.debugElement.queryAll(By.css('button'))[0]
      .nativeElement as HTMLButtonElement;
    acceptBtn.click();

    expect(emitSpy).toHaveBeenCalledOnceWith(req);
  });

  it('emits reject with payload', () => {
    create();
    const req = { userLogin: 'carol', created: new Date() } as any;
    fixture.componentRef.setInput('request', req);
    const emitSpy = spyOn(component.reject, 'emit');
    fixture.detectChanges();

    const rejectBtn = fixture.debugElement.queryAll(By.css('button'))[1]
      .nativeElement as HTMLButtonElement;
    rejectBtn.click();

    expect(emitSpy).toHaveBeenCalledOnceWith(req);
  });
});