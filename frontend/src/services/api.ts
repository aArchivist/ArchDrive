import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/files';

export interface StoredFile {
  id: string;
  fileName: string;
  url: string;
  size: number;
  uploadedAt: string;
}

export const uploadFile = async (file: File): Promise<StoredFile> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await api.post<StoredFile>('/api/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data;
};

export const getAllFiles = async (): Promise<StoredFile[]> => {
  const response = await api.get<StoredFile[]>('/api/files');
  return response.data;
};

export const downloadFile = async (fileName: string): Promise<void> => {
  const response = await api.get(`/api/files/${fileName}`, {
    responseType: 'blob',
  });

  // Extract original filename (remove UUID prefix if present)
  const originalFileName = fileName.includes('_')
    ? fileName.substring(fileName.indexOf('_') + 1)
    : fileName;

  // Create download link and trigger download
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', originalFileName);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const deleteFile = async (fileName: string): Promise<void> => {
  await api.delete(`/api/files/${fileName}`);
};

