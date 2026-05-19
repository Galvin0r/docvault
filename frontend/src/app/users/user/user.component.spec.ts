import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';

import { UserComponent } from './user.component';
import { GroupService } from '../../groups/groups.service';
import { Confirmation, ConfirmationService, MenuItem, PrimeIcons } from 'primeng/api';
import { GroupMembership, GroupRole } from '../../groups/groups.model';
import { Router } from '@angular/router';

describe('UserComponent', () => {
  let fixture: ComponentFixture<UserComponent>;
  let component: UserComponent;

  let groupService: jasmine.SpyObj<GroupService>;
  let confirmation: jasmine.SpyObj<ConfirmationService>;
  let router: jasmine.SpyObj<Router>;

  const membershipAdmin: GroupMembership = {
    id: 10,
    groupId: 1,
    groupName: 'G',
    userId: 2,
    userLogin: 'john',
    role: 'ADMIN' as GroupRole,
    created: '',
    groupVisibility: 'PUBLIC',
  };

  const membershipOwner: GroupMembership = {
    id: 11,
    groupId: 7,
    groupName: 'H',
    userId: 3,
    userLogin: 'alice',
    role: 'OWNER' as GroupRole,
    created: '',
    groupVisibility: 'PUBLIC',
  };

  beforeEach(() => {
    groupService = jasmine.createSpyObj<GroupService>('GroupService', ['removeUser', 'changeRole']);
    confirmation = jasmine.createSpyObj<ConfirmationService>('ConfirmationService', ['confirm']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    groupService.removeUser.and.returnValue(of(void 0));
    groupService.changeRole.and.returnValue(of(void 0));

    TestBed.configureTestingModule({
      declarations: [UserComponent],
      providers: [
        { provide: GroupService, useValue: groupService },
        { provide: ConfirmationService, useValue: confirmation },
        { provide: Router, useValue: router },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });

    fixture = TestBed.createComponent(UserComponent);
    component = fixture.componentInstance;
  });

  it('builds no menuItems for OWNER because the row opens the profile', () => {
    fixture.componentRef.setInput('membership', membershipOwner);
    fixture.componentRef.setInput('canManageRole', true);
    fixture.componentRef.setInput('canRemove', true);
    fixture.detectChanges();

    const items = component.menuItems() as MenuItem[];
    expect(items.length).toBe(0);
    expect(items.some((i) => typeof i.command === 'function')).toBeFalse();
  });

  it('builds menuItems for ADMIN: contains role-change command(s) and remove item when allowed', () => {
    fixture.componentRef.setInput('membership', membershipAdmin);
    fixture.componentRef.setInput('canManageRole', true);
    fixture.componentRef.setInput('canRemove', true);
    fixture.detectChanges();

    const items = component.menuItems() as MenuItem[];
    const hasRoleChange = items.some(
      (i) => typeof i.command === 'function' && i.icon !== PrimeIcons.TRASH
    );
    const hasRemove = items.some(
      (i) => i.icon === PrimeIcons.TRASH && typeof i.command === 'function'
    );
    expect(hasRoleChange).toBeTrue();
    expect(hasRemove).toBeTrue();
  });

  it('userChangeRole calls service and emits userChangedRole', (done) => {
    fixture.componentRef.setInput('membership', membershipAdmin);
    fixture.detectChanges();

    const nextRole: GroupRole = 'OWNER' as GroupRole;

    // Confirm the promotion to OWNER to trigger the "accept" branch.
    confirmation.confirm.and.callFake((cfg: Confirmation) => {
      cfg.accept?.();
      return confirmation;
    });

    component.userChangedRole.subscribe(() => {
      expect(groupService.changeRole).toHaveBeenCalledWith(1, 2, nextRole);
      done();
    });

    component.userChangeRole(nextRole);
  });

  it('navigates to user profile when card is opened', () => {
    fixture.componentRef.setInput('membership', membershipAdmin);
    fixture.detectChanges();

    component.openProfile();

    expect(router.navigate).toHaveBeenCalledWith(['/user', 'john']);
  });

  it('userRemove opens confirm; accept calls removeUser and emits userRemoved', (done) => {
    fixture.componentRef.setInput('membership', membershipAdmin);
    fixture.detectChanges();

    confirmation.confirm.and.callFake((cfg: Confirmation) => {
      cfg.accept?.();
      return confirmation;
    });

    component.userRemoved.subscribe(() => {
      expect(groupService.removeUser).toHaveBeenCalledWith(1, 2);
      done();
    });

    component.userRemove();
    expect(confirmation.confirm).toHaveBeenCalled();
  });
});
