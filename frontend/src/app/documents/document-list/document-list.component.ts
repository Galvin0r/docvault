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
import { DocumentService } from '../documents.service';
import { SearchStore } from '../../utils/search/search-store';
import { DocumentDto } from '../documents.model';

@Component({
  selector: 'app-document-list',
  standalone: false,
  templateUrl: './document-list.component.html',
  styleUrl: './document-list.component.scss',
})
export class DocumentListComponent implements OnInit {
  documentService = inject(DocumentService);
  private injector = inject(Injector);

  filtersForm = input.required<FormGroup>();

  limitOptions = input<number[]>([10, 20, 50]);
  searchStore!: SearchStore<DocumentDto>;
  page = signal(1);
  size = signal(10);

  ngOnInit(): void {
    runInInjectionContext(this.injector, () => {
      this.searchStore = new SearchStore<DocumentDto>(
        this.filtersForm(),
        (params) => this.documentService.find(params),
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
