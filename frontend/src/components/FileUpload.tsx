import { useState, useRef } from 'react';
import { uploadFileToFolder } from '../services/api';
import './FileUpload.css';

interface FileUploadProps {
  onUploadSuccess: () => void;
  currentFolder?: string | null;
}

export const FileUpload = ({ onUploadSuccess, currentFolder }: FileUploadProps) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
      setError(null);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setSelectedFile(e.dataTransfer.files[0]);
      setError(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Будь ласка, виберіть файл');
      return;
    }

    setUploading(true);
    setError(null);

    try {
      await uploadFileToFolder(selectedFile, currentFolder || undefined);
      setSelectedFile(null);
      // Reset file input
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      onUploadSuccess();
    } catch (err) {
      setError('Помилка завантаження файлу. Спробуйте ще раз.');
      console.error('Upload error:', err);
    } finally {
      setUploading(false);
    }
  };

  const handleClick = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="file-upload">
      {currentFolder && (
        <div className="current-folder">
          <i className="material-icons">folder</i>
          Поточна папка: <strong>{currentFolder.replace(/\/$/, "")}</strong>
        </div>
      )}

      <div
        className={`upload-area ${dragOver ? 'drag-over' : ''}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={handleClick}
      >
        <div className="upload-icon">
          <i className="material-icons">cloud_upload</i>
        </div>
        <div className="upload-text">
          {selectedFile ? `Вибрано: ${selectedFile.name}` : 'Перетягніть файл сюди або натисніть для вибору'}
        </div>
        <div className="upload-subtext">
          {selectedFile ? `${(selectedFile.size / 1024 / 1024).toFixed(2)} MB` : 'Підтримуються всі типи файлів'}
        </div>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        onChange={handleFileChange}
        disabled={uploading}
        style={{ display: 'none' }}
      />

      <div className="upload-controls">
        <button
          onClick={handleUpload}
          disabled={uploading || !selectedFile}
          className="upload-btn"
        >
          <i className="material-icons">file_upload</i>
          {uploading ? 'Завантаження...' : 'Завантажити файл'}
        </button>
      </div>

      {error && (
        <div className="error">
          <i className="material-icons">error</i>
          {error}
        </div>
      )}
    </div>
  );
};

