import {
  AfterViewInit,
  Component,
  effect,
  inject,
  Injector,
  runInInjectionContext,
  signal,
  viewChild,
  ViewChild,
  WritableSignal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Group, GroupMembership } from '../groups.model';
import { FormBuilder } from '@angular/forms';
import { GroupService } from '../groups.service';
import { UserListComponent } from '../../users/user-list/user-list.component';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { GroupAddUserComponent } from '../group-add-user/group-add-user.component';
import { GroupAddComponent } from '../group-add/group-add.component';

@Component({
  selector: 'app-group-edit',
  standalone: false,
  templateUrl: './group-view.component.html',
  styleUrl: './group-view.component.scss',
})
export class GroupViewComponent implements AfterViewInit {
  formBuilder = inject(FormBuilder);
  groupService = inject(GroupService);
  private injector = inject(Injector);
  router = inject(Router);

  dummyForm = this.formBuilder.group({});

  group!: WritableSignal<Group>;
  membership!: WritableSignal<GroupMembership>;

  ref: DynamicDialogRef | undefined;
  dialogService = inject(DialogService);

  list = viewChild(UserListComponent);

  constructor(private activatedRoute: ActivatedRoute) {
    activatedRoute.data.subscribe(({ group, membership }) => {
      if (group) {
        this.group = signal(group);
      }
      if (membership) {
        this.membership = signal(membership);
      }
    });
  }

  ngAfterViewInit(): void {
    runInInjectionContext(this.injector, () => {
      effect(() => {
        const cmp = this.list();
        if (!cmp || !cmp.searchStore || !this.group) return;
        const total = cmp.searchStore.items().totalElements;
        this.group.update((g) => ({ ...g, membersNumber: total }));
      });
    });
  }

  onAddMember() {
    this.ref = this.dialogService.open(GroupAddUserComponent, {
      header: 'Add member',
      styleClass: 'group-dialog',
      modal: true,
    });

    this.ref.onClose.subscribe((data: any) => {
      this.groupService
        .addMember(this.group().id, data.email)
        .subscribe(() => this.list()?.refresh());
    });
  }

  onEdit() {
    this.ref = this.dialogService.open(GroupAddComponent, {
      header: 'Edit group',
      styleClass: 'group-dialog',
      modal: true,
      data: {
        initial: this.group(),
      },
    });

    this.ref.onClose.subscribe((data: Group) => {
      this.groupService.edit(data).subscribe(() => this.group.set({ ...this.group(), ...data }));
    });
  }

  onDelete() {
    this.groupService.delete(this.group().id).subscribe(() => {
      this.router.navigate(['/groups']);
    });
  }
}
