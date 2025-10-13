import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { GroupManageComponent } from "./group-manage/group-manage.component";
import { GroupViewComponent } from "./group-view/group-view.component";
import { groupResolver, membershipResolver } from "./groups.service";

const routes: Routes = [
  {
    path: 'groups',
    children: [
      {
        path: "",
        component: GroupManageComponent
      },
      {
        path: 'edit/:id',
        component: GroupViewComponent,
        resolve: {
          group: groupResolver,
          membership: membershipResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GroupsRoutingModule {}