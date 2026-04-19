import {
  AfterViewInit,
  Component,
  computed,
  effect,
  inject,
  Injector,
  runInInjectionContext,
  signal,
  viewChild,
  WritableSignal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Group, GroupJoinRequest, GroupMembership } from '../groups.model';
import { FormBuilder } from '@angular/forms';
import { GroupService } from '../groups.service';
import { UserListComponent } from '../../users/user-list/user-list.component';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { GroupAddUserComponent } from '../group-add-user/group-add-user.component';
import { GroupAddComponent } from '../group-add/group-add.component';
import { isNotNil } from 'ramda';
import { ConfirmationService } from 'primeng/api';
import { DocumentListComponent } from '../../documents/document-list/document-list.component';
import { GroupAddDocumentDialogComponent } from '../group-add-document-dialog/group-add-document-dialog.component';

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
  confirmationSerice = inject(ConfirmationService);

  membersForm = this.formBuilder.group({});
  documentForm = this.formBuilder.group({
    titleSearch: [''],
    ownerName: [''],
    dateFrom: [null as Date | null],
    dateTo: [null as Date | null],
    groupId: [null as number | null],
  });

  group!: WritableSignal<Group>;
  membership = signal<GroupMembership | undefined>(undefined);
  joinRequest = signal<GroupJoinRequest | null>(null);

  isOwner = computed(() => !!this.membership() && this.membership()?.role === 'OWNER');
  isAdmin = computed(() => !!this.membership() && this.membership()?.role === 'ADMIN');
  isUser = computed(() => !this.membership() || this.membership()?.role === 'USER');

  ref: DynamicDialogRef | undefined;
  dialogService = inject(DialogService);

  list = viewChild(UserListComponent);
  documentList = viewChild(DocumentListComponent);

  requestsOpen = signal(false);

  constructor(private activatedRoute: ActivatedRoute) {
    activatedRoute.data.subscribe(({ group, membership, joinRequest }) => {
      if (group) {
        this.group = signal(group);
        this.documentForm.patchValue({
          groupId: group.id,
        });
      }
      this.membership.set(membership);
      if (joinRequest !== undefined) {
        this.joinRequest.set(joinRequest);
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
      dismissableMask: true,
    });

    this.ref.onClose.subscribe((data: any) => {
      this.groupService
        .addMember(this.group().id, data.email)
        .subscribe(() => this.list()?.refresh());
    });
  }

  onAddDocument() {
    this.ref = this.dialogService.open(GroupAddDocumentDialogComponent, {
      header: 'Add documents to group',
      width: '900px',
      styleClass: 'group-dialog',
      modal: true,
      dismissableMask: true,
      data: {
        groupId: this.group().id,
        groupName: this.group().name,
        onDocumentLinked: () => this.documentList()?.refresh(),
      },
    });
  }

  onEdit() {
    this.ref = this.dialogService.open(GroupAddComponent, {
      header: 'Edit group',
      styleClass: 'group-dialog',
      modal: true,
      dismissableMask: true,
      data: {
        initial: this.group(),
      },
    });

    this.ref.onClose.subscribe((data: Group) => {
      this.groupService.edit(data).subscribe(() => this.group.set({ ...this.group(), ...data }));
    });
  }

  onDelete() {
    this.confirmationSerice.confirm({
      message: 'Are you sure that you want to delete this group?',
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
        label: 'Delete',
        severity: 'danger',
      },
      accept: () => {
        this.groupService.delete(this.group().id).subscribe(() => {
          this.router.navigate(['/groups']);
        });
      },
    });
  }

  onLeave() {
    this.confirmationSerice.confirm({
      message: 'Are you sure that you want to leave this group?',
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
        label: 'Leave',
        severity: 'danger',
      },
      accept: () => {
        this.groupService.leave(this.group().id).subscribe(() => {
          if (this.group().visibility === 'PRIVATE') {
            this.router.navigate(['/groups']);
          } else {
            this.list()?.refresh();
            this.membership.set(undefined);
          }
        });
      },
    });
  }

  onJoin() {
    this.groupService.join(this.group().id).subscribe((membership: GroupMembership | null) => {
      this.list()?.refresh();
      if (isNotNil(membership)) {
        this.membership.set(membership);
      } else {
        this.groupService
          .getJoinRequest(this.group().id)
          .subscribe((joinRequest: GroupJoinRequest | null) => {
            this.joinRequest.set(joinRequest);
          });
      }
    });
  }

  onRoleChange() {
    this.groupService.getMembership(this.group().id).subscribe((membership: GroupMembership) => {
      this.membership.set(membership);
    });
  }

  onRequestChanged() {
    this.groupService.get(this.group().id).subscribe((group: Group) => {
      this.group.set(group);
    });
    this.list()?.refresh();
  }

  toggleRequests() {
    this.requestsOpen.update((v) => !v);
  }

  clearDocumentFilters() {
    this.documentForm.reset({
      groupId: this.group().id,
    });
  }
}
