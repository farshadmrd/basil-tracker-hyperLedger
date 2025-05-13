import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BasilService } from '../../services/basil.service';

@Component({
  selector: 'app-basil-list',
  templateUrl: './basil-list.component.html',
  styleUrls: ['./basil-list.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class BasilListComponent {
  selectedBasil: any = null;
  error: string = '';
  success: string = '';

  // Form data
  newBasil = {
    id: '',
    country: ''
  };

  searchBasil = {
    id: ''
  };

  updateState = {
    gps: '',
    temp: '',
    humidity: '',
    status: ''
  };

  transfer = {
    newOrgId: '',
    newName: ''
  };

  constructor(private basilService: BasilService) {}

  createBasil(): void {
    if (!this.newBasil.id || !this.newBasil.country) {
      this.error = 'Please fill in all fields';
      return;
    }

    this.basilService.createBasil(this.newBasil.id, this.newBasil.country).subscribe({
      next: (response) => {
        this.success = 'Basil created successfully';
        this.error = '';
        this.newBasil = { id: '', country: '' };
      },
      error: (err) => {
        this.error = 'Error creating basil: ' + err.message;
      }
    });
  }

  searchBasilById(): void {
    if (!this.searchBasil.id) {
      this.error = 'Please enter a basil ID';
      return;
    }

    this.basilService.getBasil(this.searchBasil.id).subscribe({
      next: (response) => {
        try {
          this.selectedBasil = response;
          // Sort transport history by timestamp in descending order (newest first)
          if (this.selectedBasil.transportHistory) {
            this.selectedBasil.transportHistory.sort((a: any, b: any) => b.timestamp - a.timestamp);
          }
          this.error = '';
        } catch (e) {
          this.error = 'Error parsing response';
        }
      },
      error: (err) => {
        if (err.status === 404) {
          this.error = `No basil found with ID: ${this.searchBasil.id}`;
          this.selectedBasil = null;
        } else {
          this.error = 'Error getting basil: ' + err.message;
        }
      }
    });
  }

  deleteBasil(id: string): void {
    if (confirm('Are you sure you want to delete this basil?')) {
      this.basilService.deleteBasil(id).subscribe({
        next: (response) => {
          this.success = 'Basil deleted successfully';
          this.error = '';
          this.selectedBasil = null;
        },
        error: (err) => {
          this.error = 'Error deleting basil: ' + err.message;
        }
      });
    }
  }

  updateBasilState(id: string): void {
    if (!this.updateState.gps || !this.updateState.temp || !this.updateState.humidity || !this.updateState.status) {
      this.error = 'Please fill in all fields';
      return;
    }

    const timestamp = Math.floor(Date.now() / 1000);
    this.basilService.updateBasilState(
      id,
      this.updateState.gps,
      timestamp,
      this.updateState.temp,
      this.updateState.humidity,
      this.updateState.status
    ).subscribe({
      next: (response) => {
        this.success = 'Basil state updated successfully';
        this.error = '';
        this.updateState = { gps: '', temp: '', humidity: '', status: '' };
        this.searchBasilById();
      },
      error: (err) => {
        this.error = 'Error updating basil state: ' + err.message;
      }
    });
  }

  transferBasilOwnership(id: string): void {
    if (!this.transfer.newOrgId || !this.transfer.newName) {
      this.error = 'Please fill in all fields';
      return;
    }

    this.basilService.transferBasilOwnership(id, this.transfer.newOrgId, this.transfer.newName).subscribe({
      next: (response) => {
        this.success = 'Basil ownership transferred successfully';
        this.error = '';
        this.transfer = { newOrgId: '', newName: '' };
        this.searchBasilById();
      },
      error: (err) => {
        this.error = 'Error transferring basil ownership: ' + err.message;
      }
    });
  }

  clearMessages(): void {
    this.error = '';
    this.success = '';
  }
}
