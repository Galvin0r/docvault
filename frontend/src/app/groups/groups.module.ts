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
import { GroupViewComponent } from './group-view/group-view.component';
import { UserModule } from '../users/user.module';
import { GroupAddUserComponent } from './group-add-user/group-add-user.component';
import { GroupJoinRequestsComponent } from './group-join-requests/group-join-requests.component';
import { GroupJoinRequestComponent } from './group-join-request/group-join-request.component';

@NgModule({
  declarations: [GroupComponent, GroupListComponent, GroupManageComponent, GroupAddComponent, GroupViewComponent, GroupAddUserComponent, GroupJoinRequestsComponent, GroupJoinRequestComponent],
  imports: [
    CommonModule,
    PrimeNgModule,
    GroupsRoutingModule,
    UtilsModule,
    FormsModule,
    ReactiveFormsModule,
    UserModule
  ]
})
export class GroupsModule {}
