import { apiGet } from './apiClient';
import type { ApiCatalogResponse } from '../types/api';

export function fetchApiCatalog() {
  return apiGet<ApiCatalogResponse>('/api/catalog');
}
