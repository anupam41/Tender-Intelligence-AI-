// GeM Portal Bid Scanner - Tender Intelligence AI

const https = require('https');
const { SEARCH_QUERIES, matches } = require('./keywords');

const GEM_URL = "https://bidplus.gem.gov.in/all-bids";
const DATA_URL = "https://bidplus.gem.gov.in/all-bids-data";
const MAX_PAGES_PER_QUERY = 3;
const CSRF_REGEX = /csrf_bd_gem_nk.*?['"]([a-fA-F0-9]{32})['"]/;

function requestHttp(url, options = {}, postData = null) {
  return new Promise((resolve, reject) => {
    const req = https.request(url, options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          data: body
        });
      });
    });

    req.on('error', err => reject(err));
    req.setTimeout(25000, () => {
      req.destroy();
      reject(new Error("Request timed out"));
    });

    if (postData) {
      req.write(postData);
    }
    req.end();
  });
}

async function getSession() {
  const res = await requestHttp(GEM_URL, {
    method: 'GET',
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
      'Accept': 'text/html,application/xhtml+xml,application/xml'
    }
  });

  const match = CSRF_REGEX.exec(res.data);
  if (!match) throw new Error("GeM CSRF token not found. The portal structure may have changed.");

  let cookie = "";
  if (res.headers['set-cookie']) {
    cookie = res.headers['set-cookie'].map(c => c.split(';')[0]).join('; ');
  }

  return { token: match[1], cookie };
}

function parseDeadline(raw) {
  if (!raw) return "";
  const m = /(20\d{2}-\d{2}-\d{2})/.exec(raw);
  return m ? m[1] : "";
}

function normalizeDoc(doc) {
  const id = doc.b_id || "";
  const bidId = Array.isArray(doc.b_bid_number) ? doc.b_bid_number[0] : (doc.b_bid_number || "");
  const category = doc.b_category_name || "";
  const title = doc.bbt_title || category;
  const dept = doc.ba_official_details_deptName || "";
  const ministry = doc.ba_official_details_minName || "";
  const endDate = parseDeadline(doc.final_end_date_sort || doc.b_bid_end_date);
  const snippet = `${category} • ${dept} • ${ministry} • ${bidId}`;

  return {
    bidId,
    title,
    snippet,
    category,
    department: dept,
    ministry,
    deadline: endDate,
    url: /^\d+$/.test(id) ? `https://bidplus.gem.gov.in/showbidDocument/${id}` : GEM_URL,
    source: "GeM Public Portal",
    status: "Ongoing GeM Listing"
  };
}

async function scanGeM(onProgress = () => {}) {
  const found = new Map();
  onProgress("Connecting to GeM portal & establishing session...");

  let session;
  try {
    session = await getSession();
  } catch (err) {
    onProgress(`Connection error: ${err.message}`);
    throw err;
  }

  const { token, cookie } = session;

  for (const query of SEARCH_QUERIES) {
    onProgress(`Searching GeM for: "${query}" (${found.size} matching tenders found)...`);

    for (let p = 1; p <= MAX_PAGES_PER_QUERY; p++) {
      const payload = {
        page: p,
        param: { searchBid: query, searchType: "fullText" },
        filter: {
          bidStatusType: "ongoing_bids",
          byType: "all",
          highBidValue: "",
          byEndDate: { from: "", to: "" },
          sort: "Bid-End-Date-Latest"
        }
      };

      const postBody = `payload=${encodeURIComponent(JSON.stringify(payload))}&csrf_bd_gem_nk=${encodeURIComponent(token)}`;

      try {
        const res = await requestHttp(DATA_URL, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': GEM_URL,
            'Origin': 'https://bidplus.gem.gov.in',
            'X-Requested-With': 'XMLHttpRequest',
            'Cookie': cookie
          }
        }, postBody);

        if (res.statusCode === 403) throw new Error("GeM denied automated access (HTTP 403).");
        if (res.statusCode !== 200) break;

        const json = JSON.parse(res.data);
        const docs = json?.response?.response?.docs;
        if (!docs || docs.length === 0) break;

        for (const doc of docs) {
          const item = normalizeDoc(doc);
          if (matches(`${item.title} ${item.snippet}`)) {
            const key = item.bidId || item.url;
            if (!found.has(key)) {
              found.set(key, item);
            }
          }
        }

        const numFound = json?.response?.response?.numFound || 0;
        if (p * 10 >= numFound || docs.length < 10) break;

        await new Promise(r => setTimeout(r, 650));
      } catch (e) {
        break;
      }
    }
  }

  onProgress(`Search completed: ${found.size} open tenders matched your equipment keywords.`);
  return Array.from(found.values());
}

module.exports = {
  scanGeM
};
