import { useState } from 'react';
import { FileUpload } from './components/FileUpload';
import { FileList } from './components/FileList';
import { FolderCreate } from './components/FolderCreate';
import './App.css';

function App() {
  const [refreshKey, setRefreshKey] = useState(0);
  const [currentFolder, setCurrentFolder] = useState<string | null>(null);

  const handleUploadSuccess = () => {
    // Trigger refresh of file list by changing key
    setRefreshKey(prev => prev + 1);
  };

  const handleFolderChange = (folder: string | null) => {
    setCurrentFolder(folder);
  };


  const getBreadcrumb = () => {
    if (!currentFolder) return null;

    const parts = currentFolder.replace(/\/$/, "").split('/');
    const breadcrumb = [
      <a key="root" href="#" onClick={() => handleFolderChange(null)}>Мій диск</a>
    ];

    let path = "";
    parts.forEach((part, index) => {
      path += part + "/";
      if (index === parts.length - 1) {
        breadcrumb.push(<span key={path}>{part}</span>);
      } else {
        breadcrumb.push(<span key={path + "sep"}> › </span>);
        breadcrumb.push(
          <a key={path} href="#" onClick={() => handleFolderChange(path)}>
            {part}
          </a>
        );
      }
    });

    return breadcrumb;
  };

  return (
    <div className="app">
      <div className="app-container">
        {/* Sidebar */}
        <aside className="sidebar">
          <div className="sidebar-header">
            <h2>ArchDrive</h2>
          </div>
          <nav>
            <ul className="sidebar-nav">
              <li className="sidebar-nav-item active">
                <i className="material-icons">folder</i>
                Мій диск
              </li>
              <li className="sidebar-nav-item">
                <i className="material-icons">access_time</i>
                Недавні
              </li>
              <li className="sidebar-nav-item">
                <i className="material-icons">star</i>
                Позначені
              </li>
              <li className="sidebar-nav-item">
                <i className="material-icons">delete</i>
                Кошик
              </li>
            </ul>
          </nav>
        </aside>

        {/* Main Content */}
        <div className="main-content">
          {/* Top Bar */}
          <header className="top-bar">
            <div></div> {/* Spacer */}
            <div className="search-container">
              <input
                type="text"
                placeholder="Пошук на ArchDrive"
                className="search-input"
              />
            </div>
            <div className="user-menu">
              <div className="user-avatar">В</div>
            </div>
          </header>

          {/* Content Area */}
          <main className="content-area">
            <div className="breadcrumb">
              {getBreadcrumb()}
            </div>

            <FileUpload
              onUploadSuccess={handleUploadSuccess}
              currentFolder={currentFolder}
            />
            <FolderCreate onFolderCreated={handleUploadSuccess} currentFolder={currentFolder} />
            <FileList
              refreshTrigger={refreshKey}
              currentFolder={currentFolder}
              onFolderChange={handleFolderChange}
            />
          </main>
        </div>
      </div>
    </div>
  );
}

export default App;
