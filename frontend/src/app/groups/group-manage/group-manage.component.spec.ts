import { CommonModule } from '@angular/common';
import { Component, Directive, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, of } from 'rxjs';
import { GroupManageComponent } from './group-manage.component';
import { GroupAddComponent } from '../group-add/group-add.component';
import { GroupService } from '../groups.service';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';

@Component({
  selector: 'p-card',
  standalone: true,
  template: `<ng-content></ng-content>`,
})
class PCardStub {}

@Component({
  selector: 'p-floatlabel',
  standalone: true,
  template: `<ng-content></ng-content>`,
})
class PFloatLabelStub {
  @Input() variant?: string;
}

@Component({
  selector: 'p-divider',
  standalone: true,
  template: ``,
})
class PDividerStub {}

@Directive({
  selector: '[pInputText]',
  standalone: true,
})
class PInputTextStub {}

@Directive({
  selector: '[pButton]',
  standalone: true,
})
class PButtonStub {
  @Input() icon?: string;
  @Input() label?: string;
  @Input() class?: string;
  @Input() type?: string;
}

@Component({
  selector: 'app-group-list',
  standalone: true,
  template: `<div class="group-list"></div>`,
})
class GroupListStub {
  @Input() filtersForm: any;
  refresh = jasmine.createSpy('refresh');
}

class GroupServiceStub {
  create = jasmine.createSpy('create').and.returnValue(of(123));
}

class RefStub {
  onClose = new Subject<any>();
  close = jasmine.createSpy('close');
}

class DialogServiceStub {
  ref = new RefStub();
  openCalls: any[] = [];
  open(comp: any, cfg?: any): DynamicDialogRef {
    this.openCalls.push({ comp, cfg });
    return this.ref as unknown as DynamicDialogRef;
  }
}

class RouterStub {
  navigate = jasmine.createSpy('navigate');
}

describe('GroupManageComponent', () => {
  let fixture: ComponentFixture<GroupManageComponent>;
  let component: GroupManageComponent;
  let dialog: DialogServiceStub;
  let groups: GroupServiceStub;
  let router: RouterStub;

  function create() {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        ReactiveFormsModule,
        PCardStub,
        PFloatLabelStub,
        PDividerStub,
        PInputTextStub,
        PButtonStub,
        GroupListStub,
      ],
      declarations: [GroupManageComponent],
      providers: [
        FormBuilder,
        { provide: GroupService, useClass: GroupServiceStub },
        { provide: DialogService, useClass: DialogServiceStub },
        { provide: Router, useClass: RouterStub },
        provideRouter([]),
      ],
    });

    fixture = TestBed.createComponent(GroupManageComponent);
    component = fixture.componentInstance;
    dialog = TestBed.inject(DialogService) as any;
    groups = TestBed.inject(GroupService) as any;
    router = TestBed.inject(Router) as any;

    fixture.detectChanges();
  }

  it('opens dialog on button click with correct config', () => {
    create();
    const btn = fixture.debugElement.query(By.css('button[pButton]')).nativeElement as HTMLButtonElement;
    btn.click();
    expect(dialog.openCalls.length).toBe(1);
    expect(dialog.openCalls[0].comp).toBe(GroupAddComponent);
    expect(dialog.openCalls[0].cfg.header).toBe('Create group');
    expect(dialog.openCalls[0].cfg.modal).toBeTrue();
    expect(dialog.openCalls[0].cfg.styleClass).toBe('group-dialog');
  });

  it('after dialog close creates group and navigates to edit', () => {
    create();
    component.onAddGroup();
    const payload = { id: null, name: 'G', description: 'D', visibility: 'PUBLIC' };
    dialog.ref.onClose.next(payload);
    expect(groups.create).toHaveBeenCalledWith(payload);
    expect(router.navigate).toHaveBeenCalledWith(['/groups/edit/', 123]);
  });


});