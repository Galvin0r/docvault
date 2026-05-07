import { FormBuilder } from '@angular/forms';
import { Observable, of, Subject } from 'rxjs';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { signal } from '@angular/core';
import { SearchStore, Fetcher } from './search-store';
import { Page } from '../../app.model';
import { DEBOUNCE_MS } from '../consts';

type GroupEntity = { id: number; name: string };

const makePage = <T>(content: T[] = [], total = 0, size = 10, number = 0): Page<T> => ({
  content,
  totalElements: total,
  size,
  number,
});

describe('SearchStore', () => {
  const formBuilder = new FormBuilder();

  function setupStore(
    initialFormValue = { q: '' },
    fetcherImpl: Fetcher<GroupEntity> = () => of(makePage<GroupEntity>([], 0, 10, 0)),
    initialPage = 1,
    initialSize = 10
  ) {
    const form = formBuilder.group(initialFormValue);
    const pageSignal = signal(initialPage);
    const sizeSignal = signal(initialSize);
    const fetchSpy = jasmine.createSpy('fetcher').and.callFake(fetcherImpl);

    const store = TestBed.runInInjectionContext(
      () => new SearchStore<GroupEntity>(form, fetchSpy, pageSignal.asReadonly(), sizeSignal.asReadonly())
    );

    return { store, form, pageSignal, sizeSignal, fetchSpy };
  }

  it('calls fetcher on construct with serialized form, size, and (page-1)', fakeAsync(() => {
    const { fetchSpy } = setupStore({ q: '  hello  ' });
    tick();
    expect(fetchSpy).toHaveBeenCalledTimes(1);
    const lastQuery = fetchSpy.calls.mostRecent().args[0];
    expect(lastQuery).toEqual({ q: 'hello', size: 10, page: 0 });
  }));

  it('updates items and loading on success', fakeAsync(() => {
    const successfulPage = makePage<GroupEntity>([{ id: 1, name: 'A' }], 1, 10, 0);
    const { store } = setupStore({ q: '' }, () => of(successfulPage));
    tick();
    expect(store.items()).toEqual(successfulPage);
    expect(store.loading()).toBeFalse();
    expect(store.error()).toBeNull();
  }));

  it('reacts to form changes after debounce and uses serializeForm', fakeAsync(() => {
    const { form, fetchSpy } = setupStore({ q: '' });
    tick();
    fetchSpy.calls.reset();

    form.get('q')!.setValue('  world  ');
    tick(DEBOUNCE_MS);

    expect(fetchSpy).toHaveBeenCalledTimes(1);
    const lastQuery = fetchSpy.calls.mostRecent().args[0];
    expect(lastQuery.q).toBe('world');
  }));

  it('reacts to page and size signal changes', fakeAsync(() => {
    const { pageSignal, sizeSignal, fetchSpy } = setupStore({ q: '' });
    tick();
    fetchSpy.calls.reset();

    pageSignal.set(3);
    tick();
    expect(fetchSpy).toHaveBeenCalledTimes(1);
    expect(fetchSpy.calls.mostRecent().args[0].page).toBe(2);

    sizeSignal.set(25);
    tick();
    expect(fetchSpy).toHaveBeenCalledTimes(2);
    expect(fetchSpy.calls.mostRecent().args[0].size).toBe(25);
  }));

  it('sets dummy page and error on fetch error, and turns loading off', fakeAsync(() => {
    const responseSubject = new Subject<Page<GroupEntity>>();
    const { store } = setupStore({ q: '' }, () => responseSubject as Observable<Page<GroupEntity>>);
    tick();

    responseSubject.error(new Error('boom'));
    tick();

    expect(store.items()).toEqual({ content: [], totalElements: 0, size: 10, number: 0 });
    expect(store.error()).toBeTruthy();
    expect(store.loading()).toBeFalse();
  }));

  it('refresh() triggers a new fetch', fakeAsync(() => {
    const { store, fetchSpy } = setupStore();
    tick();
    fetchSpy.calls.reset();

    store.refresh();
    tick();
    expect(fetchSpy).toHaveBeenCalledTimes(1);
  }));
});