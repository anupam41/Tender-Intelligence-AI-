package com.tenderintelligence.ai;

import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.text.*;

public final class TenderSearch {
 public static final String GEM_URL="https://bidplus.gem.gov.in/all-bids";
 private static final String DATA_URL="https://bidplus.gem.gov.in/all-bids-data";
 private static final int MAX_PAGES_PER_QUERY=3;
 private static final String[] QUERIES={"heat exchanger","condenser","cooler","chiller","radiator","dryer","heater","economizer","ejector","evaporator","tube bundle","cooling coil","tank","vessel"};
 private static final Pattern CSRF=Pattern.compile("csrf_bd_gem_nk.*?['\\\"]([a-fA-F0-9]{32})['\\\"]",Pattern.DOTALL);
 public static boolean allowed(String url){try{URI u=new URI(url);if(!"https".equalsIgnoreCase(u.getScheme())||u.getUserInfo()!=null)return false;String h=u.getHost();return "bidplus.gem.gov.in".equalsIgnoreCase(h);}catch(Exception e){return false;}}
 private static String first(JSONObject obj,String field){Object v=obj.opt(field);if(v instanceof JSONArray)return ((JSONArray)v).optString(0,"");return v==null?"":String.valueOf(v);}
 private static String request(String url,String data,String cookie)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
  c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36");
  c.setRequestProperty("Accept",data==null?"text/html":"application/json");
  if(cookie!=null&&!cookie.isEmpty())c.setRequestProperty("Cookie",cookie);
  if(data!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");c.setRequestProperty("Referer",GEM_URL);c.setRequestProperty("Origin","https://bidplus.gem.gov.in");c.setRequestProperty("X-Requested-With","XMLHttpRequest");try(OutputStream out=c.getOutputStream()){out.write(data.getBytes(StandardCharsets.UTF_8));}}
  int code=c.getResponseCode();InputStream stream=code>=400?c.getErrorStream():c.getInputStream();StringBuilder result=new StringBuilder();
  if(stream!=null)try(InputStream in=stream){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(result.length()+n>2000000)throw new IOException("GeM response too large");result.append(new String(buf,0,n,StandardCharsets.UTF_8));}}
  String setCookie=c.getHeaderField("Set-Cookie");c.disconnect();
  if(code==403)throw new IOException("GeM session rejected (403); retry the scan");
  if(code>=400&&code!=404)throw new IOException("GeM HTTP "+code);
  if(data==null&&setCookie!=null)sessionCookie=setCookie.split(";",2)[0];
  return result.toString();
 }
 private static String sessionCookie="";
 private static String startSession()throws Exception{
  sessionCookie="";
  String html=request(GEM_URL,null,null);Matcher m=CSRF.matcher(html);
  if(!m.find())throw new IOException("GeM CSRF token unavailable; portal may have changed");
  return m.group(1);
 }
 private static JSONObject page(String token,String query,int number)throws Exception{
  JSONObject filter=new JSONObject().put("bidStatusType","ongoing_bids").put("byType","all").put("highBidValue","").put("byEndDate",new JSONObject().put("from","").put("to","")).put("sort","Bid-End-Date-Latest");
  JSONObject payload=new JSONObject().put("page",number).put("param",new JSONObject().put("searchBid",query).put("searchType","fullText")).put("filter",filter);
  String body="payload="+URLEncoder.encode(payload.toString(),"UTF-8")+"&csrf_bd_gem_nk="+URLEncoder.encode(token,"UTF-8");
  return new JSONObject(request(DATA_URL,body,sessionCookie));
 }
 private static String deadline(String raw){if(raw==null)return "";Matcher m=Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})").matcher(raw);if(m.find())return m.group(1);return "";}
 private static JSONObject normalize(JSONObject doc)throws Exception{
  String id=first(doc,"b_id"),bid=first(doc,"b_bid_number"),category=first(doc,"b_category_name"),name=first(doc,"bbt_title");
  String department=first(doc,"ba_official_details_deptName"),ministry=first(doc,"ba_official_details_minName");
  String end=first(doc,"final_end_date_sort");
  String title=name.isEmpty()?category:name;
  String snippet=category+" • "+department+" • "+ministry+" • "+bid;
  JSONObject j=new JSONObject().put("title",title).put("snippet",snippet).put("bidId",bid).put("deadline",deadline(end)).put("source","GeM public listings").put("verification","GEM_PUBLIC_LISTING");
  j.put("url",id.matches("\\d+")?"https://bidplus.gem.gov.in/showbidDocument/"+id:GEM_URL);
  j.put("status","GeM ongoing listing — confirm on portal");
  return j;
 }
 public static final class Stats{public int requests,blocked,pages;public final ArrayList<String> errors=new ArrayList<>();}
 public static final class Result{public final ArrayList<JSONObject> tenders;public final Stats stats;Result(ArrayList<JSONObject> a,Stats s){tenders=a;stats=s;}}
 public interface Progress{void update(String message);}
 public static Result scan(Progress progress)throws Exception{
  Stats stats=new Stats();LinkedHashMap<String,JSONObject> found=new LinkedHashMap<>();
  progress.update("Opening GeM public bid listings and starting a session…");
  String token=startSession();
  for(String query:QUERIES){
   progress.update("Searching GeM for: "+query+" • "+found.size()+" matching bids collected");
   for(int p=1;p<=MAX_PAGES_PER_QUERY;p++){
    progress.update("GeM: "+query+" • page "+p+"/"+MAX_PAGES_PER_QUERY+" • "+found.size()+" candidates");
    try{
     JSONObject response=page(token,query,p);stats.requests++;stats.pages++;
     if(response.optInt("code",0)==404)break;
     if(response.optInt("code",0)!=200){stats.errors.add("GeM "+query+": response "+response.optInt("code"));break;}
     JSONObject inner=response.optJSONObject("response");if(inner==null)break;inner=inner.optJSONObject("response");if(inner==null)break;
     JSONArray docs=inner.optJSONArray("docs");if(docs==null||docs.length()==0)break;
     for(int i=0;i<docs.length();i++){JSONObject doc=docs.optJSONObject(i);if(doc==null)continue;JSONObject item=normalize(doc);
      if(!EquipmentKeywords.matches(item.optString("title")+" "+item.optString("snippet"))){stats.blocked++;continue;}
      String bid=item.optString("bidId");String key=bid.isEmpty()?item.optString("url"):bid;
      found.put(key,item);
     }
     if(p*10>=inner.optInt("numFound",0)||docs.length()<10)break;
     try{Thread.sleep(650);}catch(InterruptedException e){Thread.currentThread().interrupt();return new Result(new ArrayList<>(found.values()),stats);}
    }catch(Exception e){stats.errors.add("GeM "+query+" page "+p+": "+e.getMessage());break;}
   }
  }
  progress.update("GeM search finished. Checking closing dates and removing duplicates…");
  return new Result(new ArrayList<>(found.values()),stats);
 }
}
