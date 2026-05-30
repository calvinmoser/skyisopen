import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

interface PlaneEntry {
  file: string;
  length: number;
  facing: 'left' | 'right';
}

@Injectable({ providedIn: 'root' })
export class PlaneService {

  private static readonly ANCHOR_LENGTH = 73.9; // B77L — always maps to MAX_DISPLAY_PX
  private static readonly MAX_DISPLAY_PX = 400;

  private table: Record<string, PlaneEntry> = {};
  private ready: Promise<void>;

  constructor(private http: HttpClient) {
    this.ready = firstValueFrom(this.http.get<Record<string, PlaneEntry>>('/assets/planes/planes.json'))
      .then(data => { this.table = data; });
  }

  async getImage(codeshares: string[], operator: string, aircraftType: string): Promise<string> {
    await this.ready;
    const key = this.key(codeshares, operator, aircraftType);
    const entry = this.table[key];
    if (entry) return `/assets/planes/${entry.file}`;
    const type = aircraftType?.trim().toUpperCase();
    if (type) return `/assets/planes/${type}.png`;
    return '/assets/plane.svg';
  }

  async shouldFlip(codeshares: string[], operator: string, aircraftType: string): Promise<boolean> {
    await this.ready;
    const entry = this.table[this.key(codeshares, operator, aircraftType)];
    return entry?.facing === 'right';
  }

  async getWidth(codeshares: string[], operator: string, aircraftType: string): Promise<string> {
    await this.ready;
    const key = this.key(codeshares, operator, aircraftType);
    const entry = this.table[key];
    const length = entry?.length ?? PlaneService.ANCHOR_LENGTH;
    const px = Math.round(length / PlaneService.ANCHOR_LENGTH * PlaneService.MAX_DISPLAY_PX);
    return `${px}px`;
  }

  private key(codeshares: string[], operator: string, aircraftType: string): string {
    const type = aircraftType?.trim().toUpperCase();
    for (const cs of (codeshares || [])) {
      const marketed = cs.substring(0, 3).toUpperCase();
      const k = `${marketed}-${type}`;
      if (this.table[k]) return k;
    }
    const op = operator?.trim().toUpperCase();
    if (op && op !== '???') return `${op}-${type}`;
    return type;
  }
}
