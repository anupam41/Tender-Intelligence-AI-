// Windows 11 Supaste Controller - Tender Intelligence AI (OmniRoute Edition)

let currentDoc = null;
let currentAIReport = null;
let allGeMResults = [];

document.addEventListener('DOMContentLoaded', async () => {
  setupNavigation();
  setupGeMScanner();
  setupDocumentDropZone();
  setupSettings();
  setupShortlist();
  setupKeywordsTab();
  checkDeadlineAlerts();
});

// --- Tab Navigation ---
function setupNavigation() {
  const navItems = document.querySelectorAll('.nav-pill, .nav-item');
  const panes = document.querySelectorAll('.tab-pane');

  navItems.forEach(item => {
    item.addEventListener('click', () => {
      const targetTab = item.dataset.tab;
      navItems.forEach(n => n.classList.remove('active'));
      panes.forEach(p => p.classList.remove('active'));

      item.classList.add('active');
      const targetPane = document.getElementById(targetTab);
      if (targetPane) targetPane.classList.add('active');

      if (targetTab === 'tab-saved') loadSavedShortlist();
    });
  });

  const quickScanBtn = document.getElementById('btn-quick-scan');
  if (quickScanBtn) {
    quickScanBtn.addEventListener('click', () => {
      const gemTab = document.querySelector('[data-tab="tab-gem"]');
      if (gemTab) gemTab.click();
      const scanBtn = document.getElementById('btn-scan-gem');
      if (scanBtn) scanBtn.click();
    });
  }
}

// --- 72h Deadline Banner Alerts ---
async function checkDeadlineAlerts() {
  try {
    const upcoming = await window.api.getUpcomingDeadlines();
    const banner = document.getElementById('deadline-banner');
    const bannerText = document.getElementById('deadline-banner-text');

    if (upcoming && upcoming.length > 0) {
      banner.classList.remove('hidden');
      const listStr = upcoming.map(u => `<strong>${u.title}</strong> (Due: ${u.deadline})`).join(' • ');
      bannerText.innerHTML = `⚠️ <strong>Attention:</strong> ${upcoming.length} saved tender(s) closing within 72 hours: ${listStr}`;
    } else {
      banner.classList.add('hidden');
    }
  } catch (e) {
    console.error("Failed to check upcoming deadlines:", e);
  }
}

