import { Component, computed, input, model } from '@angular/core';
import { PaginatorState } from 'primeng/paginator';

@Component({
  selector: 'app-paginator',
  standalone: false,
  templateUrl: './paginator.component.html',
  styleUrl: './paginator.component.scss'
})
export class PaginatorComponent {
  sizeOptions = input<number[]>([10, 20, 50]);
  total = input.required<number>();

  size = model(10);
  page = model(1);

  sizeOptionsFull = computed(() => {
    return this.sizeOptions().map(v => ({ value: v, label: String(v) }));
  });
  first = computed(() => (this.page() - 1) * this.size());

  onSizeChange(size: number) {
    this.size.set(size);
    this.page.set(1);
  }

  onPageChange(event: PaginatorState) {
    this.page.set(Number(event.page) + 1);
  }
}
