import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SecurityService, userResolver } from './security.service';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, Observable } from 'rxjs';
import { UserInfo } from './security.model';

describe('SecurityService', () => {
  let service: SecurityService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SecurityService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(SecurityService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('login() -> POST "api/auth/authenticate" with body', () => {
    const body = { login: 'john', password: 'pw', rememberMe: true } as any;

    service.login(body).subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = http.expectOne('api/auth/authenticate');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(null);
  });

  it('register() -> POST "api/auth/register" with body', () => {
    const body = { email: 'a@b.c', username: 'john', password: 'pw' } as any;

    service.register(body).subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = http.expectOne('api/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(null);
  });

  it('resendActivationCode() -> POST "api/auth/resendActivation" with param "email"', () => {
    service.resendActivationCode('a@b.c').subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = http.expectOne(
      (r) => r.url === 'api/auth/resendActivation' && r.params.get('email') === 'a@b.c'
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('activateAccount() -> POST "api/auth/activateAccount" with param "token"', () => {
    service.activateAccount(123456).subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = http.expectOne(
      (r) => r.url === 'api/auth/activateAccount' && r.params.get('token') === '123456'
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('resetPassword() -> POST "api/auth/resetPassword" with param "email"', () => {
    service.resetPassword('a@b.c').subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = http.expectOne(
      (r) => r.url === 'api/auth/resetPassword' && r.params.get('email') === 'a@b.c'
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('setNewPassword() -> POST "api/auth/setNewPassword" with param "token" and "password"', () => {
    service.setNewPassword('tok123', 'Secret!1').subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = http.expectOne(
      (r) =>
        r.url === 'api/auth/setNewPassword' &&
        r.params.get('token') === 'tok123' &&
        r.params.get('password') === 'Secret!1'
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('getUserInfo() -> GET "/api/accounts/me" and returns UserInfo', () => {
    const mockUser = { id: 1, username: 'john' } as any;

    service.getUserInfo().subscribe((u) => {
      expect(u).toEqual(mockUser);
    });

    const req = http.expectOne('/api/accounts/me');
    expect(req.request.method).toBe('GET');
    req.flush(mockUser);
  });

  it('logout() -> POST "/api/accounts/logout" without body', () => {
    service.logout().subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = http.expectOne('/api/accounts/logout');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });
});

describe('userResolver', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), SecurityService],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('returns UserInfo when 200', async () => {
    const obs = TestBed.runInInjectionContext(() =>
      userResolver({} as any, {} as any)
    ) as Observable<UserInfo | null>;

    const resultPromise = firstValueFrom(obs);

    const req = http.expectOne('/api/accounts/me');
    expect(req.request.method).toBe('GET');
    req.flush({ email: 'a@b', login: 'john' });

    const result = await resultPromise;
    expect(result).toEqual({ email: 'a@b', login: 'john' });
  });

  it('returns null when error (catchError -> of(null))', async () => {
    const obs = TestBed.runInInjectionContext(() =>
      userResolver({} as any, {} as any)
    ) as Observable<UserInfo | null>;

    const resultPromise = firstValueFrom(obs);

    const req = http.expectOne('/api/accounts/me');
    req.flush('server error', { status: 500, statusText: 'Server Error' });

    const result = await resultPromise;
    expect(result).toBeNull();
  });
});
