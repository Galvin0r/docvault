import { Component, inject, Injector, input, OnInit, output, runInInjectionContext, signal } from '@angular/core';
import { GroupService } from '../groups.service';
import { SearchStore } from '../../utils/search/search-store';
import { GroupJoinRequest } from '../groups.model';
import { FormBuilder } from '@angular/forms';

@Component({
  selector: 'app-group-join-requests',
  standalone: false,
  templateUrl: './group-join-requests.component.html',
  styleUrl: './group-join-requests.component.scss'
})
export class GroupJoinRequestsComponent implements OnInit {
  groupService = inject(GroupService);
  injector = inject(Injector);
  formBuilder = inject(FormBuilder);
  dummyForm = this.formBuilder.group({});

  groupId = input.required<number>();

  page = signal(1);
  size = signal(5);

  searchStore!: SearchStore<GroupJoinRequest>;

  changed = output<void>();

  ngOnInit(): void {
    runInInjectionContext(this.injector, () => {
      this.searchStore = new SearchStore<GroupJoinRequest>(
        this.dummyForm,
        (params) => this.groupService.getRequests(this.groupId(), params),
        this.page,
        this.size
      );
    });
  }

  onPageChange(page: number) {
    if (page !== this.page()) this.page.set(page);
  }

  onAccept(request: GroupJoinRequest) {
    this.groupService.acceptRequest(request.id).subscribe(() => {
      this.changed.emit();
      this.searchStore.refresh();
    });
  }

  onReject(request: GroupJoinRequest) {
    this.groupService.rejectRequest(request.id).subscribe(() => {
      this.changed.emit();
      this.searchStore.refresh();
    });
  }
}
