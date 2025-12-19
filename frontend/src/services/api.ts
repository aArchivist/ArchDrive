import axios from 'axios';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
});

export interface StoredFile {
  id: string;
  fileName: string;
  folder?: string;
  url: string;
  size: number;
  uploadedAt: string;
}

export interface Folder {
  id: string;
  name: string;
  path: string;
  createdAt: string;
  fileCount: number;
}

export const uploadFile = async (file: File): Promise<StoredFile> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await api.post<StoredFile>(
    '/api/files/upload',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );

  return response.data;
};

export const getAllFiles = async (): Promise<StoredFile[]> => {
  const response = await api.get<StoredFile[]>('/api/files');
  return response.data;
};

export const downloadFile = async (fileName: string): Promise<void> => {
  const response = await api.get('/api/files/download', {
    params: { fileName },
    responseType: 'blob',
  });

  const originalFileName = fileName.includes('_')
    ? fileName.substring(fileName.indexOf('_') + 1)
    : fileName;

  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', originalFileName);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const previewFile = async (fileName: string): Promise<{ url: string; contentType: string; data: Blob; textContent?: string }> => {
  const response = await api.get('/api/files/preview', {
    params: { fileName },
    responseType: 'blob',
  });

  const contentType = response.headers['content-type'] || 'application/octet-stream';
  const url = window.URL.createObjectURL(response.data);

  // For text files, also get text content
  let textContent: string | undefined;
  if (contentType.startsWith('text/') || contentType.includes('javascript') || contentType.includes('json') || contentType.includes('xml')) {
    textContent = await response.data.text();
  }

  return {
    url,
    contentType,
    data: response.data,
    textContent
  };
};

export const deleteFile = async (fileName: string): Promise<void> => {
  await api.delete('/api/files', { params: { fileName } });
};

export const uploadFileToFolder = async (file: File, folder?: string): Promise<StoredFile> => {
  const formData = new FormData();
  formData.append('file', file);
  if (folder) {
    formData.append('folder', folder);
  }

  const response = await api.post<StoredFile>(
    '/api/files/upload',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );

  return response.data;
};

export const getFilesInFolder = async (folder?: string): Promise<StoredFile[]> => {
  const params = folder ? { folder } : {};
  const response = await api.get<StoredFile[]>('/api/files', { params });
  return response.data;
};

export const getFolders = async (parentFolder?: string): Promise<Folder[]> => {
  const params = parentFolder ? { parent: parentFolder } : {};
  const response = await api.get<Folder[]>('/api/files/folders', { params });
  return response.data;
};

export const createFolder = async (folderName: string, parentFolder?: string): Promise<Folder> => {
  const params: any = { name: folderName };
  if (parentFolder) {
    params.parent = parentFolder;
  }
  const response = await api.post<Folder>('/api/files/folders', null, {
    params
  });
  return response.data;
};

export const deleteFolder = async (folderName: string): Promise<void> => {
  await api.delete(`/api/files/folders/${folderName}`);
};
