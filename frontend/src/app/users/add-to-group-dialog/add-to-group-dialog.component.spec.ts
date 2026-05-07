import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AddToGroupDialogComponent } from './add-to-group-dialog.component';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { GroupService } from '../../groups/groups.service';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

describe('AddToGroupDialogComponent', () => {
    let component: AddToGroupDialogComponent;
    let fixture: ComponentFixture<AddToGroupDialogComponent>;
    let dialogRef: jasmine.SpyObj<DynamicDialogRef>;
    let groupService: jasmine.SpyObj<GroupService>;
    let messageService: jasmine.SpyObj<MessageService>;

    beforeEach(async () => {
        dialogRef = jasmine.createSpyObj('DynamicDialogRef', ['close']);
        groupService = jasmine.createSpyObj('GroupService', ['findMemberships', 'addMember']);
        messageService = jasmine.createSpyObj('MessageService', ['add']);

        groupService.findMemberships.and.returnValue(of({
            content: [
                { id: 1, groupId: 10, groupName: 'Test Group', role: 'OWNER', userId: 1, userLogin: 'admin', created: '', groupVisibility: 'PUBLIC' },
                { id: 2, groupId: 20, groupName: 'Another Group', role: 'USER', userId: 1, userLogin: 'admin', created: '', groupVisibility: 'PRIVATE' }
            ],
            totalElements: 2,
            totalPages: 1,
            size: 10,
            number: 0
        }));

        await TestBed.configureTestingModule({
            declarations: [AddToGroupDialogComponent],
            imports: [ReactiveFormsModule],
            providers: [
                { provide: DynamicDialogRef, useValue: dialogRef },
                { provide: DynamicDialogConfig, useValue: { data: { email: 'test@example.com', login: 'testuser', currentUserLogin: 'admin' } } },
                { provide: GroupService, useValue: groupService },
                { provide: MessageService, useValue: messageService }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        }).compileComponents();

        fixture = TestBed.createComponent(AddToGroupDialogComponent);
        component = fixture.componentInstance;
        component.ngOnInit();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load only OWNER and ADMIN groups on init', () => {
        expect(groupService.findMemberships).toHaveBeenCalled();
        expect(component.groups().length).toBe(1);
        expect(component.groups()[0].groupName).toBe('Test Group');
    });

    it('should set user email and login from dialog config', () => {
        expect(component.userEmail).toBe('test@example.com');
        expect(component.userLogin).toBe('testuser');
    });

    it('should close dialog on cancel', () => {
        component.onCancel();
        expect(dialogRef.close).toHaveBeenCalledWith(false);
    });

    it('should not submit if no group selected', () => {
        component.onSubmit();
        expect(groupService.addMember).not.toHaveBeenCalled();
    });

    it('should call addMember on submit with valid selection', () => {
        groupService.addMember.and.returnValue(of(void 0));
        component.selectedGroup.setValue(component.groups()[0]);

        component.onSubmit();

        expect(groupService.addMember).toHaveBeenCalledWith(10, 'test@example.com');
    });
});