import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimpleDocumentComponent } from './simple-document.component';
import { DocumentService } from '../documents.service';
import { DialogService } from 'primeng/dynamicdialog';
import { of } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FileSizePipe } from '../../utils/pipes/file-size.pipe';
import { FileExtensionPipe } from '../../utils/pipes/file-extension.pipe';

describe('SimpleDocumentComponent', () => {
    let component: SimpleDocumentComponent;
    let fixture: ComponentFixture<SimpleDocumentComponent>;
    let documentService: jasmine.SpyObj<DocumentService>;
    let dialogService: jasmine.SpyObj<DialogService>;

    const mockDocument = {
        id: 1,
        title: 'Test Doc',
        originalFilename: 'test.pdf',
        mimeType: 'application/pdf',
        size: 1024,
        uploadedAt: new Date().toISOString(),
        ownerLogin: 'testuser',
        status: 'INDEXED',
        visibility: 'PUBLIC'
    };

    beforeEach(async () => {
        documentService = jasmine.createSpyObj('DocumentService', ['download', 'delete']);
        dialogService = jasmine.createSpyObj('DialogService', ['open']);

        await TestBed.configureTestingModule({
            declarations: [SimpleDocumentComponent, FileSizePipe, FileExtensionPipe],
            providers: [
                { provide: DocumentService, useValue: documentService },
                { provide: DialogService, useValue: dialogService }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        })
            .compileComponents();

        fixture = TestBed.createComponent(SimpleDocumentComponent);
        component = fixture.componentInstance;

        // Set required inputs
        fixture.componentRef.setInput('document', mockDocument);
        fixture.componentRef.setInput('currentUserLogin', 'testuser');

        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should identify owner', () => {
        expect(component.isOwner()).toBeTrue();
    });

    it('should identify non-owner', () => {
        fixture.componentRef.setInput('currentUserLogin', 'otheruser');
        fixture.detectChanges();
        expect(component.isOwner()).toBeFalse();
    });

    it('should return correct icon for PDF', () => {
        expect(component.fileIcon()).toBe('pi-file-pdf');
    });

    it('should open info dialog', () => {
        component.openInfo();
        expect(dialogService.open).toHaveBeenCalledWith(jasmine.any(Function), jasmine.objectContaining({
            header: 'Document Information'
        }));
    });

    it('should call download service', () => {
        documentService.download.and.returnValue(of('http://download.url'));
        spyOn(window, 'open');

        component.download();

        expect(documentService.download).toHaveBeenCalledWith(1);
        expect(window.open).toHaveBeenCalledWith('http://download.url', '_blank');
    });

    it('should call delete service and emit deleted event', () => {
        documentService.delete.and.returnValue(of(void 0));
        spyOn(component.deleted, 'emit');

        component.delete();

        expect(documentService.delete).toHaveBeenCalledWith(1);
        expect(component.deleted.emit).toHaveBeenCalledWith(1);
        expect(component.deleting()).toBeFalse();
    });
});
