export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface User {
  id: string;
  employeeCode: string;
  email: string;
  fullName: string;
  role: string;
  employeeType: string;
  organizationId: string;
}

export type Role =
  | 'SYSTEM_ADMIN'
  | 'CEO'
  | 'DIVISION_DIRECTOR'
  | 'REGION_DIRECTOR'
  | 'DEPT_HEAD'
  | 'DEPUTY_HEAD'
  | 'UNIT_HEAD'
  | 'EMPLOYEE';
