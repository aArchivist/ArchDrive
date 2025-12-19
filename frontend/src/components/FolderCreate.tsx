import { useState } from 'react';
import { createFolder } from '../services/api';
import './FolderCreate.css';

interface FolderCreateProps {
  onFolderCreated: () => void;
  currentFolder?: string;
}

export const FolderCreate = ({ onFolderCreated, currentFolder }: FolderCreateProps) => {
  const [folderName, setFolderName] = useState('');
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleCreate = async () => {
    if (!folderName.trim()) {
      setError('Введіть назву папки');
      return;
    }

    setCreating(true);
    setError(null);

    try {
      await createFolder(folderName.trim(), currentFolder);
      setFolderName('');
      onFolderCreated();
    } catch (err) {
      setError('Помилка створення папки. Спробуйте ще раз.');
      console.error('Create folder error:', err);
    } finally {
      setCreating(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleCreate();
    }
  };

  return (
    <div className="folder-create">
      <h3>Створити нову папку</h3>
      <div className="create-controls">
        <input
          type="text"
          value={folderName}
          onChange={(e) => setFolderName(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="Назва папки"
          disabled={creating}
          maxLength={50}
        />
        <button
          onClick={handleCreate}
          disabled={creating || !folderName.trim()}
          className="create-btn"
        >
          <i className="material-icons">create_new_folder</i>
          {creating ? 'Створення...' : 'Створити'}
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