// --- GeM Scanner Tab ---
function setupGeMScanner() {
  const btnScan = document.getElementById('btn-scan-gem');
  const statusText = document.getElementById('gem-scan-status');
  const resultsList = document.getElementById('gem-results-list');
  const filterInput = document.getElementById('gem-filter');
  const metricCount = document.getElementById('metric-tenders-count');

  window.api.onGeMProgress((msg) => {
    statusText.textContent = msg;
  });

  btnScan.addEventListener('click', async () => {
    btnScan.disabled = true;
    btnScan.textContent = "Scanning GeM...";
    statusText.textContent = "Initiating GeM portal scan...";

    try {
      allGeMResults = await window.api.scanGeM();
      renderGeMCards(allGeMResults);
      statusText.textContent = `Completed! ${allGeMResults.length} matching tenders retrieved.`;
      if (metricCount) metricCount.textContent = allGeMResults.length;
    } catch (err) {
      statusText.textContent = "Error scanning GeM: " + err.message;
    } finally {
      btnScan.disabled = false;
      btnScan.innerHTML = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/></svg> Scan GeM Portal Now`;
    }
  });

  filterInput.addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase();
    const filtered = allGeMResults.filter(item => 
      (item.title || '').toLowerCase().includes(q) ||
      (item.bidId || '').toLowerCase().includes(q) ||
      (item.snippet || '').toLowerCase().includes(q)
    );
    renderGeMCards(filtered);
  });
}

let currentRenderedGeMItems = [];

function renderGeMCards(items) {
  currentRenderedGeMItems = items || [];
  const list = document.getElementById('gem-results-list');
  if (!items || items.length === 0) {
    list.innerHTML = `
      <div class="empty-glass-state">
        <div class="empty-icon">📡</div>
        <h3>No Matching Tenders Found</h3>
        <p>No open tenders matched the current filter criteria.</p>
      </div>`;
    return;
  }

  list.innerHTML = items.map((item, idx) => `
    <div class="tender-card">
      <div class="card-top">
        <span class="bid-badge">${escapeHtml(item.bidId || 'BID ID PENDING')}</span>
        ${item.deadline ? `<span class="deadline-badge">Due: ${escapeHtml(item.deadline)}</span>` : ''}
      </div>
      <h3>${escapeHtml(item.title || 'Untitled Tender')}</h3>
      <p>${escapeHtml(item.snippet || '')}</p>
      <div class="card-actions">
        <button class="btn-gradient-sm" onclick="openTenderByIndex(${idx})">Open GeM Bid</button>
        <button class="btn-outline-sm" onclick="inspectByIndex(${idx})">Analyze with OmniRoute</button>
      </div>
    </div>
  `).join('');
}

window.openTenderByIndex = (idx) => {
  const item = currentRenderedGeMItems[idx];
  if (item && item.url) {
    window.open(item.url, '_blank');
  }
};

window.inspectByIndex = (idx) => {
  const item = currentRenderedGeMItems[idx];
  if (!item) return;

  const navPdf = document.querySelector('[data-tab="tab-pdf"]');
  if (navPdf) navPdf.click();

  document.getElementById('doc-inspector').classList.remove('hidden');
  document.getElementById('doc-title-input').value = `GeM: ${item.title}`;
  document.getElementById('doc-deadline-input').value = item.deadline || '';
  document.getElementById('doc-file-name').textContent = item.bidId || "GeM Bid Document";
  document.getElementById('doc-page-count').textContent = "Portal Listing";
  document.getElementById('doc-score-tag').textContent = "Score: 80";
  document.getElementById('doc-text-area').value = `Bid Number: ${item.bidId}\nTitle: ${item.title}\nDepartment: ${item.department || ''}\nMinistry: ${item.ministry || ''}\nDeadline: ${item.deadline}\nSource: GeM Official Portal\nURL: ${item.url}`;

  currentDoc = {
    title: item.title,
    bidId: item.bidId,
    deadline: item.deadline,
    text: `Bid Number: ${item.bidId}\nTitle: ${item.title}\nDeadline: ${item.deadline}\nDepartment: ${item.department || ''}\nMinistry: ${item.ministry || ''}`,
    score: 80,
    matchedKeywords: ["GeM Portal Bid"]
  };
};

function escapeHtml(text) {
  if (text === null || text === undefined) return "";
  let str = (typeof text === 'string') ? text : (Array.isArray(text) ? text.join(' ') : String(text));
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

// --- Document & PDF Review Tab ---
function setupDocumentDropZone() {
  const dropZone = document.getElementById('drop-zone');
  const btnBrowse = document.getElementById('btn-open-file');

  btnBrowse.addEventListener('click', async () => {
    const filePath = await window.api.openFileDialog();
    if (filePath) loadDocumentFile(filePath);
  });

  dropZone.addEventListener('click', async () => {
    const filePath = await window.api.openFileDialog();
    if (filePath) loadDocumentFile(filePath);
  });

  dropZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropZone.classList.add('dragover');
  });

  dropZone.addEventListener('dragleave', () => {
    dropZone.classList.remove('dragover');
  });

  dropZone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropZone.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      if (file.path) {
        loadDocumentFile(file.path);
      }
    }
  });

  // Analyze with OmniRoute button
  document.getElementById('btn-run-ai').addEventListener('click', async () => {
    if (!currentDoc || !currentDoc.text) {
      alert("Please open a tender document first.");
      return;
    }

    const loading = document.getElementById('ai-loading');
    const reportView = document.getElementById('ai-report-view');

    loading.classList.remove('hidden');
    reportView.textContent = "";

    try {
      const title = document.getElementById('doc-title-input').value || currentDoc.fileName;
      const res = await window.api.analyzeTender({
        title,
        text: currentDoc.text
      });

      currentAIReport = res;
      reportView.textContent = res.report;

      if (res.deadline && !document.getElementById('doc-deadline-input').value) {
        document.getElementById('doc-deadline-input').value = res.deadline;
      }
    } catch (err) {
      reportView.textContent = "Error running OmniRoute AI analysis: " + err.message;
    } finally {
      loading.classList.add('hidden');
    }
  });

  // Save to Shortlist button
  document.getElementById('btn-save-doc').addEventListener('click', async () => {
    if (!currentDoc) {
      alert("No document currently loaded.");
      return;
    }

    const title = document.getElementById('doc-title-input').value || currentDoc.fileName;
    const deadline = document.getElementById('doc-deadline-input').value || currentDoc.deadline;
    const verdict = currentAIReport?.verdict || (currentDoc.score >= 40 ? "SHORTLIST" : "REVIEW");

    await window.api.saveTender({
      title,
      bidId: currentDoc.bidId || "",
      deadline,
      score: currentDoc.score || 0,
      verdict,
      keywords: currentDoc.matchedKeywords || [],
      aiReport: currentAIReport?.report || "",
      text: currentDoc.text?.substring(0, 5000) || ""
    });

    alert("Tender saved to Shortlist database!");
    loadSavedShortlist();
    checkDeadlineAlerts();
  });
}

async function loadDocumentFile(filePath) {
  try {
    const data = await window.api.parsePdf(filePath);
    currentDoc = data;
    currentAIReport = null;

    document.getElementById('drop-zone').classList.add('hidden');
    document.getElementById('doc-inspector').classList.remove('hidden');

    document.getElementById('doc-file-name').textContent = data.fileName;
    document.getElementById('doc-page-count').textContent = `${data.numPages} pages`;
    document.getElementById('doc-score-tag').textContent = `Relevance Score: ${data.score}`;
    document.getElementById('doc-title-input').value = data.bidId ? `Tender ${data.bidId}` : data.fileName;
    document.getElementById('doc-deadline-input').value = data.deadline || "";
    document.getElementById('doc-text-area').value = data.text;

    // Render keyword chips
    const chipContainer = document.getElementById('doc-matched-chips');
    if (data.matchedKeywords && data.matchedKeywords.length > 0) {
      chipContainer.innerHTML = data.matchedKeywords.map(kw => `<span class="chip-pill">${kw}</span>`).join('');
    } else {
      chipContainer.innerHTML = '<span>No target equipment keywords found.</span>';
    }

    document.getElementById('ai-report-view').innerHTML = `<p class="placeholder-msg">Document parsed successfully! Click "Analyze with OmniRoute" to generate the structured intelligence report.</p>`;
  } catch (err) {
    alert("Failed to parse document: " + err.message);
  }
}

// --- Saved Shortlist Tab ---
async function loadSavedShortlist() {
  try {
    const list = await window.api.getAllTenders();
    const tbody = document.getElementById('saved-table-body');
    const badge = document.getElementById('badge-saved-count');
    badge.textContent = list.length;

    if (!list || list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="empty-cell">No tenders shortlisted yet. Save tenders from the GeM Radar or Document Review.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(item => {
      const kwStr = Array.isArray(item.keywords) ? item.keywords.join(', ') : (item.keywords || '-');
      const isExpired = item.deadline && item.deadline < new Date().toISOString().split('T')[0];
      const verdictColor = item.verdict === 'GO' ? '#059669' : item.verdict === 'NO-GO' ? '#e11d48' : '#0284c7';

      return `
        <tr>
          <td><strong style="color:${verdictColor}; font-weight:700;">${item.verdict || 'SAVED'}</strong></td>
          <td>
            <strong>${escapeHtml(item.title)}</strong>
            ${item.bidId ? `<br><small style="color:var(--color-primary); font-family:monospace;">${escapeHtml(item.bidId)}</small>` : ''}
          </td>
          <td>
            ${item.deadline ? `<span style="color:${isExpired ? '#e11d48' : '#059669'}; font-weight:600;">${item.deadline}</span>` : 'Not set'}
          </td>
          <td><strong style="color:var(--color-deep)">${item.score || 0} / 100</strong></td>
          <td><small>${escapeHtml(kwStr)}</small></td>
          <td>
            <button class="btn-outline-sm" onclick="deleteSavedTender('${item.id}')">Delete</button>
          </td>
        </tr>
      `;
    }).join('');
  } catch (e) {
    console.error("Failed to load shortlist:", e);
  }
}

function setupShortlist() {
  document.getElementById('btn-export-csv').addEventListener('click', async () => {
    const res = await window.api.exportCSV();
    if (res.success) {
      alert("Shortlist exported successfully to: " + res.filePath);
    }
  });
}

window.deleteSavedTender = async (id) => {
  if (confirm("Are you sure you want to remove this tender from your shortlist?")) {
    await window.api.deleteTender(id);
    loadSavedShortlist();
    checkDeadlineAlerts();
  }
};

// --- Target Equipment Keywords Tab ---
async function setupKeywordsTab() {
  const container = document.getElementById('keywords-grouped-view');
  try {
    const { groups } = await window.api.getKeywords();
    container.innerHTML = groups.map(g => `
      <div class="equipment-card">
        <h4>${g.name}</h4>
        <p>${g.terms.join(', ')}</p>
      </div>
    `).join('');
  } catch (e) {
    console.error("Failed to load keywords tab:", e);
  }
}

// --- OmniRoute Settings Tab ---
async function setupSettings() {
  const keyInput = document.getElementById('setting-omniroute-key');
  const baseInput = document.getElementById('setting-omniroute-base');
  const modelInput = document.getElementById('setting-model-id');
  const btnSave = document.getElementById('btn-save-settings');

  try {
    const settings = await window.api.getSettings();
    if (settings.omniRouteKey || settings.openRouterKey) {
      keyInput.value = settings.omniRouteKey || settings.openRouterKey;
    }
    if (settings.omniRouteBaseUrl) {
      baseInput.value = settings.omniRouteBaseUrl;
    }
    if (settings.modelId) {
      modelInput.value = settings.modelId;
    }
  } catch (e) {
    console.error("Failed to load settings:", e);
  }

  btnSave.addEventListener('click', async () => {
    await window.api.saveSettings({
      omniRouteKey: keyInput.value.trim(),
      omniRouteBaseUrl: baseInput.value.trim() || "https://openrouter.ai/api/v1",
      modelId: modelInput.value.trim() || "openai/gpt-4o-mini"
    });
    alert("OmniRoute settings saved successfully!");
  });
}
