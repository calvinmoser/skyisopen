import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private username: string | null = null;
  private password: string | null = null;

  login(username: string, password: string) {
    this.username = username;
    this.password = password;
  }

  logout() {
    this.username = null;
    this.password = null;
  }

  isAuthenticated(): boolean {
    return this.username !== null;
  }

  getAuthHeader(): string | null {
    if (!this.username || !this.password) return null;
    return 'Basic ' + btoa(this.username + ':' + this.password);
  }
}