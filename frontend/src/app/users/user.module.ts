import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserRoutingModule } from './user-routing.module';
import { UserListComponent } from './user-list/user-list.component';
import { UserComponent } from './user/user.component';
import { ProfileComponent } from './profile/profile.component';
import { PrimeNgModule } from '../primeng/primeng.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DocumentsModule } from '../documents/documents.module';
import { SecurityModule } from '../security/security.module';
import { UtilsModule } from '../utils/utils.module';
import { AddToGroupDialogComponent } from './add-to-group-dialog/add-to-group-dialog.component';

@NgModule({
  declarations: [
    UserListComponent,
    UserComponent,
    ProfileComponent,
    AddToGroupDialogComponent
  ],
  imports: [
    CommonModule,
    UserRoutingModule,
    PrimeNgModule,
    ReactiveFormsModule,
    FormsModule,
    DocumentsModule,
    SecurityModule,
    UtilsModule
  ],
  exports: [UserComponent, UserListComponent]
})
export class UserModule { }