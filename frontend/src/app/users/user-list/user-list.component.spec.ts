import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { of } from 'rxjs';
import { UserListComponent } from './user-list.component';
import { GroupService } from '../../groups/groups.service';
import { GroupMembership, GroupRole, Visibility } from '../../groups/groups.model';
import { Page } from '../../app.model';

type QueryParams = { page: number; size: number } & Record<string, any>;

describe('UserListComponent', () => {
  let fixture: ComponentFixture<UserListComponent>;
  let component: UserListComponent;
  let groupService: jasmine.SpyObj<GroupService>;
  const fb = new FormBuilder();

  const makePage = (
    content: GroupMembership[],
    total = content.length,
    size = 10,
    number = 0
  ): Page<GroupMembership> => ({
    content,
    totalElements: total,
    size,
    number,
  });

  const members: GroupMembership[] = [
    {
      id: 1,
      groupId: 5,
      groupName: 'G',
      userId: 11,
      userLogin: 'alice',
      role: 'USER' as GroupRole,
      created: '',
      groupVisibility: 'PUBLIC' as Visibility
    },
    { id: 2, groupId: 5, groupName: 'G', userId: 12, userLogin: 'bob', role: 'ADMIN' as GroupRole, created: '', groupVisibility: 'PUBLIC'},
  ];

  beforeEach(() => {
    groupService = jasmine.createSpyObj<GroupService>('GroupService', [
      'getMembers',
      'removeUser',
      'changeRole',
    ]);
    groupService.getMembers.and.returnValue(of(makePage(members)));

    TestBed.configureTestingModule({
      declarations: [UserListComponent],
      providers: [{ provide: GroupService, useValue: groupService }],
      schemas: [NO_ERRORS_SCHEMA],
    });

    fixture = TestBed.createComponent(UserListComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('searchForm', fb.group({ q: '' }));
    fixture.componentRef.setInput('groupId', 5);
    fixture.componentRef.setInput('limitOptions', [10, 20, 50]);
    fixture.componentRef.setInput('canManageRole', true);
    fixture.componentRef.setInput('canRemove', true);
  });

  it('initializes and fetches first page', () => {
    fixture.detectChanges();

    expect(groupService.getMembers).toHaveBeenCalledTimes(1);
    const [gid, params] = groupService.getMembers.calls.mostRecent().args as [number, QueryParams];
    expect(gid).toBe(5);
    expect(params.page).toBe(0);
    expect(params.size).toBe(10);
  });

  it('page signal change triggers fetch', () => {
    fixture.detectChanges();
    groupService.getMembers.calls.reset();

    component.page.set(2);
    fixture.detectChanges();

    expect(groupService.getMembers).toHaveBeenCalledTimes(1);
    const [, params] = groupService.getMembers.calls.mostRecent().args as [number, QueryParams];
    expect(params.page).toBe(1);
  });

  it('size signal change triggers fetch', () => {
    fixture.detectChanges();
    groupService.getMembers.calls.reset();

    component.size.set(20);
    fixture.detectChanges();

    expect(groupService.getMembers).toHaveBeenCalledTimes(1);
    const [, params] = groupService.getMembers.calls.mostRecent().args as [number, QueryParams];
    expect(params.size).toBe(20);
  });

  it('refresh triggers a new fetch', () => {
    fixture.detectChanges();
    groupService.getMembers.calls.reset();

    component.searchStore.refresh();
    fixture.detectChanges();

    expect(groupService.getMembers).toHaveBeenCalledTimes(1);
  });

  it('onRoleChange refreshes and emits userRoleChanged', () => {
    fixture.detectChanges();
    groupService.getMembers.calls.reset();

    const emitted = jasmine.createSpy('emitted');
    component.userRoleChanged.subscribe(emitted);

    component.onRoleChange();
    fixture.detectChanges();

    expect(groupService.getMembers).toHaveBeenCalledTimes(1);
    expect(emitted).toHaveBeenCalled();
  });

  it('refresh() calls searchStore.refresh()', () => {
    fixture.detectChanges();
    spyOn(component.searchStore, 'refresh');

    component.refresh();

    expect(component.searchStore.refresh).toHaveBeenCalled();
  });

  it('onPageChange sets page and prevents re-set on same value', () => {
    fixture.detectChanges();
    groupService.getMembers.calls.reset();

    component.onPageChange(2);
    fixture.detectChanges();

    expect(component.page()).toBe(2);
    expect(groupService.getMembers).toHaveBeenCalledTimes(1);

    component.onPageChange(2);
    fixture.detectChanges();

    expect(groupService.getMembers).toHaveBeenCalledTimes(1);
  });

  it('onSizeChange sets size and prevents re-set on same value', () => {
    fixture.detectChanges();
    groupService.getMembers.calls.reset();

    component.onSizeChange(20);
    fixture.detectChanges();

    expect(component.size()).toBe(20);
    expect(groupService.getMembers).toHaveBeenCalledTimes(1);

    component.onSizeChange(20);
    fixture.detectChanges();

    expect(groupService.getMembers).toHaveBeenCalledTimes(1);
  });
});