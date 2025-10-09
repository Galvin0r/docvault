import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { PrimeNgModule } from '../primeng/primeng.module';
import { GroupsRoutingModule } from './groups-routing.module';
import { GroupComponent } from './group/group.component';
import { GroupListComponent } from './group-list/group-list.component';
import { UtilsModule } from '../utils/utils.module';
import { GroupManageComponent } from './group-manage/group-manage.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GroupAddComponent } from './group-add/group-add.component';

@NgModule({
  declarations: [GroupComponent, GroupListComponent, GroupManageComponent, GroupAddComponent],
  imports: [
    CommonModule,
    PrimeNgModule,
    GroupsRoutingModule,
    UtilsModule,
    FormsModule,
    ReactiveFormsModule,
  ]
})
export class GroupsModule {}
