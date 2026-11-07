package com.tenderintelligence.ai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 final ExecutorService pool=Executors.newSingleThreadExecutor();
 LinearLayout root; EditText key, model, title, deadline, body; TextView status, saved; LinearLayout resultCards, shortlistCards; TextView kpiFound,kpiFiltered,kpiSaved; EditText resultFilter; Button scanButton; LinearLayout manualTools; volatile boolean scanRunning=false; final ArrayList<JSONObject> lastResults=new ArrayList<>();
 final ArrayList<String> records=new ArrayList<>();
 final String[] keywords={"heat exchanger","shell and tube","tube bundle","condenser","surface condenser","cooler","oil cooler","air cooler","water cooler","intercooler","aftercooler","pressure vessel","storage vessel","storage tank","receiver","reactor","column","tower","separator","scrubber","demister","evaporator","reboiler","heater","feedwater heater","air preheater","economizer","radiator","chiller","heat recovery","waste heat","steam generator","boiler","tube sheet","tubesheet","baffle","tube expansion","tube replacement","retubing","tube insertion","fabrication","mechanical equipment","process equipment","piping","shell","fin tube"};
 static final int OPEN_TXT=8, SAVE_CSV=9, OPEN_PDF=10;
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(6,10,18));getWindow().setNavigationBarColor(Color.rgb(6,10,18));loadRecords();draw();}
 void showApiSettings(){
  LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(dp(18),dp(8),dp(18),dp(8));
  TextView info=text("Keys are held in memory only and are not saved. GeM public bid search needs no search API key. OpenRouter is optional for AI document analysis.",13);info.setTextColor(Color.rgb(190,204,223));layout.addView(info);
  EditText router=edit("OpenRouter API key",false);router.setInputType(129);router.setText(key.getText());layout.addView(router);
  CheckBox reveal=new CheckBox(this);reveal.setText("Show keys");reveal.setTextColor(Color.WHITE);layout.addView(reveal);
  reveal.setOnCheckedChangeListener((v,checked)->{int type=checked?InputType.TYPE_CLASS_TEXT:129;for(EditText field:new EditText[]{router}){field.setInputType(type);field.setSelection(field.length());}});
  ScrollView scroll=new ScrollView(this);scroll.addView(layout);
  new AlertDialog.Builder(this).setTitle("OpenRouter AI Settings").setView(scroll)
   .setNeutralButton("Clear keys",(d,w)->{key.setText("");status.setText("API keys cleared.");})
   .setNegativeButton("Cancel",null)
   .setPositiveButton("Save for this session",(d,w)->{key.setText(router.getText());status.setText("API settings updated for this session.");}).show();
 }
 void officialPortals(){startActivity(new Intent(this,GemBrowserActivity.class));}
 void showKeywords(){TextView info=text(EquipmentKeywords.list(),14);info.setTextColor(Color.WHITE);info.setPadding(dp(16),dp(12),dp(16),dp(12));ScrollView scroll=new ScrollView(this);scroll.addView(info);new AlertDialog.Builder(this).setTitle("Equipment keywords — GeM only").setView(scroll).setPositiveButton("Done",null).show();}
 void searchTenders(){
  if(scanRunning){status.setText("A GeM scan is already running. Please wait.");return;}
  scanRunning=true;if(scanButton!=null){scanButton.setEnabled(false);scanButton.setText("SEARCHING GEM — PLEASE WAIT");}
  status.setText("Connecting to GeM… Scanning public bid pages. This may take several minutes.");
 pool.execute(()->{
  try{
   TenderSearch.Result r=TenderSearch.scan((message)->runOnUiThread(()->status.setText(message)));
   ArrayList<JSONObject> enriched=new ArrayList<>();for(JSONObject item:r.tenders)enriched.add(TenderDetails.enrich(item));
   TenderFilter.Result filtered=TenderFilter.apply(enriched);
   runOnUiThread(()->{
    lastResults.clear();lastResults.addAll(filtered.accepted);renderSearchCards();
    StringBuilder summary=new StringBuilder("Checked "+r.stats.pages+" GeM pages. "+filtered.accepted.size()+" matching bids. "+filtered.expired+" closed and "+filtered.unknown+" unverified deadlines hidden.");
    if(!r.stats.errors.isEmpty())summary.append("\nIssue: ").append(r.stats.errors.get(0));
    if(filtered.accepted.isEmpty())summary.append("\nNo usable open bids found. GeM may have changed its public endpoint, or bid deadlines may be missing. This does NOT mean there are no tenders.");status.setText(summary.toString());scanRunning=false;if(scanButton!=null){scanButton.setEnabled(true);scanButton.setText("🚀 SEARCH GEM TENDERS");}if(kpiFound!=null)kpiFound.setText(String.valueOf(filtered.accepted.size()));if(kpiFiltered!=null)kpiFiltered.setText(String.valueOf(r.stats.blocked+filtered.blocked+filtered.expired+filtered.unknown+filtered.duplicates));

   });
  }catch(Exception e){runOnUiThread(()->{status.setText(e.getMessage()!=null&&e.getMessage().contains("403")?"GeM blocked automated search (HTTP 403). This is a portal access restriction, not a keyword problem. Tap Open GeM below to browse bids normally. Automatic scanning needs an authorized data source.":"GeM search failed: "+e.getMessage());scanRunning=false;if(scanButton!=null){scanButton.setEnabled(true);scanButton.setText("🚀 SEARCH GEM TENDERS");}});}
 });
 }
 void renderSearchCards(){
 if(resultCards==null)return;resultCards.removeAllViews();
 String q=resultFilter==null?"":resultFilter.getText().toString().trim().toLowerCase(Locale.ROOT);
 int count=0;for(JSONObject item:lastResults){String hay=(item.optString("title")+" "+item.optString("bidId")+" "+item.optString("snippet")).toLowerCase(Locale.ROOT);if(!hay.contains(q))continue;if(count++>=100)break;addResultCard(item);}
 if(count==0){TextView empty=text("No matching search results. Try another term or run a search.",13);empty.setTextColor(Color.rgb(190,204,223));resultCards.addView(empty);}
 }
 void renderShortlist(){
 if(shortlistCards==null)return;shortlistCards.removeAllViews();
 if(records.isEmpty()){TextView empty=text("No saved tenders yet.",13);empty.setTextColor(Color.rgb(190,204,223));shortlistCards.addView(empty);return;}
 for(int i=records.size()-1;i>=0;i--){
  try{
   JSONObject j=new JSONObject(records.get(i));final int index=i;
   LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(16,12,16,12);card.setBackground(panel(Color.rgb(15,23,42),Color.rgb(34,197,94),16));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,6,0,6);shortlistCards.addView(card,lp);
   TextView name=text(j.optString("title","Untitled tender"),15);name.setTextColor(Color.WHITE);card.addView(name);
   String state=j.optString("verification","MANUAL_ENTRY_UNVERIFIED");
   TextView detail=text("Status: "+state+"\nBid: "+j.optString("bidId","Not identified")+" | Deadline: "+j.optString("deadline","Unknown")+"\n"+j.optString("verdict","Review required"),12);detail.setTextColor(Color.rgb(62,218,190));card.addView(detail);
   String url=j.optString("url");if(TenderSearch.allowed(url)){Button open=new Button(this);open.setText("Open official-domain source");card.addView(open);open.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url))));}
   Button remove=new Button(this);remove.setText("Remove from shortlist");card.addView(remove);remove.setOnClickListener(v->{records.remove(index);persist();refresh();});
  }catch(Exception ignored){}
 }
 }
 void addResultCard(JSONObject j){
 LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(18,16,18,16);card.setBackgroundColor(Color.rgb(22,35,56));
 LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,8,0,8);resultCards.addView(card,lp);
 TextView name=text(j.optString("title"),17);name.setTextColor(Color.WHITE);name.setTypeface(null,1);card.addView(name);
 String deadline=j.optString("deadline");String state=j.optString("deadlineStatus");
 TextView meta=text("SEARCH RESULT ONLY • "+j.optString("source")+"\nBid: "+(j.optString("bidId").isEmpty()?"Not identified":j.optString("bidId"))+"\nDeadline: "+(deadline.isEmpty()?"Unknown":deadline)+" ("+state+")",12);meta.setTextColor(Color.rgb(62,218,190));card.addView(meta);
 TextView snippet=text(j.optString("snippet"),12);snippet.setTextColor(Color.rgb(190,204,223));snippet.setMaxLines(5);card.addView(snippet);
 Button open=new Button(this);open.setText("Open source");card.addView(open);open.setOnClickListener(v->{String url=j.optString("url");if(TenderSearch.allowed(url))startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));});
 Button saveButton=new Button(this);saveButton.setText("Save to shortlist");card.addView(saveButton);saveButton.setOnClickListener(v->{
 try{String url=j.optString("url");for(String rec:records){JSONObject existing=new JSONObject(rec);if(TenderFilter.normalizedKey(j).equals(TenderFilter.normalizedKey(existing))){status.setText("Already saved.");return;}}
 JSONObject record=new JSONObject(j.toString());record.put("verdict","REVIEW — NOT VERIFIED");record.put("text",j.optString("snippet"));records.add(record.toString());persist();refresh();status.setText("Saved for review. Source and deadline are not independently verified.");}
 catch(Exception e){status.setText("Save error: "+e.getMessage());}
 });
 }
 GradientDrawable panel(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;}
 int dp(int n){return Math.round(getResources().getDisplayMetrics().density*n);}
 void sectionLabel(String label){TextView t=text(label,18);t.setPadding(0,dp(20),0,dp(8));t.setTypeface(null,Typeface.BOLD);t.setTextColor(Color.WHITE);root.addView(t);}
 void statsRow(){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);root.addView(row);kpiFound=statCell(row,"Relevant","0",Color.rgb(52,211,153));kpiFiltered=statCell(row,"Filtered","0",Color.rgb(251,113,133));kpiSaved=statCell(row,"Shortlisted",String.valueOf(records.size()),Color.rgb(34,211,238));}
 TextView statCell(LinearLayout row,String label,String value,int color){LinearLayout cell=new LinearLayout(this);cell.setOrientation(1);cell.setPadding(dp(10),dp(10),dp(6),dp(10));cell.setBackground(panel(Color.rgb(15,23,42),Color.rgb(51,65,85),12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(2),dp(5),dp(2),dp(5));row.addView(cell,lp);TextView name=text(label,11);name.setTextColor(Color.rgb(148,163,184));cell.addView(name);TextView count=text(value,23);count.setTypeface(null,Typeface.BOLD);count.setTextColor(color);cell.addView(count);return count;}
 TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.rgb(29,41,57));v.setPadding(0,10,0,10);return v;}
 EditText edit(String hint, boolean multi){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.rgb(148,163,184));e.setBackground(panel(Color.rgb(15,23,42),Color.rgb(51,65,85),10));e.setPadding(dp(12),dp(10),dp(12),dp(10));if(multi){e.setMinLines(5);e.setGravity(Gravity.TOP);e.setInputType( android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);}return e;}
 Button addScanButton(String label,Runnable fn){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.rgb(4,18,28));b.setTypeface(null,Typeface.BOLD);b.setBackground(panel(Color.rgb(45,212,191),Color.rgb(34,211,238),14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(5),0,dp(5));root.addView(b,lp);b.setOnClickListener(v->fn.run());return b;}
 void addManualButton(String label,Runnable fn){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.rgb(165,243,252));b.setBackground(panel(Color.rgb(15,23,42),Color.rgb(51,65,85),10));manualTools.addView(b);b.setOnClickListener(v->fn.run());}
 void addSecondaryButton(String label,Runnable fn){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(Color.rgb(165,243,252));b.setBackground(panel(Color.rgb(15,23,42),Color.rgb(51,65,85),12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(44));lp.setMargins(0,dp(4),0,dp(4));root.addView(b,lp);b.setOnClickListener(v->fn.run());}
 void addButton(String label,Runnable fn){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.rgb(4,18,28));b.setTypeface(null,Typeface.BOLD);b.setBackground(panel(Color.rgb(45,212,191),Color.rgb(34,211,238),14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(5),0,dp(5));root.addView(b,lp);b.setOnClickListener(v->fn.run());}
 void draw(){
 ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(20),dp(16),dp(26));sc.addView(root);setContentView(sc);
 getWindow().getDecorView().setSystemUiVisibility(0);
 sc.setClipToPadding(false);
 int statusId=getResources().getIdentifier("status_bar_height","dimen","android");
 int inset=statusId>0?getResources().getDimensionPixelSize(statusId):dp(26);
 sc.setPadding(0,inset,0,0);
 root.setBackgroundColor(Color.rgb(6,10,18));
 TextView heading=text("Aditya Udyog • Tender Radar",23);heading.setTextColor(Color.WHITE);heading.setTypeface(null,1);root.addView(heading);
 
 TextView banner=text("GeM • Public bid discovery",13);banner.setTextColor(Color.rgb(29,220,180));root.addView(banner);
 statsRow();

 TextView disclaimer=text("Browse GeM with its own search and pagination. Automatic scanning is paused because GeM returns HTTP 403.",12);disclaimer.setTextColor(Color.rgb(159,178,205));root.addView(disclaimer);
 
 
 scanButton=addScanButton("OPEN GEM TENDER SEARCH",this::officialPortals);
 status=text("GeM blocks the automated endpoint (403). Browse public bids in the built-in browser; automatic document shortlisting is not available yet.",13);status.setTextColor(Color.rgb(45,212,191));status.setTextIsSelectable(true);root.addView(status);
 addSecondaryButton("Open GeM in external browser",()->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(TenderSearch.GEM_URL))));
 addSecondaryButton("Equipment keyword categories",this::showKeywords);
 sectionLabel("GeM tender results");
 resultFilter=edit("Filter results by title, bid ID or equipment...",false);root.addView(resultFilter);
 resultCards=new LinearLayout(this);resultCards.setOrientation(1);root.addView(resultCards);
 resultFilter.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){renderSearchCards();}public void afterTextChanged(android.text.Editable e){}});
 TextView shortlistHeading=text("Saved shortlist — review required",19);shortlistHeading.setPadding(0,dp(20),0,dp(8));shortlistHeading.setTextColor(Color.WHITE);root.addView(shortlistHeading);
 shortlistCards=new LinearLayout(this);shortlistCards.setOrientation(1);root.addView(shortlistCards);
 
 addSecondaryButton("⚙ OpenRouter AI settings",this::showApiSettings);
 addSecondaryButton("Manual PDF analysis (optional) ▾",()->{boolean visible=manualTools.getVisibility()!=View.VISIBLE;manualTools.setVisibility(visible?View.VISIBLE:View.GONE);});
 manualTools=new LinearLayout(this);manualTools.setOrientation(1);manualTools.setVisibility(View.GONE);root.addView(manualTools);
 
 key=edit("OpenRouter API key",false);key.setInputType(129);
 model=edit("Model ID (e.g. openai/gpt-4o-mini)",false);model.setText(getPreferences(0).getString("model_id","openai/gpt-4o-mini"));manualTools.addView(model);model.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){getPreferences(0).edit().putString("model_id",s.toString()).apply();}public void afterTextChanged(android.text.Editable e){}});
 addManualButton("Choose model from OpenRouter",this::chooseModel);
 title=edit("Tender title",false);manualTools.addView(title);
 deadline=edit("Deadline YYYY-MM-DD (optional)",false);manualTools.addView(deadline);
 body=edit("Tender document text / technical scope",true);manualTools.addView(body);
 addManualButton("Import PDF tender (offline text extraction)",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/pdf");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,OPEN_PDF);});
 addManualButton("Import TXT document",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("text/plain");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,OPEN_TXT);});
 addManualButton("Score and save tender",this::save);
 addManualButton("Analyze with OpenRouter AI",this::analyze);
 addManualButton("Export shortlist CSV",()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,"tender-shortlist.csv");startActivityForResult(i,SAVE_CSV);});

 saved=text("",14);saved.setTextColor(Color.WHITE);root.addView(saved);refresh();
 }
 void refresh(){renderShortlist();if(kpiSaved!=null)kpiSaved.setText(String.valueOf(records.size()));StringBuilder s=new StringBuilder("Saved tenders: "+records.size()+"\n");for(int i=records.size()-1;i>=0&&i>=records.size()-20;i--){try{JSONObject j=new JSONObject(records.get(i));s.append("\n").append(j.optString("title")).append(" • ").append(j.optString("verdict")).append(" • ").append(j.optString("deadline")).append("\n");}catch(Exception ignored){}}saved.setText(s.toString());}
 void loadRecords(){String raw=getPreferences(0).getString("records","[]");try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++)records.add(a.getString(i));}catch(Exception ignored){}}
 void persist(){getPreferences(0).edit().putString("records",new JSONArray(records).toString()).apply();}
 int score(String value){int score=0;String t=value.toLowerCase(Locale.ROOT);for(String k:keywords)if(t.contains(k))score+=20;return Math.min(100,score);}
 boolean expired(String d){return d.matches("\\d{4}-\\d{2}-\\d{2}")&&d.compareTo(new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()))<0;}
 void save(){try{String t=title.getText().toString().trim(),d=deadline.getText().toString().trim(),b=body.getText().toString();if(t.isEmpty()){status.setText("Enter tender title.");return;}int sc=score(t+" "+b);String verdict=expired(d)?"EXPIRED":sc>=40?"SHORTLIST":sc>0?"REVIEW":"NO MATCH";JSONObject j=new JSONObject();j.put("title",t);j.put("deadline",d);j.put("score",sc);j.put("verdict",verdict);j.put("text",b);records.add(j.toString());persist();status.setText("Saved • "+verdict+" • score "+sc);refresh();}catch(Exception e){status.setText(e.toString());}}
 String request(String url,String method,String token,String payload)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod(method);c.setConnectTimeout(20000);c.setReadTimeout(40000);c.setRequestProperty("Accept","application/json");if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);if(payload!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(payload.getBytes(StandardCharsets.UTF_8));}}int code=c.getResponseCode();InputStream in=code<400?c.getInputStream():c.getErrorStream();String data=new String(readBytes(in,4000000),StandardCharsets.UTF_8);c.disconnect();if(code>=400)throw new IOException("HTTP "+code+": "+data.substring(0,Math.min(300,data.length())));return data;}
 byte[] readBytes(InputStream in,int limit)throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>limit)throw new IOException("File or response exceeds "+limit+" bytes");out.write(buf,0,n);}return out.toByteArray();}
 void background(Callable<String> task){status.setText("Working...");pool.execute(()->{try{String result=task.call();runOnUiThread(()->status.setText(result));}catch(Exception e){runOnUiThread(()->status.setText("Error: "+e.getMessage()));}});}
 void chooseModel(){String token=key.getText().toString().trim();background(()->{JSONObject json=new JSONObject(request("https://openrouter.ai/api/v1/models","GET",token.isEmpty()?null:token,null));JSONArray arr=json.getJSONArray("data");ArrayList<String> ids=new ArrayList<>();for(int i=0;i<arr.length();i++)ids.add(arr.getJSONObject(i).getString("id"));Collections.sort(ids);runOnUiThread(()->{EditText filter=new EditText(this);filter.setSingleLine(true);filter.setHint("Search model name...");LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(16),0,dp(16),0);panel.addView(filter);ListView list=new ListView(this);panel.addView(list,new LinearLayout.LayoutParams(-1,dp(380)));ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>(ids));list.setAdapter(adapter);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Choose OpenRouter model").setView(panel).setNegativeButton("Cancel",null).create();filter.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){adapter.getFilter().filter(s);}public void afterTextChanged(android.text.Editable e){}});list.setOnItemClickListener((parent,v,pos,id)->{model.setText(adapter.getItem(pos));dialog.dismiss();});dialog.show();});return "Loaded "+ids.size()+" models. Search and select one.";});}
 void analyze(){String token=key.getText().toString().trim(),m=model.getText().toString().trim(),t=title.getText().toString(),b=body.getText().toString();if(token.isEmpty()||m.isEmpty()||b.isEmpty()){status.setText("Enter API key, model and tender text.");return;}background(()->{JSONObject payload=new JSONObject();payload.put("model",m);JSONArray msgs=new JSONArray();msgs.put(new JSONObject().put("role","system").put("content","You analyze Indian industrial equipment tenders. Summarize equipment, quantities, eligibility, risks and relevance. Do not invent missing details. Flag expired dates."));msgs.put(new JSONObject().put("role","user").put("content","Tender: "+t+"\n"+b.substring(0,Math.min(b.length(),20000))));payload.put("messages",msgs);JSONObject response=new JSONObject(request("https://openrouter.ai/api/v1/chat/completions","POST",token,payload.toString()));return response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","No response");});}
 String csv(String s){return "\""+s.replace("\"","\"\"").replace("\n"," ")+"\"";}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;try{if(req==OPEN_PDF){android.net.Uri uri=data.getData();status.setText("Extracting PDF text...");pool.execute(()->{try{PdfTenderReader.Result r=PdfTenderReader.read(this,uri);runOnUiThread(()->{body.setText(r.text);if(!r.bidId.isEmpty())title.setText("GeM Tender "+r.bidId);if(!r.deadline.isEmpty())deadline.setText(r.deadline);status.setText("PDF imported: "+r.pages+" pages\nBid: "+(r.bidId.isEmpty()?"Unknown":r.bidId)+"\nDeadline: "+(r.deadline.isEmpty()?"Unknown":r.deadline)+" ("+r.deadlineStatus+")\nEquipment: "+(r.keywords.isEmpty()?"No equipment keywords matched":r.keywords)+"\nImported document is NOT independently verified.");});}catch(Exception e){runOnUiThread(()->status.setText("PDF error: "+e.getMessage()));}});}else if(req==OPEN_TXT){try(InputStream in=getContentResolver().openInputStream(data.getData())){byte[] bytes=readBytes(in,1000000);if(bytes.length>1000000){status.setText("TXT too large (max 1 MB).");return;}body.setText(new String(bytes,StandardCharsets.UTF_8));status.setText("TXT imported.");}}else if(req==SAVE_CSV){StringBuilder s=new StringBuilder("Title,Deadline,Score,Verdict\n");for(String r:records){JSONObject j=new JSONObject(r);s.append(csv(j.optString("title"))).append(",").append(csv(j.optString("deadline"))).append(",").append(j.optInt("score")).append(",").append(csv(j.optString("verdict"))).append("\n");}try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(s.toString().getBytes(StandardCharsets.UTF_8));}status.setText("CSV exported.");}}catch(Exception e){status.setText("File error: "+e.getMessage());}}
 @Override protected void onDestroy(){pool.shutdownNow();super.onDestroy();}
}
