import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { MessageService } from 'primeng/api';
import { DocumentViewComponent } from './document-view.component';
import { DocumentService } from '../documents.service';
import { FileSizePipe } from '../../utils/pipes/file-size.pipe';
import { GroupService } from '../../groups/groups.service';

describe('DocumentViewComponent', () => {
  let component: DocumentViewComponent;
  let fixture: ComponentFixture<DocumentViewComponent>;
  let documentService: jasmine.SpyObj<DocumentService>;
  let groupService: jasmine.SpyObj<GroupService>;

  beforeEach(async () => {
    documentService = jasmine.createSpyObj<DocumentService>('DocumentService', [
      'get',
      'getFragments',
      'listAccess',
      'updateTitle',
      'updateVisibility',
      'grantUserAccessByLogin',
      'grantGroupAccess',
      'revokeUserAccess',
      'revokeGroupAccess',
      'download',
      'delete',
    ]);
    groupService = jasmine.createSpyObj<GroupService>('GroupService', ['findMemberships']);

    documentService.get.and.returnValue(of({
      id: 7,
      title: 'Doc',
      description: null,
      originalFilename: 'doc.txt',
      mimeType: 'text/plain',
      uploadedAt: new Date().toISOString(),
      visibility: 'PUBLIC',
      ownerId: 1,
      ownerLogin: 'roman',
      size: 1200,
      status: 'INDEXED'
    }));
    documentService.getFragments.and.returnValue(of([{ fragmentOrder: 1, content: 'First fragment' }]));
    documentService.listAccess.and.returnValue(of([]));
    groupService.findMemberships.and.returnValue(of({ content: [], totalElements: 0 } as any));

    await TestBed.configureTestingModule({
      declarations: [DocumentViewComponent, FileSizePipe],
      imports: [ReactiveFormsModule],
      providers: [
        { provide: DocumentService, useValue: documentService },
        { provide: GroupService, useValue: groupService },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
        { provide: MessageService, useValue: jasmine.createSpyObj<MessageService>('MessageService', ['add']) },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ id: '7' })),
            parent: { data: of({ userInfo: { id: 1, login: 'roman', email: 'r@d', created: '' } }) }
          }
        },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load the document and fragments', () => {
    expect(component.document()?.id).toBe(7);
    expect(component.fragments().length).toBe(1);
    expect(documentService.get).toHaveBeenCalledWith(7);
    expect(documentService.getFragments).toHaveBeenCalledWith(7, 2);
  });
});