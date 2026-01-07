import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { ProfileComponent } from "./profile/profile.component";
import { currentUserResolver, userResolver } from "../security/security.service";

const routes: Routes = [
  {
    path: 'user',
    children: [
      {
        path: ":userId",
        component: ProfileComponent,
        resolve: {
          userInfo: userResolver,
          currentUserInfo: currentUserResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class UserRoutingModule {}