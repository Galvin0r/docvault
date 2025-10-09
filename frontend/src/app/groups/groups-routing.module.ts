import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { GroupManageComponent } from "./group-manage/group-manage.component";

const routes: Routes = [
  {
    path: 'groups',
    component: GroupManageComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GroupsRoutingModule {}