import { Component, forwardRef, NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { DocumentService } from '../../documents/documents.service';
import { GroupAddDocumentDialogComponent } from './group-add-document-dialog.component';
import { DocumentDto } from '../../documents/documents.model';

@Component({
  selector: 'p-datepicker',
  standalone: false,
  template: '',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatePickerStubComponent),
      multi: true,
    },
  ],
})
class DatePickerStubComponent implements ControlValueAccessor {
  writeValue(): void {}
  registerOnChange(): void {}
  registerOnTouched(): void {}
  setDisabledState(): void {}
}

@Pipe({ name: 'fileSize', standalone: false })
class FileSizePipeStub implements PipeTransform {
  transform(value: number): string {
    return `${value} B`;
  }
}

describe('GroupAddDocumentDialogComponent', () => {
  let component: GroupAddDocumentDialogComponent;
  let fixture: ComponentFixture<GroupAddDocumentDialogComponent>;
  let documentService: jasmine.SpyObj<DocumentService>;
  let messageService: jasmine.SpyObj<MessageService>;
  let dialogRef: jasmine.SpyObj<DynamicDialogRef>;
  let onDocumentLinked: jasmine.Spy;

  const documents: DocumentDto[] = [
    {
      id: 1,
      title: 'Already Shared',
      description: null,
      originalFilename: 'shared.pdf',
      mimeType: 'application/pdf',
      uploadedAt: '2026-04-19T00:00:00Z',
      visibility: 'PRIVATE',
      ownerId: 10,
      ownerLogin: 'roman',
      size: 500,
      status: 'UPLOADED',
    },
    {
      id: 2,
      title: 'Fresh Document',
      description: null,
      originalFilename: 'fresh.docx',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      uploadedAt: '2026-04-18T00:00:00Z',
      visibility: 'PRIVATE',
      ownerId: 10,
      ownerLogin: 'roman',
      size: 900,
      status: 'UPLOADED',
    },
  ];

  beforeEach(async () => {
    documentService = jasmine.createSpyObj<DocumentService>('DocumentService', [
      'find',
      'listAccess',
      'grantGroupAccess',
    ]);
    messageService = jasmine.createSpyObj<MessageService>('MessageService', ['add']);
    dialogRef = jasmine.createSpyObj<DynamicDialogRef>('DynamicDialogRef', ['close']);
    onDocumentLinked = jasmine.createSpy('onDocumentLinked');

    documentService.find.and.returnValue(
      of({
        content: documents,
        totalElements: documents.length,
        size: 10,
        number: 0,
      })
    );
    documentService.listAccess.and.callFake((documentId: number) =>
      of(
        documentId === 1
          ? [{ id: 1, documentId: 1, userId: null, userLogin: null, groupId: 44, groupName: 'Docs' }]
          : []
      )
    );
    documentService.grantGroupAccess.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      declarations: [GroupAddDocumentDialogComponent, DatePickerStubComponent, FileSizePipeStub],
      imports: [ReactiveFormsModule],
      providers: [
        { provide: DocumentService, useValue: documentService },
        { provide: MessageService, useValue: messageService },
        { provide: DynamicDialogRef, useValue: dialogRef },
        {
          provide: DynamicDialogConfig,
          useValue: {
            data: {
              groupId: 44,
              groupName: 'Docs',
              onDocumentLinked,
            },
          },
        },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(GroupAddDocumentDialogComponent);
    component = fixture.componentInstance;
  });

  function init(): void {
    fixture.detectChanges();
    tick(250);
    fixture.detectChanges();
  }

  it('should create and load owned documents for selection', fakeAsync(() => {
    init();

    expect(component).toBeTruthy();
    expect(documentService.find).toHaveBeenCalledWith(
      jasmine.objectContaining({
        ownedOnly: true,
        page: 0,
        size: 10,
      })
    );
    expect(documentService.listAccess).toHaveBeenCalledWith(1);
    expect(documentService.listAccess).toHaveBeenCalledWith(2);
    expect((component as any).isAlreadyAdded(1)).toBeTrue();
    expect((component as any).isAlreadyAdded(2)).toBeFalse();
  }));

  it('should add a document immediately and keep the dialog open', fakeAsync(() => {
    init();

    (component as any).addDocument(documents[1]);

    expect(documentService.grantGroupAccess).toHaveBeenCalledWith(2, 44);
    expect(onDocumentLinked).toHaveBeenCalled();
    expect(messageService.add).toHaveBeenCalledWith(
      jasmine.objectContaining({
        severity: 'success',
        summary: 'Document added',
      })
    );
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect((component as any).isAlreadyAdded(2)).toBeTrue();
  }));

  it('should not add a document already shared with the group', fakeAsync(() => {
    init();

    (component as any).addDocument(documents[0]);

    expect(documentService.grantGroupAccess).not.toHaveBeenCalled();
  }));

  it('clearFilters should keep ownedOnly enabled', fakeAsync(() => {
    init();

    component['form'].patchValue({
      titleSearch: 'Report',
      dateFrom: new Date('2026-04-01T00:00:00Z'),
      dateTo: new Date('2026-04-10T00:00:00Z'),
    });

    (component as any).clearFilters();

    expect(component['form'].getRawValue()).toEqual({
      titleSearch: null,
      dateFrom: null,
      dateTo: null,
      ownedOnly: true,
    });
  }));

  it('close should close the dialog', fakeAsync(() => {
    init();

    (component as any).close();

    expect(dialogRef.close).toHaveBeenCalled();
  }));
});