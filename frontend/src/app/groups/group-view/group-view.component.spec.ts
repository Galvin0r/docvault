import { CommonModule } from '@angular/common';
import { Component, EventEmitter, forwardRef, Input, Output, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, flush } from '@angular/core/testing';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService } from 'primeng/api';
import { GroupViewComponent } from './group-view.component';
import { GroupService } from '../groups.service';
import { GroupJoinRequest, GroupMembership } from '../groups.model';

@Component({ selector: 'app-user-list', standalone: true, template: '' })
class UserListStub {
  @Input() groupId!: number;
  @Input() searchForm: any;
  @Input() canManageRole?: boolean;
  @Input() canRemove?: boolean;
  @Output() userRoleChanged = new EventEmitter<void>();
  searchStore: any = { items: () => ({ content: [], totalElements: 0 }) };
  refresh = jasmine.createSpy('refresh');
}

@Component({ selector: 'app-group-join-requests', standalone: true, template: '' })
class GroupJoinRequestsStub {
  @Input() groupId!: number;
  @Output() changed = new EventEmitter<void>();
}

@Component({
  selector: 'p-datepicker',
  standalone: false,
  template: '',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatePickerStubComponent),
      multi: true,
    },
  ],
})
class DatePickerStubComponent implements ControlValueAccessor {
  writeValue(): void {}
  registerOnChange(): void {}
  registerOnTouched(): void {}
  setDisabledState(): void {}
}

class RouterStub {
  navigate = jasmine.createSpy('navigate');
}

const mockJoinRequest: GroupJoinRequest = {
  id: 7,
  userLogin: 'testuser',
  status: 'PENDING',
  created: new Date().toISOString(),
};

class GroupServiceStub {
  addMember = jasmine.createSpy('addMember').and.returnValue(of(void 0));
  edit = jasmine.createSpy('edit').and.returnValue(of(void 0));
  delete = jasmine.createSpy('delete').and.returnValue(of(void 0));
  leave = jasmine.createSpy('leave').and.returnValue(of(void 0));
  join = jasmine.createSpy('join').and.returnValue(of(null));
  getJoinRequest = jasmine.createSpy('getJoinRequest').and.returnValue(of(mockJoinRequest));
  getMembership = jasmine
    .createSpy('getMembership')
    .and.returnValue(of({ role: 'ADMIN' } as GroupMembership));
  get = jasmine.createSpy('get').and.returnValue(
    of({
      id: 1,
      name: 'Updated',
      description: '',
      visibility: 'PUBLIC',
      membersNumber: 5,
      created: new Date(),
      requestsNumber: 0,
      allowedToAccess: true,
    })
  );
}

class FakeRef {
  onClose = new Subject<any>();
  onMaximize = new Subject<any>();
  onMinimize = new Subject<any>();
  onDestroy = new Subject<any>();
  close = jasmine.createSpy('close').and.callFake((v?: any) => {
    this.onClose.next(v);
    this.onClose.complete();
  });
  destroy = jasmine.createSpy('destroy').and.callFake(() => {
    this.onDestroy.complete();
  });
  minimize = jasmine.createSpy('minimize');
  maximize = jasmine.createSpy('maximize');
}

class DialogServiceStub {
  last?: FakeRef;
  lastComponent?: unknown;
  lastConfig?: unknown;
  open(component?: unknown, config?: unknown): DynamicDialogRef {
    this.lastComponent = component;
    this.lastConfig = config;
    this.last = new FakeRef();
    return this.last as unknown as DynamicDialogRef;
  }
}

class ConfirmationServiceStub {
  last: any;
  confirm(cfg: any) {
    this.last = cfg;
  }
}

