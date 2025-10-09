import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { Page } from "../app.model";
import { Group } from "./groups.model";

@Injectable({ providedIn: 'root' })
export class GroupService {
  constructor(private httpClient: HttpClient) {}

  find(params: Record<string, any>): Observable<Page<Group>> {
    return this.httpClient.get<Page<Group>>('/api/groups', { params: params });
  }

  create(group: Group): Observable<void> {
    return this.httpClient.post<void>('/api/groups', group);
  }
}