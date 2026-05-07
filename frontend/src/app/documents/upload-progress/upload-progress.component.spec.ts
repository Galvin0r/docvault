import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UploadProgressComponent } from './upload-progress.component';
import { DocumentService } from '../documents.service';
import { signal } from '@angular/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('UploadProgressComponent', () => {
    let component: UploadProgressComponent;
    let fixture: ComponentFixture<UploadProgressComponent>;
    let documentService: jasmine.SpyObj<DocumentService>;

    const mockUploads = signal([
        { id: '1', filename: 'test.txt', progress: 50, state: 'UPLOADING' },
        { id: '2', filename: 'done.txt', progress: 100, state: 'DONE' }
    ]);

    beforeEach(async () => {
        documentService = jasmine.createSpyObj('DocumentService', ['cancelUpload', 'removeUpload']);
        (documentService as any).uploads = mockUploads;

        await TestBed.configureTestingModule({
            declarations: [UploadProgressComponent],
            providers: [
                { provide: DocumentService, useValue: documentService }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        })
            .compileComponents();

        fixture = TestBed.createComponent(UploadProgressComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should toggle minimize', () => {
        expect(component.isMinimized()).toBeFalse();
        component.toggleMinimize();
        expect(component.isMinimized()).toBeTrue();
    });

    it('should call cancelUpload', () => {
        const event = new MouseEvent('click');
        spyOn(event, 'stopPropagation');

        component.cancelUpload('1', event);

        expect(documentService.cancelUpload).toHaveBeenCalledWith('1');
        expect(event.stopPropagation).toHaveBeenCalled();
    });

    it('should identify completed uploads', () => {
        expect(component.hasCompletedUploads()).toBeTrue();
    });

    it('should clear all completed uploads', () => {
        component.clearAll();
        expect(documentService.removeUpload).toHaveBeenCalledWith('2');
        expect(documentService.removeUpload).not.toHaveBeenCalledWith('1');
    });
});