// AI Tender Analysis - OpenRouter Integration

const https = require('https');

async function analyzeTender(apiKey, modelId, title, text) {
  if (!apiKey || !apiKey.trim()) {
    throw new Error("OpenRouter API key is required. Enter it in Settings.");
  }
  if (!text || text.trim().length < 20) {
    throw new Error("Tender document text is too short to analyze.");
  }

  const model = modelId || "openai/gpt-4o-mini";
  const truncatedText = text.substring(0, 35000); // 35k chars safe context

  const systemPrompt = `You are an expert Indian Government Tender Intelligence AI (specializing in GeM, CPPP, and IREPS tenders for industrial equipment like heat exchangers, coolers, boilers, and pressure vessels).
Analyze this tender and provide a structured intelligence report strictly adhering to this format:

🎯 [1. SCOPE & BUYER]
• Authority / Buyer Name:
• Project / Procurement Scope:
• Required Equipment & Quantities:

💰 [2. FINANCIAL REQUIREMENTS]
• Estimated Tender Value:
• EMD (Earnest Money Deposit):
• Performance Security (PBG %):
• Annual Financial Turnover Requirement:

📅 [3. CRITICAL DATES]
• Bid Submission Deadline (YYYY-MM-DD):
• Pre-bid Meeting Date & Location:
• Technical Bid Opening Date:

✅ [4. QUALIFICATION & ELIGIBILITY]
• Past Experience (Years & Similar Project Value):
• Mandatory Certifications / Standards (e.g. ISO, ASME, IBR):
• Make-in-India (MII) Preference / Local Content %:

⚠️ [5. RISK FACTORS & CONTRACT TERMS]
• Liquidated Damages / Penalties (% per week, max %):
• Payment Milestones:
• Delivery / Completion Period:
• Guarantee / Warranty Terms:

⚖️ [6. BID VERDICT]
• Recommendation: [GO / NO-GO / CONDITIONAL]
• Concise Rationale: (1-2 sentences on feasibility and risk)

State 'Not specified' if a detail is absent. Do not invent missing facts.`;

  const payload = JSON.stringify({
    model: model,
    messages: [
      { role: "system", content: systemPrompt },
      { role: "user", content: `Tender Title: ${title}\n\nDocument Text:\n${truncatedText}` }
    ]
  });

  return new Promise((resolve, reject) => {
    const req = https.request('https://openrouter.ai/api/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiKey.trim()}`,
        'HTTP-Referer': 'https://github.com/anupam41/Tender-Intelligence-AI-',
        'X-Title': 'Tender Intelligence AI Windows'
      }
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          if (res.statusCode >= 400 || json.error) {
            reject(new Error(json.error?.message || `OpenRouter HTTP ${res.statusCode}: ${body.substring(0, 200)}`));
            return;
          }
          const content = json.choices?.[0]?.message?.content || "No response received.";
          
          // Extract auto-detected deadline if present
          const dateMatch = /\b(202[4-9]-\d{2}-\d{2})\b/.exec(content);
          const deadline = dateMatch ? dateMatch[1] : null;

          // Extract verdict
          let verdict = "REVIEW";
          if (/\[?GO\]?/i.test(content) && !/NO-GO/i.test(content)) verdict = "GO";
          else if (/NO-GO/i.test(content)) verdict = "NO-GO";

          resolve({
            report: content,
            deadline,
            verdict
          });
        } catch (e) {
          reject(new Error("Failed to parse AI response: " + e.message));
        }
      });
    });

    req.on('error', err => reject(err));
    req.write(payload);
    req.end();
  });
}

module.exports = {
  analyzeTender
};
