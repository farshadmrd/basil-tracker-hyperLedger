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

  // Organization selection
  organizations = [
    { id: 'Org1MSP', name: 'Greenhouse', disabled: false },
    { id: 'Org2MSP', name: 'Supermarket', disabled: true }
  ];
  selectedOrganization = this.organizations[0]; // Default to Greenhouse

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
    selectedOrganization: null as any
  };

  constructor(private basilService: BasilService) {}

  // Check if current organization can create new basil (only Org1 can always create)
  get canCreate(): boolean {
    // Org1 (Greenhouse) can always create new basil plants
    return this.selectedOrganization.id === 'Org1MSP';
  }

  // Check if current organization has permission for write operations (update/transfer)
  get canWrite(): boolean {
    if (!this.selectedBasil) {
      return false;
    }
    
    // Org2 (disabled) can never modify
    if (this.selectedOrganization.disabled) {
      return false;
    }
    
    // Org1 can only modify if they own the basil
    return this.selectedBasil.currentOwner?.orgId === this.selectedOrganization.id;
  }

  // Check if current organization can delete the selected basil (owner can delete)
  get canDelete(): boolean {
    if (!this.selectedBasil) {
      return false;
    }
    
    // Both organizations can only delete if they own the basil
    return this.selectedBasil.currentOwner?.orgId === this.selectedOrganization.id;
  }

  // Handle organization change
  onOrganizationChange(): void {
    this.clearMessages();
    if (this.selectedOrganization.disabled) {
      this.error = `${this.selectedOrganization.name} organization has read-only access. Create, Update, Delete, and Transfer operations are disabled.`;
    } else {
      // Clear any previous error messages when switching to Org1
      this.error = '';
    }
  }

  createBasil(): void {
    if (!this.canCreate) {
      this.error = 'Operation not permitted for the supermarket organization.';
      return;
    }

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
    if (!this.canDelete) {
      this.error = 'You can only delete basils that you own.';
      return;
    }

    if (confirm('Are you sure you want to delete this basil?')) {
      this.basilService.deleteBasil(id).subscribe({
        next: (response) => {
          console.log('Delete response:', response);
          this.success = response || 'Basil deleted successfully';
          this.error = '';
          this.selectedBasil = null;
        },
        error: (err) => {
          console.error('Delete error details:', err);
          let errorMessage = 'Error deleting basil';
          
          if (err.error && typeof err.error === 'string') {
            errorMessage += ': ' + err.error;
          } else if (err.message) {
            errorMessage += ': ' + err.message;
          } else if (err.status) {
            errorMessage += ': HTTP ' + err.status;
          }
          
          this.error = errorMessage;
        }
      });
    }
  }

  updateBasilState(id: string): void {
    if (!this.canWrite) {
      this.error = 'You can only modify basils that you own.';
      return;
    }

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
        console.log('Update response:', response);
        this.success = response || 'Basil state updated successfully';
        this.error = '';
        this.updateState = { gps: '', temp: '', humidity: '', status: '' };
        this.searchBasilById();
      },
      error: (err) => {
        console.error('Update error details:', err);
        let errorMessage = 'Error updating basil state';
        
        if (err.error && typeof err.error === 'string') {
          errorMessage += ': ' + err.error;
        } else if (err.message) {
          errorMessage += ': ' + err.message;
        } else if (err.status) {
          errorMessage += ': HTTP ' + err.status;
        }
        
        this.error = errorMessage;
      }
    });
  }

  transferBasilOwnership(id: string): void {
    if (!this.canWrite) {
      this.error = 'You can only transfer ownership of basils that you own.';
      return;
    }

    if (!this.transfer.selectedOrganization) {
      this.error = 'Please select an organization';
      return;
    }

    this.basilService.transferBasilOwnership(id, this.transfer.selectedOrganization.id, this.transfer.selectedOrganization.name).subscribe({
      next: (response) => {
        this.success = 'Basil ownership transferred successfully';
        this.error = '';
        this.transfer = { selectedOrganization: null };
        this.searchBasilById();
      },
      error: (err) => {
        console.error('Transfer error details:', err);
        let errorMessage = 'Error transferring basil ownership';
        
        if (err.error && typeof err.error === 'string') {
          errorMessage += ': ' + err.error;
        } else if (err.message) {
          errorMessage += ': ' + err.message;
        } else if (err.status) {
          errorMessage += ': HTTP ' + err.status;
        }
        
        this.error = errorMessage;
      }
    });
  }

  clearMessages(): void {
    this.error = '';
    this.success = '';
  }
}
