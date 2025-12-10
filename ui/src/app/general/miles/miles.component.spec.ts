/* tslint:disable:no-unused-variable */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { MilesComponent } from './miles.component';

describe('MilesComponent', () => {
  let component: MilesComponent;
  let fixture: ComponentFixture<MilesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MilesComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MilesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
