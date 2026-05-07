import { computed, effect, Signal, signal } from '@angular/core';
import { Page } from '../../app.model';
import { FormGroup } from '@angular/forms';
import { debounceTime, distinctUntilChanged, Observable, startWith } from 'rxjs';
import { DEBOUNCE_MS } from '../consts';
import { toSignal } from '@angular/core/rxjs-interop';
import { serializeForm } from '../functions';

export type Fetcher<T> = (q: Record<string, any>) => Observable<Page<T>>;

const dummyPage = { content: [], totalElements: 0, size: 10, number: 0 };

export class SearchStore<T> {
  readonly items = signal<Page<T>>(dummyPage);
  readonly loading = signal(false);
  readonly error = signal<unknown | null>(null);

  readonly first = computed(() => (this.page() - 1) * this.size());
  private _tick = signal(0);

  refresh = () => this._tick.set(this._tick() + 1);

  constructor(
    private form: FormGroup,
    private fetcher: Fetcher<T>,
    private page: Signal<number>,
    private size: Signal<number>
  ) {
    const formSignal = toSignal(
      this.form.valueChanges.pipe(
        startWith(this.form.getRawValue()),
        debounceTime(DEBOUNCE_MS),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b))
      ),
      { initialValue: this.form.getRawValue() }
    );

    effect(() => {
      formSignal();
      page();
      size();
      this._tick();

      this.loading.set(true);
      this.error.set(null);

      const queryParams: Record<string, any> = {
        ...serializeForm(form),
        size: size(),
        page: page() - 1,
      };

      this.loading.set(true);
      this.error.set(null);

      this.fetcher(queryParams)
        .subscribe({
          next: (result) => {
            this.items.set(result);
            this.loading.set(false);
          },
          error: (e) => {
            this.items.set(dummyPage);
            this.error.set(e);
            this.loading.set(false);
          }
        });
    });
  }
}