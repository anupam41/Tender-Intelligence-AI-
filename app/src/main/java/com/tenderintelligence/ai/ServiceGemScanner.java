package com.tenderintelligence.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;

/** Service-owned WebView scanner. No Activity lifecycle dependency. Runs on the main thread. */
final class ServiceGemScanner {
 interface Listener { void progress(int category,int page,int bids,String detail); void finished(boolean complete,String reason); }
 private static final String GEM="https://bidplus.gem.gov.in/all-bids";
 private final Context context;
 private final Listener listener;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final LinkedHashMap<String,JSONObject> bids=new LinkedHashMap<>();
 private WebView web;
 private int category,page,stalls,retries;
 private String signature="";
 private boolean running,ready;
 ServiceGemScanner(Context context,Listener listener){this.context=context;this.listener=listener;}
 void start(){
  if(running)return;
  running=true;
  category=ScanCheckpoint.exists(context)?ScanCheckpoint.keywordIndex(context):0;
  try{
   JSONArray saved=new JSONArray(context.getSharedPreferences("GemBrowserActivity",0).getString("gem_collected","[]"));
   for(int i=0;i<saved.length();i++){JSONObject j=saved.optJSONObject(i);if(j!=null&&!j.optString("bidId").isEmpty())bids.put(j.optString("bidId"),j);}
  }catch(Exception ignored){}
  web=new WebView(context);
  WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
  web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView view,String url){ready=true;handler.postDelayed(()->search(),1000);}});
  web.loadUrl(GEM);
  listener.progress(category,0,bids.size(),"Loading GeM");
 }
 private void search(){
  if(!running||!ready)return;
  if(category>=ScanPlan.size()){finish(true,"Scan complete");return;}
  page=0;stalls=0;retries=0;signature="";
  ScanCheckpoint.save(context,category,0);
  String keyword=ScanPlan.TERMS[category];
  listener.progress(category,0,bids.size(),"Searching "+keyword);
  // Headless WebView has no layout: use DOM selectors rather than screen coordinates.
  String js="(function(){try{var inputs=Array.from(document.querySelectorAll('input')).filter(e=>!e.disabled);var el=inputs.find(e=>/searchBid|search_bid|bid_search/i.test((e.id||'')+' '+(e.name||'')))||inputs.find(e=>/search|text/i.test(e.type||''));if(!el)return 'NO_INPUT';var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;setter.call(el,"+JSONObject.quote(keyword)+");el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));var buttons=Array.from(document.querySelectorAll('button,input[type=submit],a,[role=button]'));var button=buttons.find(e=>/search/i.test((e.innerText||e.value||e.title||'').trim()))||buttons.find(e=>e.type==='submit');if(!button)return 'NO_BUTTON';button.click();return 'OK';}catch(e){return 'ERROR'}})()";
  web.evaluateJavascript(js,result->{
   if(!running)return;
   if(!result.contains("OK")){if(++retries<=5){handler.postDelayed(this::search,2500);return;}finish(false,"GeM search controls unavailable");return;}
   handler.postDelayed(this::step,3000);
  });
 }
 private void step(){
  if(!running)return;
  web.evaluateJavascript(GemBidExtractor.listingScript(),result->{
   if(!running)return;
   try{
    JSONObject payload=new JSONObject(new JSONArray("["+result+"]").getString(0));
    if(payload.has("error"))throw new IllegalStateException(payload.optString("error"));
    JSONArray items=payload.optJSONArray("items");
    if(items==null||items.length()==0){
     if(++stalls<12){handler.postDelayed(this::step,2000);return;}
     finish(false,"No detectable GeM bid cards; category retained for retry");return;
    }
    StringBuilder fingerprint=new StringBuilder();
    for(int i=0;i<items.length();i++){
     JSONObject j=items.optJSONObject(i);if(j==null)continue;
     String id=j.optString("bidId");if(!id.matches("GEM/20\\d{2}/B/\\d+"))continue;
     if(fingerprint.length()<180)fingerprint.append(id);
     if(!bids.containsKey(id))bids.put(id,j);
    }
    if(fingerprint.toString().equals(signature)){
     if(++stalls<10){handler.postDelayed(this::step,2000);return;}
     finish(false,"GeM pagination stalled; checkpoint retained");return;
    }
    signature=fingerprint.toString();stalls=0;page++;
    JSONArray saved=new JSONArray();for(JSONObject j:bids.values())saved.put(j);
    context.getSharedPreferences("GemBrowserActivity",0).edit().putString("gem_collected",saved.toString()).apply();
    ScanCheckpoint.save(context,category,page);
    listener.progress(category,page,bids.size(),"Scanning "+ScanPlan.TERMS[category]);
    if(page>=25){finish(false,"25-page safety limit reached");return;}
    String next="(function(){var a=Array.from(document.querySelectorAll('a,button')).find(e=>!e.disabled&&e.getAttribute('aria-disabled')!=='true'&&!e.classList.contains('disabled')&&/^(next|next page|›|»|>)$/i.test((e.innerText||'').trim()));if(!a)return 'END';a.click();return 'NEXT'})()";
    web.evaluateJavascript(next,out->{if(!running)return;if(out.contains("NEXT"))handler.postDelayed(this::step,2000);else{category++;search();}});
   }catch(Exception e){finish(false,"GeM collection failed: "+e.getMessage());}
  });
 }
 void stop(){finish(false,"Stopped by user");}
 private void finish(boolean complete,String reason){
  if(!running)return;running=false;handler.removeCallbacksAndMessages(null);
  if(complete)ScanCheckpoint.clear(context);
  if(web!=null){web.stopLoading();web.destroy();web=null;}
  listener.finished(complete,reason);
 }
}
