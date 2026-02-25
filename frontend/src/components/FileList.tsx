import { useEffect, useState } from 'react';
import * as api from '../services/api';
import type { StoredFile, Folder } from '../services/api';
import './FileList.css';

// FilePreview component
const FilePreview = ({ url, contentType, fileName, textContent }: { url: string; contentType: string; fileName: string; textContent?: string }) => {
  const renderPreview = () => {
    if (contentType.startsWith('image/')) {
      return <img src={url} alt={fileName} className="preview-image" />;
    }

    if (contentType.startsWith('video/')) {
      return (
        <video controls className="preview-video">
          <source src={url} type={contentType} />
          Ваш браузер не підтримує відтворення відео.
        </video>
      );
    }

    if (contentType.startsWith('audio/')) {
      return (
        <audio controls className="preview-audio">
          <source src={url} type={contentType} />
          Ваш браузер не підтримує відтворення аудіо.
        </audio>
      );
    }

    if (contentType === 'application/pdf') {
      return (
        <iframe
          src={url}
          className="preview-pdf"
          title={`PDF Preview: ${fileName}`}
        />
      );
    }

    if (contentType.startsWith('text/') || contentType.includes('javascript') || contentType.includes('json') || contentType.includes('xml')) {
      return (
        <TextFilePreview textContent={textContent} />
      );
    }

    return (
      <div className="preview-unsupported">
        <i className="material-icons">description</i>
        <p>Попередній перегляд недоступний для цього типу файлу</p>
        <p className="preview-file-type">{contentType}</p>
      </div>
    );
  };

  return (
    <div className="file-preview">
      {renderPreview()}
    </div>
  );
};

// Text file preview component
const TextFilePreview = ({ textContent }: { textContent?: string }) => {
  if (!textContent) {
    return (
      <div className="preview-error">
        <i className="material-icons">error</i>
        <p>Не вдалося завантажити текстовий вміст</p>
      </div>
    );
  }

  return (
    <pre className="preview-text">
      <code>{textContent}</code>
    </pre>
  );
};

interface FileListProps {
  refreshTrigger?: number;
  currentFolder?: string | null;
  onFolderChange?: (folder: string | null) => void;
}

