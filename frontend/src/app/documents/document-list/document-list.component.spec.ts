import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DocumentListComponent } from './document-list.component';
import { DocumentService } from '../documents.service';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('DocumentListComponent', () => {
    let component: DocumentListComponent;
    let fixture: ComponentFixture<DocumentListComponent>;
    let documentService: jasmine.SpyObj<DocumentService>;
    const fb = new FormBuilder();

    beforeEach(async () => {
        documentService = jasmine.createSpyObj('DocumentService', ['find']);
        documentService.find.and.returnValue(of({ content: [], totalElements: 0, size: 10, number: 0 }));

        await TestBed.configureTestingModule({
            declarations: [DocumentListComponent],
            imports: [ReactiveFormsModule],
            providers: [
                { provide: DocumentService, useValue: documentService }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        })
            .compileComponents();

        fixture = TestBed.createComponent(DocumentListComponent);
        component = fixture.componentInstance;

        fixture.componentRef.setInput('filtersForm', fb.group({}));
        fixture.componentRef.setInput('currentUserLogin', 'testuser');

        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should initialize searchStore onInit', () => {
        expect(component.searchStore).toBeDefined();
    });

    it('should update page onPageChange', () => {
        component.onPageChange(2);
        expect(component.page()).toBe(2);
    });

    it('should update size onSizeChange', () => {
        component.onSizeChange(20);
        expect(component.size()).toBe(20);
    });
});