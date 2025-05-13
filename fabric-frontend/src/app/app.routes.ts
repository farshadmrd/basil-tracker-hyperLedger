import { Routes } from '@angular/router';
import { BasilListComponent } from './components/basil-list/basil-list.component';

export const routes: Routes = [
  { path: '', component: BasilListComponent },
  { path: '**', redirectTo: '' }
];
