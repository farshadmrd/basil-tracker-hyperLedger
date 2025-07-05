import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class BasilService {
  private apiUrl = 'http://localhost:8080/api/basil';

  constructor(private http: HttpClient) { }

  createBasil(id: string, country: string): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}?id=${id}&country=${country}`, {});
  }

  getBasil(id: string): Observable<string> {
    return this.http.get<string>(`${this.apiUrl}/${id}`);
  }

  deleteBasil(id: string): Observable<string> {
    return this.http.delete<string>(`${this.apiUrl}/${id}`);
  }

  updateBasilState(id: string, gps: string, timestamp: number, temp: string, humidity: string, status: string): Observable<string> {
    return this.http.put<string>(
      `${this.apiUrl}/${id}/state?gps=${gps}&timestamp=${timestamp}&temp=${temp}&humidity=${humidity}&status=${status}`,
      {}
    );
  }

  transferBasilOwnership(id: string, newOrgId: string, newName: string): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/${id}/transfer?newOrgId=${newOrgId}&newName=${newName}`,
      {},
      { responseType: 'text' }
    );
  }
}
