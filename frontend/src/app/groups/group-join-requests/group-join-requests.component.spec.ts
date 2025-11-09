import { CommonModule } from '@angular/common';
import { Component, ContentChild, EventEmitter, Input, Output, TemplateRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { GroupJoinRequestsComponent } from './group-join-requests.component';
import { GroupService } from '../groups.service';

@Component({
  selector: 'p-card',
  standalone: true,
  template: `
    <ng-content></ng-content>
  `,
})
class PCardStub {}

@Component({
  selector: 'p-badge',
  standalone: true,
  template: `
    <span class="badge">{{ value }}</span>
  `,
})
class PBadgeStub {
  @Input() value: any;
  @Input() severity?: string;
}

@Component({
  selector: 'p-dataview',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ng-container *ngTemplateOutlet="listTpl; context: { $implicit: value }"></ng-container>
  `,
})
class PDataViewStub {
  @Input() value: any[] = [];
  @ContentChild('list', { read: TemplateRef }) listTpl!: TemplateRef<any>;
}

@Component({
  selector: 'app-group-join-request',
  standalone: true,
  template: `
    <div class="row">{{ request?.userLogin }}</div>
    <button class="accept" (click)="accept.emit(request)"></button>
    <button class="reject" (click)="reject.emit(request)"></button>
  `,
})
class GroupJoinRequestStub {
  @Input() request: any;
  @Output() accept = new EventEmitter<any>();
  @Output() reject = new EventEmitter<any>();
}

@Component({
  selector: 'app-paginator',
  standalone: true,
  template: `
    <button class="next" (click)="pageChange.emit((page ?? 1) + 1)">Next</button>
  `,
})
class PaginatorStub {
  @Input() total?: number;
  @Input() page?: number;
  @Input() size?: number;
  @Input() isSimplePaginator?: boolean;
  @Output() pageChange = new EventEmitter<number>();
}

class GroupServiceStub {
  getRequests = jasmine
    .createSpy('getRequests')
    .and.returnValue(of({ content: [], totalElements: 0 }));
  acceptRequest = jasmine.createSpy('acceptRequest').and.returnValue(of(void 0));
  rejectRequest = jasmine.createSpy('rejectRequest').and.returnValue(of(void 0));
}

describe('GroupJoinRequestsComponent (real template)', () => {
  let fixture: ComponentFixture<GroupJoinRequestsComponent>;
  let component: GroupJoinRequestsComponent;
  let service: GroupServiceStub;

  function createWith(items: { content: any[]; totalElements: number }) {
    service = new GroupServiceStub();
    service.getRequests.and.returnValue(of(items));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        ReactiveFormsModule,
        PCardStub,
        PBadgeStub,
        PDataViewStub,
        GroupJoinRequestStub,
        PaginatorStub,
      ],
      declarations: [GroupJoinRequestsComponent],
      providers: [FormBuilder, { provide: GroupService, useValue: service }],
    });

    fixture = TestBed.createComponent(GroupJoinRequestsComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('groupId', 123);
    fixture.detectChanges();

    const store = { items: () => items, refresh: jasmine.createSpy('refresh') } as any;
    (component as any).searchStore = store;
    fixture.detectChanges();
    return store;
  }

  it('renders list items and badge with total count', () => {
    const content = [
      { id: 1, userLogin: 'alice', created: new Date() },
      { id: 2, userLogin: 'bob', created: new Date() },
    ];
    createWith({ content, totalElements: content.length });

    const rows = fixture.debugElement.queryAll(By.directive(GroupJoinRequestStub));
    expect(rows.length).toBe(2);

    const badge = fixture.debugElement.query(By.css('.badge')).nativeElement as HTMLElement;
    expect((badge.textContent || '').trim()).toBe('2');
  });

  it('accepts a request: calls service, emits changed, and refreshes store', () => {
    const content = [{ id: 10, userLogin: 'x', created: new Date() }];
    const store = createWith({ content, totalElements: 1 });
    const changedSpy = spyOn(component.changed, 'emit');

    const row = fixture.debugElement.query(By.directive(GroupJoinRequestStub))
      .componentInstance as GroupJoinRequestStub;
    row.accept.emit(content[0]);

    expect(service.acceptRequest).toHaveBeenCalledWith(10);
    expect(changedSpy).toHaveBeenCalled();
    expect(store.refresh).toHaveBeenCalled();
  });

  it('rejects a request: calls service, emits changed, and refreshes store', () => {
    const content = [{ id: 11, userLogin: 'y', created: new Date() }];
    const store = createWith({ content, totalElements: 1 });
    const changedSpy = spyOn(component.changed, 'emit');

    const row = fixture.debugElement.query(By.directive(GroupJoinRequestStub))
      .componentInstance as GroupJoinRequestStub;
    row.reject.emit(content[0]);

    expect(service.rejectRequest).toHaveBeenCalledWith(11);
    expect(changedSpy).toHaveBeenCalled();
    expect(store.refresh).toHaveBeenCalled();
  });

  it('updates page signal when paginator emits pageChange', () => {
    createWith({ content: [], totalElements: 0 });

    const paginator = fixture.debugElement.query(By.directive(PaginatorStub))
      .componentInstance as PaginatorStub;
    paginator.pageChange.emit(4);
    expect(component.page()).toBe(4);

    component.onPageChange(5);
    expect(component.page()).toBe(5);

    component.onPageChange(5);
    expect(component.page()).toBe(5);
  });
});
