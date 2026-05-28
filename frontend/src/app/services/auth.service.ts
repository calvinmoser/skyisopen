import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private authHeader: string | null = sessionStorage.getItem('auth');

  login(username: string, password: string) {
    this.authHeader = btoa(username + ':' + password);
    sessionStorage.setItem('auth', this.authHeader);
  }

  logout() {
    this.authHeader = null;
    sessionStorage.removeItem('auth');
  }

  isAuthenticated(): boolean {
    return this.authHeader !== null;
  }

  getAuthHeader(): string | null {
    return this.authHeader ? 'Basic ' + this.authHeader : null;
  }
}
