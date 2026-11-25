// Local Storage & Database - Tender Intelligence AI Windows

const fs = require('fs');
const path = require('path');
const os = require('os');

const DATA_DIR = path.join(os.homedir(), '.tender_intelligence');
const DB_FILE = path.join(DATA_DIR, 'saved_tenders.json');
const SETTINGS_FILE = path.join(DATA_DIR, 'settings.json');

function ensureDir() {
  if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  }
}

function loadDB() {
  ensureDir();
  if (!fs.existsSync(DB_FILE)) {
    return [];
  }
  try {
    const raw = fs.readFileSync(DB_FILE, 'utf8');
    return JSON.parse(raw);
  } catch (e) {
    return [];
  }
}

function saveDB(list) {
  ensureDir();
  fs.writeFileSync(DB_FILE, JSON.stringify(list, null, 2), 'utf8');
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
  ensureDir();
  if (!fs.existsSync(SETTINGS_FILE)) {
    return {
      openRouterKey: "",
      modelId: "openai/gpt-4o-mini",
      theme: "dark"
    };
  }
  try {
    return JSON.parse(fs.readFileSync(SETTINGS_FILE, 'utf8'));
  } catch (e) {
    return { openRouterKey: "", modelId: "openai/gpt-4o-mini", theme: "dark" };
  }
}

function saveSettings(settings) {
  ensureDir();
  const current = loadSettings();
  const merged = { ...current, ...settings };
  fs.writeFileSync(SETTINGS_FILE, JSON.stringify(merged, null, 2), 'utf8');
  return merged;
}

function exportShortlistCSV() {
  const list = loadDB();
  const headers = ["Title", "Bid ID", "Deadline", "Score", "Verdict", "Matched Keywords", "URL"];
  const rows = list.map(item => [
    `"${(item.title || '').replace(/"/g, '""')}"`,
    `"${(item.bidId || '').replace(/"/g, '""')}"`,
    `"${(item.deadline || '').replace(/"/g, '""')}"`,
    item.score || 0,
    `"${(item.verdict || '').replace(/"/g, '""')}"`,
    `"${(Array.isArray(item.keywords) ? item.keywords.join(', ') : (item.keywords || '')).replace(/"/g, '""')}"`,
    `"${(item.url || '').replace(/"/g, '""')}"`
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
