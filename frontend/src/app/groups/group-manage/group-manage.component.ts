import { Component, inject, OnDestroy, ViewChild } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { GroupAddComponent } from '../group-add/group-add.component';
import { Group } from '../groups.model';
import { GroupService } from '../groups.service';
import { GroupListComponent } from '../group-list/group-list.component';

@Component({
  selector: 'app-group-manage',
  standalone: false,
  templateUrl: './group-manage.component.html',
  styleUrl: './group-manage.component.scss'
})
export class GroupManageComponent implements OnDestroy {
  formBuilder = inject(FormBuilder);
  groupService = inject(GroupService);
  searchForm = this.formBuilder.group({
    name: ['']
  });

  ref: DynamicDialogRef | undefined;
  dialogService = inject(DialogService);

  @ViewChild(GroupListComponent) list!: GroupListComponent;

  onAddGroup() {
    this.ref = this.dialogService.open(GroupAddComponent, {
      header: 'Create group',
      styleClass: 'group-dialog',
      modal: true
    });

    this.ref.onClose.subscribe((data: Group) => {
      this.groupService.create(data).subscribe(() => this.list.refresh());
    });
  }

  ngOnDestroy(): void {
    if (this.ref) {
      this.ref.close(); 
    }
  }
}
