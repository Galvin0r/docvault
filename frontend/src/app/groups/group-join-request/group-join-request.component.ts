import { Component, input, output } from '@angular/core';
import { GroupJoinRequest } from '../groups.model';

@Component({
  selector: 'app-group-join-request',
  standalone: false,
  templateUrl: './group-join-request.component.html',
  styleUrl: './group-join-request.component.scss'
})
export class GroupJoinRequestComponent {
  request = input.required<GroupJoinRequest>();

  accept = output<GroupJoinRequest>();
  reject = output<GroupJoinRequest>();

  onAccept() {
    this.accept.emit(this.request());
  }

  onReject() {
    this.reject.emit(this.request());
  }
}