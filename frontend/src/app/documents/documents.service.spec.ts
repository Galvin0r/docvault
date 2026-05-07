import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { DocumentService } from './documents.service';

describe('DocumentService', () => {
  let service: DocumentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        DocumentService,
        { provide: MessageService, useValue: jasmine.createSpyObj<MessageService>('MessageService', ['add']) },
      ],
    });

    service = TestBed.inject(DocumentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('find should GET /api/document with params passthrough', () => {
    const response = { content: [], totalElements: 0, size: 10, number: 0 };

    service.find({ groupId: 4, ownedOnly: true, page: 1, size: 10 }).subscribe((value) => {
      expect(value).toEqual(response);
    });

    const req = http.expectOne(
      (request) =>
        request.url === '/api/document' &&
        request.params.get('groupId') === '4' &&
        request.params.get('ownedOnly') === 'true' &&
        request.params.get('page') === '1' &&
        request.params.get('size') === '10'
    );
    expect(req.request.method).toBe('GET');
    req.flush(response);
  });

  it('listAccess should GET document access entries', () => {
    const response = [
      { id: 1, documentId: 9, userId: null, userLogin: null, groupId: 4, groupName: 'Docs' },
    ];

    service.listAccess(9).subscribe((value) => {
      expect(value).toEqual(response);
    });

    const req = http.expectOne('/api/document/9/access');
    expect(req.request.method).toBe('GET');
    req.flush(response);
  });

  it('grantGroupAccess should PUT group share endpoint', () => {
    service.grantGroupAccess(9, 4).subscribe((value) => {
      expect(value).toBeNull();
    });

    const req = http.expectOne('/api/document/9/access/groups/4');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('revokeGroupAccess should DELETE group share endpoint', () => {
    service.revokeGroupAccess(9, 4).subscribe((value) => {
      expect(value).toBeNull();
    });

    const req = http.expectOne('/api/document/9/access/groups/4');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});