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
   if(q.isEmpty()){state.setText("Select or type an equipment keyword first.");return;}
   state.setText("Submitting GeM bid search…");
   ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(term.getWindowToken(),0);
   term.clearFocus();
   String js="(function(){try{"+
    "var inputs=Array.from(document.querySelectorAll('input')).filter(e=>{var r=e.getBoundingClientRect();return r.width>100&&r.height>0&&!e.disabled});"+
    "var el=inputs.find(e=>/searchBid|search_bid|bid_search/i.test((e.id||'')+' '+(e.name||'')))||inputs.find(e=>/heat exchanger|head exchanger|oil cooler/i.test(e.value||''))||inputs.find(e=>{var p=e.parentElement;return p&&/contains/i.test((p.parentElement&&p.parentElement.innerText||'').slice(0,90))})||inputs.filter(e=>/text|search/i.test(e.type||'text')).slice(-1)[0];"+
    "if(!el)return 'NO_BID_INPUT';"+
    "var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;setter.call(el,"+org.json.JSONObject.quote(q)+");"+
    "el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));"+
    "var r=el.getBoundingClientRect();"+
    "var candidates=Array.from(document.querySelectorAll('button,input[type=submit],a,[role=button]')).filter(e=>{var b=e.getBoundingClientRect();return b.width>18&&b.height>18&&b.left>=r.right-12&&b.left<r.right+140&&Math.abs(b.top-r.top)<35&&!e.disabled});"+
    "candidates.sort((a,b)=>Math.abs(a.getBoundingClientRect().left-r.right)-Math.abs(b.getBoundingClientRect().left-r.right));"+
    "var btn=candidates[0];if(!btn)return 'BID_INPUT_SET_BUT_BUTTON_NOT_FOUND';"+
    "el.blur();btn.click();return 'SEARCH_CLICKED';"+
    "}catch(e){return 'ERROR:'+e.message}})()";
   browser.evaluateJavascript(js,result->{
    if(result.contains("SEARCH_CLICKED"))state.setText("GeM bid-search button clicked for "+q+". Check the results below.");
    else if(result.contains("NO_BID_INPUT"))state.setText("GeM bid search field not found. Use the search box inside the webpage.");
    else state.setText("Keyword entered, but GeM's search button was not detected. Tap the blue magnifying glass beside GeM's bid search field.");
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
   @Override public void onPageFinished(WebView view,String url){state.setText("GeM loaded. Choose a keyword, then tap Search.");}
   @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError error){if(req.isForMainFrame())state.setText("Unable to load GeM in WebView. Try opening in your browser.");}
  });
  browser.setDownloadListener((url,userAgent,disposition,mime,length)->{Uri uri=Uri.parse(url);if(safe(uri))startActivity(new Intent(Intent.ACTION_VIEW,uri));else state.setText("Download blocked: external URL");});
  browser.loadUrl(GEM);
 }
 private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(Color.BLACK);return b;}
 @Override public void onBackPressed(){if(browser!=null&&browser.canGoBack())browser.goBack();else super.onBackPressed();}
 @Override protected void onDestroy(){if(browser!=null){browser.destroy();browser=null;}super.onDestroy();}
}
