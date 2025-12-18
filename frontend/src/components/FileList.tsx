import { useEffect, useState } from 'react';
import { getAllFiles, deleteFile, downloadFile } from '../services/api';
import type { StoredFile } from '../services/api';
import './FileList.css';

interface FileListProps {
  refreshTrigger?: number;
}

export const FileList = ({ refreshTrigger }: FileListProps) => {
  const [files, setFiles] = useState<StoredFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);

  const loadFiles = async () => {
    try {
      setLoading(true);
      const fileList = await getAllFiles();
      setFiles(fileList);
      setError(null);
    } catch (err) {
      setError('Помилка завантаження списку файлів');
      console.error('Load files error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFiles();
  }, [refreshTrigger]);

  const handleDownload = async (fileName: string) => {
    try {
      await downloadFile(fileName);
    } catch (err) {
      setError('Помилка завантаження файлу');
      console.error('Download error:', err);
    }
  };

  const handleDelete = async (fileName: string) => {
    if (!window.confirm('Ви впевнені, що хочете видалити цей файл?')) {
      return;
    }

    try {
      setDeleting(fileName);
      await deleteFile(fileName);
      await loadFiles(); // Reload list after deletion
    } catch (err) {
      setError('Помилка видалення файлу');
      console.error('Delete error:', err);
    } finally {
      setDeleting(null);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  const formatDate = (dateString: string): string => {
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return 'Невідома дата';
      }
      return date.toLocaleString('uk-UA');
    } catch (e) {
      return 'Невідома дата';
    }
  };

  if (loading) {
    return <div className="file-list">Завантаження...</div>;
  }

  return (
    <div className="file-list">
      <div className="file-list-header">
        <h2>Завантажені файли</h2>
        <button onClick={loadFiles} className="refresh-btn">Оновити</button>
      </div>
      
      {error && <p className="error">{error}</p>}
      
      {files.length === 0 ? (
        <p className="no-files">Файлів поки що немає</p>
      ) : (
        <table className="files-table">
          <thead>
            <tr>
              <th>Назва файлу</th>
              <th>Розмір</th>
              <th>Дата завантаження</th>
              <th>Дії</th>
            </tr>
          </thead>
          <tbody>
            {files.map((file) => (
              <tr key={file.id}>
                <td>{file.fileName}</td>
                <td>{formatFileSize(file.size)}</td>
                <td>{formatDate(file.uploadedAt)}</td>
                <td>
                  <div className="action-buttons">
                    <button
                      onClick={() => handleDownload(file.id)}
                      className="download-link"
                    >
                      Завантажити
                    </button>
                    <button
                      onClick={() => handleDelete(file.id)}
                      disabled={deleting === file.id}
                      className="delete-btn"
                    >
                      {deleting === file.id ? 'Видалення...' : 'Видалити'}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

