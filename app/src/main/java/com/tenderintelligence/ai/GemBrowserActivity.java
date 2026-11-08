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
 private TextView scanProgressLabel;
 private boolean scanMode=true;
 private EditText term;
 private TextView state;
 private final LinkedHashMap<String,JSONObject> collected=new LinkedHashMap<>();
 private TextView collectedCount;
 private static final String OMNI_URL="http://127.0.0.1:20128/v1/chat/completions";
 private EditText pendingPdfScope;
 private final LinkedHashMap<String,JSONObject> aiReviews=new LinkedHashMap<>();
 private static final int PICK_TENDER_PDF=4207;
 private static final String GEM="https://bidplus.gem.gov.in/all-bids";
 private boolean safe(Uri uri){
  return uri!=null&&"https".equalsIgnoreCase(uri.getScheme())&&"bidplus.gem.gov.in".equalsIgnoreCase(uri.getHost());
 }
 @Override public void onCreate(Bundle bundle){
  super.onCreate(bundle);
  getWindow().setStatusBarColor(themeColor(0xfff4faf7,0xff0a121a));getWindow().setNavigationBarColor(themeColor(0xfff4faf7,0xff0a121a));getWindow().getDecorView().setSystemUiVisibility(darkMode()?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setBackgroundColor(themeColor(0xfff4faf7,0xff0a121a));setContentView(page);page.setOnApplyWindowInsetsListener((v,insets)->{int top=android.os.Build.VERSION.SDK_INT>=30?insets.getInsets(android.view.WindowInsets.Type.statusBars()).top:insets.getSystemWindowInsetTop();v.setPadding(0,top,0,0);return insets;});
  TextView heading=new TextView(this);heading.setText("Tender Radar");heading.setTextSize(30);heading.setTypeface(null,Typeface.BOLD);heading.setTextColor(themeColor(0xff16272b,0xffeaf8f5));heading.setPadding(dp(24),dp(18),dp(24),dp(4));page.addView(heading);
  TextView note=new TextView(this);note.setText("Discover industrial tenders • GeM");note.setTextColor(themeColor(0xff65787d,0xff9bb2b6));note.setPadding(dp(24),0,dp(24),dp(18));page.addView(note);
  Button aiSettingsButton=button("⚙");aiSettingsButton.setTextSize(14);aiSettingsButton.setTextColor(themeColor(0xff195d5c,0xffb7f1e8));aiSettingsButton.setBackground(glass(themeColor(0xffe8f7f5,0xff162a30),22));aiSettingsButton.setElevation(dp(2));LinearLayout.LayoutParams settingsLp=new LinearLayout.LayoutParams(-1,dp(48));settingsLp.setMargins(dp(20),0,dp(20),dp(12));page.addView(aiSettingsButton,settingsLp);aiSettingsButton.setOnClickListener(v->showAiSettings());
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
  term=new EditText(this);term.setSingleLine(true);term.setTextColor(Color.WHITE);term.setHintTextColor(Color.LTGRAY);term.setHint("Or type equipment keyword");term.setTextSize(14);findRow.addView(term,new LinearLayout.LayoutParams(0,44,1));
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
  batchButton=button("Find tenders");batchButton.setTextSize(17);batchButton.setTypeface(null,Typeface.BOLD);batchButton.setTextColor(Color.WHITE);batchButton.setBackground(rounded(Color.rgb(0,175,152),22,Color.rgb(0,175,152)));batchButton.setElevation(dp(3));LinearLayout.LayoutParams heroLp=new LinearLayout.LayoutParams(-1,dp(60));heroLp.setMargins(dp(20),0,dp(20),dp(12));page.addView(batchButton,heroLp);
  batchButton.setOnClickListener(v->{if(batchRunning){batchRunning=false;scanProgress.setIndeterminate(false);stopAutoCollect("Batch stopped.");batchButton.setText("Find tenders");}else startBatch();});
  scanProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);scanProgress.setMax(batchTerms.length);scanProgress.setProgress(0);scanProgress.setIndeterminate(false);scanProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(0,175,152)));scanProgress.setVisibility(View.GONE);LinearLayout.LayoutParams progressLp=new LinearLayout.LayoutParams(-1,dp(5));progressLp.setMargins(dp(24),0,dp(24),0);page.addView(scanProgress,progressLp);
  scanProgressLabel=new TextView(this);scanProgressLabel.setTextColor(Color.rgb(47,99,99));scanProgressLabel.setPadding(dp(24),dp(10),dp(24),dp(5));scanProgressLabel.setTextSize(13);scanProgressLabel.setText("Ready to scan • Results will appear below");page.addView(scanProgressLabel);
  collectedCount=new TextView(this);collectedCount.setTextColor(Color.rgb(0,129,115));collectedCount.setPadding(dp(24),dp(4),dp(24),dp(4));collectedCount.setTextSize(13);page.addView(collectedCount);
  restoreCollected();restoreAiReviews();getPreferences(0).edit().remove("openrouter_key_iv").remove("openrouter_key_cipher").remove("gemini_iv").remove("gemini_encrypted").remove("groq_iv").remove("groq_encrypted").remove("remember_openrouter_key").remove("remember_provider_keys").apply();updateCollectedCount();
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
  dashboard=new ScrollView(this);dashboard.setFillViewport(true);dashboard.setVisibility(View.VISIBLE);showingDashboard=true;dashboard.setBackgroundColor(themeColor(0xfff4faf7,0xff0a121a));contentLayer.addView(dashboard,new FrameLayout.LayoutParams(-1,-1));
  dashboardList=new LinearLayout(this);dashboardList.setOrientation(1);dashboardList.setPadding(dp(20),dp(14),dp(20),dp(24));dashboardList.setBackgroundColor(themeColor(0xfff4faf7,0xff0a121a));dashboard.addView(dashboardList);
  heroContent=new LinearLayout(this);heroContent.setOrientation(1);dashboardList.addView(heroContent);
  for(View item:new View[]{heading,note,aiSettingsButton,scanRing,statsStrip,batchButton,scanProgress,scanProgressLabel,collectedCount,state}){android.view.ViewGroup parent=(android.view.ViewGroup)item.getParent();if(parent!=null)parent.removeView(item);heroContent.addView(item);}
  heading.setPadding(dp(4),dp(10),0,dp(2));note.setPadding(dp(4),0,0,dp(14));
  aiSettingsButton.setText("⚙  Settings");LinearLayout.LayoutParams settingsBarLp=new LinearLayout.LayoutParams(-1,dp(48));settingsBarLp.setMargins(dp(4),dp(2),dp(4),dp(12));aiSettingsButton.setLayoutParams(settingsBarLp);
  LinearLayout.LayoutParams ringParams=new LinearLayout.LayoutParams(dp(208),dp(208));ringParams.gravity=Gravity.CENTER_HORIZONTAL;ringParams.setMargins(0,dp(4),0,dp(12));scanRing.setLayoutParams(ringParams);
  statsStrip.setPadding(0,dp(4),0,dp(8));LinearLayout.LayoutParams scanLp=new LinearLayout.LayoutParams(-1,dp(56));scanLp.setMargins(dp(4),dp(4),dp(4),dp(12));batchButton.setLayoutParams(scanLp);
  scanProgressLabel.setPadding(dp(4),dp(12),0,dp(4));collectedCount.setPadding(dp(4),0,0,dp(4));state.setPadding(dp(4),dp(4),0,dp(18));
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
 private void updateCollectedCount(){int matches=0;for(JSONObject j:collected.values())if(relevantTitle(j.optString("title")))matches++;if(bidTotalValue!=null)bidTotalValue.setText(String.valueOf(collected.size()));if(matchTotalValue!=null)matchTotalValue.setText(String.valueOf(matches));if(collectedCount!=null)collectedCount.setText("Collected: "+collected.size()+" • Title matches: "+matches+" • Review closing dates");}
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
     if(autoPages>=(batchRunning?25:100)){if(batchRunning){autoCollecting=false;autoHandler.postDelayed(this::nextBatch,600);}else stopAutoCollect("Reached page safety limit.");return;}
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
 @Override protected void onDestroy(){autoCollecting=false;autoHandler.removeCallbacksAndMessages(null);if(browser!=null){browser.destroy();browser=null;}super.onDestroy();}
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
   if(!relevantTitle(bid.optString("title"))||aiReviews.containsKey(id)||analyzingBids.contains(id)||failedAutoReviews.contains(id)||apiFailedReviews.contains(id))continue;
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
  tile.setPadding(dp(6),dp(10),dp(6),dp(10));
  tile.setBackground(glass(themeColor(0xfff9fffd,0xff1a3036),22));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(70),1);
  lp.setMargins(0,0,dp(6),0);row.addView(tile,lp);
  TextView count=new TextView(this);count.setText(value);count.setTextSize(21);count.setTypeface(null,Typeface.BOLD);count.setTextColor(darkMode()?0xff7de4d7:color);count.setGravity(Gravity.CENTER);tile.addView(count,new LinearLayout.LayoutParams(-1,-2));
  TextView caption=new TextView(this);caption.setText(label);caption.setTextSize(11);caption.setTextColor(themeColor(0xff71858a,0xffa3bdc1));caption.setGravity(Gravity.CENTER);tile.addView(caption,new LinearLayout.LayoutParams(-1,-2));
 }
 private void renderDashboard(){
  if(dashboardList==null)return;
  while(dashboardList.getChildCount()>1)dashboardList.removeViewAt(1);
  int white=themeColor(0xff1b2d31,0xffeff9f8),muted=themeColor(0xff67797d,0xff9fb4b7),teal=themeColor(0xff008b7e,0xff57d7c6);
  TextView resultsHeading=dashText("Matching tenders",24,white);resultsHeading.setTypeface(null,Typeface.BOLD);dashboardList.addView(resultsHeading);
  ArrayList<JSONObject> open=new ArrayList<>();int expired=0,missing=0,analyzed=0;
  Date now=new Date();
  for(JSONObject j:collected.values()){
   if(!relevantTitle(j.optString("title")))continue;
   Date due=parsedDeadline(j.optString("deadlineText"));
   if(due==null){missing++;continue;}
   if(!due.after(now)){expired++;continue;}
   open.add(j);if(aiReviews.containsKey(j.optString("bidId")))analyzed++;
  }
  Collections.sort(open,(a,b)->{
   int byScore=Integer.compare(qualificationScore(b),qualificationScore(a));
   if(byScore!=0)return byScore;
   Date ad=parsedDeadline(a.optString("deadlineText")),bd=parsedDeadline(b.optString("deadlineText"));
   return ad==null||bd==null?0:ad.compareTo(bd);
  });
  LinearLayout metrics=new LinearLayout(this);metrics.setOrientation(0);metrics.setPadding(0,dp(8),0,dp(8));dashboardList.addView(metrics);addMetric(metrics,String.valueOf(open.size()),"Matches",teal);addMetric(metrics,String.valueOf(analyzed),"Reviewed",0xff25756e);addMetric(metrics,String.valueOf(analyzingBids.size()),"Analyzing",0xffb77722);addMetric(metrics,String.valueOf(Math.max(0,open.size()-analyzed-analyzingBids.size()-failedAutoReviews.size()-apiFailedReviews.size())),"Waiting",0xff72848a);
  TextView caution=dashText(expired+" expired hidden • "+missing+" missing dates hidden\nAI scores are indicative; verify GeM documents and deadlines.",12,muted);caution.setPadding(0,dp(4),0,dp(12));dashboardList.addView(caution);

  if(open.isEmpty())dashboardList.addView(dashText("No open matching tenders found. Run a GeM search and collect results.",15,white));
  for(JSONObject bid:open){
   String id=bid.optString("bidId"),title=bid.optString("title"),due=bid.optString("deadlineText");
   JSONObject ai=aiReviews.get(id);
   LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(16),dp(14),dp(16),dp(16));
   android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
   bg.setColor(themeColor(0xfffbfffe,0xff14242c));bg.setCornerRadius(dp(22));bg.setStroke(dp(1),themeColor(0xffd9eae7,0xff29434a));card.setBackground(bg);card.setElevation(dp(1));
   LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(7),0,dp(10));dashboardList.addView(card,cp);
   TextView cardTitle=dashText(title,18,white);cardTitle.setTypeface(null,Typeface.BOLD);cardTitle.setMaxLines(2);cardTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(cardTitle);
   if(analyzingBids.contains(id)){ProgressBar spinner=new ProgressBar(this);spinner.setIndeterminate(true);spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(teal));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(28),dp(28));sp.setMargins(0,dp(4),0,dp(4));card.addView(spinner,sp);card.addView(dashText("Reading document / AI processing…",12,teal));}
   TextView bidId=dashText(id+"  •  "+workCategory(title),13,teal);bidId.setPadding(0,dp(7),0,dp(8));card.addView(bidId);
   card.addView(dashText("Closing  "+due+"\n"+(ai==null?(analyzingBids.contains(id)?"● Analyzing now":apiFailedReviews.contains(id)?"AI connection issue • Retry after reconnect":failedAutoReviews.contains(id)?"Document inaccessible • Manual PDF needed":"◷ Waiting in AI queue"):ai.optString("verdict","Needs review")+" • Provisional AI assessment"),13,muted));
   LinearLayout actions=new LinearLayout(this);actions.setOrientation(0);card.addView(actions);
   Button openBid=button("View document");openBid.setTextColor(Color.rgb(38,91,91));openBid.setBackground(glass(themeColor(0xffeaf6f4,0xff1a3036),22));LinearLayout.LayoutParams openLp=new LinearLayout.LayoutParams(0,dp(48),1);openLp.setMargins(0,0,dp(6),0);actions.addView(openBid,openLp);openBid.setOnClickListener(v->openBidPdf(bid));
   Button analyze=button(analyzingBids.contains(id)?"Analyzing…":ai==null?(apiFailedReviews.contains(id)?"Retry after reconnect":failedAutoReviews.contains(id)?"Retry AI review":"AI pending…"):ai.optInt("score",0)+"% • Review");analyze.setTextColor(Color.WHITE);analyze.setTypeface(null,Typeface.BOLD);analyze.setBackground(rounded(Color.rgb(0,164,146),22,Color.rgb(0,164,146)));LinearLayout.LayoutParams analyzeLp=new LinearLayout.LayoutParams(0,dp(48),1);analyzeLp.setMargins(dp(6),0,0,0);actions.addView(analyze,analyzeLp);
   analyze.setEnabled(!analyzingBids.contains(id));if(analyzingBids.contains(id)){analyze.setText("◌ Analyzing");analyze.setAlpha(0.78f);}analyze.setOnClickListener(v->{if(aiReviews.containsKey(id))showSavedReview(id);else{failedAutoReviews.remove(id);apiFailedReviews.remove(id);aiConnectionPaused=false;analyzeInline(bid);}});
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
  JSONObject j=aiReviews.get(id);if(j==null){new android.app.AlertDialog.Builder(this).setMessage("No AI assessment saved for this bid yet.").setPositiveButton("OK",null).show();return;}
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(18),dp(12),dp(18),dp(12));
  TextView verdict=new TextView(this);verdict.setText(j.optString("verdict")+" • "+j.optInt("score")+"/100 (indicative)");verdict.setTextSize(20);verdict.setTypeface(null,Typeface.BOLD);verdict.setTextColor(j.optString("verdict").equals("Relevant")?Color.rgb(0,110,88):Color.rgb(120,82,20));content.addView(verdict);
  TextView priority=new TextView(this);priority.setText("Tender priority: "+j.optString("priority","Needs review")+" • Fabrication suitability requires technical verification");priority.setTextSize(14);priority.setPadding(0,dp(6),0,dp(4));content.addView(priority);
  TextView caution=new TextView(this);caution.setText("Evidence: "+j.optString("source","Older review: document source not recorded")+"\nProvisional AI assessment • Full technical specification not verified • Score reflects relevance only, not eligibility or bid readiness");caution.setTextSize(12);caution.setPadding(0,dp(8),0,dp(12));content.addView(caution);
  String report=j.optString("report").replaceAll("(?m)^#{1,6}\\\\s*","").replace("**","").replace("__","");
  TextView body=new TextView(this);body.setText(report);body.setTextSize(15);body.setTextColor(Color.DKGRAY);body.setTextIsSelectable(true);content.addView(body);
  JSONObject bid=collected.get(id);
  if(bid!=null){Button link=button("View GeM bid document");content.addView(link);link.setOnClickListener(v->openBidPdf(bid));}
  ScrollView scroll=new ScrollView(this);scroll.addView(content);
  new android.app.AlertDialog.Builder(this).setTitle("AI review • "+id).setView(scroll).setPositiveButton("Close",null).show();
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
  TextView info=new TextView(this);info.setText("Local OmniRoute • Termux must be running on this phone. Enter the local OmniRoute gateway key below. Your provider keys stay in OmniRoute.");content.addView(info);
  TextView address=new TextView(this);address.setText("Server: http://127.0.0.1:20128/v1");address.setPadding(0,dp(12),0,dp(8));content.addView(address);
  EditText model=new EditText(this);model.setSingleLine(true);model.setHint("OmniRoute model ID");model.setText(omniModel());content.addView(model);
  EditText apiKey=new EditText(this);apiKey.setSingleLine(true);apiKey.setHint("OmniRoute gateway API key");apiKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);apiKey.setText(OmniSecret.load(this));content.addView(apiKey);
  TextView result=new TextView(this);result.setText("Not tested");result.setPadding(0,dp(12),0,dp(12));content.addView(result);
  Button test=button("Test local connection");content.addView(test);
  test.setOnClickListener(v->{
   String id=model.getText().toString().trim();if(id.isEmpty()){model.setError("Enter a model ID");return;}
   try{OmniSecret.save(this,apiKey.getText().toString().trim());}catch(Exception e){result.setText("Cannot save encrypted key: "+e.getMessage());return;}getPreferences(0).edit().putString("omni_model",id).apply();test.setEnabled(false);result.setText("Testing local OmniRoute…");
   new Thread(()->{String message;try{String answer=requestProviderReview("Reply briefly.","Reply OK");message="Connected • "+answer.substring(0,Math.min(answer.length(),100));}
    catch(Exception e){message="Connection failed: "+e.getMessage();}
    String status=message;runOnUiThread(()->{test.setEnabled(true);result.setText(status);if(status.startsWith("Connected")){aiConnectionPaused=false;apiFailedReviews.clear();scheduleAutoReview();}});}).start();
  });
  ScrollView scroll=new ScrollView(this);scroll.addView(content);
  new android.app.AlertDialog.Builder(this).setTitle("OmniRoute AI settings").setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Save",(dialog,which)->{
   String id=model.getText().toString().trim();if(!id.isEmpty()){try{OmniSecret.save(this,apiKey.getText().toString().trim());getPreferences(0).edit().putString("omni_model",id).apply();aiConnectionPaused=false;apiFailedReviews.clear();scheduleAutoReview();}catch(Exception e){state.setText("Unable to save OmniRoute key securely");}}
  }).show();
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
  if(requestCode!=PICK_TENDER_PDF||resultCode!=RESULT_OK||data==null||data.getData()==null||pendingPdfScope==null)return;
  EditText target=pendingPdfScope;Uri uri=data.getData();target.setText("Extracting selected PDF…");
  new Thread(()->{
   try{PdfTenderReader.Result result=PdfTenderReader.read(this,uri);runOnUiThread(()->{target.setText(result.text);state.setText("Imported "+result.pages+" PDF pages. Review the extracted scope, then tap Analyze.");});}
   catch(Exception e){runOnUiThread(()->{target.setText("");new android.app.AlertDialog.Builder(this).setTitle("PDF import failed").setMessage(e.getMessage()).setPositiveButton("OK",null).show();});}
  }).start();
 }
 private TextView addGlassStat(LinearLayout parent,String label){LinearLayout tile=new LinearLayout(this);tile.setOrientation(1);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(12),dp(8),dp(12),dp(8));tile.setBackground(glass(themeColor(0xffe7f7f3,0xff1a3036),22));tile.setElevation(dp(1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(80),1);lp.setMargins(dp(5),0,dp(5),0);parent.addView(tile,lp);TextView value=new TextView(this);value.setText("0");value.setTextSize(27);value.setTypeface(null,Typeface.BOLD);value.setTextColor(themeColor(0xff183e3b,0xffe3fcf8));value.setGravity(Gravity.CENTER);value.setIncludeFontPadding(false);tile.addView(value,new LinearLayout.LayoutParams(-1,-2));TextView caption=new TextView(this);caption.setText(label);caption.setTextSize(12);caption.setTextColor(themeColor(0xff6b8080,0xffa9c4c3));caption.setGravity(Gravity.CENTER);tile.addView(caption,new LinearLayout.LayoutParams(-1,-2));return value;}
 private class ScanRing extends View {private int value=0;private final android.graphics.Paint paint=new android.graphics.Paint(3);ScanRing(){super(GemBrowserActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}void setValue(int v){value=Math.max(0,Math.min(batchTerms.length,v));invalidate();}@Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),cx=w/2,cy=h/2,r=Math.min(w,h)*.40f;paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(15));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setColor(themeColor(0xffc9eae4,0xff294d52));c.drawCircle(cx,cy,r,paint);paint.setShader(new android.graphics.LinearGradient(0,0,w,h,0xff0d998e,0xff49d3bf,android.graphics.Shader.TileMode.CLAMP));c.drawArc(cx-r,cy-r,cx+r,cy+r,-90,360f*value/batchTerms.length,false,paint);paint.setShader(null);paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(themeColor(0xff1c3034,0xffe9f8f5));paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextAlign(android.graphics.Paint.Align.CENTER);paint.setTextSize(dp(44));c.drawText(value+"/"+batchTerms.length,cx,cy+dp(4),paint);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(dp(12));c.drawText("Equipment groups",cx,cy+dp(28),paint);c.drawText("scanned",cx,cy+dp(44),paint);}}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private android.graphics.drawable.GradientDrawable glass(int color,int radius){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,darkMode()?new int[]{0xff263f46,color,0xff13262e}:new int[]{0xfffcffff,color,0xffe2f3f0});g.setCornerRadius(dp(radius));g.setStroke(dp(1),themeColor(0xffd5e9e6,0xff38555d));return g;}
 private android.graphics.drawable.GradientDrawable rounded(int color,int radius,int border){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);return d;}
 private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(Color.BLACK);b.setMinHeight(dp(44));b.setPadding(dp(4),0,dp(4),0);return b;}
 @Override public void onBackPressed(){if(browser!=null&&browser.canGoBack())browser.goBack();else super.onBackPressed();}
}
