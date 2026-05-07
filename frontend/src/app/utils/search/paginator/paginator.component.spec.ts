import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { PaginatorComponent } from './paginator.component';

describe('PaginatorComponent', () => {
  let fixture: ComponentFixture<PaginatorComponent>;
  let component: PaginatorComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PaginatorComponent],
      schemas: [NO_ERRORS_SCHEMA],
    });

    fixture = TestBed.createComponent(PaginatorComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('total', 100);
    fixture.detectChanges();
  });

  it('initializes with defaults and computed values', () => {
    expect(component.size()).toBe(10);
    expect(component.page()).toBe(1);
    expect(component.first()).toBe(0);
    expect(component.sizeOptionsFull()).toEqual([
      { value: 10, label: '10' },
      { value: 20, label: '20' },
      { value: 50, label: '50' },
    ]);
  });

  it('respects custom sizeOptions input', () => {
    fixture.componentRef.setInput('sizeOptions', [5, 10]);
    fixture.detectChanges();
    expect(component.sizeOptionsFull()).toEqual([
      { value: 5, label: '5' },
      { value: 10, label: '10' },
    ]);
  });

  it('first() reflects page and size', () => {
    component.size.set(20);
    component.page.set(3);
    expect(component.first()).toBe((3 - 1) * 20);
  });

  it('onSizeChange sets size and resets page to 1', () => {
    component.page.set(4);
    component.onSizeChange(50);
    expect(component.size()).toBe(50);
    expect(component.page()).toBe(1);
  });

  it('onPageChange sets page = event.page + 1', () => {
    component.page.set(1);
    component.onPageChange({ page: 2 } as any);
    expect(component.page()).toBe(3);
  });

  it('container has correct justification class based on isSimplePaginator', () => {
    fixture.componentRef.setInput('isSimplePaginator', false);
    fixture.detectChanges();
    let container = fixture.debugElement.query(By.css('div.flex')).nativeElement as HTMLDivElement;
    expect(container.classList.contains('justify-end')).toBeTrue();

    fixture.componentRef.setInput('isSimplePaginator', true);
    fixture.detectChanges();
    container = fixture.debugElement.query(By.css('div.flex')).nativeElement as HTMLDivElement;
    expect(container.classList.contains('justify-center')).toBeTrue();
  });
});