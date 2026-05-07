import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { firstValueFrom, Observable } from 'rxjs';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import {
  GroupService,
  groupResolver,
  membershipResolver,
  joinRequestResolver,
} from './groups.service';
import { Group, GroupJoinRequest, GroupMembership } from './groups.model';
import { Page } from '../app.model';

const page = <T>(content: T[] = [], total = 0, size = 0, number = 0): Page<T> => ({
  content,
  totalElements: total,
  size,
  number,
});

describe('GroupService', () => {
  let service: GroupService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), GroupService],
    });
    service = TestBed.inject(GroupService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('find -> GET /api/groups with params passthrough', () => {
    const params = { page: 2, size: 25, q: 'abc' };
    const resp: Page<Group> = page([], 0, 25, 2);
    service.find(params).subscribe((r) => expect(r).toEqual(resp));
    const req = http.expectOne(
      (r) =>
        r.url === '/api/groups' &&
        r.params.get('page') === '2' &&
        r.params.get('size') === '25' &&
        r.params.get('q') === 'abc'
    );
    expect(req.request.method).toBe('GET');
    req.flush(resp);
  });

  it('create -> POST /api/groups with body', () => {
    const g = { id: 1, name: 'X' } as Group;
    service.create(g).subscribe((v) => expect(v).toBeNull());
    const req = http.expectOne('/api/groups');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(g);
    req.flush(null);
  });

  it('edit -> PATCH /api/groups/:id with body', () => {
    const g = { id: 7, name: 'A' } as Group;
    service.edit(g).subscribe((v) => expect(v).toBeNull());
    const req = http.expectOne('/api/groups/7');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(g);
    req.flush(null);
  });

  it('get -> GET /api/groups/:id', () => {
    const mock = { id: 3, name: 'G' } as Group;
    service.get(3).subscribe((r) => expect(r).toEqual(mock));
    const req = http.expectOne('/api/groups/3');
    expect(req.request.method).toBe('GET');
    req.flush(mock);
  });

  it('getMembers -> GET /api/groups/:id/members with defaults page=0,size=10', () => {
    const resp: Page<GroupMembership> = page([], 0, 10, 0);
    service.getMembers(9).subscribe((r) => expect(r).toEqual(resp));
    const req = http.expectOne(
      (r) =>
        r.url === '/api/groups/9/members' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10'
    );
    expect(req.request.method).toBe('GET');
    req.flush(resp);
  });

  it('getMembers merges and overrides defaults when params provided', () => {
    const resp: Page<GroupMembership> = page([], 0, 50, 2);
    service
      .getMembers(9, { page: 2, size: 50, role: 'ADMIN' })
      .subscribe((r) => expect(r).toEqual(resp));
    const req = http.expectOne(
      (r) =>
        r.url === '/api/groups/9/members' &&
        r.params.get('page') === '2' &&
        r.params.get('size') === '50' &&
        r.params.get('role') === 'ADMIN'
    );
    expect(req.request.method).toBe('GET');
    req.flush(resp);
  });

  it('addMember -> POST /api/groups/:id/members with email param', () => {
    service.addMember(5, 'a@b.c').subscribe((v) => expect(v).toBeNull());
    const req = http.expectOne(
      (r) => r.url === '/api/groups/5/members' && r.params.get('email') === 'a@b.c'
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('getMembership -> GET /api/groups/:id/members/me', () => {
    const m = { userId: 1, role: 'USER' } as unknown as GroupMembership;
    service.getMembership(6).subscribe((r) => expect(r).toEqual(m));
    const req = http.expectOne('/api/groups/6/members/me');
    expect(req.request.method).toBe('GET');
    req.flush(m);
  });

  it('delete -> DELETE /api/groups/:id', () => {
    service.delete(11).subscribe((v) => expect(v).toBeNull());
    const req = http.expectOne('/api/groups/11');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('leave -> DELETE /api/groups/:id/leave', () => {
    service.leave(12).subscribe((v) => expect(v).toBeNull());
    const req = http.expectOne('/api/groups/12/leave');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('join -> POST /api/groups/:id/members/me returns membership or null', () => {
    const m = { userId: 2, role: 'USER' } as unknown as GroupMembership;
    service.join(13).subscribe((r) => expect(r).toEqual(m));
    const req1 = http.expectOne('/api/groups/13/members/me');
    expect(req1.request.method).toBe('POST');
    req1.flush(m);

    service.join(13).subscribe((r) => expect(r).toBeNull());
    const req2 = http.expectOne('/api/groups/13/members/me');
    req2.flush(null);
  });

  it('getJoinRequest -> GET /api/groups/:id/requests/me returns request or null', () => {
    const jr = { id: 1, status: 'PENDING' } as unknown as GroupJoinRequest;
    service.getJoinRequest(14).subscribe((r) => expect(r).toEqual(jr));
    const r1 = http.expectOne('/api/groups/14/requests/me');
    expect(r1.request.method).toBe('GET');
    r1.flush(jr);

    service.getJoinRequest(14).subscribe((r) => expect(r).toBeNull());
    const r2 = http.expectOne('/api/groups/14/requests/me');
    r2.flush(null);
  });

  it('changeRole -> PATCH /api/groups/:id/members/:userId/role with role param', () => {
    service.changeRole(7, 3, 'ADMIN').subscribe((v) => expect(v).toBeNull());
    const req = http.expectOne(
      (r) => r.url === '/api/groups/7/members/3/role' && r.params.get('role') === 'ADMIN'
    );
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('removeUser -> DELETE /api/groups/:id/members/:userId', () => {
    service.removeUser(7, 3).subscribe((v) => expect(v).toBeNull());
    const req = http.expectOne('/api/groups/7/members/3');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('getRequests -> GET /api/groups/:id/requests with params', () => {
    const resp: Page<GroupJoinRequest> = page([], 0, 5, 1);
    service
      .getRequests(8, { page: 1, size: 5, status: 'PENDING' })
      .subscribe((r) => expect(r).toEqual(resp));
    const req = http.expectOne(
      (r) =>
        r.url === '/api/groups/8/requests' &&
        r.params.get('page') === '1' &&
        r.params.get('size') === '5' &&
        r.params.get('status') === 'PENDING'
    );
    expect(req.request.method).toBe('GET');
    req.flush(resp);
  });

  it('acceptRequest -> POST /api/groups/requests/:id/accept', () => {
    service.acceptRequest(22).subscribe((v) => expect(v).toBeNull());
    const req = http.expectOne('/api/groups/requests/22/accept');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('rejectRequest -> POST /api/groups/requests/:id/reject', () => {
    service.rejectRequest(22).subscribe((v) => expect(v).toBeNull());
    const req = http.expectOne('/api/groups/requests/22/reject');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });
});

describe('Group resolvers', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), GroupService],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const state = {} as RouterStateSnapshot;

  it('groupResolver returns Group', async () => {
    const route = { params: { id: 5 } } as unknown as ActivatedRouteSnapshot;
    const obs = TestBed.runInInjectionContext(() =>
      groupResolver(route, state)
    ) as Observable<Group>;
    const p = firstValueFrom(obs);

    const req = http.expectOne('/api/groups/5');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 5, name: 'G' });

    const result = await p;
    expect(result).toEqual({ id: 5, name: 'G' } as any);
  });

  it('membershipResolver returns membership on 200 and null on error', async () => {
    const route = { params: { id: 9 } } as unknown as ActivatedRouteSnapshot;

    const obs1 = TestBed.runInInjectionContext(() =>
      membershipResolver(route, state)
    ) as Observable<GroupMembership | null>;
    const p1 = firstValueFrom(obs1);
    const r1 = http.expectOne('/api/groups/9/members/me');
    expect(r1.request.method).toBe('GET');
    r1.flush({ userId: 1, role: 'USER' });
    expect(await p1).toEqual({ userId: 1, role: 'USER' } as any);

    const obs2 = TestBed.runInInjectionContext(() =>
      membershipResolver(route, state)
    ) as Observable<GroupMembership | null>;
    const p2 = firstValueFrom(obs2);
    const r2 = http.expectOne('/api/groups/9/members/me');
    r2.flush('err', { status: 500, statusText: 'Server Error' });
    expect(await p2).toBeNull();
  });

  it('joinRequestResolver returns GroupJoinRequest or null', async () => {
    const route = { params: { id: 4 } } as unknown as ActivatedRouteSnapshot;

    const obs1 = TestBed.runInInjectionContext(() =>
      joinRequestResolver(route, state)
    ) as Observable<GroupJoinRequest | null>;
    const p1 = firstValueFrom(obs1);
    const r1 = http.expectOne('/api/groups/4/requests/me');
    expect(r1.request.method).toBe('GET');
    r1.flush({ id: 1, status: 'PENDING' });
    expect(await p1).toEqual({ id: 1, status: 'PENDING' } as any);

    const obs2 = TestBed.runInInjectionContext(() =>
      joinRequestResolver(route, state)
    ) as Observable<GroupJoinRequest | null>;
    const p2 = firstValueFrom(obs2);
    const r2 = http.expectOne('/api/groups/4/requests/me');
    r2.flush(null);
    expect(await p2).toBeNull();
  });
});