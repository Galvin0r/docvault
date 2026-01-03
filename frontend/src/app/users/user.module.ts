import { NgModule } from '@angular/core';
import { UserComponent } from './user/user.component';
import { PrimeNgModule } from '../primeng/primeng.module';
import { UserListComponent } from './user-list/user-list.component';
import { UtilsModule } from '../utils/utils.module';
import { ProfileComponent } from './profile/profile.component';
import { UserRoutingModule } from './user-routing.modu;e';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SecurityModule } from '../security/security.module';

@NgModule({
  declarations: [UserComponent, UserListComponent, ProfileComponent],
  imports: [
    PrimeNgModule,
    UtilsModule,
    UserRoutingModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SecurityModule,
  ],
  exports: [UserComponent, UserListComponent],
})
export class UserModule {}
