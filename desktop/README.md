# Tender Intelligence AI — Windows 11 Edition

Native desktop application for **Tender Intelligence AI**, engineered for Windows 11 with Fluent Design, drag-and-drop tender document review, live GeM public bid monitoring, and AI-driven bidding intelligence.

---

## Key Features

1. **Targeted Equipment Scanner**:
   - Monitored keywords: *Heat Exchanger, Condenser, Cooler, Chiller, Radiator, Dryer, Heater, Economizer, Ejector, Evaporator, “U” Tube bundle, Cooling coil, Tank, Vessels*.
2. **Split-Screen Document Inspector**:
   - Drag & drop tender `.pdf` or `.txt` files directly from Windows File Explorer.
   - Real-time keyword highlighting, relevance scoring, and page count detection.
3. **Live GeM Tender Radar**:
   - Scans public tender listings on `bidplus.gem.gov.in` matching your equipment categories without requiring login credentials.
4. **Structured AI Evaluation (OpenRouter)**:
   - Deep LLM analysis extracting:
     - 🎯 Scope of Work & Quantities
     - 💰 Financial Criteria (EMD, PBG %, Annual Turnover)
     - 📅 Critical Dates & Deadlines
     - ✅ Qualification Requirements (Experience years, certifications)
     - ⚠️ Risk Factors & Penalty Clauses (Liquidated Damages)
     - ⚖️ Bid Verdict (`GO`, `NO-GO`, or `CONDITIONAL`)
5. **Local Persistent Database**:
   - Offline storage of shortlisted tenders.
   - 72-hour deadline alert banners.
   - CSV export for spreadsheets.

---

## Running on Windows 11

### 1. Install Dependencies
In the `desktop` folder:
```powershell
cd desktop
npm install
```

### 2. Start the App
```powershell
npm start
```

### 3. Build Windows 11 Standalone `.exe` Installer
```powershell
npm run dist
```
The installer will be generated in the `desktop/dist/` directory.