describe('GroupViewComponent (real template)', () => {
  let fixture: ComponentFixture<GroupViewComponent>;
  let component: GroupViewComponent;
  let router: RouterStub;
  let svc: GroupServiceStub;
  let dialog: DialogServiceStub;
  let confirm: ConfirmationServiceStub;

  function create(initial?: { group?: any; membership?: any; joinRequest?: any }) {
    const group = initial?.group ?? {
      id: 1,
      name: 'Test Group',
      description: 'D',
      visibility: 'PUBLIC',
      membersNumber: 0,
      created: new Date(),
      requestsNumber: 0,
      allowedToAccess: true,
    };
    const membership = initial?.membership;
    const joinRequest = initial?.joinRequest ?? null;

    router = new RouterStub();
    svc = new GroupServiceStub();
    dialog = new DialogServiceStub();
    confirm = new ConfirmationServiceStub();

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CommonModule, ReactiveFormsModule, UserListStub, GroupJoinRequestsStub],
      declarations: [GroupViewComponent, DatePickerStubComponent],
      providers: [
        FormBuilder,
        { provide: Router, useValue: router },
        { provide: DialogService, useValue: dialog },
        { provide: ConfirmationService, useValue: confirm },
        { provide: ActivatedRoute, useValue: { data: of({ group, membership, joinRequest }) } },
        { provide: GroupService, useValue: svc },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });

    fixture = TestBed.createComponent(GroupViewComponent);
    component = fixture.componentInstance;

    if (initial?.membership === undefined && initial?.group !== undefined) {
      component.membership.set({ role: 'OWNER' } as GroupMembership);
    }

    const fakeChild = new UserListStub();
    fakeChild.searchStore = { items: () => ({ content: [], totalElements: 5 }) };
    spyOn(component as any, 'list').and.returnValue(fakeChild);

    fixture.detectChanges();
    return fakeChild;
  }

  it('renders group name and updates membersNumber from user list store effect', fakeAsync(() => {
    create();
    flush();
    fixture.detectChanges();
    const title = (fixture.nativeElement.querySelector('h1') as HTMLElement)?.textContent || '';
    expect(title).toContain('Test Group');
    expect(component.group().membersNumber).toBe(5);
  }));

  it('onAddMember opens dialog, creates member, refreshes list', () => {
    const child = create();
    component.onAddMember();
    dialog.last!.close({ email: 'user@x.y' });
    expect(svc.addMember).toHaveBeenCalledWith(1, 'user@x.y');
    expect(child.refresh).toHaveBeenCalled();
  });

  it('onAddDocument opens dialog with a refresh callback for the document list', () => {
    create({ membership: { role: 'USER', userLogin: 'roman' } });
    const documentList = { refresh: jasmine.createSpy('refresh') };
    spyOn(component as any, 'documentList').and.returnValue(documentList);

    component.onAddDocument();

    expect(dialog.last).toBeTruthy();
    const config = dialog.lastConfig as any;
    expect(config.data.groupId).toBe(1);
    expect(config.data.groupName).toBe('Test Group');

    config.data.onDocumentLinked();

    expect(documentList.refresh).toHaveBeenCalled();
  });

  it('onRequestChanged reloads group and refreshes list', () => {
    const child = create();
    component.onRequestChanged();
    expect(svc.get).toHaveBeenCalledWith(1);
    expect(child.refresh).toHaveBeenCalled();
    expect(component.group().name).toBe('Updated');
  });

  it('onLeave PUBLIC refreshes list and clears membership', () => {
    const child = create({
      group: {
        id: 1,
        name: 'G',
        description: '',
        visibility: 'PUBLIC',
        membersNumber: 0,
        created: new Date(),
        requestsNumber: 0,
        allowedToAccess: true,
      },
      membership: { role: 'USER' },
    });
    component.onLeave();
    confirm.last.accept();
    expect(svc.leave).toHaveBeenCalledWith(1);
    expect(child.refresh).toHaveBeenCalled();
    expect(component.membership()).toBeUndefined();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('effect should return early if list component is not ready', fakeAsync(() => {
    const group = {
      id: 1,
      name: 'Test Group',
      description: 'D',
      visibility: 'PUBLIC',
      membersNumber: 0,
      created: new Date(),
      requestsNumber: 0,
      allowedToAccess: true,
    };
    router = new RouterStub();
    svc = new GroupServiceStub();
    dialog = new DialogServiceStub();
    confirm = new ConfirmationServiceStub();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CommonModule, ReactiveFormsModule, UserListStub, GroupJoinRequestsStub],
      declarations: [GroupViewComponent, DatePickerStubComponent],
      providers: [
        FormBuilder,
        { provide: Router, useValue: router },
        { provide: DialogService, useValue: dialog },
        { provide: ConfirmationService, useValue: confirm },
        {
          provide: ActivatedRoute,
          useValue: { data: of({ group, membership: { role: 'OWNER' }, joinRequest: null }) },
        },
        { provide: GroupService, useValue: svc },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });
    fixture = TestBed.createComponent(GroupViewComponent);
    component = fixture.componentInstance;

    spyOn(component as any, 'list').and.returnValue(undefined);
    spyOn(component.group, 'update');

    fixture.detectChanges();
    flush();

    expect(component.group.update).not.toHaveBeenCalled();
  }));

  it('onEdit opens dialog, edits group, updates signal', () => {
    create();
    const originalGroup = component.group();
    const updatedGroupData = { ...originalGroup, name: 'New Name' };
    component.onEdit();

    expect(dialog.last).toBeTruthy();
    dialog.last!.close(updatedGroupData);

    expect(svc.edit).toHaveBeenCalledWith(updatedGroupData);
    expect(component.group().name).toBe('New Name');
  });

  it('onDelete opens confirmation, deletes group, navigates', () => {
    create();
    component.onDelete();

    expect(confirm.last).toBeTruthy();
    confirm.last.accept();

    expect(svc.delete).toHaveBeenCalledWith(1);
    expect(router.navigate).toHaveBeenCalledWith(['/groups']);
  });

  it('onLeave PRIVATE navigates away', () => {
    const child = create({
      group: {
        id: 1,
        name: 'G',
        description: '',
        visibility: 'PRIVATE',
        membersNumber: 0,
        created: new Date(),
        requestsNumber: 0,
        allowedToAccess: true,
      },
      membership: { role: 'USER' },
    });

    component.onLeave();
    confirm.last.accept();

    expect(svc.leave).toHaveBeenCalledWith(1);
    expect(router.navigate).toHaveBeenCalledWith(['/groups']);
    expect(child.refresh).not.toHaveBeenCalled();
    expect(component.membership()).toBeDefined();
  });

  it('onJoin sets membership if returned (e.g., public group)', () => {
    const child = create({ membership: undefined });
    const newMembership = { role: 'USER' } as GroupMembership;
    svc.join.and.returnValue(of(newMembership));

    component.onJoin();

    expect(svc.join).toHaveBeenCalledWith(1);
    expect(child.refresh).toHaveBeenCalled();
    expect(component.membership()).toBe(newMembership);
    expect(svc.getJoinRequest).not.toHaveBeenCalled();
  });

  it('onJoin fetches join request if membership is null (e.g., request-only group)', () => {
    const child = create({ membership: undefined });
    component.onJoin();

    expect(svc.join).toHaveBeenCalledWith(1);
    expect(child.refresh).toHaveBeenCalled();
    expect(svc.getJoinRequest).toHaveBeenCalledWith(1);
    expect(component.joinRequest()).toEqual(mockJoinRequest);
    expect(component.membership()).toBeUndefined();
  });

  it('onRoleChange fetches and updates the membership', () => {
    create();
    const updatedMembership = { role: 'ADMIN' } as GroupMembership;

    component.onRoleChange();

    expect(svc.getMembership).toHaveBeenCalledWith(1);
    expect(component.membership()).toEqual(updatedMembership);
  });

  it('toggleRequests should flip requestsOpen signal', () => {
    create();
    expect(component.requestsOpen()).toBeFalse();
    component.toggleRequests();
    expect(component.requestsOpen()).toBeTrue();
    component.toggleRequests();
    expect(component.requestsOpen()).toBeFalse();
  });
});
