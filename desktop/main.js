// Electron Main Process - Tender Intelligence AI Windows 11

const { app, BrowserWindow, ipcMain, dialog, shell } = require('electron');
const path = require('path');
const fs = require('fs');
const pdfParse = require('pdf-parse');

const { scanGeM } = require('./src/gemScanner');
const { analyzeTender } = require('./src/aiAnalyzer');
const { matches, getMatchedKeywords, calculateScore, ALL_KEYWORDS, GROUPS } = require('./src/keywords');
const db = require('./src/db');

let mainWindow = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1360,
    height: 880,
    minWidth: 1024,
    minHeight: 700,
    backgroundColor: '#0f172a',
    title: 'Tender Intelligence AI - Windows 11',
    icon: path.join(__dirname, 'assets', 'icon.png'),
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: false
    }
  });

  mainWindow.loadFile(path.join(__dirname, 'src', 'index.html'));

  // Open external links in default browser
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

// --- IPC Handlers ---

// 1. GeM Scanner
ipcMain.handle('gem:scan', async (event) => {
  const onProgress = (msg) => {
    event.sender.send('gem:progress', msg);
  };
  return await scanGeM(onProgress);
});

// 2. PDF & Document Extraction
ipcMain.handle('pdf:parse-file', async (event, filePath) => {
  if (!fs.existsSync(filePath)) throw new Error("File not found: " + filePath);
  const dataBuffer = fs.readFileSync(filePath);
  
  const ext = path.extname(filePath).toLowerCase();
  let text = "";
  let numPages = 1;

  if (ext === '.pdf') {
    const parsed = await pdfParse(dataBuffer);
    text = parsed.text || "";
    numPages = parsed.numpages || 1;
  } else if (ext === '.txt') {
    text = dataBuffer.toString('utf8');
  } else {
    throw new Error("Unsupported format. Please select a .pdf or .txt tender document.");
  }

  const matchedKeywords = getMatchedKeywords(text);
  const score = calculateScore(text);

  // Extract bid ID pattern (e.g. GEM/2026/B/123456)
  const bidIdMatch = /(GEM\/\d{4}\/[B|R]\/\d+|\b[A-Z0-9]{2,}\/\d{4,}\/[A-Z0-9\/-]+)/i.exec(text);
  const bidId = bidIdMatch ? bidIdMatch[1] : "";

  // Extract closing deadline
  const deadlineMatch = /\b(202[4-9]-\d{2}-\d{2})\b/.exec(text);
  const deadline = deadlineMatch ? deadlineMatch[1] : "";

  return {
    filePath,
    fileName: path.basename(filePath),
    text,
    numPages,
    matchedKeywords,
    score,
    bidId,
    deadline
  };
});

// 3. AI Analysis
ipcMain.handle('ai:analyze', async (event, { title, text }) => {
  const settings = db.loadSettings();
  return await analyzeTender(settings.openRouterKey, settings.modelId, title, text);
});

// 4. File Open Dialog
ipcMain.handle('dialog:open-pdf', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    title: 'Select Tender PDF or Text Document',
    filters: [
      { name: 'Tender Documents', extensions: ['pdf', 'txt'] },
      { name: 'All Files', extensions: ['*'] }
    ],
    properties: ['openFile']
  });
  if (result.canceled || result.filePaths.length === 0) return null;
  return result.filePaths[0];
});

// 5. Database operations
ipcMain.handle('db:get-all', async () => db.getAllTenders());
ipcMain.handle('db:save', async (event, item) => db.saveTender(item));
ipcMain.handle('db:delete', async (event, id) => db.deleteTender(id));
ipcMain.handle('db:upcoming', async () => db.getUpcomingDeadlines(3));

// 6. Settings
ipcMain.handle('settings:get', async () => db.loadSettings());
ipcMain.handle('settings:save', async (event, settings) => db.saveSettings(settings));

// 7. Keywords
ipcMain.handle('keywords:get-all', async () => ({ all: ALL_KEYWORDS, groups: GROUPS }));

// 8. Export CSV
ipcMain.handle('export:csv', async () => {
  const csvData = db.exportShortlistCSV();
  const result = await dialog.showSaveDialog(mainWindow, {
    title: 'Save Tender Shortlist CSV',
    defaultPath: 'tender-shortlist.csv',
    filters: [{ name: 'CSV Document', extensions: ['csv'] }]
  });
  if (!result.canceled && result.filePath) {
    fs.writeFileSync(result.filePath, csvData, 'utf8');
    return { success: true, filePath: result.filePath };
  }
  return { success: false };
});
