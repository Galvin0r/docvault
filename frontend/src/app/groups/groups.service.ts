import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of } from 'rxjs';
import { Page } from '../app.model';
import { Group, GroupMembership } from './groups.model';
import { ActivatedRouteSnapshot, ResolveFn } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class GroupService {
  constructor(private httpClient: HttpClient) {}

  find(params: Record<string, any>): Observable<Page<Group>> {
    return this.httpClient.get<Page<Group>>('/api/groups', { params: params });
  }

  create(group: Group): Observable<void> {
    return this.httpClient.post<void>('/api/groups', group);
  }

  edit(group: Group): Observable<void> {
    return this.httpClient.patch<void>(`/api/groups/${group.id}`, group);
  }

  get(id: number): Observable<Group> {
    return this.httpClient.get<Group>(`/api/groups/${id}`);
  }

  getMembers(id: number, params: Record<string, any> = {}): Observable<Page<GroupMembership>> {
    const finalParams = { page: 0, size: 10, ...params };
    return this.httpClient.get<Page<GroupMembership>>(`/api/groups/${id}/members`, {
      params: finalParams,
    });
  }

  addMember(id: number, email: string): Observable<void> {
    return this.httpClient.post<void>(`/api/groups/${id}/members`, null, {
      params: { email },
    });
  }

  getMembership(id: number): Observable<GroupMembership> {
    return this.httpClient.get<GroupMembership>(`/api/groups/${id}/members/me`);
  }

  delete(id: number): Observable<void> {
    return this.httpClient.delete<void>(`/api/groups/${id}`);
  }
}

export const groupResolver: ResolveFn<Group> = (route: ActivatedRouteSnapshot) =>
  inject(GroupService).get(route.params['id']);

export const membershipResolver: ResolveFn<GroupMembership | null> = (
  route: ActivatedRouteSnapshot
) =>
  inject(GroupService)
    .getMembership(route.params['id'])
    .pipe(catchError(() => of(null)));
