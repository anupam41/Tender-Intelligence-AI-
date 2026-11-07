package com.tenderintelligence.ai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
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
 LinearLayout root; EditText key, model, title, deadline, body; TextView status, saved;
 final ArrayList<String> records=new ArrayList<>();
 final String[] keywords={"heat exchanger","shell and tube","tube bundle","condenser","surface condenser","cooler","oil cooler","air cooler","water cooler","intercooler","aftercooler","pressure vessel","storage vessel","storage tank","receiver","reactor","column","tower","separator","scrubber","demister","evaporator","reboiler","heater","feedwater heater","air preheater","economizer","radiator","chiller","heat recovery","waste heat","steam generator","boiler","tube sheet","tubesheet","baffle","tube expansion","tube replacement","retubing","tube insertion","fabrication","mechanical equipment","process equipment","piping","shell","fin tube"};
 static final int OPEN_TXT=8, SAVE_CSV=9;
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(23,92,211));loadRecords();draw();}
 void officialPortals(){String[] names={"GeM — bidplus.gem.gov.in","CPPP — eprocure.gov.in","CPPP — etenders.gov.in","IREPS — ireps.gov.in","BHEL — tenders.bhel.com","NTPC — ntpctender.ntpc.co.in","IOCL — iocletenders.nic.in","SAIL — sailtenders.co.in"};String[] urls={"https://bidplus.gem.gov.in/all-bids","https://eprocure.gov.in/eprocure/app","https://etenders.gov.in/eprocure/app","https://www.ireps.gov.in/","https://tenders.bhel.com/","https://ntpctender.ntpc.co.in/","https://iocletenders.nic.in/","https://sailtenders.co.in/"};new AlertDialog.Builder(this).setTitle("Official tender portals").setItems(names,(dialog,which)->{Intent i=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(urls[which]));startActivity(i);}).setNegativeButton("Cancel",null).show();}
 TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.rgb(29,41,57));v.setPadding(0,10,0,10);return v;}
 EditText edit(String hint, boolean multi){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.rgb(148,163,184));if(multi){e.setMinLines(5);e.setGravity(Gravity.TOP);e.setInputType( android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);}return e;}
 void addButton(String label,Runnable fn){Button b=new Button(this);b.setText(label);root.addView(b);b.setAllCaps(false);b.setOnClickListener(v->fn.run());}
 void draw(){
 ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(22,18,22,26);sc.addView(root);setContentView(sc);
 root.setBackgroundColor(Color.rgb(10,18,35));
 TextView heading=text("Aditya Udyog Official\\nTender Radar",25);heading.setTextColor(Color.WHITE);heading.setTypeface(null,1);root.addView(heading);
 TextView subtitle=text("Official Government & PSU tender intelligence • Verified sources only",13);subtitle.setTextColor(Color.rgb(159,178,205));root.addView(subtitle);
 TextView banner=text("🔒 OFFICIAL SOURCE ALLOWLIST\\nGeM • CPPP • IREPS • BHEL • NTPC • IOCL • SAIL",13);banner.setTextColor(Color.rgb(29,220,180));root.addView(banner);
 TextView scanTitle=text("1-Tap Official Government & PSU Tender Scanner",20);scanTitle.setTextColor(Color.WHITE);scanTitle.setTypeface(null,1);root.addView(scanTitle);
 TextView disclaimer=text("Scan opens official tender portals. Automated multi-portal crawling is not enabled yet. Only imported or manually verified notices are scored.",12);disclaimer.setTextColor(Color.rgb(159,178,205));root.addView(disclaimer);
 addButton("🚀 OPEN OFFICIAL TENDER PORTALS",this::officialPortals);
 TextView stats=text("✓ Saved official tenders: "+records.size()+"    •    45 industrial keywords",13);stats.setTextColor(Color.rgb(29,220,180));root.addView(stats);
 TextView section=text("Tender Import & AI Analysis",20);section.setTextColor(Color.WHITE);root.addView(section);
 key=edit("OpenRouter API key (memory only)",false);key.setInputType(129);root.addView(key);
 model=edit("Model ID (e.g. openai/gpt-4o-mini)",false);model.setText("openai/gpt-4o-mini");root.addView(model);
 addButton("Choose model from OpenRouter",this::chooseModel);
 title=edit("Tender title",false);root.addView(title);
 deadline=edit("Deadline YYYY-MM-DD (optional)",false);root.addView(deadline);
 body=edit("Tender document text / technical scope",true);root.addView(body);
 addButton("Import TXT document",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("text/plain");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,OPEN_TXT);});
 addButton("Score and save tender",this::save);
 addButton("Analyze with OpenRouter AI",this::analyze);
 addButton("Export shortlist CSV",()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,"tender-shortlist.csv");startActivityForResult(i,SAVE_CSV);});
 status=text("Ready. Import or paste a tender to begin.",14);status.setTextColor(Color.rgb(29,220,180));root.addView(status);
 saved=text("",14);saved.setTextColor(Color.WHITE);root.addView(saved);refresh();
 }
 void refresh(){StringBuilder s=new StringBuilder("Saved tenders: "+records.size()+"\n");for(int i=records.size()-1;i>=0&&i>=records.size()-20;i--){try{JSONObject j=new JSONObject(records.get(i));s.append("\n").append(j.optString("title")).append(" • ").append(j.optString("verdict")).append(" • ").append(j.optString("deadline")).append("\n");}catch(Exception ignored){}}saved.setText(s.toString());}
 void loadRecords(){String raw=getPreferences(0).getString("records","[]");try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++)records.add(a.getString(i));}catch(Exception ignored){}}
 void persist(){getPreferences(0).edit().putString("records",new JSONArray(records).toString()).apply();}
 int score(String value){int score=0;String t=value.toLowerCase(Locale.ROOT);for(String k:keywords)if(t.contains(k))score+=20;return Math.min(100,score);}
 boolean expired(String d){return d.matches("\\d{4}-\\d{2}-\\d{2}")&&d.compareTo(new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()))<0;}
 void save(){try{String t=title.getText().toString().trim(),d=deadline.getText().toString().trim(),b=body.getText().toString();if(t.isEmpty()){status.setText("Enter tender title.");return;}int sc=score(t+" "+b);String verdict=expired(d)?"EXPIRED":sc>=40?"SHORTLIST":sc>0?"REVIEW":"NO MATCH";JSONObject j=new JSONObject();j.put("title",t);j.put("deadline",d);j.put("score",sc);j.put("verdict",verdict);j.put("text",b);records.add(j.toString());persist();status.setText("Saved • "+verdict+" • score "+sc);refresh();}catch(Exception e){status.setText(e.toString());}}
 String request(String url,String method,String token,String payload)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod(method);c.setConnectTimeout(20000);c.setReadTimeout(40000);c.setRequestProperty("Accept","application/json");if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);if(payload!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(payload.getBytes(StandardCharsets.UTF_8));}}int code=c.getResponseCode();InputStream in=code<400?c.getInputStream():c.getErrorStream();String data=new String(readBytes(in,4000000),StandardCharsets.UTF_8);c.disconnect();if(code>=400)throw new IOException("HTTP "+code+": "+data.substring(0,Math.min(300,data.length())));return data;}
 byte[] readBytes(InputStream in,int limit)throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>limit)throw new IOException("File or response exceeds "+limit+" bytes");out.write(buf,0,n);}return out.toByteArray();}
 void background(Callable<String> task){status.setText("Working...");pool.execute(()->{try{String result=task.call();runOnUiThread(()->status.setText(result));}catch(Exception e){runOnUiThread(()->status.setText("Error: "+e.getMessage()));}});}
 void chooseModel(){String token=key.getText().toString().trim();background(()->{JSONObject json=new JSONObject(request("https://openrouter.ai/api/v1/models","GET",token.isEmpty()?null:token,null));JSONArray arr=json.getJSONArray("data");ArrayList<String> ids=new ArrayList<>();for(int i=0;i<arr.length();i++)ids.add(arr.getJSONObject(i).getString("id"));Collections.sort(ids);runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Select OpenRouter model").setItems(ids.toArray(new String[0]),(d,w)->model.setText(ids.get(w))).setNegativeButton("Cancel",null).show());return "Loaded "+ids.size()+" models. Select one.";});}
 void analyze(){String token=key.getText().toString().trim(),m=model.getText().toString().trim(),t=title.getText().toString(),b=body.getText().toString();if(token.isEmpty()||m.isEmpty()||b.isEmpty()){status.setText("Enter API key, model and tender text.");return;}background(()->{JSONObject payload=new JSONObject();payload.put("model",m);JSONArray msgs=new JSONArray();msgs.put(new JSONObject().put("role","system").put("content","You analyze Indian industrial equipment tenders. Summarize equipment, quantities, eligibility, risks and relevance. Do not invent missing details. Flag expired dates."));msgs.put(new JSONObject().put("role","user").put("content","Tender: "+t+"\n"+b.substring(0,Math.min(b.length(),20000))));payload.put("messages",msgs);JSONObject response=new JSONObject(request("https://openrouter.ai/api/v1/chat/completions","POST",token,payload.toString()));return response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","No response");});}
 String csv(String s){return "\""+s.replace("\"","\"\"").replace("\n"," ")+"\"";}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;try{if(req==OPEN_TXT){try(InputStream in=getContentResolver().openInputStream(data.getData())){byte[] bytes=readBytes(in,1000000);if(bytes.length>1000000){status.setText("TXT too large (max 1 MB).");return;}body.setText(new String(bytes,StandardCharsets.UTF_8));status.setText("TXT imported.");}}else if(req==SAVE_CSV){StringBuilder s=new StringBuilder("Title,Deadline,Score,Verdict\n");for(String r:records){JSONObject j=new JSONObject(r);s.append(csv(j.optString("title"))).append(",").append(csv(j.optString("deadline"))).append(",").append(j.optInt("score")).append(",").append(csv(j.optString("verdict"))).append("\n");}try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(s.toString().getBytes(StandardCharsets.UTF_8));}status.setText("CSV exported.");}}catch(Exception e){status.setText("File error: "+e.getMessage());}}
 @Override protected void onDestroy(){pool.shutdownNow();super.onDestroy();}
}
