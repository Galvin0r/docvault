import { Component, input } from '@angular/core';
import { Group } from '../groups.model';

@Component({
  selector: 'app-group',
  standalone: false,
  templateUrl: './group.component.html',
  styleUrl: './group.component.scss'
})
export class GroupComponent {
  group = input.required<Group>();

  goToGroup() {
    
  }
}
