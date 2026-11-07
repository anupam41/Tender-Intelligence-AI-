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

/** User-driven browser for public GeM bid listings. Does not scrape or bypass access controls. */
public class GemBrowserActivity extends Activity {
 private WebView browser;
 private EditText term;
 private TextView state;
 private static final String GEM="https://bidplus.gem.gov.in/all-bids";
 private boolean safe(Uri uri){
  return uri!=null&&"https".equalsIgnoreCase(uri.getScheme())&&"bidplus.gem.gov.in".equalsIgnoreCase(uri.getHost());
 }
 @Override public void onCreate(Bundle bundle){
  super.onCreate(bundle);
  getWindow().setStatusBarColor(Color.rgb(6,10,18));
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setBackgroundColor(Color.rgb(6,10,18));setContentView(page);
  TextView heading=new TextView(this);heading.setText("GeM Public Bid Browser");heading.setTextSize(20);heading.setTypeface(null,Typeface.BOLD);heading.setTextColor(Color.WHITE);heading.setPadding(16,16,16,8);page.addView(heading);
  TextView note=new TextView(this);note.setText("Browse GeM pages manually. Use GeM's search and pagination. This browser does not automatically download or analyze tenders.");note.setTextColor(Color.LTGRAY);note.setPadding(16,0,16,8);page.addView(note);
  LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);page.addView(controls);
  Button back=button("Back");controls.addView(back,new LinearLayout.LayoutParams(0,52,1));back.setOnClickListener(v->{if(browser.canGoBack())browser.goBack();else finish();});
  Button home=button("GeM home");controls.addView(home,new LinearLayout.LayoutParams(0,52,1));home.setOnClickListener(v->browser.loadUrl(GEM));
  Button reload=button("Reload");controls.addView(reload,new LinearLayout.LayoutParams(0,52,1));reload.setOnClickListener(v->browser.reload());
  LinearLayout findRow=new LinearLayout(this);findRow.setOrientation(LinearLayout.HORIZONTAL);page.addView(findRow);
  term=new EditText(this);term.setSingleLine(true);term.setTextColor(Color.WHITE);term.setHintTextColor(Color.LTGRAY);term.setHint("Search GeM bid keywords");findRow.addView(term,new LinearLayout.LayoutParams(0,54,1));
  Button find=button("Find");findRow.addView(find);find.setOnClickListener(v->{
   String q=term.getText().toString().trim();
   if(q.isEmpty()){state.setText("Enter an equipment keyword first.");return;}
   state.setText("Submitting GeM bid search…");
   String js="(function(){"+
    "var fields=Array.from(document.querySelectorAll('input[type=text],input[type=search],input:not([type])'));"+
    "var el=fields.find(e=>{var r=e.getBoundingClientRect();return r.width>80&&r.height>0&&(/keyword|search/i.test((e.placeholder||'')+' '+(e.getAttribute('aria-label')||'')))&&!!e.closest('form, .bid-listing, .search, .search-box, body')});"+
    "if(!el)return 'SEARCH_FIELD_NOT_FOUND';"+
    "el.focus();var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;setter.call(el,"+org.json.JSONObject.quote(q)+");"+
    "el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));"+
    "var scope=el.closest('form')||el.parentElement;"+
    "var btn=scope&&scope.querySelector('button[type=submit],input[type=submit],button');"+
    "if(!btn&&scope&&scope.parentElement)btn=scope.parentElement.querySelector('button[type=submit],button');"+
    "if(btn){btn.click();return 'SUBMITTED'}"+
    "if(el.form){if(el.form.requestSubmit)el.form.requestSubmit();else el.form.submit();return 'SUBMITTED'}"+
    "return 'SEARCH_BUTTON_NOT_FOUND';"+
    "})()";
   browser.evaluateJavascript(js,result->{
    if(result.contains("SUBMITTED"))state.setText("GeM search submitted. Waiting for results…");
    else state.setText("Could not locate GeM search controls. Use the search box inside the GeM page.");
   });
  });
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
   @Override public void onPageFinished(WebView view,String url){state.setText("GeM page loaded. Find searches bid listings; Next requests the next results page.");}
   @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError error){if(req.isForMainFrame())state.setText("Unable to load GeM in WebView. Try opening in your browser.");}
  });
  browser.setDownloadListener((url,userAgent,disposition,mime,length)->{Uri uri=Uri.parse(url);if(safe(uri))startActivity(new Intent(Intent.ACTION_VIEW,uri));else state.setText("Download blocked: external URL");});
  browser.loadUrl(GEM);
 }
 private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(12);return b;}
 @Override public void onBackPressed(){if(browser!=null&&browser.canGoBack())browser.goBack();else super.onBackPressed();}
 @Override protected void onDestroy(){if(browser!=null){browser.destroy();browser=null;}super.onDestroy();}
}
