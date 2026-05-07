import { Component, inject, viewChild } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { DialogService } from 'primeng/dynamicdialog';
import { GroupAddComponent } from '../group-add/group-add.component';
import { Group } from '../groups.model';
import { GroupService } from '../groups.service';
import { GroupListComponent } from '../group-list/group-list.component';
import { Router } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'app-group-manage',
  standalone: false,
  templateUrl: './group-manage.component.html',
  styleUrl: './group-manage.component.scss'
})
export class GroupManageComponent {
  formBuilder = inject(FormBuilder);
  groupService = inject(GroupService);
  router = inject(Router);

  searchForm = this.formBuilder.group({
    name: ['']
  });

  dialogService = inject(DialogService);

  list = viewChild.required(GroupListComponent);

  onAddGroup() {
    this.dialogService.open(GroupAddComponent, {
      header: 'Create group',
      styleClass: 'group-dialog',
      modal: true,
      dismissableMask: true
    }).onClose.pipe(
      filter(data => !!data),
    ).subscribe((data: Group) => {
      this.groupService.create(data).subscribe(groupId => this.router.navigate(['/groups/edit/', groupId]));
    });
  }
}