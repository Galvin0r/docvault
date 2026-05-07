import {
  Component,
  inject,
  Injector,
  input,
  OnInit,
  runInInjectionContext,
  signal,
} from '@angular/core';
import { FormGroup } from '@angular/forms';
import { GroupService } from '../groups.service';
import { SearchStore } from '../../utils/search/search-store';
import { Group } from '../groups.model';

@Component({
  selector: 'app-group-list',
  standalone: false,
  templateUrl: './group-list.component.html',
  styleUrl: './group-list.component.scss',
})
export class GroupListComponent implements OnInit {
  groupService = inject(GroupService);
  private injector = inject(Injector);

  filtersForm = input.required<FormGroup>();

  limitOptions = input<number[]>([10, 20, 50]);
  searchStore!: SearchStore<Group>;
  page = signal(1);
  size = signal(10);

  ngOnInit(): void {
    runInInjectionContext(this.injector, () => {
      this.searchStore = new SearchStore<Group>(
        this.filtersForm(),
        (params) => this.groupService.find(params),
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
}