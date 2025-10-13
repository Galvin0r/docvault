import { Component, input } from '@angular/core';
import { GroupMembership } from '../../groups/groups.model';

@Component({
  selector: 'app-user',
  standalone: false,
  templateUrl: './user.component.html',
  styleUrl: './user.component.scss'
})
export class UserComponent {
  membership = input.required<GroupMembership>();

  goToUser() {
    
  }
}