export const FileList = ({ refreshTrigger, currentFolder, onFolderChange }: FileListProps) => {
  const [files, setFiles] = useState<StoredFile[]>([]);
  const [folders, setFolders] = useState<Folder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);
  const [previewFile, setPreviewFile] = useState<{ name: string; url: string; contentType: string; textContent?: string } | null>(null);

  const loadFiles = async () => {
    try {
      setLoading(true);
      const fileList = await api.getFilesInFolder(currentFolder || undefined);
      const folderList = await api.getFolders(currentFolder || undefined);
      setFiles(fileList);
      setFolders(folderList);
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
  }, [refreshTrigger, currentFolder]);

  // Handle ESC key to close preview
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && previewFile) {
        closePreview();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [previewFile]);

  const handleFolderClick = (folderPath: string | null) => {
    if (onFolderChange) {
      onFolderChange(folderPath);
    }
  };

  const handleGoBack = () => {
    if (!currentFolder) return;

    // Extract parent folder path
    const pathParts = currentFolder.replace(/\/$/, '').split('/');
    if (pathParts.length > 1) {
      // Go to parent folder
      const parentPath = pathParts.slice(0, -1).join('/') + '/';
      handleFolderClick(parentPath);
    } else {
      // Go to root
      handleFolderClick(null);
    }
  };

  const handleDeleteFolder = async (folderName: string) => {
    if (!window.confirm(`Ви впевнені, що хочете видалити папку "${folderName}" та всі файли в ній?`)) {
      return;
    }

    try {
      await api.deleteFolder(folderName);
      await loadFiles();
      if (currentFolder === folderName + "/") {
        handleFolderClick(null); // Return to root if current folder is deleted
      }
    } catch (err) {
      setError('Помилка видалення папки');
      console.error('Delete folder error:', err);
    }
  };

  const handleDownload = async (fileName: string) => {
    try {
      await api.downloadFile(fileName);
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
      await api.deleteFile(fileName);
      await loadFiles(); // Reload list after deletion
    } catch (err) {
      setError('Помилка видалення файлу');
      console.error('Delete error:', err);
    } finally {
      setDeleting(null);
    }
  };

  const handlePreview = async (file: StoredFile) => {
    try {
      const preview = await api.previewFile(file.id);
      setPreviewFile({
        name: file.fileName,
        url: preview.url,
        contentType: preview.contentType,
        textContent: preview.textContent
      });
    } catch (err) {
      setError('Помилка завантаження попереднього перегляду');
      console.error('Preview error:', err);
    }
  };

  const closePreview = () => {
    if (previewFile) {
      window.URL.revokeObjectURL(previewFile.url);
      setPreviewFile(null);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    if (i === 0) {
      // Для байтів показуємо точну кількість
      return bytes + ' ' + sizes[i];
    }

    // Для інших одиниць показуємо 2 знаки після коми
    const value = bytes / Math.pow(k, i);
    return value.toFixed(2) + ' ' + sizes[i];
  };


  if (loading) {
    return <div className="file-list">Завантаження...</div>;
  }

  const getCurrentFolderName = () => {
    if (!currentFolder) return "Корінь";
    const folder = folders.find(f => f.path === currentFolder);
    return folder ? folder.name : currentFolder.replace(/\/$/, "");
  };

  const getFileIcon = (fileName: string) => {
    const extension = fileName.split('.').pop()?.toLowerCase();
    switch (extension) {
      case 'pdf':
        return 'picture_as_pdf';
      case 'doc':
      case 'docx':
        return 'description';
      case 'xls':
      case 'xlsx':
        return 'table_chart';
      case 'ppt':
      case 'pptx':
        return 'slideshow';
      case 'txt':
        return 'article';
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
        return 'image';
      case 'mp4':
      case 'avi':
      case 'mov':
        return 'videocam';
      case 'mp3':
      case 'wav':
        return 'audiotrack';
      case 'zip':
      case 'rar':
        return 'archive';
      default:
        return 'insert_drive_file';
    }
  };

  const canPreviewFile = (fileName: string): boolean => {
    const extension = fileName.split('.').pop()?.toLowerCase();
    const previewableExtensions = [
      // Images
      'jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp', 'ico',
      // Text files
      'txt', 'md', 'json', 'xml', 'html', 'htm', 'css', 'js', 'ts', 'java', 'py', 'sql', 'yaml', 'yml',
      // Documents
      'pdf',
      // Video
      'mp4', 'webm', 'ogg', 'avi', 'mov', 'wmv',
      // Audio
      'mp3', 'wav', 'aac', 'flac'
    ];
    return extension ? previewableExtensions.includes(extension) : false;
  };

  return (
    <div className="file-list">
      <div className="file-list-header">
        <h2>{getCurrentFolderName()}</h2>
        <div className="header-actions">
          <button onClick={loadFiles} className="refresh-btn">
            <i className="material-icons">refresh</i>
            Оновити
          </button>
          {currentFolder && (
            <button onClick={handleGoBack} className="back-btn">
              <i className="material-icons">arrow_back</i>
              Назад
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="error">
          <i className="material-icons">error</i>
          {error}
        </div>
      )}

      {folders.length === 0 && files.length === 0 ? (
        <div className="no-files">
          <i className="material-icons">cloud_off</i>
          {currentFolder ? `У папці "${getCurrentFolderName()}" файлів поки що немає` : 'Файлів поки що немає'}
        </div>
      ) : (
        <div className="files-grid">
          {/* Folders */}
          {folders.map((folder) => (
            <div key={folder.id} className="folder-card" onClick={() => handleFolderClick(folder.path)}>
              <div className="folder-icon">
                <i className="material-icons">folder</i>
              </div>
              <div className="folder-name">{folder.name}</div>
              <div className="folder-info">{folder.fileCount} елементів</div>
              <div className="file-actions">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDeleteFolder(folder.name);
                  }}
                  title="Видалити папку"
                >
                  <i className="material-icons">delete</i>
                </button>
              </div>
            </div>
          ))}

          {/* Files */}
          {files.map((file) => (
            <div
              key={file.id}
              className="file-card"
              onClick={() => canPreviewFile(file.fileName) && handlePreview(file)}
              style={{ cursor: canPreviewFile(file.fileName) ? 'pointer' : 'default' }}
            >
              <div className="file-icon">
                <i className="material-icons">{getFileIcon(file.fileName)}</i>
              </div>
              <div className="file-name">{file.fileName}</div>
              <div className="file-info">{formatFileSize(file.size)}</div>

              {/* Preview indicator */}
              {canPreviewFile(file.fileName) && (
                <div className="preview-indicator">
                  <i className="material-icons">visibility</i>
                </div>
              )}

              <div className="file-actions">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDownload(file.id);
                  }}
                  title="Завантажити"
                >
                  <i className="material-icons">download</i>
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDelete(file.id);
                  }}
                  disabled={deleting === file.id}
                  title="Видалити"
                >
                  <i className="material-icons">delete</i>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Preview Modal */}
      {previewFile && (
        <div className="preview-modal-overlay" onClick={closePreview}>
          <div className="preview-modal" onClick={(e) => e.stopPropagation()}>
            <div className="preview-modal-header">
              <h3>{previewFile.name}</h3>
              <button className="preview-close-btn" onClick={closePreview}>
                <i className="material-icons">close</i>
              </button>
            </div>
            <div className="preview-modal-content">
              <FilePreview url={previewFile.url} contentType={previewFile.contentType} fileName={previewFile.name} textContent={previewFile.textContent} />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

