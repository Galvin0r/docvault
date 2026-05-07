import { Component, inject, Injector, input, OnInit, output, runInInjectionContext, signal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { GroupMembership } from '../../groups/groups.model';
import { SearchStore } from '../../utils/search/search-store';
import { GroupService } from '../../groups/groups.service';

@Component({
  selector: 'app-user-list',
  standalone: false,
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss'
})
export class UserListComponent implements OnInit {
  private injector = inject(Injector);
  groupService = inject(GroupService);

  searchForm = input.required<FormGroup>();
  groupId = input.required<number>();
  limitOptions = input<number[]>([10, 20, 50]);
  canManageRole = input(false);
  canRemove = input(false);

  userRoleChanged = output<void>();

  searchStore!: SearchStore<GroupMembership>;

  page = signal(1);
  size = signal(10);

  ngOnInit(): void {
    runInInjectionContext(this.injector, () => {
      this.searchStore = new SearchStore<GroupMembership>(
        this.searchForm(),
        (params) => this.groupService.getMembers(this.groupId(), params),
        this.page,
        this.size
      );
    });
  }

  onPageChange(page: number) {
    if (page !== this.page()) this.page.set(page);
  }

  onSizeChange(size: number) {
    if (size !== this.size()) this.size.set(size);
  }

  refresh() {
    this.searchStore.refresh();
  }

  onRoleChange() {
    this.refresh();
    this.userRoleChanged.emit();
  }
}