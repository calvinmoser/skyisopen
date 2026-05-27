import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-login-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './login-dialog.component.html'
})
export class LoginDialogComponent {
  username = '';
  password = '';

  constructor(private dialogRef: MatDialogRef<LoginDialogComponent>) {}

  submit() {
    if (this.username && this.password) {
      this.dialogRef.close({ username: this.username, password: this.password });
    }
  }

  cancel() {
    this.dialogRef.close();
  }
}