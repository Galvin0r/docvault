import {
  HTTP_INTERCEPTORS,
  HttpClient,
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { AuthInterceptor } from './auth.interceptor';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

describe('AuthInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: { url: string; navigate: jasmine.Spy };

  beforeEach(() => {
    router = { url: '/home', navigate: jasmine.createSpy('navigate') };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    router.navigate.calls.reset();
    router.url = '/home';
  });

  it('navigates to /login on 401 for non-excluded URLs (and not already on /login)', () => {
    http.get('/api/groups').subscribe({
      next: () => fail('expected error'),
      error: (err) => expect(err.status).toBe(401),
    });

    const req = httpMock.expectOne('/api/groups');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('does NOT navigate for 401 on /api/accounts/me (excluded endpoint)', () => {
    http.get('/api/accounts/me').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/accounts/me');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does NOT navigate for 401 on public document search', () => {
    http.get('/api/document/search').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/document/search');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does NOT navigate when current route is already /login', () => {
    router.url = '/login';

    http.get('/api/groups').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/groups');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does NOT navigate for non-401 errors', () => {
    http.get('/api/groups').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/groups');
    req.flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('always rethrows the error (propagates to subscriber)', () => {
    let received: any;

    http.get('/api/groups').subscribe({
      next: () => fail('expected error'),
      error: (err) => (received = err),
    });

    const req = httpMock.expectOne('/api/groups');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(received).toBeTruthy();
    expect(received.status).toBe(401);
  });
});