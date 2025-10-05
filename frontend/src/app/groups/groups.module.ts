import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { PrimeNgModule } from "../primeng/primeng.module";
import { GroupComponent } from './group/group.component';
import { GroupListComponent } from './group-list/group-list.component';

@NgModule({
  declarations: [
    GroupComponent,
    GroupListComponent
  ],
  imports: [CommonModule, PrimeNgModule]
})
export class GroupsModule {}