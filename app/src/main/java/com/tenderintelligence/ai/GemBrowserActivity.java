package com.tenderintelligence.ai;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** User-driven browser for public GeM bid listings. Does not scrape or bypass access controls. */
public class GemBrowserActivity extends Activity {
 private WebView browser;
 private ScrollView dashboard;
 private LinearLayout dashboardList;
 private boolean showingDashboard=false;
 private int tenderSortMode=0; // 0 relevance, 1 closing soon, 2 highest AI score
 private boolean autoCollecting=false;
 private int autoPages=0,autoStalls=0;
 private String autoSignature="";
 private final android.os.Handler autoHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 private Button autoButton;
 private boolean batchRunning=false;
 private boolean gemPageReady=false;
 private int searchRetries=0;
 private int batchIndex=0;
 private String[] batchTerms={"Heat exchanger","Tube bundle","Retubing","Condenser","Oil cooler","Sample cooler","SWAS cooler","Pressure vessel","Economizer","Air preheater","LP heater","HP heater"};
 private Button batchButton;
 private ProgressBar scanProgress;
 private ScanRing scanRing;
 private TextView bidTotalValue,matchTotalValue;
 private LinearLayout heroContent;
 private boolean darkMode(){return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;}
 private int themeColor(int light,int dark){return darkMode()?dark:light;}
 // Android 12+ wallpaper-derived Material You palette, with safe fallback.
 private int systemTone(String tone,int fallback){
  if(android.os.Build.VERSION.SDK_INT<31)return fallback;
  int id=getResources().getIdentifier("system_"+tone,"color","android");
  if(id==0)return fallback;
  try{return getColor(id);}catch(Exception ignored){return fallback;}
 }
 private int dynamicPrimary(){return systemTone(darkMode()?"accent1_200":"accent1_600",themeColor(0xff007f73,0xff70e0d0));}
 private int dynamicOnPrimary(){return systemTone(darkMode()?"accent1_900":"accent1_0",darkMode()?0xff18242b:Color.WHITE);}
 private int dynamicSurface(){return darkMode()?0xff0b0b0d:0xfff1f1f3;}
 private int dynamicCard(){return darkMode()?0xff202022:0xffffffff;}
 private int dynamicTonal(){return systemTone(darkMode()?"accent1_800":"accent1_100",themeColor(0xffe9e1f5,0xff42384e));}
 private int dynamicText(){return systemTone(darkMode()?"neutral1_50":"neutral1_900",themeColor(0xff1c1b1f,0xffe6e1e5));}
 private int dynamicMuted(){return systemTone(darkMode()?"neutral2_300":"neutral2_600",themeColor(0xff66636b,0xffc7c3cc));}
 private TextView scanProgressLabel;
 private Button managementHeaderButton;
 private boolean scanMode=true;
 private EditText term;
 private TextView state;
 private final LinkedHashMap<String,JSONObject> collected=new LinkedHashMap<>();
 private TextView collectedCount;
 private static final String OMNI_URL="http://127.0.0.1:20128/v1/chat/completions";
 private EditText pendingPdfScope;
 private final LinkedHashMap<String,JSONObject> aiReviews=new LinkedHashMap<>();
 private static final int PICK_TENDER_PDF=4207, SAVE_SHORTLIST_DOCX=4208;
 private final LinkedHashMap<String,JSONObject> shortlisted=new LinkedHashMap<>();
 private JSONObject pendingWordBid;
 private static final String GEM="https://bidplus.gem.gov.in/all-bids";
 private boolean safe(Uri uri){
  return uri!=null&&"https".equalsIgnoreCase(uri.getScheme())&&"bidplus.gem.gov.in".equalsIgnoreCase(uri.getHost());
 }
 @Override public void onCreate(Bundle bundle){
  super.onCreate(bundle);
  if(bundle!=null)sessionReportId=bundle.getString("report_session_id");
  if(sessionReportId==null)sessionReportId=newSessionReportId();
  getWindow().setStatusBarColor(themeColor(0xfff4faf7,0xff0a121a));getWindow().setNavigationBarColor(themeColor(0xfff4faf7,0xff0a121a));getWindow().getDecorView().setSystemUiVisibility(darkMode()?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setBackgroundColor(dynamicSurface());setContentView(page);page.setOnApplyWindowInsetsListener((v,insets)->{int top=android.os.Build.VERSION.SDK_INT>=30?insets.getInsets(android.view.WindowInsets.Type.statusBars()).top:insets.getSystemWindowInsetTop();v.setPadding(0,top,0,0);return insets;});
  LinearLayout header=new LinearLayout(this);header.setOrientation(0);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(20),dp(16),dp(20),dp(14));header.setBackground(flatShape(dynamicCard(),24));LinearLayout.LayoutParams headerLp=new LinearLayout.LayoutParams(-1,-2);headerLp.setMargins(dp(12),dp(8),dp(12),0);page.addView(header,headerLp);
  LinearLayout brand=new LinearLayout(this);brand.setOrientation(1);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
  TextView heading=new TextView(this);heading.setText("Tender Radar");heading.setTextSize(27);heading.setTypeface(null,Typeface.BOLD);heading.setTextColor(dynamicText());brand.addView(heading);
  TextView note=new TextView(this);note.setText("Your industrial tender workspace");note.setTextSize(13);note.setTextColor(dynamicMuted());brand.addView(note);
  managementHeaderButton=button("▣ "+shortlisted.size());managementHeaderButton.setContentDescription("Tender management, "+shortlisted.size()+" saved");managementHeaderButton.setTextSize(14);managementHeaderButton.setTextColor(dynamicPrimary());managementHeaderButton.setBackground(flatShape(dynamicTonal(),14));LinearLayout.LayoutParams manageHeaderLp=new LinearLayout.LayoutParams(dp(72),dp(48));manageHeaderLp.setMargins(dp(8),0,0,0);header.addView(managementHeaderButton,manageHeaderLp);managementHeaderButton.setOnClickListener(v->showShortlist());
  Button aiSettingsButton=button("⚙");aiSettingsButton.setContentDescription("AI settings");aiSettingsButton.setTextSize(23);aiSettingsButton.setTextColor(dynamicPrimary());aiSettingsButton.setBackground(flatShape(dynamicTonal(),14));LinearLayout.LayoutParams settingsLp=new LinearLayout.LayoutParams(dp(52),dp(52));settingsLp.setMargins(dp(10),0,0,0);header.addView(aiSettingsButton,settingsLp);aiSettingsButton.setOnClickListener(v->showAiSettings());
  View headerSpace=new View(this);page.addView(headerSpace,new LinearLayout.LayoutParams(1,dp(8)));
  LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);controls.setVisibility(View.GONE);page.addView(controls);
  Button back=button("Back");controls.addView(back,new LinearLayout.LayoutParams(0,40,1));back.setOnClickListener(v->{if(browser.canGoBack())browser.goBack();else finish();});
  Button home=button("GeM home");controls.addView(home,new LinearLayout.LayoutParams(0,40,1));home.setOnClickListener(v->browser.loadUrl(GEM));
  Button reload=button("Reload");controls.addView(reload,new LinearLayout.LayoutParams(0,52,1));reload.setOnClickListener(v->browser.reload());
  Spinner picker=new Spinner(this);
  String[] options={"Choose equipment keyword","Heat exchanger","Condenser","Sample cooler","Oil cooler","SWAS cooler","Air cooler","Water cooler","Tube bundle","U tube bundle","Cooling coil","Chiller","Radiator","Dryer","Heater","Economizer","Ejector","Evaporator","Tank","Pressure vessel","Vent condenser","Surface condenser","Gland steam condenser","Turbine oil cooler","Generator air cooler","Stator water cooler","Seal oil cooler","BFP lub oil cooler","Air fin fan cooler","Inter cooler","After cooler","Vacuum pump water cooler","LP heater","HP heater","Air preheater","Fin tube","Plate heat exchanger","Retubing","Hydrojet cleaning","Reboiler","Air receiver","Storage tank","Expansion bellows","FRP cooling tower","Vaporizer","Blender","Reactor"};
  ArrayAdapter<String> pickerAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,options){
   @Override public View getView(int position,View convertView,android.view.ViewGroup parent){View v=super.getView(position,convertView,parent);if(v instanceof TextView)((TextView)v).setTextColor(Color.WHITE);return v;}
   @Override public View getDropDownView(int position,View convertView,android.view.ViewGroup parent){View v=super.getDropDownView(position,convertView,parent);if(v instanceof TextView){((TextView)v).setTextColor(Color.BLACK);v.setBackgroundColor(Color.WHITE);}return v;}
  };pickerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);picker.setAdapter(pickerAdapter);picker.setVisibility(View.GONE);page.addView(picker);
  picker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(position>0)term.setText(options[position]);}});
  LinearLayout findRow=new LinearLayout(this);findRow.setOrientation(LinearLayout.HORIZONTAL);findRow.setVisibility(View.GONE);page.addView(findRow);
  term=new EditText(this);term.setSingleLine(true);term.setTextColor(themeColor(0xff1b2d31,0xffeff9f8));term.setHintTextColor(themeColor(0xff65787d,0xff9bb2b6));term.setHint("Or type equipment keyword");term.setTextSize(14);findRow.addView(term,new LinearLayout.LayoutParams(0,44,1));
  Button find=button("Search");findRow.addView(find);find.setOnClickListener(v->{
   String q=term.getText().toString().trim();
   state.setText("Submitting GeM bid search…");
   ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(term.getWindowToken(),0);
   term.clearFocus();
   String js="(function(){try{"+
    "var inputs=Array.from(document.querySelectorAll('input')).filter(e=>{var r=e.getBoundingClientRect();return r.width>100&&r.height>0&&!e.disabled});"+
    "var el=inputs.find(e=>/searchBid|search_bid|bid_search/i.test((e.id||'')+' '+(e.name||'')))||inputs.find(e=>/heat exchanger|head exchanger|oil cooler/i.test(e.value||''))||inputs.find(e=>{var p=e.parentElement;return p&&/contains/i.test((p.parentElement&&p.parentElement.innerText||'').slice(0,90))})||inputs.filter(e=>/text|search/i.test(e.type||'text')).slice(-1)[0];"+
    "if(!el)return 'NO_BID_INPUT';"+
    "var keyword="+org.json.JSONObject.quote(q)+";if(!keyword.trim())keyword=(el.value||'').trim();if(!keyword)return 'NO_KEYWORD';"+
    "var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;setter.call(el,keyword);"+
    "el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));"+
    "var r=el.getBoundingClientRect();"+
    "var bidSection=Array.from(document.querySelectorAll('h1,h2,h3,h4,div,span')).find(e=>e.children.length===0&&/^(Bid Listing)$/i.test((e.textContent||'').trim()));"+
    "var candidates=Array.from(document.querySelectorAll('button,input[type=submit],a,[role=button]')).filter(e=>{var b=e.getBoundingClientRect();return b.width>18&&b.height>18&&b.left>=r.right-12&&b.left<r.right+145&&Math.abs(b.top-r.top)<38&&!e.disabled});"+
    "candidates.sort((a,b)=>Math.abs(a.getBoundingClientRect().left-r.right)-Math.abs(b.getBoundingClientRect().left-r.right));"+
    "var btn=candidates[0];if(!btn)return 'BID_INPUT_SET_BUT_BUTTON_NOT_FOUND';"+
    "el.blur();btn.click();return 'SEARCH_CLICKED';"+
    "}catch(e){return 'ERROR:'+e.message}})()";
   browser.evaluateJavascript(js,result->{
    if(result.contains("SEARCH_CLICKED"))state.setText("GeM search submitted. Check results below.");
    else if(result.contains("NO_KEYWORD"))state.setText("Type a keyword in GeM Bid Listing or choose one above.");
    else if(result.contains("NO_BID_INPUT"))state.setText("GeM bid search field not found. Use the search box inside the webpage.");
    else state.setText("Keyword entered, but GeM's search button was not detected. Tap the blue magnifying glass beside GeM's bid search field.");
   });
  });
  LinearLayout captureRow=new LinearLayout(this);captureRow.setOrientation(LinearLayout.HORIZONTAL);captureRow.setVisibility(View.GONE);page.addView(captureRow);
  Button collect=button("Collect visible bids");captureRow.addView(collect,new LinearLayout.LayoutParams(0,dp(52),2));collect.setOnClickListener(v->collectVisible());
  Button review=button("Tender dashboard");captureRow.addView(review,new LinearLayout.LayoutParams(0,dp(52),2));review.setOnClickListener(v->toggleDashboard());
  autoButton=button("Auto collect all pages");autoButton.setVisibility(View.GONE);page.addView(autoButton,new LinearLayout.LayoutParams(-1,dp(48)));
  autoButton.setOnClickListener(v->{if(autoCollecting){batchRunning=false;stopAutoCollect("Stopped by user.");}else startAutoCollect();});
  scanRing=new ScanRing();LinearLayout.LayoutParams ringLp=new LinearLayout.LayoutParams(dp(208),dp(208));ringLp.gravity=Gravity.CENTER_HORIZONTAL;ringLp.setMargins(0,dp(2),0,dp(12));page.addView(scanRing,ringLp);
  LinearLayout statsStrip=new LinearLayout(this);statsStrip.setOrientation(LinearLayout.HORIZONTAL);statsStrip.setPadding(dp(20),0,dp(20),dp(12));page.addView(statsStrip);bidTotalValue=addGlassStat(statsStrip,"Bids");matchTotalValue=addGlassStat(statsStrip,"Matches");
  batchButton=button("Find tenders");batchButton.setTextSize(17);batchButton.setTypeface(null,Typeface.BOLD);batchButton.setTextColor(dynamicOnPrimary());batchButton.setBackground(rounded(dynamicPrimary(),16,dynamicPrimary()));batchButton.setElevation(0);LinearLayout.LayoutParams heroLp=new LinearLayout.LayoutParams(-1,dp(60));heroLp.setMargins(dp(20),0,dp(20),dp(12));page.addView(batchButton,heroLp);
  batchButton.setOnClickListener(v->{if(batchRunning){batchRunning=false;scanProgress.setIndeterminate(false);stopAutoCollect("Batch stopped.");batchButton.setText("Find tenders");}else startBatch();});
  scanProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);scanProgress.setMax(batchTerms.length);scanProgress.setProgress(0);scanProgress.setIndeterminate(false);scanProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(dynamicPrimary()));scanProgress.setVisibility(View.GONE);LinearLayout.LayoutParams progressLp=new LinearLayout.LayoutParams(-1,dp(5));progressLp.setMargins(dp(24),0,dp(24),0);page.addView(scanProgress,progressLp);
  scanProgressLabel=new TextView(this);scanProgressLabel.setTextColor(dynamicMuted());scanProgressLabel.setPadding(dp(24),dp(10),dp(24),dp(5));scanProgressLabel.setTextSize(13);scanProgressLabel.setText("Ready to scan • Results will appear below");page.addView(scanProgressLabel);
  collectedCount=new TextView(this);collectedCount.setTextColor(dynamicPrimary());collectedCount.setPadding(dp(24),dp(4),dp(24),dp(4));collectedCount.setTextSize(13);page.addView(collectedCount);
  restoreAiReviews();restoreShortlist();if(bundle==null){migratePreviousReports();clearPreviousSearchSession();}getPreferences(0).edit().remove("openrouter_key_iv").remove("openrouter_key_cipher").remove("gemini_iv").remove("gemini_encrypted").remove("groq_iv").remove("groq_encrypted").remove("remember_openrouter_key").remove("remember_provider_keys").apply();updateCollectedCount();
  Button next=button("Next");findRow.addView(next);next.setOnClickListener(v->{
   state.setText("Looking for GeM next results page…");
   String js="(function(){"+
    "var links=Array.from(document.querySelectorAll('a,button'));"+
    "var visible=e=>{var r=e.getBoundingClientRect();return r.width>0&&r.height>0&&!e.disabled&&e.getAttribute('aria-disabled')!=='true'&&!e.classList.contains('disabled')};"+
    "var el=links.find(e=>visible(e)&&(/^(next|next page|›|»|>)$/i.test((e.innerText||'').trim())||/next/i.test(e.getAttribute('aria-label')||'')||/next/i.test(e.getAttribute('rel')||'')));"+
    "if(!el)return 'NEXT_NOT_FOUND';el.click();return 'NEXT_CLICKED';"+
    "})()";
   browser.evaluateJavascript(js,result->{
    if(result.contains("NEXT_CLICKED"))state.setText("Next page requested. Waiting for GeM…");
    else state.setText("Next-page control not found. Use GeM pagination inside the webpage.");
   });
  });
  state=new TextView(this);state.setTextColor(Color.rgb(102,119,121));state.setText("Opening GeM…");state.setPadding(dp(24),dp(4),dp(24),dp(10));state.setTextSize(12);page.addView(state);
  FrameLayout contentLayer=new FrameLayout(this);page.addView(contentLayer,new LinearLayout.LayoutParams(-1,0,1));
  browser=new WebView(this);contentLayer.addView(browser,new FrameLayout.LayoutParams(-1,-1));
  dashboard=new ScrollView(this);dashboard.setFillViewport(true);dashboard.setVisibility(View.VISIBLE);showingDashboard=true;dashboard.setBackgroundColor(dynamicSurface());contentLayer.addView(dashboard,new FrameLayout.LayoutParams(-1,-1));
  dashboardList=new LinearLayout(this);dashboardList.setOrientation(1);dashboardList.setPadding(dp(20),dp(8),dp(20),dp(24));dashboardList.setBackgroundColor(dynamicSurface());dashboard.addView(dashboardList);
  heroContent=new LinearLayout(this);heroContent.setOrientation(1);dashboardList.addView(heroContent);
  // Keep title, subtitle and compact settings gear together in the top header.
  // Previously these were detached and stacked as three giant dashboard rows.
  // Single cohesive scanning card matching the approved dashboard concept.
  LinearLayout scanCard=new LinearLayout(this);scanCard.setOrientation(1);scanCard.setPadding(dp(16),dp(14),dp(16),dp(14));
  scanCard.setBackground(glass(dynamicCard(),16));
  LinearLayout.LayoutParams scanCardLp=new LinearLayout.LayoutParams(-1,-2);scanCardLp.setMargins(0,dp(4),0,dp(12));heroContent.addView(scanCard,scanCardLp);
  LinearLayout scanOverview=new LinearLayout(this);scanOverview.setOrientation(0);scanOverview.setGravity(Gravity.CENTER_VERTICAL);scanCard.addView(scanOverview);
  ((android.view.ViewGroup)scanRing.getParent()).removeView(scanRing);
  LinearLayout.LayoutParams ringParams=new LinearLayout.LayoutParams(dp(104),dp(104));ringParams.setMargins(0,0,dp(14),0);scanOverview.addView(scanRing,ringParams);
  LinearLayout scanSummary=new LinearLayout(this);scanSummary.setOrientation(1);scanSummary.setGravity(Gravity.CENTER_VERTICAL);
  scanOverview.addView(scanSummary,new LinearLayout.LayoutParams(0,-2,1));
  TextView scanHeading=dashText("Scanning tenders",18,dynamicText());scanHeading.setTypeface(null,Typeface.BOLD);scanSummary.addView(scanHeading);
  for(View item:new View[]{scanProgressLabel,collectedCount}){
   ((android.view.ViewGroup)item.getParent()).removeView(item);scanSummary.addView(item);
   item.setPadding(0,dp(3),0,0);
  }
  scanProgressLabel.setTextSize(12);collectedCount.setTextSize(12);
  ((android.view.ViewGroup)batchButton.getParent()).removeView(batchButton);
  LinearLayout.LayoutParams scanLp=new LinearLayout.LayoutParams(-1,dp(50));scanLp.setMargins(0,dp(12),0,0);scanCard.addView(batchButton,scanLp);
  ((android.view.ViewGroup)scanProgress.getParent()).removeView(scanProgress);scanCard.addView(scanProgress,new LinearLayout.LayoutParams(-1,dp(4)));
  ((android.view.ViewGroup)state.getParent()).removeView(state);state.setTextSize(12);state.setTextColor(dynamicMuted());state.setPadding(dp(3),dp(8),dp(3),0);state.setMinLines(2);state.setMaxLines(2);state.setEllipsize(android.text.TextUtils.TruncateAt.END);scanCard.addView(state);
  // Hidden counters still receive scan updates, while the summary displays live totals.
  statsStrip.setVisibility(View.GONE);
  heading.setPadding(0,0,0,dp(2));note.setPadding(0,0,0,0);
  heading.setTextColor(dynamicText());note.setTextColor(dynamicMuted());
  header.setBackground(glass(dynamicCard(),16));
  aiSettingsButton.setText("⚙");aiSettingsButton.setContentDescription("AI settings");
  aiSettingsButton.setTextColor(dynamicText());aiSettingsButton.setBackground(glass(dynamicSurface(),16));
  LinearLayout.LayoutParams compactSettings=new LinearLayout.LayoutParams(dp(44),dp(44));compactSettings.setMargins(dp(8),0,0,0);aiSettingsButton.setLayoutParams(compactSettings);
  // Align the header with the dashboard cards and use a real vector bookmark.
  headerLp.setMargins(dp(20),dp(8),dp(20),0);header.setLayoutParams(headerLp);
  header.setPadding(dp(16),dp(16),dp(16),dp(14));
  heading.setTextSize(24);heading.setMaxLines(1);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
  heading.setAutoSizeTextTypeUniformWithConfiguration(18,24,1,android.util.TypedValue.COMPLEX_UNIT_SP);
  note.setMaxLines(2);note.setEllipsize(android.text.TextUtils.TruncateAt.END);
  android.graphics.drawable.Drawable savedIcon=getDrawable(R.drawable.ic_saved_tenders).mutate();
  savedIcon.setTint(dynamicPrimary());savedIcon.setBounds(0,0,dp(20),dp(20));
  managementHeaderButton.setCompoundDrawablesRelative(savedIcon,null,null,null);managementHeaderButton.setCompoundDrawablePadding(dp(6));
  managementHeaderButton.setText(String.valueOf(shortlisted.size()));managementHeaderButton.setMinWidth(dp(64));managementHeaderButton.setMinimumWidth(dp(64));managementHeaderButton.setSingleLine(true);managementHeaderButton.setMaxWidth(dp(96));managementHeaderButton.setEllipsize(android.text.TextUtils.TruncateAt.END);
  managementHeaderButton.setPadding(dp(10),0,dp(10),0);
  LinearLayout.LayoutParams savedLp=new LinearLayout.LayoutParams(-2,dp(48));savedLp.setMargins(dp(8),0,0,0);managementHeaderButton.setLayoutParams(savedLp);
  compactSettings.width=dp(48);compactSettings.height=dp(48);aiSettingsButton.setLayoutParams(compactSettings);
  scanProgressLabel.setMinLines(2);scanProgressLabel.setMaxLines(2);scanProgressLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
  collectedCount.setMinLines(2);collectedCount.setMaxLines(2);collectedCount.setEllipsize(android.text.TextUtils.TruncateAt.END);
  WebSettings settings=browser.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setSupportZoom(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);
  browser.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
    Uri url=request.getUrl();if(safe(url))return false;
    if("https".equalsIgnoreCase(url.getScheme())){startActivity(new Intent(Intent.ACTION_VIEW,url));return true;}
    return true;
   }
   @Override public void onPageFinished(WebView view,String url){gemPageReady=true;if(!batchRunning)state.setText("GeM loaded. Tap Find tenders to scan.");}
   @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError error){if(req.isForMainFrame())state.setText("Unable to load GeM in WebView. Try opening in your browser.");}
  });
  browser.setDownloadListener((url,userAgent,disposition,mime,length)->{Uri uri=Uri.parse(url);if(safe(uri))startActivity(new Intent(Intent.ACTION_VIEW,uri));else state.setText("Download blocked: external URL");});
  browser.loadUrl(GEM);renderDashboard();scheduleAutoReview();
 }
 private void updateCollectedCount(){int matches=0;for(JSONObject j:collected.values())if(relevantTitle(j.optString("title")))matches++;if(bidTotalValue!=null)bidTotalValue.setText(String.valueOf(collected.size()));if(matchTotalValue!=null)matchTotalValue.setText(String.valueOf(matches));if(collectedCount!=null)collectedCount.setText(matches+" matching tenders • "+collected.size()+" bids collected");}
 // Search data is session-only; shortlist records and their saved reviews are permanent.
 private void clearPreviousSearchSession(){
  collected.clear();
  getPreferences(0).edit().remove("gem_collected").apply();
 }
 private void restoreCollected(){
  try{JSONArray a=new JSONArray(getPreferences(0).getString("gem_collected","[]"));for(int i=0;i<a.length();i++){JSONObject j=a.optJSONObject(i);if(j!=null&&!j.optString("bidId").isEmpty())collected.put(j.optString("bidId"),j);}}catch(Exception ignored){}
 }
 private void persistCollected(){
  JSONArray a=new JSONArray();for(JSONObject j:collected.values())a.put(j);
  getPreferences(0).edit().putString("gem_collected",a.toString()).apply();
 }
 private void collectVisible(){collectVisible(null);}
 private void collectVisible(Runnable done){
  state.setText("Reading visible GeM listing cards…");
  String js="(function(){try{var found=[],seen={};"+
   "var ids=Array.from(document.querySelectorAll('a,span,strong,b,div')).filter(e=>e.children.length<3&&/GEM\\/20\\d{2}\\/B\\/\\d+/i.test(e.textContent||''));"+
   "for(var node of ids){var match=(node.textContent||'').match(/GEM\\/20\\d{2}\\/B\\/\\d+/i);if(!match||seen[match[0]])continue;"+
   "var card=node;for(var n=0;n<7&&card.parentElement;n++){var t=card.innerText||'';if(/End Date\\s*:/i.test(t)&&/Items?\\s*:/i.test(t)&&t.length<3500)break;card=card.parentElement;}"+
   "var text=card.innerText||'';if(text.length>6000)continue;"+
   "var item=text.match(/Items?\\s*:\\s*([^\\n]+)/i),end=text.match(/End Date\\s*:\\s*(\\d{2}-\\d{2}-\\d{4}(?:\\s+\\d{1,2}:\\d{2}\\s*(?:AM|PM))?)/i);"+
   "var anchor=node.closest('a[href]')||Array.from(card.querySelectorAll('a[href]')).find(a=>(a.textContent||'').includes(match[0]));"+
   "var buyerMatch=text.match(/Department Name And Address\\s*:\\s*([^\\n]+)(?:\\n([^\\n]+))?/i);var buyer=buyerMatch?((buyerMatch[2]||buyerMatch[1]||'').trim()):'';"+
   "var docs=Array.from(card.querySelectorAll('a[href]')).filter(a=>/\\.pdf(?:[?#]|$)|showbidDocument\\/\\d+|download.*(bid|document|specification|atc)/i.test(a.href||'')).map(a=>a.href).filter(u=>/^https:\\/\\/([a-z0-9-]+\\.)*gem\\.gov\\.in(?:[:\\/]|$)/i.test(u)).slice(0,3);"+
   "seen[match[0]]=true;found.push({bidId:match[0],title:item?item[1].trim():'GeM bid',deadlineText:end?end[1].trim():'',buyer:buyer,documentLinks:docs,url:anchor?anchor.href:'',source:'GeM visible listing'});if(found.length>=100)break;}"+
   "return JSON.stringify({items:found,matchedNodes:ids.length});}catch(e){return JSON.stringify({error:e.message})}})()";
  browser.evaluateJavascript(js,result->{
   try{
    String decoded=new JSONArray("["+result+"]").getString(0);
    JSONObject payload=new JSONObject(decoded);if(payload.has("error")){state.setText("GeM collection error: "+payload.optString("error"));return;}
    JSONArray items=payload.optJSONArray("items");if(items==null)throw new Exception("Missing items");int added=0;
    for(int i=0;i<items.length();i++){
     JSONObject j=items.optJSONObject(i);if(j==null)continue;String id=j.optString("bidId");
     if(!id.matches("GEM/20\\d{2}/B/\\d+"))continue;
     String url=j.optString("url");if(!safe(Uri.parse(url)))j.put("url",GEM);
     if(!collected.containsKey(id)){collected.put(id,j);added++;}else{JSONObject old=collected.get(id);if(old!=null){if(old.optString("buyer").isEmpty()&&!j.optString("buyer").isEmpty())old.put("buyer",j.optString("buyer"));if(old.optJSONArray("documentLinks")==null&&j.optJSONArray("documentLinks")!=null)old.put("documentLinks",j.optJSONArray("documentLinks"));}}
    }
    persistCollected();updateCollectedCount();if(showingDashboard)renderDashboard();scheduleAutoReview();
    state.setText("Detected "+items.length()+" GeM bid IDs; saved "+added+" new bids.");
    if(done!=null)done.run();
   }catch(Exception e){state.setText("Could not parse bid cards. GeM page format may have changed.");}
  });
 }
 private void startBatch(){
  if(autoCollecting){state.setText("Stop the current collection first.");return;}
  batchRunning=true;batchIndex=0;searchRetries=0;scanProgress.setIndeterminate(true);scanProgress.setProgress(0);if(scanRing!=null)scanRing.setValue(0);scanProgressLabel.setText("Starting scan • 0 / "+batchTerms.length+" equipment groups");batchButton.setText("Stop scanning");nextBatch();
 }
 private void nextBatch(){
  if(!batchRunning)return;
  if(batchIndex>=batchTerms.length){
   batchRunning=false;autoCollecting=false;scanProgress.setIndeterminate(false);autoButton.setText("Auto collect all pages");
   batchButton.setText("Find tenders • Scan equipment categories");
   updateCollectedCount();if(scanProgress!=null){scanProgress.setProgress(batchTerms.length);if(scanRing!=null)scanRing.setValue(batchTerms.length);scanProgressLabel.setText("Scan complete • "+collected.size()+" unique bids collected");}if(showingDashboard)renderDashboard();state.setText("Scan complete. Review matching tenders below.");return;
  }
  if(!gemPageReady){state.setText("Waiting for GeM listing page to load…");autoHandler.postDelayed(this::nextBatch,1200);return;}
  String keyword=batchTerms[batchIndex++];term.setText(keyword);if(scanProgress!=null){scanProgress.setProgress(batchIndex-1);if(scanRing!=null)scanRing.setValue(batchIndex-1);scanProgressLabel.setText("Scanning "+batchIndex+" / "+batchTerms.length+" • "+keyword+" • "+collected.size()+" bids collected");}
  state.setText("Searching "+batchIndex+"/"+batchTerms.length+": "+keyword);
  String js="(function(){try{var inputs=Array.from(document.querySelectorAll('input')).filter(e=>{var r=e.getBoundingClientRect();return r.width>100&&r.height>0&&!e.disabled});"+
   "var el=inputs.find(e=>/searchBid|search_bid|bid_search/i.test((e.id||'')+' '+(e.name||'')))||inputs.find(e=>{var p=e.parentElement;return p&&/contains/i.test((p.parentElement&&p.parentElement.innerText||'').slice(0,90))})||inputs.filter(e=>/text|search/i.test(e.type||'text')).slice(-1)[0];"+
   "if(!el)return 'NO_INPUT';var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;setter.call(el,"+JSONObject.quote(keyword)+");el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));"+
   "var r=el.getBoundingClientRect();var buttons=Array.from(document.querySelectorAll('button,input[type=submit],a,[role=button]')).filter(e=>{var b=e.getBoundingClientRect();return b.width>18&&b.height>18&&b.left>=r.right-12&&b.left<r.right+145&&Math.abs(b.top-r.top)<38&&!e.disabled});"+
   "buttons.sort((a,b)=>Math.abs(a.getBoundingClientRect().left-r.right)-Math.abs(b.getBoundingClientRect().left-r.right));if(!buttons.length)return 'NO_BUTTON';el.blur();buttons[0].click();return 'OK';}catch(e){return 'ERROR'}})()";
  browser.evaluateJavascript(js,result->{
   if(!batchRunning)return;
   if(!result.contains("OK")){if(searchRetries++<4){batchIndex--;state.setText("Waiting for GeM search controls… retry "+searchRetries+"/4");autoHandler.postDelayed(this::nextBatch,1800);return;}state.setText("GeM search controls unavailable for "+keyword+". Portal may have changed or blocked loading.");batchRunning=false;batchButton.setText("Find tenders • Scan equipment categories");return;}
   searchRetries=0;
   autoCollecting=true;autoPages=0;autoStalls=0;autoSignature="";
   autoButton.setText("Stop auto collection");
   autoHandler.postDelayed(this::autoStep,2600);
  });
 }
 private void stopAutoCollect(String message){
  autoCollecting=false;autoHandler.removeCallbacksAndMessages(null);
  if(autoButton!=null)autoButton.setText("Auto collect all pages");
  state.setText(message+" Pages processed: "+autoPages+" • Total unique bids: "+collected.size());
 }
 private void startAutoCollect(){
  if(browser==null)return;
  autoCollecting=true;autoPages=0;autoStalls=0;autoSignature="";
  autoButton.setText("Stop auto collection");
  state.setText("Auto collection started. Keep the app open.");
  autoHandler.postDelayed(this::autoStep,600);
 }
 private void autoStep(){
  if(!autoCollecting)return;
  String js="(function(){var cards=Array.from(document.querySelectorAll('a,span,strong,b,div')).filter(e=>e.children.length<3&&/GEM\\/20\\d{2}\\/B\\/\\d+/i.test(e.textContent||''));var ids=Array.from(new Set(cards.map(e=>(e.textContent||'').match(/GEM\\/20\\d{2}\\/B\\/\\d+/i)).filter(Boolean).map(m=>m[0])));var next=Array.from(document.querySelectorAll('a,button')).find(e=>{var t=(e.innerText||'').trim(),r=e.getBoundingClientRect();return r.width>0&&r.height>0&&!e.disabled&&e.getAttribute('aria-disabled')!=='true'&&!e.classList.contains('disabled')&&/^(next|next page|›|»|>)$/i.test(t)});return JSON.stringify({signature:ids.slice(0,4).join('|')+'#'+ids.length,count:ids.length,hasNext:!!next})})()";
  browser.evaluateJavascript(js,result->{
   if(!autoCollecting)return;
   try{
    JSONObject data=new JSONObject(new JSONArray("["+result+"]").getString(0));
    String signature=data.optString("signature");int count=data.optInt("count");
    if(count==0){if(++autoStalls>=8){autoCollecting=false;batchRunning=false;if(batchButton!=null)batchButton.setText("Find tenders • Scan equipment categories");if(scanProgressLabel!=null)scanProgressLabel.setText("Scan paused • GeM returned no detectable bid cards for "+batchTerms[Math.max(0,batchIndex-1)]);state.setText("No GeM bid cards detected. Check that the GeM listing loaded and displays results. Scan stopped instead of skipping categories.");return;}autoHandler.postDelayed(this::autoStep,1500);return;}
    if(signature.equals(autoSignature)){if(++autoStalls>=8){autoCollecting=false;batchRunning=false;if(batchButton!=null)batchButton.setText("Find tenders • Scan equipment categories");state.setText("GeM results did not change. Scan stopped to avoid silently skipping bids. Open the GeM listing and check search or pagination.");return;}autoHandler.postDelayed(this::autoStep,1500);return;}
    autoSignature=signature;autoStalls=0;autoPages++;if(scanProgressLabel!=null&&batchRunning)scanProgressLabel.setText("Scanning "+batchIndex+"/"+batchTerms.length+" • page "+autoPages+" • "+collected.size()+" collected");
    state.setText("Collecting page "+autoPages+"…");
    collectVisible(()->{
     if(!autoCollecting)return;
     if(autoPages>=(batchRunning?25:100)){if(batchRunning){batchRunning=false;autoCollecting=false;scanProgress.setIndeterminate(false);batchButton.setText("Find tenders");scanProgressLabel.setText("Scan incomplete • page safety limit reached for "+batchTerms[Math.max(0,batchIndex-1)]);state.setText("Stopped at 25 pages for this category. Results collected so far are retained; scan was not complete.");}else stopAutoCollect("Reached page safety limit.");return;}
     String nextJs="(function(){var a=Array.from(document.querySelectorAll('a,button')).find(e=>{var r=e.getBoundingClientRect();return r.width>0&&r.height>0&&!e.disabled&&e.getAttribute('aria-disabled')!=='true'&&!e.classList.contains('disabled')&&/^(next|next page|›|»|>)$/i.test((e.innerText||'').trim())});if(!a)return 'END';a.click();return 'NEXT'})()";
     browser.evaluateJavascript(nextJs,out->{
      if(!autoCollecting)return;
      if(!out.contains("NEXT")){if(batchRunning){autoCollecting=false;autoHandler.postDelayed(this::nextBatch,800);}else stopAutoCollect("Reached the last available page.");return;}
      state.setText("Collected "+autoPages+" pages, "+collected.size()+" unique bids. Waiting for next GeM page…");
      autoHandler.postDelayed(this::autoStep,1800);
     });
    });
   }catch(Exception e){stopAutoCollect("Could not inspect GeM pagination.");}
  });
 }
 @Override protected void onDestroy(){saveShortlistWordAutomatically();autoCollecting=false;autoHandler.removeCallbacksAndMessages(null);if(browser!=null){browser.destroy();browser=null;}super.onDestroy();}
 private Date parsedDeadline(String value){
  try{SimpleDateFormat format=new SimpleDateFormat("dd-MM-yyyy h:mm a",Locale.ENGLISH);format.setLenient(false);return format.parse(value.trim());}catch(Exception e){return null;}
 }
 private boolean relevantTitle(String value){
  String t=value.toLowerCase(Locale.ROOT);
  if(t.matches(".*(foot massager|room heater|gas stove|hot plate|hair dryer|water heater|space heater|fan heater|electric heater|hand heater|heat shrink|heat gun|laboratory heater).*"))return false;
  return t.matches(".*(heat exchanger|condenser|retub|re-tub|hydrojet|industrial cooler|oil cooler|sample cooler|sampling cooler|swas cooler|tube bundle|pressure vessel|evaporator|economi[sz]er|industrial chiller|industrial radiator|generator radiator|cooling coil|air preheater|air pre heater|shell and tube|shell & tube|tubesheet|tube sheet|reboiler|industrial heater|lp heater|hp heater|fin tube|finned tube|stator water cooler|generator air cooler|seal oil cooler|intercooler|inter cooler|aftercooler|after cooler|air fin fan|vaporizer|expansion bellow|frp cooling tower|air receiver|storage tank|process tank|industrial blender|industrial reactor|flue gas cooler|heat recovery).*");
 }

 private String workCategory(String title){
  String t=title.toLowerCase(Locale.ROOT);
  if(t.matches(".*(retub|re-tub|refurbish|overhaul|hydrojet|cleaning of heat exchanger|repair of heat exchanger|maintenance of heat exchanger).*"))return "Refurbishment / service";
  if(t.matches(".*(tube bundle|u tube bundle|u-tube bundle|tubesheet|tube sheet).*"))return "Tube bundle / components";
  if(t.matches(".*(heat exchanger|condenser|industrial cooler|oil cooler|sample cooler|sampling cooler|swas cooler|pressure vessel|reboiler|economi[sz]er|evaporator|air preheater|air pre heater|lp heater|hp heater|fin tube|finned tube|stator water cooler|generator air cooler|seal oil cooler|intercooler|inter cooler|aftercooler|after cooler|air fin fan|vaporizer|expansion bellow|frp cooling tower|air receiver|storage tank|process tank|industrial blender|industrial reactor|flue gas cooler|heat recovery).*"))return "Equipment manufacturing";
  return "Other equipment / manual review";
 }
 private int qualificationScore(JSONObject bid){
  String title=bid.optString("title").toLowerCase(Locale.ROOT);
  int points=workCategory(title).equals("Refurbishment / service")?75:workCategory(title).equals("Tube bundle / components")?70:workCategory(title).equals("Equipment manufacturing")?65:35;
  if(title.matches(".*(supply of spare|spare part|consumable|gasket only|seal only|repair kit).*"))points-=25;
  JSONObject ai=aiReviews.get(bid.optString("bidId"));
  if(ai!=null){
   String verdict=ai.optString("verdict");
   if("Not relevant".equals(verdict))points=5;
   else if("Relevant".equals(verdict))points=Math.min(90,points+10);
  }
  return Math.max(0,points);
 }
 private String qualificationLabel(JSONObject bid){
  int score=qualificationScore(bid);
  return score>=70?"High-priority review":score>=50?"Review scope":"Low-priority review";
 }
 private void toggleDashboard(){
  showingDashboard=!showingDashboard;
  dashboard.setVisibility(showingDashboard?View.VISIBLE:View.GONE);
  browser.setVisibility(View.VISIBLE);
  if(showingDashboard){renderDashboard();scheduleAutoReview();}
  state.setText(showingDashboard?"Dashboard: open tenders with title matches. Technical eligibility requires document review.":"GeM browser ready.");
 }
 private TextView dashText(String value,int size,int color){
  TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(10),dp(8),dp(10),dp(8));return t;
 }

 private final java.util.HashSet<String> analyzingBids=new java.util.HashSet<>();
 private final java.util.HashSet<String> failedAutoReviews=new java.util.HashSet<>();
 private final java.util.HashSet<String> apiFailedReviews=new java.util.HashSet<>();
 private boolean aiConnectionPaused=false;
 private boolean autoReviewActive=false;
 private String currentReviewId="";
 private final Runnable autoReviewTask=()->{
  if(autoReviewActive||aiConnectionPaused||!showingDashboard)return;
  for(JSONObject bid:collected.values()){
   String id=bid.optString("bidId");
   if(shortlisted.containsKey(id)||!relevantTitle(bid.optString("title"))||aiReviews.containsKey(id)||analyzingBids.contains(id)||failedAutoReviews.contains(id)||apiFailedReviews.contains(id))continue;
   Date due=parsedDeadline(bid.optString("deadlineText"));
   if(due==null||!due.after(new Date()))continue;
   autoReviewActive=true;currentReviewId=id;analyzeInline(bid);return;
  }
 };
 private void scheduleAutoReview(){autoHandler.removeCallbacks(autoReviewTask);autoHandler.postDelayed(autoReviewTask,900);}
 private void finishAutoReview(){autoReviewActive=false;currentReviewId="";scheduleAutoReview();}
 private void analyzeInline(JSONObject bid){
  String id=bid.optString("bidId");
  if(analyzingBids.contains(id))return;
  if(aiReviews.containsKey(id)){showSavedReview(id);return;}
  
  analyzingBids.add(id);renderDashboard();
  EditText extracted=new EditText(this);
  extracted.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence x,int a,int c,int z){}
   public void onTextChanged(CharSequence x,int a,int b,int c){}
   public void afterTextChanged(android.text.Editable x){
    if(!"auto_pdf_ready".equals(extracted.getTag())||x.length()<80)return;
    extracted.setTag("ai_running");
    String document=x.toString(),modelId=omniModel();
    final String evidenceType="Public GeM PDF (attachments and complete technical specifications not verified)";
    state.setText("Analyzing "+id+" with AI…");
    new Thread(()->{
     try{
      String response=requestProviderReview("You review Indian industrial fabrication tenders for ADITYA UDYOG HEATXCHANGER PVT. LTD. Its user-supplied catalogue describes manufacturing, refurbishment and services for shell-and-tube, fin-tube and plate heat exchangers, condensers, turbine/generator/stator/oil/air coolers, LP/HP heaters, air preheaters, U-tube and copper/brass tube bundles, radiators, vaporizers, pressure vessels, storage tanks, reactors, blenders, expansion bellows, FRP cooling towers, and customized retubing, hydrojet cleaning and rubber/FRP lining. The facility catalogue lists plate rolling, tube flaring, MIG/TIG/ARC welding, machining, hydraulic press, NDT and third-party inspection arrangements. Treat these as claimed general capabilities, NOT proof of qualification for a specific tender. Prioritize manufacturing, refurbishment, retubing, tube bundles, heat transfer equipment and plant service work; reject unrelated household appliances, generic spare parts and standalone material supply unless document evidence establishes fabrication scope. Return these clearly labeled concise sections in order: Verdict (Relevant/Unclear/Not relevant); Buyer; Equipment; Quantity; Closing date; Scope; Fabrication fit (Suitable/Possible/Unclear/Not suitable, with reason); Missing specifications (drawings, material grades, dimensions, design pressure, tests and inspection only where not provided); Risks; Recommendation (Quote candidate/Manual review/Reject). Distinguish supply of spare tubes from fabrication of a complete exchanger, tube bundle or retubing. Only use supplied document evidence; mark unknown fields Not provided. Never invent specifications or claim the tender is verified. Distinguish industrial equipment from household appliances.","EVIDENCE STATUS: "+evidenceType+"\\nDo not assume the full buyer specification, drawings or attachments were provided. Explicitly list what must be checked before quotation.\\n"+"GeM bid: "+bid.optString("bidId")+"\\nListing title: "+bid.optString("title")+"\\nClosing date from listing: "+bid.optString("deadlineText")+"\\nDocument excerpt:\\n"+document.substring(0,Math.min(document.length(),18000)));
      runOnUiThread(()->{saveAiReview(bid,response,modelId,evidenceType);state.setText("Provisional AI review saved for "+bid.optString("bidId"));renderDashboard();});

     }catch(Exception e){runOnUiThread(()->{state.setText("AI connection failed for "+id+". Analysis paused until connection is restored in Settings.");apiFailedReviews.add(id);aiConnectionPaused=true;});}
     finally{runOnUiThread(()->{analyzingBids.remove(id);renderDashboard();finishAutoReview();});}
    }).start();
   }
  });
  Button trigger=button("Loading");trigger.setTag("auto_review");
  state.setText("Reading GeM document for "+id+"…");
  loadPublicPdfInto(bid,extracted,trigger);
  autoHandler.postDelayed(()->{
   if(analyzingBids.contains(id)&&!"ai_running".equals(extracted.getTag())&&extracted.length()<80){
    analyzingBids.remove(id);failedAutoReviews.add(id);renderDashboard();finishAutoReview();
    state.setText("Public PDF not available for "+id+". Manual import may be needed.");
   }
  },45000);
 }
 private void onAutoDocumentFailure(JSONObject bid,String reason){
  String id=bid.optString("bidId");
  if(!analyzingBids.remove(id))return;
  failedAutoReviews.add(id);
  state.setText(reason+" • "+id+" skipped; continuing queue.");
  renderDashboard();
  finishAutoReview();
 }
 private void addMetric(LinearLayout row,String value,String label,int color){
  LinearLayout tile=new LinearLayout(this);tile.setOrientation(1);tile.setGravity(Gravity.CENTER);
  tile.setPadding(dp(14),dp(8),dp(14),dp(8));
  tile.setBackground(flatShape(dynamicCard(),24));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(78),1);
  boolean firstTile=row.getChildCount()==0;lp.setMargins(firstTile?0:dp(4),0,firstTile?dp(4):0,0);row.addView(tile,lp);
  View symbolView=new View(this){
   private final android.graphics.Paint ink=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
   @Override protected void onDraw(android.graphics.Canvas canvas){
    super.onDraw(canvas);
    float cx=getWidth()/2f,cy=getHeight()/2f,unit=dp(1);
    ink.setColor(dynamicPrimary());ink.setStyle(android.graphics.Paint.Style.STROKE);
    ink.setStrokeWidth(dp(2));ink.setStrokeCap(android.graphics.Paint.Cap.ROUND);
    ink.setStrokeJoin(android.graphics.Paint.Join.ROUND);
    if("Matches".equals(label)){
     canvas.drawCircle(cx-unit*2,cy-unit*2,unit*5.5f,ink);
     canvas.drawLine(cx+unit*2,cy+unit*2,cx+unit*8,cy+unit*8,ink);
    }else if("Reviewed".equals(label)){
     canvas.drawCircle(cx,cy,unit*8,ink);
     android.graphics.Path check=new android.graphics.Path();
     check.moveTo(cx-unit*4,cy);check.lineTo(cx-unit,cy+unit*3);check.lineTo(cx+unit*5,cy-unit*4);
     canvas.drawPath(check,ink);
    }else if("Analyzing".equals(label)){
     canvas.drawArc(cx-unit*8,cy-unit*8,cx+unit*8,cy+unit*8,-90,265,false,ink);
     ink.setStyle(android.graphics.Paint.Style.FILL);
     canvas.drawCircle(cx+unit*7.7f,cy-unit*.8f,unit*1.5f,ink);
    }else{
     canvas.drawCircle(cx,cy,unit*8,ink);
     canvas.drawLine(cx,cy,cx,cy-unit*4.5f,ink);
     canvas.drawLine(cx,cy,cx+unit*3.5f,cy+unit*2,ink);
    }
   }
  };
  symbolView.setContentDescription(label+" status icon");
  LinearLayout topLine=new LinearLayout(this);topLine.setOrientation(0);topLine.setGravity(Gravity.CENTER_VERTICAL);tile.addView(topLine,new LinearLayout.LayoutParams(-1,0,1));
  topLine.addView(symbolView,new LinearLayout.LayoutParams(dp(26),dp(26)));
  if("Analyzing".equals(label)&&!"0".equals(value)){
   android.view.animation.RotateAnimation spin=new android.view.animation.RotateAnimation(0,360,android.view.animation.Animation.RELATIVE_TO_SELF,.5f,android.view.animation.Animation.RELATIVE_TO_SELF,.5f);
   spin.setDuration(1300);spin.setRepeatCount(android.view.animation.Animation.INFINITE);
   spin.setInterpolator(new android.view.animation.LinearInterpolator());symbolView.startAnimation(spin);
  }
  topLine.addView(new View(this),new LinearLayout.LayoutParams(0,dp(1),1));
  TextView count=new TextView(this);count.setText(value);count.setTextSize(23);count.setTypeface(null,Typeface.BOLD);count.setTextColor(dynamicPrimary());count.setGravity(Gravity.RIGHT);topLine.addView(count,new LinearLayout.LayoutParams(-2,-2));
  TextView caption=new TextView(this);caption.setText(label);caption.setTextSize(12);caption.setTextColor(dynamicMuted());caption.setGravity(Gravity.LEFT);tile.addView(caption,new LinearLayout.LayoutParams(-1,-2));
 }
 private void restoreShortlist(){
  try{JSONArray a=new JSONArray(getPreferences(0).getString("shortlisted_tenders","[]"));for(int i=0;i<a.length();i++){JSONObject j=a.optJSONObject(i);if(j!=null&&!j.optString("bidId").isEmpty())shortlisted.put(j.optString("bidId"),j);}}catch(Exception ignored){}
 }
 private String sessionReportId;
 private String selectedHistoryReportId;
 private String newSessionReportId(){return new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",java.util.Locale.US).format(new java.util.Date());}
 private java.io.File reportsFolder(){
  java.io.File base=getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
  java.io.File folder=new java.io.File(base==null?getFilesDir():base,"TenderRadar");
  if(!folder.exists()&&!folder.mkdirs())android.util.Log.w("TenderRadar","Report folder unavailable");
  return folder;
 }
 private java.io.File historyFile(String session){return new java.io.File(reportsFolder(),"Tender_Radar_"+session+".docx");}
 private void openHistoryReport(String session){
  java.io.File file=historyFile(session);
  if(!file.exists()){saveHistoryReports();file=historyFile(session);}
  if(!file.exists()){android.widget.Toast.makeText(this,"Report unavailable",android.widget.Toast.LENGTH_SHORT).show();return;}
  try{
   android.net.Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
   Intent view=new Intent(Intent.ACTION_VIEW);
   view.setDataAndType(uri,"application/vnd.openxmlformats-officedocument.wordprocessingml.document");
   view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   startActivity(Intent.createChooser(view,"Open Word report"));
  }catch(Exception e){android.widget.Toast.makeText(this,"Install a Word-compatible viewer or use Export",android.widget.Toast.LENGTH_LONG).show();}
 }
 private void migratePreviousReports(){
  JSONArray records=historyRecords();
  java.util.HashSet<String> ids=new java.util.HashSet<>();
  for(int i=0;i<records.length();i++){JSONObject rec=records.optJSONObject(i);if(rec!=null){JSONObject bid=rec.optJSONObject("bid");if(bid!=null)ids.add(bid.optString("bidId"));}}
  boolean changed=false;
  for(JSONObject bid:shortlisted.values()){
   String id=bid.optString("bidId");if(id.isEmpty()||ids.contains(id))continue;
   JSONObject rec=new JSONObject();
   try{rec.put("session","previous_saved");rec.put("savedAt",System.currentTimeMillis());rec.put("bid",new JSONObject(bid.toString()));records.put(rec);changed=true;}catch(Exception ignored){}
  }
  if(changed)getPreferences(0).edit().putString("shortlist_history_v1",records.toString()).apply();
  saveHistoryReports();
 }
 private boolean isPreviouslyShortlisted(String id){
  if(shortlisted.containsKey(id))return true;
  JSONArray records=historyRecords();
  for(int i=0;i<records.length();i++){JSONObject rec=records.optJSONObject(i);JSONObject bid=rec==null?null:rec.optJSONObject("bid");if(bid!=null&&id.equals(bid.optString("bidId")))return true;}
  return false;
 }
 private JSONArray historyRecords(){
  try{return new JSONArray(getPreferences(0).getString("shortlist_history_v1","[]"));}
  catch(Exception e){return new JSONArray();}
 }
 private void saveHistoryRecord(JSONObject bid){
  JSONArray records=historyRecords();
  JSONObject record=new JSONObject();
  try{
   record.put("session",sessionReportId);
   record.put("savedAt",System.currentTimeMillis());
   record.put("bid",new JSONObject(bid.toString()));
   records.put(record);
   getPreferences(0).edit().putString("shortlist_history_v1",records.toString()).apply();
  }catch(Exception e){android.util.Log.e("TenderRadar","Could not store session history",e);}
  saveHistoryReports();
 }
 private void saveHistoryReports(){
  JSONArray records=historyRecords();
  java.util.LinkedHashMap<String,java.util.ArrayList<JSONObject>> groups=new java.util.LinkedHashMap<>();
  for(int i=0;i<records.length();i++){
   JSONObject rec=records.optJSONObject(i);if(rec==null)continue;
   String key=rec.optString("session");JSONObject bid=rec.optJSONObject("bid");
   if(key.isEmpty()||bid==null)continue;
   if(!groups.containsKey(key))groups.put(key,new java.util.ArrayList<>());
   groups.get(key).add(bid);
  }
  for(java.util.Map.Entry<String,java.util.ArrayList<JSONObject>> group:groups.entrySet())
   try{TenderWordExport.writeAll(historyFile(group.getKey()),group.getValue(),aiReviews);}
   catch(Exception e){android.util.Log.e("TenderRadar","Session Word history failed",e);}
 }
 private void exportHistoryReport(String session){
  selectedHistoryReportId=session;
  Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
  intent.putExtra(Intent.EXTRA_TITLE,"Tender_Radar_"+session+".docx");
  try{startActivityForResult(intent,4210);}catch(Exception e){state.setText("Word export is unavailable.");}
 }
 private void showWordHistory(){
  JSONArray records=historyRecords();
  java.util.LinkedHashMap<String,Integer> groups=new java.util.LinkedHashMap<>();
  for(int i=0;i<records.length();i++){
   JSONObject rec=records.optJSONObject(i);if(rec==null)continue;
   String key=rec.optString("session");if(!key.isEmpty())groups.put(key,groups.containsKey(key)?groups.get(key)+1:1);
  }
  LinearLayout list=new LinearLayout(this);list.setOrientation(1);list.setPadding(dp(14),dp(8),dp(14),dp(8));
  ScrollView scroll=new ScrollView(this);scroll.addView(list);
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Word report history").setView(scroll).setPositiveButton("Close",null).create();
  if(groups.isEmpty())list.addView(dashText("No session reports yet. Shortlist a tender to create one.",14,uiText()));
  java.util.ArrayList<String> keys=new java.util.ArrayList<>(groups.keySet());java.util.Collections.reverse(keys);
  for(String key:keys){
   String display=key;
   try{java.util.Date date=new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",java.util.Locale.US).parse(key);
    display=new java.text.SimpleDateFormat("dd MMM yyyy, h:mm a",java.util.Locale.getDefault()).format(date);
   }catch(Exception ignored){}
   list.addView(dashText(display+" • "+groups.get(key)+" tender(s)",14,uiText()));
   Button open=button("Open Word report");list.addView(open);
   open.setOnClickListener(v->openHistoryReport(key));
   Button export=button("Export copy");list.addView(export);
   export.setOnClickListener(v->exportHistoryReport(key));
  }
  dialog.show();styleDialog(dialog);
 }
 private void persistShortlist(){
  JSONArray a=new JSONArray();for(JSONObject j:shortlisted.values())a.put(j);
  getPreferences(0).edit().putString("shortlisted_tenders",a.toString()).apply();
  saveShortlistWordAutomatically();saveHistoryReports();
 }
 private void shortlistTender(JSONObject bid){
  String id=bid.optString("bidId");
  if(id.isEmpty()||isPreviouslyShortlisted(id))return;
  shortlisted.put(id,bid);
  persistShortlist();saveHistoryRecord(bid);
  renderDashboard();updateCollectedCount();scheduleAutoReview();
  android.widget.Toast.makeText(this,"Saved to Tender management",android.widget.Toast.LENGTH_SHORT).show();
 }
 private java.io.File shortlistWordFile(){return new java.io.File(getFilesDir(),"Tender_Radar_Shortlisted.docx");}
 private void saveShortlistWordAutomatically(){
  if(shortlisted.isEmpty())return;
  try{TenderWordExport.writeAll(shortlistWordFile(),shortlisted.values(),aiReviews);}
  catch(Exception e){android.util.Log.e("TenderRadar","Cannot save shortlist report",e);}
 }
 private void openSavedShortlistWord(){
  java.io.File file=shortlistWordFile();
  if(!file.exists()){android.widget.Toast.makeText(this,"No saved Word report yet",android.widget.Toast.LENGTH_SHORT).show();return;}
  exportAllShortlistedWord();
 }
 private void exportAllShortlistedWord(){
  if(shortlisted.isEmpty())return;
  Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
  intent.putExtra(Intent.EXTRA_TITLE,"Tender_Radar_Shortlisted.docx");
  try{startActivityForResult(intent,4209);}catch(Exception e){state.setText("No Word document provider available.");}
 }
 private void exportShortlistedWord(JSONObject bid){
  pendingWordBid=bid;
  Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
  intent.putExtra(Intent.EXTRA_TITLE,"GeM_"+bid.optString("bidId").replace("/","_")+"_shortlist.docx");
  try{startActivityForResult(intent,SAVE_SHORTLIST_DOCX);}catch(Exception e){state.setText("No document provider available for Word export.");}
 }
 private static final String[] TENDER_STAGES={"Shortlisted","Pending","Quoted","Won","Lost"};
 private String tenderStage(JSONObject bid){
  String value=bid.optString("trackingStage","Shortlisted");
  for(String stage:TENDER_STAGES)if(stage.equals(value))return value;
  return "Shortlisted";
 }
 private void chooseTenderStage(JSONObject bid,android.app.AlertDialog parent){
  String current=tenderStage(bid);int selected=0;
  for(int i=0;i<TENDER_STAGES.length;i++)if(TENDER_STAGES[i].equals(current))selected=i;
  new android.app.AlertDialog.Builder(this).setTitle("Tender status • "+bid.optString("bidId"))
   .setSingleChoiceItems(TENDER_STAGES,selected,(d,which)->{
    try{bid.put("trackingStage",TENDER_STAGES[which]);}catch(Exception ignored){}
    shortlisted.put(bid.optString("bidId"),bid);persistShortlist();
    d.dismiss();parent.dismiss();renderDashboard();showShortlist();
   }).setNegativeButton("Cancel",null).show();
 }
 private void showShortlist(){
  LinearLayout list=new LinearLayout(this);list.setOrientation(1);list.setPadding(dp(14),dp(8),dp(14),dp(12));
  ScrollView scroll=new ScrollView(this);scroll.addView(list);
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Tender management • "+shortlisted.size()+" saved").setView(scroll).setPositiveButton("Close",null).create();
  int[] counts=new int[TENDER_STAGES.length];for(JSONObject item:shortlisted.values())for(int i=0;i<TENDER_STAGES.length;i++)if(TENDER_STAGES[i].equals(tenderStage(item)))counts[i]++;
  StringBuilder overview=new StringBuilder();for(int i=0;i<TENDER_STAGES.length;i++){if(i>0)overview.append("  •  ");overview.append(TENDER_STAGES[i]).append(" ").append(counts[i]);}
  TextView pipeline=dashText(overview.toString(),13,uiAccent());pipeline.setPadding(dp(4),0,dp(4),dp(14));list.addView(pipeline);
  LinearLayout reports=new LinearLayout(this);reports.setOrientation(1);reports.setPadding(dp(16),dp(14),dp(16),dp(12));reports.setBackground(flatShape(dynamicCard(),20));list.addView(reports);
  TextView reportTitle=dashText("Word reports",17,uiText());reportTitle.setTypeface(null,Typeface.BOLD);reports.addView(reportTitle);
  if(!shortlisted.isEmpty()){
   Button saved=button("Open saved report");saved.setBackground(flatShape(dynamicSurface(),12));reports.addView(saved,spacedAction(0));saved.setOnClickListener(v->openSavedShortlistWord());
   Button all=button("Export all to Word");all.setBackground(flatShape(dynamicSurface(),12));reports.addView(all,spacedAction(8));all.setOnClickListener(v->exportAllShortlistedWord());
  }
  Button history=button("Report history");history.setTextColor(dynamicText());history.setBackground(flatShape(dynamicSurface(),12));LinearLayout.LayoutParams historyLp=spacedAction(8);reports.addView(history,historyLp);history.setOnClickListener(v->showWordHistory());
  TextView section=dashText("Saved tenders",17,uiText());section.setTypeface(null,Typeface.BOLD);section.setPadding(dp(4),dp(18),0,dp(8));list.addView(section);
  if(shortlisted.isEmpty())list.addView(dashText("No shortlisted tenders yet.",15,uiMuted()));
  for(JSONObject bid:new ArrayList<>(shortlisted.values())){
   String id=bid.optString("bidId");
   LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(16),dp(14),dp(16),dp(14));card.setBackground(flatShape(dynamicCard(),20));
   LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(12));list.addView(card,cp);
   TextView name=dashText(bid.optString("title","Untitled tender"),16,uiText());name.setTypeface(null,Typeface.BOLD);name.setMaxLines(3);name.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(name);
   TextView meta=dashText(id+"  •  "+tenderStage(bid),13,uiAccent());meta.setPadding(0,dp(6),0,dp(8));card.addView(meta);
   Button statusButton=button("Change status");statusButton.setBackground(flatShape(dynamicTonal(),12));statusButton.setTextColor(uiText());card.addView(statusButton,spacedAction(8));statusButton.setOnClickListener(v->chooseTenderStage(bid,dialog));
   LinearLayout row=new LinearLayout(this);row.setOrientation(0);LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(-1,-2);rowLp.setMargins(0,dp(8),0,0);card.addView(row,rowLp);
   Button word=button("Save Word");LinearLayout.LayoutParams half1=new LinearLayout.LayoutParams(0,dp(44),1);half1.setMargins(0,0,dp(4),0);word.setBackground(flatShape(dynamicSurface(),12));row.addView(word,half1);word.setOnClickListener(v->exportShortlistedWord(bid));
   Button open=button("GeM document");LinearLayout.LayoutParams half2=new LinearLayout.LayoutParams(0,dp(44),1);half2.setMargins(dp(4),0,0,0);open.setBackground(flatShape(dynamicSurface(),12));row.addView(open,half2);open.setOnClickListener(v->openBidPdf(bid));
   Button restore=button("Remove from shortlist");restore.setTextColor(uiMuted());restore.setBackground(flatShape(dynamicSurface(),12));card.addView(restore,spacedAction(8));
   restore.setOnClickListener(v->{shortlisted.remove(id);persistShortlist();dialog.dismiss();renderDashboard();updateCollectedCount();showShortlist();});
  }
  dialog.setOnShowListener(x->styleDialog(dialog));dialog.show();styleDialog(dialog);
 }
 private void addReviewSection(LinearLayout parent,String heading,String body){
  if(body==null||body.trim().isEmpty())return;
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(12),dp(12),dp(12),dp(12));
  panel.setBackground(glass(themeColor(0xffedf7f4,0xff22373d),16));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(4));parent.addView(panel,lp);
  TextView title=dashText(heading,15,uiAccent());title.setTypeface(null,Typeface.BOLD);panel.addView(title);
  TextView text=dashText(body.trim(),14,uiText());text.setTextIsSelectable(true);text.setPadding(0,dp(6),0,0);panel.addView(text);
 }
 private void addStructuredReview(LinearLayout parent,String raw){
  // Reorganize only headings that the AI actually supplied; never invent evidence.
  String[] titles={"Technical requirements","Manufacturing suitability","Eligibility requirements","Missing information","Recommendation"};
  String[] patterns={"technical|specifications?|scope of work|equipment details",
   "fabrication fit|manufacturing|suitability|capability",
   "eligibility|qualification|experience|turnover|emd",
   "missing|unclear|not provided|unknown|information required|limitations?",
   "recommendation|next steps?|action required|conclusion|verdict"};
  java.util.LinkedHashMap<Integer,StringBuilder> sections=new java.util.LinkedHashMap<>();
  StringBuilder other=new StringBuilder();int active=-1;
  for(String line:raw.replace("**","").replace("__","").split("\\r?\\n")){
   String clean=line.trim().replaceFirst("^#{1,6}\\s*","").replaceFirst("^[-*]\\s+","");
   if(clean.isEmpty())continue;
   java.util.regex.Matcher heading=java.util.regex.Pattern.compile("^([A-Za-z][A-Za-z /-]{2,45}):\\s*(.*)$").matcher(clean);
   boolean matched=false;
   if(heading.matches()){
    String label=heading.group(1).toLowerCase(java.util.Locale.ROOT);
    for(int i=0;i<patterns.length;i++)if(label.matches(".*("+patterns[i]+").*")){
     active=i;sections.putIfAbsent(i,new StringBuilder());
     if(!heading.group(2).isEmpty())sections.get(i).append(heading.group(2)).append("\\n");
     matched=true;break;
    }
   }
   if(!matched){
    if(active>=0)sections.get(active).append(clean).append("\\n");
    else other.append(clean).append("\\n");
   }
  }
  if(sections.isEmpty()){addReviewSection(parent,"AI summary",raw.replace("**","").replace("__",""));return;}
  if(other.length()>0)addReviewSection(parent,"Overview",other.toString());
  for(int i=0;i<titles.length;i++)if(sections.containsKey(i))addReviewSection(parent,titles[i],sections.get(i).toString());
  parent.addView(dashText("Sections are reorganized from the saved AI response; omitted sections were not provided by the model.",12,uiMuted()));
 }
 private void showTenderDetails(JSONObject bid){
  String id=bid.optString("bidId",""),title=bid.optString("title","Untitled tender");
  JSONObject review=aiReviews.get(id);
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(16),dp(10),dp(16),dp(12));
  TextView heading=dashText(title,21,uiText());heading.setTypeface(null,Typeface.BOLD);heading.setTextIsSelectable(true);content.addView(heading);
  TextView identifier=dashText(id+"  •  "+workCategory(title),13,uiAccent());identifier.setTextIsSelectable(true);content.addView(identifier);
  String buyer=bid.optString("buyer",bid.optString("buyerName",""));
  if(!buyer.isEmpty())content.addView(dashText("Buyer: "+buyer,14,uiText()));
  content.addView(dashText("Closing date: "+bid.optString("deadlineText","Not provided"),14,uiText()));
  TextView section=dashText("AI assessment",17,uiText());section.setTypeface(null,Typeface.BOLD);content.addView(section);
  if(review==null){
   content.addView(dashText("No completed AI review yet. The tender must be checked against its technical documents.",14,uiMuted()));
   Button run=button(analyzingBids.contains(id)?"AI review in progress":"Run AI review");run.setBackground(flatShape(dynamicTonal(),12));run.setTextColor(uiText());content.addView(run,spacedAction(12));
   run.setEnabled(!analyzingBids.contains(id));
   run.setOnClickListener(v->{failedAutoReviews.remove(id);apiFailedReviews.remove(id);aiConnectionPaused=false;analyzeInline(bid);});
  }else{
   content.addView(dashText(review.optString("verdict","Needs review")+"  •  "+review.optInt("score",0)+"/100 (indicative)",16,uiAccent()));
   content.addView(dashText("Priority: "+review.optString("priority","Needs review"),14,uiText()));
   content.addView(dashText("Evidence: "+review.optString("source","Source not recorded"),13,uiMuted()));
   addStructuredReview(content,review.optString("report","No summary provided"));
  }
  content.addView(dashText("Assessment is provisional. Confirm specifications, buyer requirements, eligibility and deadlines on GeM.",12,uiMuted()));
  Button document=button("Open GeM document");document.setBackground(flatShape(dynamicSurface(),12));content.addView(document,spacedAction(12));document.setOnClickListener(v->openBidPdf(bid));
  if(!isPreviouslyShortlisted(id)){
   Button save=button("Shortlist tender");save.setTextColor(uiAccent());save.setBackground(flatShape(dynamicSurface(),12));content.addView(save,spacedAction(10));
   save.setOnClickListener(v->{shortlistTender(bid);save.setEnabled(false);save.setText("Saved to Tender management");});
  }else content.addView(dashText("Already saved in Tender management / history",13,uiAccent()));
  ScrollView scroll=new ScrollView(this);scroll.addView(content);
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Tender details").setView(scroll).setPositiveButton("Close",null).create();
  dialog.setOnShowListener(v->styleDialog(dialog));dialog.show();styleDialog(dialog);
 }
 private void renderDashboard(){
  if(dashboardList==null)return;
  while(dashboardList.getChildCount()>1)dashboardList.removeViewAt(1);
  int white=dynamicText(),muted=dynamicMuted(),teal=dynamicPrimary();
  if(managementHeaderButton!=null){managementHeaderButton.setText(String.valueOf(shortlisted.size()));managementHeaderButton.setContentDescription("Tender management, "+shortlisted.size()+" saved");}
  LinearLayout resultsToolbar=new LinearLayout(this);resultsToolbar.setOrientation(0);resultsToolbar.setGravity(Gravity.CENTER_VERTICAL);dashboardList.addView(resultsToolbar,new LinearLayout.LayoutParams(-1,dp(48)));
  TextView resultsHeading=dashText("Matching tenders",22,white);resultsHeading.setTypeface(null,Typeface.BOLD);resultsToolbar.addView(resultsHeading,new LinearLayout.LayoutParams(0,-2,1));
  ArrayList<JSONObject> open=new ArrayList<>();int expired=0,missing=0,analyzed=0;
  Date now=new Date();
  for(JSONObject j:collected.values()){
   if(isPreviouslyShortlisted(j.optString("bidId"))||!relevantTitle(j.optString("title")))continue;
   Date due=parsedDeadline(j.optString("deadlineText"));
   if(due==null){missing++;continue;}
   if(!due.after(now)){expired++;continue;}
   open.add(j);if(aiReviews.containsKey(j.optString("bidId")))analyzed++;
  }
  Collections.sort(open,(a,b)->{
   Date ad=parsedDeadline(a.optString("deadlineText")),bd=parsedDeadline(b.optString("deadlineText"));
   int deadlineOrder=ad==null||bd==null?0:ad.compareTo(bd);
   if(tenderSortMode==1)return deadlineOrder;
   if(tenderSortMode==2){
    JSONObject ar=aiReviews.get(a.optString("bidId")),br=aiReviews.get(b.optString("bidId"));
    if(ar!=null&&br==null)return -1;if(ar==null&&br!=null)return 1;
    if(ar!=null&&br!=null){int aiOrder=Integer.compare(br.optInt("score",0),ar.optInt("score",0));if(aiOrder!=0)return aiOrder;}
   }
   int byScore=Integer.compare(qualificationScore(b),qualificationScore(a));
   return byScore!=0?byScore:deadlineOrder;
  });
  String[] sortLabels={"Relevance","Closing soon","AI score"};
  Button sort=button("Sort: "+sortLabels[tenderSortMode]+"  ▾");sort.setTextSize(13);sort.setTextColor(uiAccent());sort.setBackgroundColor(Color.TRANSPARENT);sort.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
  LinearLayout.LayoutParams sortLp=new LinearLayout.LayoutParams(-2,dp(40));sortLp.setMargins(dp(8),0,0,0);resultsToolbar.addView(sort,sortLp);
  sort.setOnClickListener(v->showSortDialog(sortLabels));
  LinearLayout metricsGrid=new LinearLayout(this);metricsGrid.setOrientation(1);dashboardList.addView(metricsGrid);
  LinearLayout metricsTop=new LinearLayout(this);metricsTop.setOrientation(0);metricsGrid.addView(metricsTop);
  addMetric(metricsTop,String.valueOf(open.size()),"Matches",teal);addMetric(metricsTop,String.valueOf(analyzed),"Reviewed",teal);
  LinearLayout metricsBottom=new LinearLayout(this);metricsBottom.setOrientation(0);
  LinearLayout.LayoutParams bottomLp=new LinearLayout.LayoutParams(-1,-2);bottomLp.setMargins(0,dp(8),0,dp(8));metricsGrid.addView(metricsBottom,bottomLp);
  addMetric(metricsBottom,String.valueOf(analyzingBids.size()),"Analyzing",teal);
  addMetric(metricsBottom,String.valueOf(Math.max(0,open.size()-analyzed-analyzingBids.size()-failedAutoReviews.size()-apiFailedReviews.size())),"Waiting",teal);
  TextView caution=dashText(expired+" expired hidden • "+missing+" missing dates hidden\nAI scores are indicative; verify GeM documents and deadlines.",12,muted);caution.setPadding(0,dp(4),0,dp(12));dashboardList.addView(caution);

  if(open.isEmpty())dashboardList.addView(dashText("No open matching tenders found. Run a GeM search and collect results.",15,white));
  for(JSONObject bid:open){
   String id=bid.optString("bidId"),title=bid.optString("title"),due=bid.optString("deadlineText");
   JSONObject ai=aiReviews.get(id);
   LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(18),dp(17),dp(18),dp(18));
   android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
   bg.setColor(dynamicCard());bg.setCornerRadius(dp(24));card.setBackground(bg);card.setElevation(0);
   LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(9),0,dp(12));dashboardList.addView(card,cp);
   TextView cardTitle=dashText(title,18,white);cardTitle.setTypeface(null,Typeface.BOLD);cardTitle.setMaxLines(3);cardTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);cardTitle.setPadding(dp(2),dp(3),dp(2),dp(5));card.addView(cardTitle);
   cardTitle.setOnClickListener(v->showTenderDetails(bid));
   TextView bidId=dashText(id+"  •  "+workCategory(title),12,teal);bidId.setPadding(dp(2),dp(4),dp(2),dp(4));card.addView(bidId);
   boolean isAnalyzing=ai==null&&analyzingBids.contains(id);
   boolean isConnectionError=ai==null&&apiFailedReviews.contains(id);
   boolean isDocumentError=ai==null&&failedAutoReviews.contains(id);
   boolean isReviewed=ai!=null;
   String status=isReviewed?"Reviewed • "+ai.optString("verdict","Needs review")+" • "+ai.optInt("score",0)+"/100":
    isAnalyzing?"Analyzing technical documents":isConnectionError?"AI connection failed • Retry":
    isDocumentError?"Document unavailable • Manual review":"Waiting for AI review";
   int statusColor=isReviewed?dynamicPrimary():
    isAnalyzing?dynamicPrimary():
    (isConnectionError||isDocumentError)?themeColor(0xff9b4b13,0xffffc58a):themeColor(0xff526b75,0xffb4c7ce);
   int statusSurface=isReviewed?dynamicTonal():
    isAnalyzing?dynamicTonal():
    (isConnectionError||isDocumentError)?themeColor(0xffffeddb,0xff4c3223):themeColor(0xffeaf0f1,0xff26363e);
   TextView closing=dashText("Closes "+due,13,muted);closing.setPadding(dp(2),dp(4),dp(2),dp(5));card.addView(closing);
   LinearLayout statusRow=new LinearLayout(this);statusRow.setOrientation(0);statusRow.setGravity(Gravity.CENTER_VERTICAL);
   statusRow.setPadding(dp(10),dp(6),dp(10),dp(6));statusRow.setBackground(flatShape(statusSurface,12));
   LinearLayout.LayoutParams statusLp=new LinearLayout.LayoutParams(-1,-2);statusLp.setMargins(0,dp(2),0,dp(8));card.addView(statusRow,statusLp);
   if(isAnalyzing){
    ProgressBar spinner=new ProgressBar(this);spinner.setIndeterminate(true);spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(statusColor));
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(16),dp(16));sp.setMargins(0,0,dp(8),0);statusRow.addView(spinner,sp);
   }else{
    TextView marker=dashText(isReviewed?"✓":(isConnectionError||isDocumentError)?"!":"○",14,statusColor);
    marker.setTypeface(null,Typeface.BOLD);
    LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-2,-2);mp.setMargins(0,0,dp(8),0);statusRow.addView(marker,mp);
   }
   TextView statusText=dashText(status,12,statusColor);statusText.setTypeface(null,Typeface.BOLD);statusRow.addView(statusText,new LinearLayout.LayoutParams(0,-2,1));
   // ColorOS-style grouped actions: flat rows, subtle dividers, one accent.
   LinearLayout actions=new LinearLayout(this);actions.setOrientation(1);card.addView(actions);
   Button details=button("Tender details  ›");details.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
   details.setTextColor(dynamicText());details.setBackgroundColor(Color.TRANSPARENT);details.setPadding(dp(12),0,dp(12),0);
   actions.addView(details,new LinearLayout.LayoutParams(-1,dp(46)));details.setOnClickListener(v->showTenderDetails(bid));
   View divider1=new View(this);divider1.setBackgroundColor(themeColor(0xffe9e9eb,0xff45414b));actions.addView(divider1,new LinearLayout.LayoutParams(-1,dp(1)));
   Button openBid=button("GeM document  ↗");openBid.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
   openBid.setTextColor(dynamicText());openBid.setBackgroundColor(Color.TRANSPARENT);openBid.setPadding(dp(12),0,dp(12),0);
   actions.addView(openBid,new LinearLayout.LayoutParams(-1,dp(46)));openBid.setOnClickListener(v->openBidPdf(bid));
   View divider2=new View(this);divider2.setBackgroundColor(themeColor(0xffe9e9eb,0xff45414b));actions.addView(divider2,new LinearLayout.LayoutParams(-1,dp(1)));
   Button shortlist=button("＋  Add to shortlist");shortlist.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
   shortlist.setTextColor(dynamicPrimary());shortlist.setBackgroundColor(Color.TRANSPARENT);shortlist.setPadding(dp(12),0,dp(12),0);
   actions.addView(shortlist,new LinearLayout.LayoutParams(-1,dp(46)));shortlist.setOnClickListener(v->shortlistTender(bid));
  }
 }
 private void reviewCollected(){
  if(collected.isEmpty()){state.setText("No bids collected. Search GeM, then tap Collect visible bids.");return;}
  ArrayList<JSONObject> matches=new ArrayList<>();int expired=0,unverified=0;
  Date now=new Date();
  for(JSONObject j:collected.values()){
   if(!relevantTitle(j.optString("title")))continue;
   Date due=parsedDeadline(j.optString("deadlineText"));
   if(due==null){unverified++;continue;}
   if(!due.after(now)){expired++;continue;}
   matches.add(j);
  }
  Collections.sort(matches,(a,b)->Integer.compare(qualificationScore(b),qualificationScore(a)));
  int high=0,medium=0,low=0;for(JSONObject item:matches){int score=qualificationScore(item);if(score>=70)high++;else if(score>=50)medium++;else low++;}
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(14),dp(8),dp(14),dp(8));
  TextView summary=new TextView(this);summary.setText("Title-based qualification: "+high+" high • "+medium+" review • "+low+" low\nOpen-date candidates: "+matches.size()+" / "+collected.size()+" collected\nExpired candidates hidden: "+expired+" • Dates missing: "+unverified+"\nSaved AI assessments: "+aiReviews.size()+" (document-based, not independently verified).");summary.setTextColor(Color.DKGRAY);summary.setPadding(0,0,0,dp(12));content.addView(summary);
  for(JSONObject j:matches){
   String id=j.optString("bidId");
   TextView card=new TextView(this);JSONObject review=aiReviews.get(id);card.setText(qualificationLabel(j)+" • "+workCategory(j.optString("title"))+"\n"+id+"\n"+j.optString("title")+"\nCloses: "+j.optString("deadlineText")+(review==null?"\nAI: Not analyzed":"\nAI: "+review.optString("verdict","Needs review")+" • "+review.optInt("score",0)+"/100")+"\nTap for bid options");card.setTextColor(Color.rgb(0,86,120));card.setTextSize(15);card.setPadding(dp(10),dp(12),dp(10),dp(12));content.addView(card);
   card.setOnClickListener(v->openBidPdf(j));
   View divider=new View(this);divider.setBackgroundColor(Color.LTGRAY);content.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
  }
  if(matches.isEmpty()){TextView empty=new TextView(this);empty.setText("No open-date keyword matches yet. Some bids may require reading documents to determine relevance.");empty.setTextColor(Color.DKGRAY);content.addView(empty);}
  ScrollView scroll=new ScrollView(this);scroll.addView(content);
  new android.app.AlertDialog.Builder(this).setTitle("Qualified GeM candidates").setView(scroll)
   .setPositiveButton("Close",null)
   .setNeutralButton("Clear collected bids",(d,w)->new android.app.AlertDialog.Builder(this).setTitle("Clear collected bids?").setNegativeButton("Cancel",null).setPositiveButton("Clear",(x,y)->{collected.clear();persistCollected();updateCollectedCount();}).show()).show();
 }
 private void openBidPdf(JSONObject bid){
  String id=bid.optString("bidId","");
  JSONArray links=bid.optJSONArray("documentLinks");
  String url="";
  if(links!=null)for(int i=0;i<links.length();i++){
   String candidate=links.optString(i,"");
   if(safe(Uri.parse(candidate))){url=candidate;break;}
  }
  if(url.isEmpty()){
   java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)^GEM/20\\d{2}/B/(\\d+)$").matcher(id);
   if(m.matches())url="https://bidplus.gem.gov.in/showbidDocument/"+m.group(1);
  }
  if(url.isEmpty()||!safe(Uri.parse(url))){showDocumentLinks(bid);return;}
  try{
   startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
   state.setText("Opening GeM bid document for "+id+". Download depends on GeM access.");
  }catch(Exception e){state.setText("Unable to open bid document in browser.");}
 }
 private void showDocumentLinks(JSONObject bid){
  JSONArray links=bid.optJSONArray("documentLinks");
  ArrayList<String> urls=new ArrayList<>();
  if(links!=null)for(int i=0;i<links.length();i++){String u=links.optString(i);if(safe(Uri.parse(u)))urls.add(u);}
  if(urls.isEmpty()){new android.app.AlertDialog.Builder(this).setTitle("GeM technical documents").setMessage("No public document URL was exposed on this listing card. Open the GeM bid and use its document/download section, then import the PDF for AI analysis. Login-only documents cannot be fetched automatically.").setPositiveButton("Open bid", (d,w)->{String url=bid.optString("url");browser.setVisibility(View.VISIBLE);dashboard.setVisibility(View.GONE);showingDashboard=false;browser.loadUrl(safe(Uri.parse(url))?url:GEM);}).setNegativeButton("Close",null).show();return;}
  String[] labels=new String[urls.size()];for(int i=0;i<labels.length;i++)labels[i]="GeM document "+(i+1);
  new android.app.AlertDialog.Builder(this).setTitle("Public document links").setItems(labels,(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(urls.get(w))))).setNegativeButton("Close",null).show();
 }
 private void restoreAiReviews(){
  try{JSONArray a=new JSONArray(getPreferences(0).getString("gem_ai_reviews","[]"));for(int i=0;i<a.length();i++){JSONObject j=a.optJSONObject(i);if(j!=null&&!j.optString("bidId").isEmpty())aiReviews.put(j.optString("bidId"),j);}}catch(Exception ignored){}
 }
 private void saveAiReview(JSONObject bid,String response,String modelId){saveAiReview(bid,response,modelId,"User-imported or supplied text (technical completeness not verified)");}
 private void saveAiReview(JSONObject bid,String response,String modelId,String evidenceType){
  String verdict="Needs review";
  java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("(?i)verdict[^\\\\n]{0,50}?(not relevant|irrelevant|unclear|needs review|relevant)").matcher(response);
  if(matcher.find()){String v=matcher.group(1).toLowerCase(Locale.ROOT);verdict=v.equals("relevant")?"Relevant":v.equals("not relevant")||v.equals("irrelevant")?"Not relevant":"Needs review";}
  int score=verdict.equals("Relevant")?60:verdict.equals("Not relevant")?10:40;
  String lowerReport=response.toLowerCase(Locale.ROOT);
  String priority=verdict.equals("Not relevant")?"Low":verdict.equals("Relevant")&&lowerReport.contains("fabrication fit:")&&!lowerReport.contains("fabrication fit: unclear")?"Review for quotation":"Needs review";
  try{JSONObject j=new JSONObject().put("bidId",bid.optString("bidId")).put("verdict",verdict).put("score",score).put("priority",priority).put("report",response).put("model",modelId).put("reviewedAt",System.currentTimeMillis()).put("source",evidenceType).put("evidenceVerified",false);aiReviews.put(bid.optString("bidId"),j);
   JSONArray a=new JSONArray();for(JSONObject item:aiReviews.values())a.put(item);getPreferences(0).edit().putString("gem_ai_reviews",a.toString()).apply();
  }catch(Exception e){state.setText("Assessment displayed but could not be saved");}
 }
 private void showSavedReview(String id){
  JSONObject bid=collected.get(id);
  if(bid==null)bid=shortlisted.get(id);
  if(bid!=null){showTenderDetails(bid);return;}
  showStyledDialog(new android.app.AlertDialog.Builder(this).setTitle("AI review").setMessage("Tender details are no longer available in this session.").setPositiveButton("Close",null));
 }
 private boolean safeGemDocument(Uri uri){
  String host=uri==null?null:uri.getHost();
  return uri!=null&&"https".equalsIgnoreCase(uri.getScheme())&&host!=null&&(host.equals("gem.gov.in")||host.endsWith(".gem.gov.in"));
 }
 private void discoverBidPdf(JSONObject bid,EditText target,Button trigger){
  String url=bid.optString("url","");
  Uri uri=Uri.parse(url);
  if(!safeGemDocument(uri)||url.equals(GEM)||url.contains("/all-bids")){
   trigger.setEnabled(true);target.setHint("No public document URL detected for this bid. Import the PDF if needed.");if("auto_review".equals(trigger.getTag()))onAutoDocumentFailure(bid,"No direct document URL");return;
  }
  trigger.setEnabled(false);
  state.setText("Checking public bid page for PDF documents…");
  new Thread(()->{
   HttpURLConnection conn=null;
   try{
    conn=(HttpURLConnection)new URL(url).openConnection();
    conn.setConnectTimeout(12000);conn.setReadTimeout(16000);conn.setInstanceFollowRedirects(false);
    if(conn.getResponseCode()!=200)throw new IOException("GeM bid page HTTP "+conn.getResponseCode());
    String mime=conn.getContentType();
    if(mime==null||!mime.toLowerCase(Locale.ROOT).contains("text/html"))throw new IOException("Bid page is not public HTML");
    ByteArrayOutputStream output=new ByteArrayOutputStream();
    try(InputStream in=conn.getInputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(output.size()+n>1024*1024)break;output.write(buffer,0,n);}}
    String html=output.toString("UTF-8").replace("&amp;","&");
    java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("(?i)(?:https?://|/)[^\\\"'\\s<>]+[.]pdf(?:[?][^\\\"'\\s<>]*)?").matcher(html);
    String found="";
    while(matcher.find()){
     String candidate=new URL(new URL(url),matcher.group()).toString();
     if(safeGemDocument(Uri.parse(candidate))){found=candidate;break;}
    }
    final String pdf=found;
    runOnUiThread(()->{
     trigger.setEnabled(true);
     if(pdf.isEmpty()){target.setHint("GeM did not expose a public PDF. Import a document if available.");if("auto_review".equals(trigger.getTag()))onAutoDocumentFailure(bid,"No public PDF found");return;}
     try{JSONArray links=bid.optJSONArray("documentLinks");if(links==null){links=new JSONArray();bid.put("documentLinks",links);}links.put(pdf);}catch(Exception ignored){}
     loadPublicPdfInto(bid,target,trigger);
    });
   }catch(Exception e){String error=e.getMessage();runOnUiThread(()->{trigger.setEnabled(true);target.setHint("Automatic document lookup unavailable: "+error+". Import a PDF if needed.");if("auto_review".equals(trigger.getTag()))onAutoDocumentFailure(bid,"GeM document lookup blocked");});}
   finally{if(conn!=null)conn.disconnect();}
  }).start();
 }
 private void loadPublicPdfInto(JSONObject bid,EditText target,Button trigger){
  JSONArray links=bid.optJSONArray("documentLinks");
  String link="";
  if(links!=null)for(int i=0;i<links.length();i++){String candidate=links.optString(i);if(safeGemDocument(Uri.parse(candidate))){link=candidate;break;}}
  if(link.isEmpty()){discoverBidPdf(bid,target,trigger);return;}
  final String pdfLink=link;trigger.setEnabled(false);target.setText("Reading GeM PDF…");
  new Thread(()->{
   File file=null;HttpURLConnection conn=null;
   try{
    conn=(HttpURLConnection)new URL(pdfLink).openConnection();
    conn.setConnectTimeout(15000);conn.setReadTimeout(30000);conn.setInstanceFollowRedirects(false);
    conn.setRequestProperty("Accept","application/pdf");
    int code=conn.getResponseCode();if(code!=200)throw new IOException("GeM returned HTTP "+code);
    String mime=conn.getContentType();
    if(mime==null||!mime.toLowerCase(Locale.ROOT).contains("pdf"))throw new IOException("GeM did not provide a public PDF");
    file=File.createTempFile("gem_bid_",".pdf",getCacheDir());
    try(InputStream in=conn.getInputStream();OutputStream out=new FileOutputStream(file)){
     byte[] buf=new byte[8192];int n;long total=0;
     while((n=in.read(buf))!=-1){total+=n;if(total>12*1024*1024)throw new IOException("PDF exceeds 12 MB");out.write(buf,0,n);}
    }
    PdfTenderReader.Result result=PdfTenderReader.read(this,Uri.fromFile(file));
    runOnUiThread(()->{target.setTag("auto_pdf_ready");target.setText(result.text);trigger.setEnabled(true);state.setText("GeM PDF extracted ("+result.pages+" pages). Review scope and tap Analyze.");});
   }catch(Exception e){String message=e.getMessage();runOnUiThread(()->{target.setText("");trigger.setEnabled(true);if("auto_review".equals(trigger.getTag()))onAutoDocumentFailure(bid,"Public PDF download failed");else new android.app.AlertDialog.Builder(this).setTitle("Automatic PDF retrieval unavailable").setMessage(message+"\\nThis GeM document may require a browser session. You can still import it manually.").setPositiveButton("OK",null).show();});}
   finally{if(conn!=null)conn.disconnect();if(file!=null)file.delete();}
  }).start();
 }


 private String omniModel(){return getPreferences(0).getString("omni_model","openrouter/liquid/lfm-3.1-7b:free");}
 private String requestProviderReview(String instructions,String prompt) throws Exception{
  HttpURLConnection conn=(HttpURLConnection)new URL(OMNI_URL).openConnection();
  try{
   conn.setRequestMethod("POST");conn.setConnectTimeout(3500);conn.setReadTimeout(65000);conn.setDoOutput(true);
   conn.setRequestProperty("Content-Type","application/json");
   String gatewayKey=OmniSecret.load(this);if(!gatewayKey.isEmpty())conn.setRequestProperty("Authorization","Bearer "+gatewayKey);
   JSONObject body=new JSONObject().put("model",omniModel()).put("max_tokens",2000).put("messages",new JSONArray().put(new JSONObject().put("role","system").put("content",instructions)).put(new JSONObject().put("role","user").put("content",prompt)));
   try(OutputStream out=conn.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
   int code=conn.getResponseCode();
   if(code<200||code>=300){String detail="";try(InputStream err=conn.getErrorStream()){if(err!=null){ByteArrayOutputStream eb=new ByteArrayOutputStream();byte[] tmp=new byte[1024];int n;while((n=err.read(tmp))!=-1&&eb.size()<3000)eb.write(tmp,0,n);detail=new JSONObject(eb.toString("UTF-8")).optJSONObject("error").optString("message","");}}catch(Exception ignored){}throw new IOException("OmniRoute HTTP "+code+(detail.isEmpty()?"":": "+detail));}
   ByteArrayOutputStream buffer=new ByteArrayOutputStream();
   try(InputStream in=conn.getInputStream()){byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1){if(buffer.size()+n>250000)throw new IOException("AI response too large");buffer.write(chunk,0,n);}}
   String answer=new JSONObject(buffer.toString("UTF-8")).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","");
   if(answer.trim().isEmpty())throw new IOException("OmniRoute returned an empty response");return answer;
  }catch(java.net.ConnectException e){throw new IOException("OmniRoute is offline. Start omniroute in Termux.",e);}
  finally{conn.disconnect();}
 }
 private void showAiSettings(){
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(20),dp(12),dp(20),dp(12));
  TextView info=new TextView(this);info.setTextColor(uiText());info.setText("Local OmniRoute • Termux must be running on this phone. Enter the local OmniRoute gateway key below. Your provider keys stay in OmniRoute.");content.addView(info);
  TextView address=new TextView(this);address.setTextColor(uiMuted());address.setText("Server: http://127.0.0.1:20128/v1");address.setPadding(0,dp(12),0,dp(8));content.addView(address);
  EditText model=new EditText(this);model.setSingleLine(true);model.setHint("OmniRoute model ID");model.setText(omniModel());styleDialogField(model);content.addView(model);
  EditText apiKey=new EditText(this);apiKey.setSingleLine(true);apiKey.setHint("OmniRoute gateway API key");apiKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);apiKey.setText(OmniSecret.load(this));styleDialogField(apiKey);content.addView(apiKey);
  TextView result=new TextView(this);result.setTextColor(uiMuted());result.setText("Not tested");result.setPadding(0,dp(12),0,dp(12));content.addView(result);
  Button test=button("Test local connection");content.addView(test,spacedAction(10));
  test.setOnClickListener(v->{
   String id=model.getText().toString().trim();if(id.isEmpty()){model.setError("Enter a model ID");return;}
   try{OmniSecret.save(this,apiKey.getText().toString().trim());}catch(Exception e){result.setText("Cannot save encrypted key: "+e.getMessage());return;}getPreferences(0).edit().putString("omni_model",id).apply();test.setEnabled(false);result.setText("Testing local OmniRoute…");
   new Thread(()->{String message;try{String answer=requestProviderReview("Reply briefly.","Reply OK");message="Connected • "+answer.substring(0,Math.min(answer.length(),100));}
    catch(Exception e){message="Connection failed: "+e.getMessage();}
    String status=message;runOnUiThread(()->{test.setEnabled(true);result.setText(status);if(status.startsWith("Connected")){aiConnectionPaused=false;apiFailedReviews.clear();scheduleAutoReview();}});}).start();
  });
  ScrollView scroll=new ScrollView(this);scroll.addView(content);
  showStyledDialog(new android.app.AlertDialog.Builder(this).setTitle("OmniRoute AI settings").setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Save",(dialog,which)->{
   String id=model.getText().toString().trim();if(!id.isEmpty()){try{OmniSecret.save(this,apiKey.getText().toString().trim());getPreferences(0).edit().putString("omni_model",id).apply();aiConnectionPaused=false;apiFailedReviews.clear();scheduleAutoReview();}catch(Exception e){state.setText("Unable to save OmniRoute key securely");}}
  }));;
 }
 private void showAiDialog(JSONObject bid){
  LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(dp(16),dp(8),dp(16),dp(8));
  TextView explanation=new TextView(this);explanation.setText("The app will try to retrieve the public GeM bid document automatically. OmniRoute runs locally in Termux. If GeM does not expose the document, manual import remains available.");layout.addView(explanation);
  TextView provider=new TextView(this);provider.setText("AI provider: Local OmniRoute • "+omniModel());layout.addView(provider);
  Button configure=button("AI settings / Test connection");layout.addView(configure);configure.setOnClickListener(v->showAiSettings());
  EditText scope=new EditText(this);scope.setHint("Tender text loads automatically when a public document is available");scope.setMinLines(5);scope.setMaxLines(12);layout.addView(scope);
  Button automatic=button("Auto-read available GeM tender PDF");layout.addView(automatic);automatic.setOnClickListener(v->loadPublicPdfInto(bid,scope,automatic));
  Button importPdf=button("Import tender PDF (no copying)");layout.addView(importPdf);
  importPdf.setOnClickListener(v->{pendingPdfScope=scope;Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.setType("application/pdf");pick.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(pick,PICK_TENDER_PDF);});
  EditText publicPdfUrl=new EditText(this);publicPdfUrl.setHint("Optional public GeM PDF URL (https://...)");publicPdfUrl.setSingleLine(true);layout.addView(publicPdfUrl);
  Button fetchPdf=button("Read public GeM PDF link");layout.addView(fetchPdf);
  fetchPdf.setOnClickListener(v->{
   String link=publicPdfUrl.getText().toString().trim();Uri u=Uri.parse(link);String host=u.getHost();
   if(!"https".equalsIgnoreCase(u.getScheme())||host==null||!(host.equals("gem.gov.in")||host.endsWith(".gem.gov.in"))){publicPdfUrl.setError("Use a public HTTPS GeM document link");return;}
   fetchPdf.setEnabled(false);scope.setText("Reading public GeM PDF…");
   new Thread(()->{
    File file=null;
    try{
     URL url=new URL(link);HttpURLConnection conn=(HttpURLConnection)url.openConnection();conn.setConnectTimeout(15000);conn.setReadTimeout(30000);conn.setInstanceFollowRedirects(false);conn.setRequestProperty("Accept","application/pdf");
     int code=conn.getResponseCode();if(code!=200)throw new IOException("GeM returned HTTP "+code+"; this document may require a session");
     String mime=conn.getContentType();if(mime==null||!mime.toLowerCase(Locale.ROOT).contains("pdf"))throw new IOException("GeM did not return a PDF");
     file=File.createTempFile("gem_public_",".pdf",getCacheDir());try(InputStream in=conn.getInputStream();OutputStream out=new FileOutputStream(file)){byte[] buf=new byte[8192];int n;long total=0;while((n=in.read(buf))!=-1){total+=n;if(total>12*1024*1024)throw new IOException("PDF exceeds 12 MB");out.write(buf,0,n);}}conn.disconnect();
     PdfTenderReader.Result result=PdfTenderReader.read(this,Uri.fromFile(file));String extracted=result.text;
     runOnUiThread(()->{scope.setTag("auto_pdf_ready");scope.setText(extracted);fetchPdf.setEnabled(true);state.setText("Public PDF extracted: "+result.pages+" pages. Review before analyzing.");});
    }catch(Exception e){String error=e.getMessage();runOnUiThread(()->{scope.setText("");fetchPdf.setEnabled(true);new android.app.AlertDialog.Builder(this).setTitle("Cannot read public PDF").setMessage(error+"\nUse Import tender PDF if the GeM link requires a session.").setPositiveButton("OK",null).show();});}
    finally{if(file!=null)file.delete();}
   }).start();
  });
  ScrollView scroll=new ScrollView(this);scroll.addView(layout);
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("AI technical review: "+bid.optString("bidId")).setView(scroll)
   .setNegativeButton("Cancel",null).setPositiveButton("Analyze",null).create();
  dialog.setOnShowListener(ignored->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
    String modelId=omniModel(),document=scope.getText().toString().trim();
    if(modelId.isEmpty()){showAiSettings();return;}
    if(document.length()<80){scope.setError("Paste at least 80 characters of actual tender scope");scope.requestFocus();return;}

    dialog.dismiss();
    state.setText("Analyzing tender scope with local OmniRoute…");
    android.app.AlertDialog progress=new android.app.AlertDialog.Builder(this).setTitle("Analyzing tender").setMessage("Contacting OmniRoute in Termux. This can take up to a minute.").setCancelable(false).create();progress.show();
    new Thread(()->{
     try{
      String response=requestProviderReview("You review Indian industrial fabrication tenders for ADITYA UDYOG HEATXCHANGER PVT. LTD. Its user-supplied catalogue describes manufacturing, refurbishment and services for shell-and-tube, fin-tube and plate heat exchangers, condensers, turbine/generator/stator/oil/air coolers, LP/HP heaters, air preheaters, U-tube and copper/brass tube bundles, radiators, vaporizers, pressure vessels, storage tanks, reactors, blenders, expansion bellows, FRP cooling towers, and customized retubing, hydrojet cleaning and rubber/FRP lining. The facility catalogue lists plate rolling, tube flaring, MIG/TIG/ARC welding, machining, hydraulic press, NDT and third-party inspection arrangements. Treat these as claimed general capabilities, NOT proof of qualification for a specific tender. Prioritize manufacturing, refurbishment, retubing, tube bundles, heat transfer equipment and plant service work; reject unrelated household appliances, generic spare parts and standalone material supply unless document evidence establishes fabrication scope. Return these clearly labeled concise sections in order: Verdict (Relevant/Unclear/Not relevant); Buyer; Equipment; Quantity; Closing date; Scope; Fabrication fit (Suitable/Possible/Unclear/Not suitable, with reason); Missing specifications (drawings, material grades, dimensions, design pressure, tests and inspection only where not provided); Risks; Recommendation (Quote candidate/Manual review/Reject). Distinguish supply of spare tubes from fabrication of a complete exchanger, tube bundle or retubing. Only use supplied document evidence; mark unknown fields Not provided. Never invent specifications or claim the tender is verified. Distinguish industrial equipment from household appliances.","GeM bid: "+bid.optString("bidId")+"\\nListing title: "+bid.optString("title")+"\\nClosing date from listing: "+bid.optString("deadlineText")+"\\nDocument excerpt:\\n"+document.substring(0,Math.min(document.length(),18000)));
      runOnUiThread(()->{progress.dismiss();saveAiReview(bid,response,modelId);state.setText("AI review saved for "+bid.optString("bidId"));showSavedReview(bid.optString("bidId"));});
     }catch(Exception e){runOnUiThread(()->{progress.dismiss();state.setText("AI review failed: "+e.getMessage());new android.app.AlertDialog.Builder(this).setTitle("OmniRoute analysis failed").setMessage(e.getMessage()+"\nCheck Termux server, model availability, and provider limits.").setPositiveButton("OK",null).show();});}
    }).start();
   }));
  scope.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence t,int start,int count,int after){}
   public void onTextChanged(CharSequence t,int start,int before,int count){}
   public void afterTextChanged(android.text.Editable t){
    if("auto_pdf_ready".equals(scope.getTag())&&t.length()>=80){
     scope.setTag(null);
     scope.post(()->{if(dialog.isShowing())dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();});
    }
   }
  });
  dialog.show();
  loadPublicPdfInto(bid,scope,automatic);
 }
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
  super.onActivityResult(requestCode,resultCode,data);
  if(requestCode==4210){if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null&&selectedHistoryReportId!=null){
   try{
    java.io.File file=historyFile(selectedHistoryReportId);
    try(java.io.InputStream in=new java.io.FileInputStream(file);java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){
     if(out==null)throw new java.io.IOException("Destination unavailable");
     byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
    }
    state.setText("Session Word report exported.");
   }catch(Exception e){state.setText("Session export failed: "+e.getMessage());}
  }selectedHistoryReportId=null;return;}
  if(requestCode==4209){if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null){try{TenderWordExport.writeAll(this,data.getData(),shortlisted.values(),aiReviews);state.setText("Combined shortlist Word report exported.");}catch(Exception e){state.setText("Word export failed: "+e.getMessage());}}return;}
  if(requestCode==SAVE_SHORTLIST_DOCX){if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null&&pendingWordBid!=null){try{TenderWordExport.write(this,data.getData(),pendingWordBid,aiReviews.get(pendingWordBid.optString("bidId")));state.setText("Shortlisted tender exported to Word.");}catch(Exception e){state.setText("Word export failed: "+e.getMessage());}}pendingWordBid=null;return;}
  if(requestCode!=PICK_TENDER_PDF||resultCode!=RESULT_OK||data==null||data.getData()==null||pendingPdfScope==null)return;
  EditText target=pendingPdfScope;Uri uri=data.getData();target.setText("Extracting selected PDF…");
  new Thread(()->{
   try{PdfTenderReader.Result result=PdfTenderReader.read(this,uri);runOnUiThread(()->{target.setText(result.text);state.setText("Imported "+result.pages+" PDF pages. Review the extracted scope, then tap Analyze.");});}
   catch(Exception e){runOnUiThread(()->{target.setText("");new android.app.AlertDialog.Builder(this).setTitle("PDF import failed").setMessage(e.getMessage()).setPositiveButton("OK",null).show();});}
  }).start();
 }
 private TextView addGlassStat(LinearLayout parent,String label){LinearLayout tile=new LinearLayout(this);tile.setOrientation(1);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(12),dp(8),dp(12),dp(8));tile.setBackground(glass(dynamicCard(),16));tile.setElevation(0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(80),1);lp.setMargins(dp(5),0,dp(5),0);parent.addView(tile,lp);TextView value=new TextView(this);value.setText("0");value.setTextSize(27);value.setTypeface(null,Typeface.BOLD);value.setTextColor(dynamicText());value.setGravity(Gravity.CENTER);value.setIncludeFontPadding(false);tile.addView(value,new LinearLayout.LayoutParams(-1,-2));TextView caption=new TextView(this);caption.setText(label);caption.setTextSize(12);caption.setTextColor(dynamicMuted());caption.setGravity(Gravity.CENTER);tile.addView(caption,new LinearLayout.LayoutParams(-1,-2));return value;}
 private class ScanRing extends View {
  private int value=0;private final android.graphics.Paint paint=new android.graphics.Paint(3);
  ScanRing(){super(GemBrowserActivity.this);setContentDescription("0 of "+batchTerms.length+" groups scanned");}
  void setValue(int v){value=Math.max(0,Math.min(batchTerms.length,v));setContentDescription(value+" of "+batchTerms.length+" groups scanned");invalidate();}
  @Override protected void onDraw(android.graphics.Canvas c){
   super.onDraw(c);float w=getWidth(),h=getHeight(),cx=w/2,cy=h/2,r=Math.min(w,h)*.37f;
   paint.setShader(null);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(7));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
   paint.setColor(dynamicTonal());c.drawCircle(cx,cy,r,paint);
   paint.setColor(dynamicPrimary());c.drawArc(cx-r,cy-r,cx+r,cy+r,-90,360f*value/Math.max(1,batchTerms.length),false,paint);
   paint.setStyle(android.graphics.Paint.Style.FILL);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextAlign(android.graphics.Paint.Align.CENTER);
   paint.setColor(dynamicText());paint.setTextSize(dp(22));c.drawText(value+"/"+batchTerms.length,cx,cy+dp(1),paint);
   paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(dp(10));paint.setColor(dynamicMuted());c.drawText("Groups",cx,cy+dp(16),paint);
  }
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private android.graphics.drawable.GradientDrawable flatShape(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 private android.graphics.drawable.GradientDrawable glass(int color,int radius){return flatShape(color,24);}
 private android.graphics.drawable.GradientDrawable rounded(int color,int radius,int border){return flatShape(color,24);}
 private int adaptiveColor(int attr,int fallback){
  android.util.TypedValue value=new android.util.TypedValue();
  if(getTheme().resolveAttribute(attr,value,true)){
   if(value.resourceId!=0){try{return getColor(value.resourceId);}catch(Exception ignored){}}
   if(value.type>=android.util.TypedValue.TYPE_FIRST_COLOR_INT&&value.type<=android.util.TypedValue.TYPE_LAST_COLOR_INT)return value.data;
  }
  return fallback;
 }
 private int adaptiveSurface(){return adaptiveColor(android.R.attr.colorBackground,themeColor(0xfff6faf9,0xff101b21));}
 private int adaptivePrimary(){return adaptiveColor(android.R.attr.colorAccent,themeColor(0xff008f82,0xff70e0d0));}
 private int uiText(){return dynamicText();}
 private int uiMuted(){return dynamicMuted();}
 private int uiSurface(){return dynamicCard();}
 private int uiAccent(){return dynamicPrimary();}
 private void styleDialog(android.app.AlertDialog dialog){
  if(dialog.getWindow()==null)return;
  android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();
  background.setColor(dynamicCard());background.setCornerRadius(dp(24));
  dialog.getWindow().setBackgroundDrawable(background);
  int titleId=getResources().getIdentifier("alertTitle","id","android");
  TextView title=dialog.findViewById(titleId);if(title!=null)title.setTextColor(uiText());
  for(int which:new int[]{-1,-2,-3}){
   Button b=dialog.getButton(which);if(b!=null){b.setTextColor(uiAccent());b.setAllCaps(false);}
  }
 }
 private android.app.AlertDialog showStyledDialog(android.app.AlertDialog.Builder builder){
  android.app.AlertDialog d=builder.create();d.setOnShowListener(x->styleDialog(d));d.show();styleDialog(d);return d;
 }
 private void styleDialogField(EditText e){
  e.setTextColor(uiText());e.setHintTextColor(uiMuted());
  e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(uiAccent()));
 }
 private LinearLayout.LayoutParams spacedAction(int topDp){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(44));p.setMargins(0,dp(topDp),0,0);return p;}
 private Button button(String label){Button b=new SpringButton();b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(uiText());b.setBackground(flatShape(dynamicCard(),12));b.setStateListAnimator(null);b.setElevation(0);b.setMinHeight(dp(44));b.setPadding(dp(12),0,dp(12),0);return b;}
 private boolean motionEnabled(){return android.animation.ValueAnimator.areAnimatorsEnabled();}
 private void showSortDialog(String[] labels){
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(20),dp(12),dp(20),dp(12));
  RadioGroup choices=new RadioGroup(this);choices.setOrientation(1);content.addView(choices);
  android.content.res.ColorStateList tint=new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{dynamicPrimary(),dynamicMuted()});
  for(int i=0;i<labels.length;i++){RadioButton option=new RadioButton(this);option.setId(View.generateViewId());option.setText(labels[i]);option.setTextSize(16);option.setTextColor(dynamicText());option.setButtonTintList(tint);option.setPadding(dp(8),dp(8),dp(8),dp(8));option.setMinHeight(dp(56));choices.addView(option,new RadioGroup.LayoutParams(-1,-2));option.setChecked(i==tenderSortMode);}
  ScrollView scroll=new ScrollView(this);scroll.addView(content);
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this,darkMode()?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert).setTitle("Sort matching tenders").setView(scroll).setNegativeButton("Cancel",null).create();
  choices.setOnCheckedChangeListener((group,id)->{for(int i=0;i<group.getChildCount();i++)if(group.getChildAt(i).getId()==id){tenderSortMode=i;dialog.dismiss();renderDashboard();break;}});
  dialog.show();styleDialog(dialog);
 }
 // Press feedback does not consume touch events or delay existing click actions.
 private class SpringButton extends Button {
  SpringButton(){super(GemBrowserActivity.this);}
  @Override public void setPressed(boolean pressed){
   boolean changed=pressed!=isPressed();super.setPressed(pressed);if(!changed)return;
   animate().cancel();
   if(!motionEnabled()){setScaleX(1f);setScaleY(1f);return;}
   animate().scaleX(pressed?.97f:1f).scaleY(pressed?.97f:1f).setDuration(pressed?100:320)
    .setInterpolator(pressed?new android.view.animation.DecelerateInterpolator():new android.view.animation.OvershootInterpolator(1.4f)).start();
  }
  @Override protected void onDetachedFromWindow(){animate().cancel();setScaleX(1f);setScaleY(1f);super.onDetachedFromWindow();}
 }
 @Override protected void onSaveInstanceState(Bundle out){out.putString("report_session_id",sessionReportId);super.onSaveInstanceState(out);}
 private long lastBackPressMs=0L;
 @Override public void onBackPressed(){
  // A WebView's internal GeM page history must not intercept exit from the dashboard.
  if(!showingDashboard&&browser!=null&&browser.canGoBack()){browser.goBack();return;}
  long now=android.os.SystemClock.elapsedRealtime();
  if(now-lastBackPressMs<2000L){super.onBackPressed();return;}
  lastBackPressMs=now;
  android.widget.Toast.makeText(this,"Back again to exit Tender Radar",android.widget.Toast.LENGTH_SHORT).show();
 }
}
