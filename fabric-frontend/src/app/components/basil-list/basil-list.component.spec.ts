import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BasilListComponent } from './basil-list.component';

describe('BasilListComponent', () => {
  let component: BasilListComponent;
  let fixture: ComponentFixture<BasilListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BasilListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BasilListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
