import { TestBed } from '@angular/core/testing';

import { AeroAPIServiceService } from './aero-apiservice.service';

describe('AeroAPIServiceService', () => {
  let service: AeroAPIServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AeroAPIServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
