import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { MessageService } from 'primeng/api';
import { SelectButtonModule } from 'primeng/selectbutton';
import { DocumentViewComponent } from './document-view.component';
import { DocumentService } from '../documents.service';
import { FileSizePipe } from '../../utils/pipes/file-size.pipe';
import { GroupService } from '../../groups/groups.service';

describe('DocumentViewComponent', () => {
  let component: DocumentViewComponent;
  let fixture: ComponentFixture<DocumentViewComponent>;
  let documentService: jasmine.SpyObj<DocumentService>;
  let groupService: jasmine.SpyObj<GroupService>;
  let router: jasmine.SpyObj<Router>;
  let messageService: jasmine.SpyObj<MessageService>;

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
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    messageService = jasmine.createSpyObj<MessageService>('MessageService', ['add']);

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
      imports: [ReactiveFormsModule, SelectButtonModule],
      providers: [
        { provide: DocumentService, useValue: documentService },
        { provide: GroupService, useValue: groupService },
        { provide: Router, useValue: router },
        { provide: MessageService, useValue: messageService },
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

  it('should save changed title and visibility', () => {
    documentService.updateTitle.and.returnValue(of({} as any));
    documentService.updateVisibility.and.returnValue(of({} as any));

    component.openEditDialog();
    component.editForm.setValue({ title: 'Updated doc', visibility: 'PRIVATE' });
    component.saveDetails();

    expect(documentService.updateTitle).toHaveBeenCalledWith(7, 'Updated doc');
    expect(documentService.updateVisibility).toHaveBeenCalledWith(7, 'PRIVATE');
    expect(messageService.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    expect(component.editDialogVisible()).toBeFalse();
    expect(component.saving()).toBeFalse();
  });

  it('should not save unchanged details', () => {
    component.openEditDialog();
    component.saveDetails();

    expect(documentService.updateTitle).not.toHaveBeenCalled();
    expect(documentService.updateVisibility).not.toHaveBeenCalled();
  });

  it('should share with a user and refresh access', () => {
    documentService.grantUserAccessByLogin.and.returnValue(of({} as any));
    documentService.listAccess.calls.reset();
    component.shareForm.patchValue({ userLogin: '  anna  ' });

    component.shareWithUser();

    expect(documentService.grantUserAccessByLogin).toHaveBeenCalledWith(7, 'anna');
    expect(component.shareForm.controls.userLogin.value).toBe('');
    expect(documentService.listAccess).toHaveBeenCalledWith(7);
    expect(component.sharingUser()).toBeFalse();
  });

  it('should share with an unshared group', () => {
    documentService.grantGroupAccess.and.returnValue(of({} as any));
    documentService.listAccess.calls.reset();

    component.shareWithGroup(42);

    expect(documentService.grantGroupAccess).toHaveBeenCalledWith(7, 42);
    expect(component.groupShareDialogVisible()).toBeFalse();
    expect(documentService.listAccess).toHaveBeenCalledWith(7);
    expect(component.sharingGroup()).toBeFalse();
  });

  it('should not share with a group that already has access', () => {
    component.accessEntries.set([{ id: 1, groupId: 42, groupName: 'Team' } as any]);

    component.shareWithGroup(42);

    expect(documentService.grantGroupAccess).not.toHaveBeenCalled();
    expect(component.isGroupShared(42)).toBeTrue();
    expect(component.isGroupShared(null)).toBeFalse();
  });

  it('should search owner groups and react to pagination changes', () => {
    groupService.findMemberships.and.returnValue(of({
      content: [{ id: 5, groupId: 9, groupName: 'Editors' }],
      totalElements: 1,
    } as any));
    component.shareForm.patchValue({ groupName: ' edit ' });

    component.searchGroups(2);
    component.onGroupPageChange(3);
    component.onGroupPageChange(3);
    component.onGroupSizeChange(10);
    component.onGroupSizeChange(10);

    expect(groupService.findMemberships).toHaveBeenCalledWith(jasmine.objectContaining({
      userLogin: 'roman',
      groupName: 'edit',
      page: 1,
      size: 5,
    }));
    expect(component.groupMemberships()).toEqual([{ id: 5, groupId: 9, groupName: 'Editors' } as any]);
    expect(component.groupTotal()).toBe(1);
    expect(groupService.findMemberships).toHaveBeenCalledTimes(3);
  });

  it('should revoke access and navigate for document actions', () => {
    documentService.revokeUserAccess.and.returnValue(of({} as any));
    documentService.revokeGroupAccess.and.returnValue(of({} as any));
    documentService.download.and.returnValue(of('/download/doc'));
    documentService.delete.and.returnValue(of({} as any));
    spyOn(window, 'open');
    documentService.listAccess.calls.reset();

    component.revokeUser(8);
    component.revokeGroup(9);
    component.download();
    component.deleteDocument();
    component.openOwner();

    expect(documentService.revokeUserAccess).toHaveBeenCalledWith(7, 8);
    expect(documentService.revokeGroupAccess).toHaveBeenCalledWith(7, 9);
    expect(documentService.listAccess).toHaveBeenCalledTimes(2);
    expect(window.open).toHaveBeenCalledWith('/download/doc', '_blank');
    expect(router.navigate).toHaveBeenCalledWith(['/']);
    expect(router.navigate).toHaveBeenCalledWith(['/user', 'roman']);
    expect(component.deleting()).toBeFalse();
  });

  it('should classify common document file kinds', () => {
    expect(component.fileKind({ mimeType: 'application/pdf' } as any)).toBe('PDF');
    expect(component.fileKind({ originalFilename: 'notes.docx' } as any)).toBe('DOC');
    expect(component.fileKind({ mimeType: 'text/plain' } as any)).toBe('TXT');
    expect(component.fileKind({ originalFilename: 'book.epub' } as any)).toBe('EPUB');
    expect(component.fileKind({ originalFilename: 'archive.bin' } as any)).toBe('FILE');
    expect(component.fileKindClass({ originalFilename: 'book.epub' } as any)).toBe('kind-epub');
  });
});