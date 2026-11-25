// Local Storage & Database - Tender Intelligence AI Windows

const fs = require('fs');
const path = require('path');
const os = require('os');

function getDataDir() {
  let dir = null;
  try {
    const electron = require('electron');
    const app = electron.app || (electron.remote && electron.remote.app);
    if (app && app.getPath) {
      dir = path.join(app.getPath('userData'), 'tender_intelligence');
    }
  } catch (e) {}

  if (!dir) {
    const base = process.env.APPDATA || os.homedir();
    dir = path.join(base, '.tender_intelligence');
  }

  try {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    return dir;
  } catch (err) {
    const fallback = path.join(__dirname, '..', '.data');
    if (!fs.existsSync(fallback)) {
      fs.mkdirSync(fallback, { recursive: true });
    }
    return fallback;
  }
}

function getDbFile() {
  return path.join(getDataDir(), 'saved_tenders.json');
}

function getSettingsFile() {
  return path.join(getDataDir(), 'settings.json');
}

function loadDB() {
  const dbFile = getDbFile();
  if (!fs.existsSync(dbFile)) {
    return [];
  }
  try {
    const raw = fs.readFileSync(dbFile, 'utf8');
    return JSON.parse(raw);
  } catch (e) {
    return [];
  }
}

function saveDB(list) {
  const dbFile = getDbFile();
  fs.writeFileSync(dbFile, JSON.stringify(list, null, 2), 'utf8');
}

function getAllTenders() {
  return loadDB();
}

function saveTender(item) {
  const list = loadDB();
  const id = item.id || `TND_${Date.now()}`;
  const record = {
    ...item,
    id,
    updatedAt: new Date().toISOString()
  };

  const idx = list.findIndex(x => (x.bidId && x.bidId === item.bidId) || x.id === id);
  if (idx >= 0) {
    list[idx] = record;
  } else {
    list.unshift(record);
  }

  saveDB(list);
  return record;
}

function deleteTender(id) {
  const list = loadDB();
  const filtered = list.filter(x => x.id !== id);
  saveDB(filtered);
  return true;
}

function getUpcomingDeadlines(daysAhead = 3) {
  const list = loadDB();
  const now = new Date();
  now.setHours(0, 0, 0, 0);

  const future = new Date(now.getTime() + daysAhead * 24 * 60 * 60 * 1000);
  future.setHours(23, 59, 59, 999);

  return list.filter(item => {
    if (!item.deadline) return false;
    const d = new Date(item.deadline);
    return !isNaN(d.getTime()) && d >= now && d <= future;
  });
}

function loadSettings() {
  const settingsFile = getSettingsFile();
  if (!fs.existsSync(settingsFile)) {
    return {
      omniRouteKey: "",
      omniRouteBaseUrl: "https://openrouter.ai/api/v1",
      modelId: "openai/gpt-4o-mini",
      theme: "light"
    };
  }
  try {
    const data = JSON.parse(fs.readFileSync(settingsFile, 'utf8'));
    if (!data.omniRouteKey && data.openRouterKey) data.omniRouteKey = data.openRouterKey;
    if (!data.omniRouteBaseUrl) data.omniRouteBaseUrl = "https://openrouter.ai/api/v1";
    return data;
  } catch (e) {
    return { omniRouteKey: "", omniRouteBaseUrl: "https://openrouter.ai/api/v1", modelId: "openai/gpt-4o-mini", theme: "light" };
  }
}

function saveSettings(settings) {
  const settingsFile = getSettingsFile();
  const current = loadSettings();
  const merged = { ...current, ...settings };
  fs.writeFileSync(settingsFile, JSON.stringify(merged, null, 2), 'utf8');
  return merged;
}

function safeStr(val) {
  if (val === null || val === undefined) return "";
  if (typeof val === 'string') return val;
  if (Array.isArray(val)) return val.join(', ');
  return String(val);
}

function exportShortlistCSV() {
  const list = loadDB();
  const headers = ["Title", "Bid ID", "Deadline", "Score", "Verdict", "Matched Keywords", "URL"];
  const rows = list.map(item => [
    `"${safeStr(item.title).replace(/"/g, '""')}"`,
    `"${safeStr(item.bidId).replace(/"/g, '""')}"`,
    `"${safeStr(item.deadline).replace(/"/g, '""')}"`,
    item.score || 0,
    `"${safeStr(item.verdict).replace(/"/g, '""')}"`,
    `"${safeStr(item.keywords).replace(/"/g, '""')}"`,
    `"${safeStr(item.url).replace(/"/g, '""')}"`
  ]);

  return [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
}

module.exports = {
  getAllTenders,
  saveTender,
  deleteTender,
  getUpcomingDeadlines,
  loadSettings,
  saveSettings,
  exportShortlistCSV
};
