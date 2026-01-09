import { CommonModule } from '@angular/common';
import {
  Component,
  ContentChild,
  EventEmitter,
  Input,
  Output,
  TemplateRef,
  AfterContentInit,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { GroupListComponent } from './group-list.component';
import { GroupService } from '../groups.service';

@Component({
  selector: 'app-group',
  standalone: true,
  template: `
    <div class="row">{{ group?.name }}</div>
  `,
})
class GroupStub {
  @Input() group: any;
}

@Component({
  selector: 'p-dataview',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (listTpl) {
      <ng-container *ngTemplateOutlet="listTpl; context: ctx"></ng-container>
    }
  `,
})
class DataViewStub implements AfterContentInit, OnChanges {
  @Input() value: any[] = [];
  @ContentChild('list', { read: TemplateRef }) listTpl!: TemplateRef<any>;
  ctx: any = { $implicit: this.value };
  ngAfterContentInit() {
    this.ctx = { $implicit: this.value };
  }
  ngOnChanges(ch: SimpleChanges) {
    if (ch['value']) this.ctx = { $implicit: this.value };
  }
}

@Component({
  selector: 'app-paginator',
  standalone: true,
  template: `
    <div class="paginator">
      <span class="badge">{{ total }}</span>
      <button class="emit-page" (click)="pageChange.emit((page ?? 1) + 1)"></button>
      <button class="emit-size" (click)="sizeChange.emit((size ?? 10) + 5)"></button>
    </div>
  `,
})
class PaginatorStub {
  @Input() total?: number;
  @Input() sizeOptions?: number[];
  @Input() page?: number;
  @Input() size?: number;
  @Output() pageChange = new EventEmitter<number>();
  @Output() sizeChange = new EventEmitter<number>();
}

class FakeSearchStore<T> {
  constructor(private data: { content: T[]; totalElements: number }) { }
  items() {
    return this.data;
  }
  set(v: { content: T[]; totalElements: number }) {
    this.data = v;
  }
  refresh = jasmine.createSpy('refresh');
}

class GroupServiceStub {
  find = jasmine.createSpy('find').and.returnValue(of({ content: [], totalElements: 0 }));
}

describe('GroupListComponent', () => {
  let fixture: ComponentFixture<GroupListComponent>;
  let component: GroupListComponent;
  let store: FakeSearchStore<any>;

  function createWith(items: { content: any[]; totalElements: number }) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CommonModule, ReactiveFormsModule, GroupStub, DataViewStub, PaginatorStub],
      declarations: [GroupListComponent],
      providers: [FormBuilder, { provide: GroupService, useClass: GroupServiceStub }],
    });

    fixture = TestBed.createComponent(GroupListComponent);
    component = fixture.componentInstance;

    const fb = TestBed.inject(FormBuilder);
    const filters = fb.group({});
    fixture.componentRef.setInput('filtersForm', filters as FormGroup);

    fixture.detectChanges();

    store = new FakeSearchStore(items);
    (component as any).searchStore = store;

    fixture.detectChanges();
  }

  afterEach(() => {
    fixture.destroy();
  });

  it('renders groups and reflects total in DOM', () => {
    const content = [
      { id: 1, name: 'A' },
      { id: 2, name: 'B' },
    ];
    createWith({ content, totalElements: content.length });

    const rows = fixture.debugElement.queryAll(By.directive(GroupStub));
    expect(rows.length).toBe(2);

    const badge = fixture.debugElement.query(By.css('.badge')).nativeElement as HTMLElement;
    expect(badge.textContent?.trim()).toBe('2');
  });

  it('updates page on paginator pageChange', () => {
    createWith({ content: [], totalElements: 0 });
    const pag = fixture.debugElement.query(By.directive(PaginatorStub))
      .componentInstance as PaginatorStub;
    pag.pageChange.emit(3);
    expect(component.page()).toBe(3);
  });

  it('updates size on paginator sizeChange', () => {
    createWith({ content: [], totalElements: 0 });
    const pag = fixture.debugElement.query(By.directive(PaginatorStub))
      .componentInstance as PaginatorStub;
    pag.sizeChange.emit(25);
    expect(component.size()).toBe(25);
  });

  it('passes limitOptions input to paginator', () => {
    createWith({ content: [], totalElements: 0 });
    fixture.componentRef.setInput('limitOptions', [5, 10, 15]);
    fixture.detectChanges();
    const pag = fixture.debugElement.query(By.directive(PaginatorStub))
      .componentInstance as PaginatorStub;
    expect(pag.sizeOptions).toEqual([5, 10, 15]);
  });

  it('refresh() calls store.refresh', () => {
    createWith({ content: [], totalElements: 0 });
    (component as any).searchStore = store;
    component.refresh();
    expect(store.refresh).toHaveBeenCalled();
  });
});
