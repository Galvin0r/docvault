import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { GroupService } from '../../groups/groups.service';
import { GroupMembership } from '../../groups/groups.model';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

@Component({
    selector: 'app-add-to-group-dialog',
    standalone: false,
    templateUrl: './add-to-group-dialog.component.html',
    styleUrl: './add-to-group-dialog.component.scss'
})
export class AddToGroupDialogComponent implements OnInit {
    private dialogRef = inject(DynamicDialogRef);
    private dialogConfig = inject(DynamicDialogConfig);
    private groupService = inject(GroupService);
    private messageService = inject(MessageService);

    userEmail: string = '';
    userLogin: string = '';
    currentUserLogin: string = '';

    groups = signal<GroupMembership[]>([]);
    loading = signal(false);
    submitting = signal(false);

    selectedGroup = new FormControl<GroupMembership | null>(null, Validators.required);

    ngOnInit(): void {
        this.userEmail = this.dialogConfig.data?.email ?? '';
        this.userLogin = this.dialogConfig.data?.login ?? '';
        this.currentUserLogin = this.dialogConfig.data?.currentUserLogin ?? '';
        this.loadGroups();
    }

    loadGroups(): void {
        this.loading.set(true);
        this.groupService.findMemberships({ userLogin: this.currentUserLogin, size: 100 })
            .pipe(finalize(() => this.loading.set(false)))
            .subscribe({
                next: (page) => {
                    const adminGroups = page.content.filter(
                        m => m.role === 'OWNER' || m.role === 'ADMIN'
                    );
                    this.groups.set(adminGroups);
                }
            });
    }

    onSubmit(): void {
        if (this.selectedGroup.invalid || !this.selectedGroup.value) return;

        const group = this.selectedGroup.value;
        this.submitting.set(true);

        this.groupService.addMember(group.groupId, this.userEmail)
            .pipe(finalize(() => this.submitting.set(false)))
            .subscribe({
                next: () => {
                    this.messageService.add({
                        severity: 'success',
                        summary: 'Success',
                        detail: `${this.userLogin} added to ${group.groupName}`
                    });
                    this.dialogRef.close(true);
                },
                error: (err) => {
                    this.messageService.add({
                        severity: 'error',
                        summary: 'Error',
                        detail: err.error?.message ?? 'Failed to add user to group'
                    });
                }
            });
    }

    onCancel(): void {
        this.dialogRef.close(false);
    }
}
