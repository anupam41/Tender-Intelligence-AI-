// Electron Preload Bridge - Tender Intelligence AI Windows 11

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  scanGeM: () => ipcRenderer.invoke('gem:scan'),
  onGeMProgress: (callback) => {
    ipcRenderer.removeAllListeners('gem:progress');
    ipcRenderer.on('gem:progress', (event, msg) => callback(msg));
  },
  parsePdf: (filePath) => ipcRenderer.invoke('pdf:parse-file', filePath),
  analyzeTender: (data) => ipcRenderer.invoke('ai:analyze', data),
  openFileDialog: () => ipcRenderer.invoke('dialog:open-pdf'),
  getAllTenders: () => ipcRenderer.invoke('db:get-all'),
  saveTender: (item) => ipcRenderer.invoke('db:save', item),
  deleteTender: (id) => ipcRenderer.invoke('db:delete', id),
  getUpcomingDeadlines: () => ipcRenderer.invoke('db:upcoming'),
  getSettings: () => ipcRenderer.invoke('settings:get'),
  saveSettings: (settings) => ipcRenderer.invoke('settings:save', settings),
  getKeywords: () => ipcRenderer.invoke('keywords:get-all'),
  exportCSV: () => ipcRenderer.invoke('export:csv')
});
