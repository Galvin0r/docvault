import { Component, computed, inject, Injector, OnInit, runInInjectionContext, signal } from '@angular/core';
import { UserInfo } from '../../security/security.model';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { GroupService } from '../../groups/groups.service';
import { SearchStore } from '../../utils/search/search-store';
import { GroupMembership } from '../../groups/groups.model';
import { UserService } from '../user.service';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { SecurityService } from '../../security/security.service';

@Component({
  selector: 'app-profile',
  standalone: false,
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private injector = inject(Injector);
  private router = inject(Router);
  private userService = inject(UserService);
  private messageService = inject(MessageService);
  private securityService = inject(SecurityService);

  activatedRoute = inject(ActivatedRoute);
  fb = inject(FormBuilder);
  groupService = inject(GroupService);

  userInfo = signal<UserInfo | null>(null);
  currentUserInfo = signal<UserInfo | null>(null);
  canChange = computed(() => this.userInfo()?.login === this.currentUserInfo()?.login);

  limitOptions = [5, 10, 20];
  searchStore!: SearchStore<GroupMembership>;
  page = signal(1);
  size = signal(5);

  form = this.fb.group({
    userLogin: [this.userInfo()?.login],
    groupName: ['']
  });

  documentForm = this.fb.group({
    titleSearch: [''],
    ownerName: [''],
    dateFrom: [null as string | null],
    dateTo: [null as string | null]
  });

  editingUsername = signal(false);
  saving = signal(false);
  submittedEdit = signal(false);

  editLoginCtrl = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.minLength(8)]
  });

  constructor() {
    this.activatedRoute.data.subscribe(({ userInfo, currentUserInfo }) => {
      this.userInfo.set(userInfo);
      this.currentUserInfo.set(currentUserInfo);

      this.form.patchValue({
        userLogin: userInfo?.login ?? '',
      });
      this.documentForm.patchValue({
        ownerName: userInfo?.login ?? ''
      });
    });
  }

  ngOnInit(): void {
    runInInjectionContext(this.injector, () => {
      this.searchStore = new SearchStore<GroupMembership>(
        this.form,
        (params) => this.groupService.findMemberships(params),
        this.page,
        this.size
      );
    });
  }

  onEditUsername() {
    this.submittedEdit.set(false);
    this.editingUsername.set(true);
    this.editLoginCtrl.reset(this.userInfo()?.login ?? '');
  }

  cancelEditUsername() {
    this.editingUsername.set(false);
    this.editLoginCtrl.reset();
    this.submittedEdit.set(false);
  }

  saveUsername() {
    this.submittedEdit.set(true);
    if (this.editLoginCtrl.invalid) return;

    const newLogin = this.editLoginCtrl.value.trim();
    if (!newLogin || newLogin === this.userInfo()?.login) {
      this.editingUsername.set(false);
      return;
    }

    this.saving.set(true);
    this.userService.changeLogin(newLogin)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated: UserInfo) => {
          this.userInfo.set(updated);
          if (this.canChange()) this.currentUserInfo.set(updated);
          this.form.patchValue({ userLogin: updated.login }, { emitEvent: true });

          this.editingUsername.set(false);
          this.submittedEdit.set(false);
          this.editLoginCtrl.reset();
          this.editLoginCtrl.updateValueAndValidity();

          this.router.navigate(['/user', updated.login], { replaceUrl: true });
        },
        error: (err: HttpErrorResponse) => {
          this.saving.set(false);

          const error = err.error.code as string;
          if (error === 'user.login_taken') {
            this.editLoginCtrl.setErrors({ taken: true });
          }
          this.editLoginCtrl.markAsTouched();
        }
      });
  }

  onResetPassword() {
    this.securityService.resetPassword(this.currentUserInfo()?.email ?? '').subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Email with instructions sent to ' + this.currentUserInfo()?.email
        });
      },
      error: (e) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: e.appCode
        })
      }
    })
  }

  goToGroup(membership: GroupMembership) {
    this.router.navigate(['/groups/edit', membership.groupId]);
  }

  onPageChange(page: number) {
    if (page !== this.page()) this.page.set(page);
  }

  onSizeChange(size: number) {
    if (size !== this.size()) this.size.set(size);
  }

  openGroups() {
    this.router.navigate(["/groups"]);
  }

  clearFilters() {
    this.documentForm.reset({
      ownerName: this.userInfo()?.login ?? ''
    });
  }
}
