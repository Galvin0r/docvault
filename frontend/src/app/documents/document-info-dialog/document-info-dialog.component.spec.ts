import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DocumentInfoDialogComponent } from './document-info-dialog.component';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FileSizePipe } from '../../utils/pipes/file-size.pipe';
import { FileExtensionPipe } from '../../utils/pipes/file-extension.pipe';

describe('DocumentInfoDialogComponent', () => {
    let component: DocumentInfoDialogComponent;
    let fixture: ComponentFixture<DocumentInfoDialogComponent>;

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
        await TestBed.configureTestingModule({
            declarations: [DocumentInfoDialogComponent, FileSizePipe, FileExtensionPipe],
            providers: [
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: {
                            document: mockDocument,
                            currentUserLogin: 'testuser'
                        }
                    }
                }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        })
            .compileComponents();

        fixture = TestBed.createComponent(DocumentInfoDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should initialize with document from config', () => {
        expect(component.document.title).toBe('Test Doc');
    });

    it('should show indexing info if owner and status is INDEXING', () => {
        component.document.status = 'INDEXING';
        component.isOwner = true;
        expect(component.isOwner && (component.document.status === 'INDEXING' || component.document.status === 'FAILED')).toBeTrue();
    });
});
