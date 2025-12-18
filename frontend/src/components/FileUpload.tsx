import { useState } from 'react';
import { uploadFile } from '../services/api';
import './FileUpload.css';

interface FileUploadProps {
  onUploadSuccess: () => void;
}

export const FileUpload = ({ onUploadSuccess }: FileUploadProps) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
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
      await uploadFile(selectedFile);
      setSelectedFile(null);
      // Reset file input
      const fileInput = document.getElementById('file-input') as HTMLInputElement;
      if (fileInput) {
        fileInput.value = '';
      }
      onUploadSuccess();
    } catch (err) {
      setError('Помилка завантаження файлу. Спробуйте ще раз.');
      console.error('Upload error:', err);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="file-upload">
      <h2>Завантажити файл</h2>
      <div className="upload-controls">
        <input
          id="file-input"
          type="file"
          onChange={handleFileChange}
          disabled={uploading}
        />
        <button onClick={handleUpload} disabled={uploading || !selectedFile}>
          {uploading ? 'Завантаження...' : 'Завантажити'}
        </button>
      </div>
      {selectedFile && (
        <p className="selected-file">Вибрано: {selectedFile.name}</p>
      )}
      {error && <p className="error">{error}</p>}
    </div>
  );
};

