import { NgModule } from "@angular/core";
import { UserComponent } from "./user/user.component";
import { PrimeNgModule } from "../primeng/primeng.module";
import { UserListComponent } from './user-list/user-list.component';
import { UtilsModule } from "../utils/utils.module";

@NgModule({
  declarations: [UserComponent, UserListComponent],
  imports: [PrimeNgModule, UtilsModule],
  exports: [UserComponent, UserListComponent]
})
export class UserModule {}