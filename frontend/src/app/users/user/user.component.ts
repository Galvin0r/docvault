import { Component, computed, inject, input, output } from '@angular/core';
import {
  getRoleByKey,
  getRoleByValue,
  GroupMembership,
  GroupRole,
} from '../../groups/groups.model';
import { ConfirmationService, MenuItem, PrimeIcons } from 'primeng/api';
import { GroupService } from '../../groups/groups.service';
import { TitleCasePipe } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-user',
  standalone: false,
  templateUrl: './user.component.html',
  styleUrl: './user.component.scss',
})
export class UserComponent {
  canManageRole = input(false);
  canRemove = input(false);
  membership = input.required<GroupMembership>();
  userRemoved = output<void>();
  userChangedRole = output<void>();
  confirmationSerice = inject(ConfirmationService);

  groupService = inject(GroupService);
  router = inject(Router);
  titleCase = new TitleCasePipe();

  menuItems = computed(() => {
    const items: MenuItem[] = [];
    if (this.membership().role !== 'OWNER') {
      if (this.canManageRole()) {
        const nextRole = getRoleByKey(this.membership().role);
        const prevRole = getRoleByValue(this.membership().role);
        if (nextRole) {
          items.push({
            label: 'Promote to ' + this.titleCase.transform(nextRole),
            icon: PrimeIcons.ARROW_UP,
            command: () => this.userChangeRole(nextRole),
          });
        }
        if (prevRole) {
          items.push({
            label: 'Demote to ' + this.titleCase.transform(prevRole),
            icon: PrimeIcons.ARROW_DOWN,
            command: () => this.userChangeRole(prevRole),
          });
        }
      }

      if (this.canRemove()) {
        items.push({
          label: 'Remove from group',
          icon: PrimeIcons.TRASH,
          command: () => this.userRemove(),
        });
      }
    }
    return items;
  });

  openProfile() {
    this.router.navigate(['/user', this.membership().userLogin]);
  }

  userRemove() {
    this.confirmationSerice.confirm({
      message: 'Are you sure you want to remove this user from group?',
      header: 'Confirmation',
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: {
        label: 'Cancel',
        severity: 'secondary',
        outlined: true,
      },
      acceptButtonProps: {
        label: 'Remove',
        severity: 'danger',
      },
      accept: () => {
        this.groupService
          .removeUser(this.membership().groupId, this.membership().userId)
          .subscribe(() => {
            this.userRemoved.emit();
          });
      },
    });
  }

  userChangeRole(newRole: GroupRole) {
    var accept = () => {
      this.groupService
      .changeRole(this.membership().groupId, this.membership().userId, newRole)
      .subscribe(() => {
        this.userChangedRole.emit();
      });
    }
    if (newRole === 'OWNER') {
      this.confirmationSerice.confirm({
        message: 'Are you sure you want to promote this user to Owner? You will be demoted to Admin.',
        header: 'Confirmation',
        closable: true,
        closeOnEscape: true,
        icon: 'pi pi-exclamation-triangle',
        rejectButtonProps: {
          label: 'Cancel',
          severity: 'secondary',
          outlined: true,
        },
        acceptButtonProps: {
          label: 'Accept',
          severity: 'danger',
        },
        accept: accept,
      });
    } else {
      accept();
    }
  }
}
