import { Component, inject, input } from '@angular/core';
import { Group } from '../groups.model';
import { Router } from '@angular/router';

@Component({
  selector: 'app-group',
  standalone: false,
  templateUrl: './group.component.html',
  styleUrl: './group.component.scss'
})
export class GroupComponent {
  group = input.required<Group>();
  router = inject(Router);

  goToGroup() {
    this.router.navigate(['/groups/edit', this.group().id]);
  }
}