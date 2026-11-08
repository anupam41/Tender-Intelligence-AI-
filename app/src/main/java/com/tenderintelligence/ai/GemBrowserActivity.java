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
 private EditText term;
 private TextView state;
 private final LinkedHashMap<String,JSONObject> collected=new LinkedHashMap<>();
 private TextView collectedCount;
 private String openRouterKey="";
 private EditText pendingPdfScope;
 private static final int PICK_TENDER_PDF=4207;
 private static final String GEM="https://bidplus.gem.gov.in/all-bids";
 private boolean safe(Uri uri){
  return uri!=null&&"https".equalsIgnoreCase(uri.getScheme())&&"bidplus.gem.gov.in".equalsIgnoreCase(uri.getHost());
 }
 @Override public void onCreate(Bundle bundle){
  super.onCreate(bundle);
  getWindow().setStatusBarColor(Color.rgb(6,10,18));
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setBackgroundColor(Color.rgb(6,10,18));setContentView(page);page.setOnApplyWindowInsetsListener((v,insets)->{int top=android.os.Build.VERSION.SDK_INT>=30?insets.getInsets(android.view.WindowInsets.Type.statusBars()).top:insets.getSystemWindowInsetTop();v.setPadding(0,top,0,0);return insets;});
  TextView heading=new TextView(this);heading.setText("GeM Bid Search");heading.setTextSize(19);heading.setTypeface(null,Typeface.BOLD);heading.setTextColor(Color.WHITE);heading.setPadding(16,8,16,4);page.addView(heading);
  TextView note=new TextView(this);note.setText("Search GeM bids using the selected equipment keyword.");note.setTextColor(Color.LTGRAY);note.setPadding(16,0,16,2);page.addView(note);
  LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);page.addView(controls);
  Button back=button("Back");controls.addView(back,new LinearLayout.LayoutParams(0,40,1));back.setOnClickListener(v->{if(browser.canGoBack())browser.goBack();else finish();});
  Button home=button("GeM home");controls.addView(home,new LinearLayout.LayoutParams(0,40,1));home.setOnClickListener(v->browser.loadUrl(GEM));
  Button reload=button("Reload");controls.addView(reload,new LinearLayout.LayoutParams(0,52,1));reload.setOnClickListener(v->browser.reload());
  Spinner picker=new Spinner(this);
  String[] options={"Choose equipment keyword","Heat exchanger","Condenser","Sample cooler","Oil cooler","SWAS cooler","Air cooler","Water cooler","Tube bundle","U tube bundle","Cooling coil","Chiller","Radiator","Dryer","Heater","Economizer","Ejector","Evaporator","Tank","Pressure vessel"};
  ArrayAdapter<String> pickerAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,options){
   @Override public View getView(int position,View convertView,android.view.ViewGroup parent){View v=super.getView(position,convertView,parent);if(v instanceof TextView)((TextView)v).setTextColor(Color.WHITE);return v;}
   @Override public View getDropDownView(int position,View convertView,android.view.ViewGroup parent){View v=super.getDropDownView(position,convertView,parent);if(v instanceof TextView){((TextView)v).setTextColor(Color.BLACK);v.setBackgroundColor(Color.WHITE);}return v;}
  };pickerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);picker.setAdapter(pickerAdapter);page.addView(picker);
  picker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(position>0)term.setText(options[position]);}});
  LinearLayout findRow=new LinearLayout(this);findRow.setOrientation(LinearLayout.HORIZONTAL);page.addView(findRow);
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
  LinearLayout captureRow=new LinearLayout(this);captureRow.setOrientation(LinearLayout.HORIZONTAL);page.addView(captureRow);
  Button collect=button("Collect visible bids");captureRow.addView(collect,new LinearLayout.LayoutParams(0,dp(52),2));collect.setOnClickListener(v->collectVisible());
  Button review=button("Review shortlist");captureRow.addView(review,new LinearLayout.LayoutParams(0,dp(52),2));review.setOnClickListener(v->reviewCollected());
  collectedCount=new TextView(this);collectedCount.setTextColor(Color.rgb(45,212,191));collectedCount.setPadding(12,2,12,2);page.addView(collectedCount);
  restoreCollected();updateCollectedCount();
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
  state=new TextView(this);state.setTextColor(Color.rgb(45,212,191));state.setText("Opening GeM…");state.setPadding(16,5,16,5);page.addView(state);
  browser=new WebView(this);page.addView(browser,new LinearLayout.LayoutParams(-1,0,1));
  WebSettings settings=browser.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setSupportZoom(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);
  browser.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
    Uri url=request.getUrl();if(safe(url))return false;
    if("https".equalsIgnoreCase(url.getScheme())){startActivity(new Intent(Intent.ACTION_VIEW,url));return true;}
    return true;
   }
   @Override public void onPageFinished(WebView view,String url){state.setText("Enter a keyword in GeM or select one above, then tap Search.");}
   @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError error){if(req.isForMainFrame())state.setText("Unable to load GeM in WebView. Try opening in your browser.");}
  });
  browser.setDownloadListener((url,userAgent,disposition,mime,length)->{Uri uri=Uri.parse(url);if(safe(uri))startActivity(new Intent(Intent.ACTION_VIEW,uri));else state.setText("Download blocked: external URL");});
  browser.loadUrl(GEM);
 }
 private void updateCollectedCount(){if(collectedCount!=null){int matches=0;for(JSONObject j:collected.values())if(relevantTitle(j.optString("title")))matches++;collectedCount.setText("Collected: "+collected.size()+" • Title matches: "+matches+" • Review closing dates");}}
 private void restoreCollected(){
  try{JSONArray a=new JSONArray(getPreferences(0).getString("gem_collected","[]"));for(int i=0;i<a.length();i++){JSONObject j=a.optJSONObject(i);if(j!=null&&!j.optString("bidId").isEmpty())collected.put(j.optString("bidId"),j);}}catch(Exception ignored){}
 }
 private void persistCollected(){
  JSONArray a=new JSONArray();for(JSONObject j:collected.values())a.put(j);
  getPreferences(0).edit().putString("gem_collected",a.toString()).apply();
 }
 private void collectVisible(){
  state.setText("Reading visible GeM listing cards…");
  String js="(function(){try{var found=[],seen={};"+
   "var ids=Array.from(document.querySelectorAll('a,span,strong,b,div')).filter(e=>e.children.length<3&&/GEM\\/20\\d{2}\\/B\\/\\d+/i.test(e.textContent||''));"+
   "for(var node of ids){var match=(node.textContent||'').match(/GEM\\/20\\d{2}\\/B\\/\\d+/i);if(!match||seen[match[0]])continue;"+
   "var card=node;for(var n=0;n<7&&card.parentElement;n++){var t=card.innerText||'';if(/End Date\\s*:/i.test(t)&&/Items?\\s*:/i.test(t)&&t.length<3500)break;card=card.parentElement;}"+
   "var text=card.innerText||'';if(text.length>6000)continue;"+
   "var item=text.match(/Items?\\s*:\\s*([^\\n]+)/i),end=text.match(/End Date\\s*:\\s*(\\d{2}-\\d{2}-\\d{4}(?:\\s+\\d{1,2}:\\d{2}\\s*(?:AM|PM))?)/i);"+
   "var anchor=node.closest('a[href]')||Array.from(card.querySelectorAll('a[href]')).find(a=>(a.textContent||'').includes(match[0]));"+
   "seen[match[0]]=true;found.push({bidId:match[0],title:item?item[1].trim():'GeM bid',deadlineText:end?end[1].trim():'',url:anchor?anchor.href:'',source:'GeM visible listing'});if(found.length>=100)break;}"+
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
     if(!collected.containsKey(id)){collected.put(id,j);added++;}
    }
    persistCollected();updateCollectedCount();
    state.setText("Detected "+items.length()+" GeM bid IDs; saved "+added+" new bids. "+(items.length()==0?"No bid IDs detected in page DOM. Scroll to results and retry.":"Tap Next and collect again."));
   }catch(Exception e){state.setText("Could not parse bid cards. GeM page format may have changed.");}
  });
 }
 private Date parsedDeadline(String value){
  try{SimpleDateFormat format=new SimpleDateFormat("dd-MM-yyyy h:mm a",Locale.ENGLISH);format.setLenient(false);return format.parse(value.trim());}catch(Exception e){return null;}
 }
 private boolean relevantTitle(String value){
  String t=value.toLowerCase(Locale.ROOT);
  if(t.matches(".*(foot massager|room heater|gas stove|hot plate|hair dryer|water heater|space heater|fan heater|electric heater|hand heater|heat shrink|heat gun|laboratory heater).*"))return false;
  return t.matches(".*(heat exchanger|condenser|retub|re-tub|industrial cooler|oil cooler|sample cooler|swas cooler|tube bundle|pressure vessel|evaporator|economi[sz]er|industrial chiller|radiator|cooling coil|air preheater|shell and tube|shell & tube|tubesheet|tube sheet|reboiler|industrial heater).*");
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
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(14),dp(8),dp(14),dp(8));
  TextView summary=new TextView(this);summary.setText("Open-date keyword candidates: "+matches.size()+" / "+collected.size()+" collected\nExpired candidates hidden: "+expired+" • Dates missing: "+unverified+"\nAI document analysis not yet performed.");summary.setTextColor(Color.DKGRAY);summary.setPadding(0,0,0,dp(12));content.addView(summary);
  for(JSONObject j:matches){
   String id=j.optString("bidId");
   TextView card=new TextView(this);card.setText(id+"\n"+j.optString("title")+"\nCloses: "+j.optString("deadlineText")+"\nTap to open GeM bid");card.setTextColor(Color.rgb(0,86,120));card.setTextSize(15);card.setPadding(dp(10),dp(12),dp(10),dp(12));content.addView(card);
   card.setOnClickListener(v->showBidActions(j));
   View divider=new View(this);divider.setBackgroundColor(Color.LTGRAY);content.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
  }
  if(matches.isEmpty()){TextView empty=new TextView(this);empty.setText("No open-date keyword matches yet. Some bids may require reading documents to determine relevance.");empty.setTextColor(Color.DKGRAY);content.addView(empty);}
  ScrollView scroll=new ScrollView(this);scroll.addView(content);
  new android.app.AlertDialog.Builder(this).setTitle("GeM shortlist candidates").setView(scroll)
   .setPositiveButton("Close",null)
   .setNeutralButton("Clear collected bids",(d,w)->new android.app.AlertDialog.Builder(this).setTitle("Clear collected bids?").setNegativeButton("Cancel",null).setPositiveButton("Clear",(x,y)->{collected.clear();persistCollected();updateCollectedCount();}).show()).show();
 }
 private void showBidActions(JSONObject bid){
  String id=bid.optString("bidId");
  new android.app.AlertDialog.Builder(this).setTitle(id).setItems(new String[]{"Open GeM bid","Analyze tender text with OpenRouter AI"},(d,which)->{
   if(which==0){String url=bid.optString("url");Uri uri=Uri.parse(url);if(safe(uri)&&!GEM.equals(url))startActivity(new Intent(Intent.ACTION_VIEW,uri));else{browser.loadUrl(GEM);state.setText("GeM direct link unavailable. Search bid ID: "+id);}}
   else showAiDialog(bid);
  }).setNegativeButton("Cancel",null).show();
 }
 private void showAiDialog(JSONObject bid){
  LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(dp(16),dp(8),dp(16),dp(8));
  TextView explanation=new TextView(this);explanation.setText("Paste the tender scope or extracted document text. AI cannot verify a bid from its truncated title alone. OpenRouter key stays in memory.");layout.addView(explanation);
  EditText api=new EditText(this);api.setHint("OpenRouter API key");api.setSingleLine(true);api.setInputType(129);api.setText(openRouterKey);layout.addView(api);
  EditText model=new EditText(this);model.setHint("OpenRouter model");model.setSingleLine(true);model.setText("openai/gpt-4o-mini");layout.addView(model);
  EditText scope=new EditText(this);scope.setHint("Paste tender document / technical scope");scope.setMinLines(5);scope.setMaxLines(12);layout.addView(scope);
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
     runOnUiThread(()->{scope.setText(extracted);fetchPdf.setEnabled(true);state.setText("Public PDF extracted: "+result.pages+" pages. Review before analyzing.");});
    }catch(Exception e){String error=e.getMessage();runOnUiThread(()->{scope.setText("");fetchPdf.setEnabled(true);new android.app.AlertDialog.Builder(this).setTitle("Cannot read public PDF").setMessage(error+"\nUse Import tender PDF if the GeM link requires a session.").setPositiveButton("OK",null).show();});}
    finally{if(file!=null)file.delete();}
   }).start();
  });
  ScrollView scroll=new ScrollView(this);scroll.addView(layout);
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("AI technical review: "+bid.optString("bidId")).setView(scroll)
   .setNegativeButton("Cancel",null).setPositiveButton("Analyze",null).create();
  dialog.setOnShowListener(ignored->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
    String token=api.getText().toString().trim(),modelId=model.getText().toString().trim(),document=scope.getText().toString().trim();
    openRouterKey=token;
    if(token.isEmpty()){api.setError("Enter OpenRouter API key");api.requestFocus();return;}
    if(modelId.isEmpty()){model.setError("Enter model ID");model.requestFocus();return;}
    if(document.length()<80){scope.setError("Paste at least 80 characters of actual tender scope");scope.requestFocus();return;}
    dialog.dismiss();
    state.setText("Analyzing tender scope with OpenRouter…");
    android.app.AlertDialog progress=new android.app.AlertDialog.Builder(this).setTitle("Analyzing tender").setMessage("Contacting OpenRouter. This can take up to a minute.").setCancelable(false).create();progress.show();
    new Thread(()->{
     try{
      JSONObject payload=new JSONObject().put("model",modelId);
      JSONArray messages=new JSONArray();
      messages.put(new JSONObject().put("role","system").put("content","You review Indian industrial fabrication tenders for heat exchangers, condensers, tube bundles, retubing and pressure vessels. Return concise sections: Verdict (Relevant/Unclear/Not relevant), Equipment, Scope, Fabrication fit, Risks, Missing information. Never invent specifications or claim the tender is verified. Distinguish industrial equipment from household appliances."));
      messages.put(new JSONObject().put("role","user").put("content","GeM bid: "+bid.optString("bidId")+"\\nListing title: "+bid.optString("title")+"\\nClosing date from listing: "+bid.optString("deadlineText")+"\\nDocument excerpt:\\n"+document.substring(0,Math.min(document.length(),18000))));
      payload.put("messages",messages);
      HttpURLConnection conn=(HttpURLConnection)new URL("https://openrouter.ai/api/v1/chat/completions").openConnection();
      conn.setRequestMethod("POST");conn.setConnectTimeout(20000);conn.setReadTimeout(60000);conn.setDoOutput(true);conn.setRequestProperty("Authorization","Bearer "+token);conn.setRequestProperty("Content-Type","application/json");
      try(OutputStream out=conn.getOutputStream()){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}
      int code=conn.getResponseCode();if(code>=400){conn.disconnect();throw new IOException("OpenRouter HTTP "+code);}
      ByteArrayOutputStream output=new ByteArrayOutputStream();try(InputStream in=conn.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(output.size()+n>200000)throw new IOException("Response too large");output.write(buf,0,n);}}
      conn.disconnect();
      String response=new JSONObject(output.toString("UTF-8")).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","No analysis returned");
      runOnUiThread(()->{progress.dismiss();state.setText("AI review completed for "+bid.optString("bidId"));new android.app.AlertDialog.Builder(this).setTitle("AI assessment (not verified)").setMessage(response).setPositiveButton("Close",null).show();});
     }catch(Exception e){runOnUiThread(()->{progress.dismiss();state.setText("AI review failed: "+e.getMessage());new android.app.AlertDialog.Builder(this).setTitle("OpenRouter analysis failed").setMessage(e.getMessage()+"\nCheck model availability, API key and internet connection.").setPositiveButton("OK",null).show();});}
    }).start();
   }));
  dialog.show();
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
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(Color.BLACK);b.setMinHeight(dp(44));b.setPadding(dp(4),0,dp(4),0);return b;}
 @Override public void onBackPressed(){if(browser!=null&&browser.canGoBack())browser.goBack();else super.onBackPressed();}
 @Override protected void onDestroy(){if(browser!=null){browser.destroy();browser=null;}super.onDestroy();}
}
