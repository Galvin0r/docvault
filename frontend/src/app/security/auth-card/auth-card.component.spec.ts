import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Output,
  Directive,
  Input,
  TemplateRef,
  ContentChildren,
  QueryList,
  AfterContentInit,
} from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { AuthCardComponent } from './auth-card.component';

@Directive({
  selector: '[pTemplate]',
  standalone: false,
})
export class MockPTemplateDirective {
  @Input('pTemplate') name!: string;
  constructor(public templateRef: TemplateRef<any>) {}
}

@Component({
  selector: 'p-card',
  standalone: false,
  template: '<ng-container *ngTemplateOutlet="contentTemplate"></ng-container>',
})
class MockPCardComponent implements AfterContentInit {
  @ContentChildren(MockPTemplateDirective) templates!: QueryList<MockPTemplateDirective>;
  contentTemplate: TemplateRef<any> | null = null;

  ngAfterContentInit() {
    const contentTpl = this.templates.find((tpl) => tpl.name === 'content');
    if (contentTpl) {
      this.contentTemplate = contentTpl.templateRef;
    }
  }
}

@Component({
  selector: 'p-divider',
  standalone: false,
  template: '',
})
class MockPDividerComponent {}

@Component({
  selector: 'app-logo',
  standalone: false,
  template: '',
})
class MockLogoComponent {}

@Component({
  selector: 'app-google-button',
  standalone: false,
  template: '<button>Google</button>',
})
class MockGoogleButtonComponent {
  @Output() continue = new EventEmitter<void>();
}

@Component({
  standalone: false,
  template: `
    <app-auth-card
      [title]="hostTitle"
      [subtitle]="hostSubtitle"
      [showGoogle]="hostShowGoogle"
      (googleContinue)="onHostGoogleContinue()"
    >
      <div card-body>Test Body Content</div>
      <div card-footer>Test Footer Content</div>
    </app-auth-card>
  `,
})
class TestHostComponent {
  hostTitle = 'Login Title';
  hostSubtitle = '';
  hostShowGoogle = false;
  onHostGoogleContinue = jasmine.createSpy('onHostGoogleContinue');
}

describe('AuthCardComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let hostComponent: TestHostComponent;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule],
      declarations: [
        AuthCardComponent,
        TestHostComponent,
        MockPCardComponent,
        MockPTemplateDirective,
        MockPDividerComponent,
        MockLogoComponent,
        MockGoogleButtonComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    hostComponent = fixture.componentInstance;
    compiled = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('should create the component via test host', () => {
    expect(hostComponent).toBeTruthy();
    expect(compiled.querySelector('app-auth-card')).toBeTruthy();
  });

  it('should display the title input', () => {
    const h1 = compiled.querySelector('h1');
    expect(h1).toBeTruthy();
    expect(h1?.textContent).toContain('Login Title');

    hostComponent.hostTitle = 'Register Title';
    fixture.detectChanges();
    expect(h1?.textContent).toContain('Register Title');
  });

  it('should not display the subtitle <p> tag when subtitle is empty', () => {
    hostComponent.hostSubtitle = '';
    fixture.detectChanges();
    const p = compiled.querySelector('p.font-thin');
    expect(p).toBeFalsy();
  });

  it('should display the subtitle when provided', () => {
    hostComponent.hostSubtitle = 'Please enter your details';
    fixture.detectChanges();
    const p = compiled.querySelector('p.font-thin');
    expect(p).toBeTruthy();
    expect(p?.textContent).toContain('Please enter your details');
  });

  it('should apply space-y-2 class when subtitle is absent', () => {
    hostComponent.hostSubtitle = '';
    fixture.detectChanges();
    const div = compiled.querySelector('h1')?.parentElement;
    expect(div?.classList.contains('space-y-2')).toBe(true);
    expect(div?.classList.contains('space-y-1')).toBe(false);
  });

  it('should apply space-y-1 class when subtitle is present', () => {
    hostComponent.hostSubtitle = 'A subtitle';
    fixture.detectChanges();
    const div = compiled.querySelector('h1')?.parentElement;
    expect(div?.classList.contains('space-y-1')).toBe(true);
    expect(div?.classList.contains('space-y-2')).toBe(false);
  });

  it('should not show the Google button or divider by default', () => {
    expect(compiled.querySelector('p-divider')).toBeFalsy();
    expect(compiled.querySelector('app-google-button')).toBeFalsy();
  });

  it('should show the Google button and divider when showGoogle is true', () => {
    hostComponent.hostShowGoogle = true;
    fixture.detectChanges();
    expect(compiled.querySelector('p-divider')).toBeTruthy();
    expect(compiled.querySelector('app-google-button')).toBeTruthy();
  });

  it('should project content into [card-body]', () => {
    const bodyContent = compiled.querySelector('[card-body]');
    expect(bodyContent).toBeTruthy();
    expect(bodyContent?.textContent).toContain('Test Body Content');
  });

  it('should project content into [card-footer]', () => {
    const footerContent = compiled.querySelector('[card-footer]');
    expect(footerContent).toBeTruthy();
    expect(footerContent?.textContent).toContain('Test Footer Content');
  });

  it('should emit googleContinue event when app-google-button emits continue', () => {
    hostComponent.hostShowGoogle = true;
    fixture.detectChanges();

    const googleButtonDebugEl = fixture.debugElement.query(By.directive(MockGoogleButtonComponent));
    expect(googleButtonDebugEl).toBeTruthy();
    googleButtonDebugEl.triggerEventHandler('continue', null);

    expect(hostComponent.onHostGoogleContinue).toHaveBeenCalledTimes(1);
  });
});
