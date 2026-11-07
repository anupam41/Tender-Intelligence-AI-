package com.tenderintelligence.ai;

import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class TenderSearch {
 private static final String[] HOSTS={"gem.gov.in","eprocure.gov.in","etenders.gov.in","bhel.com","ntpc.co.in","iocletenders.nic.in","wbtenders.gov.in","sailtenders.co.in","ireps.gov.in","gailtenders.in","npcil.nic.in","coalindiatenders.nic.in"};
 private static final String[] BATCHES={
  "\"Sample Cooler\" OR \"SWAS Cooler\" OR \"Cooling coil\" tender",
  "\"Shell and Tube Heat Exchanger\" OR \"Heat Exchanger\" tender",
  "\"Turbine oil cooler\" OR \"Lube oil cooler\" OR \"Seal Oil cooler\" tender",
  "\"Surface Condenser\" OR \"Gland steam Condenser\" tender",
  "\"U Tube bundle\" OR \"Tube bundle\" retubing tender",
  "\"Generator Air Cooler\" OR \"Stator water cooler\" tender",
  "\"HP heater\" OR \"LP heater\" OR \"Economizer\" tender",
  "\"Pressure Vessel\" OR \"Air Receiver\" tender"
 };
 private static final String[] TERMS={"sample cooler","swas cooler","cooling coil","heat exchanger","shell and tube","condenser","oil cooler","tube bundle","retubing","air cooler","stator water cooler","hp heater","lp heater","economizer","pressure vessel","air receiver","evaporator","reboiler","gland steam","after cooler","inter cooler","water cooler","heater"};
 private static final String[] NEGATIVE={"drinking water cooler","water purifier","tube clamp","led lamp","plastic tube","split ac","window ac","research paper","annual report"};
 public static boolean allowed(String url){try{URI u=new URI(url);if(!"https".equalsIgnoreCase(u.getScheme())||u.getUserInfo()!=null)return false;String h=u.getHost();if(h==null)return false;h=h.toLowerCase(Locale.ROOT);for(String domain:HOSTS)if(h.equals(domain)||h.endsWith("."+domain))return true;return false;}catch(Exception e){return false;}}
 private static String post(String endpoint,JSONObject payload,String header,String secret)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");if(header!=null)c.setRequestProperty(header,secret);
  try(OutputStream out=c.getOutputStream()){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}
  int status=c.getResponseCode();InputStream in=status>=400?c.getErrorStream():c.getInputStream();if(in==null)throw new IOException("HTTP "+status);
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;try(InputStream input=in){while((n=input.read(buf))!=-1){if(out.size()+n>2000000)throw new IOException("Response too large");out.write(buf,0,n);}}
  c.disconnect();String data=new String(out.toByteArray(),StandardCharsets.UTF_8);if(status>=400)throw new IOException("HTTP "+status+" from search provider (response body hidden to protect API credentials)");return data;
 }
 private static void accept(JSONArray source,boolean serper,LinkedHashMap<String,JSONObject> found,Stats stats)throws Exception{
  for(int i=0;i<source.length();i++){JSONObject item=source.optJSONObject(i);if(item==null)continue;
   String url=item.optString(serper?"link":"url"),title=item.optString("title"),snippet=item.optString(serper?"snippet":"content");
   if(!allowed(url)){stats.blocked++;continue;}String text=(title+" "+snippet).toLowerCase(Locale.ROOT);
   boolean relevant=false;for(String term:TERMS)if(text.contains(term)){relevant=true;break;}for(String term:NEGATIVE)if(text.contains(term)){relevant=false;break;}
   if(!relevant){stats.blocked++;continue;}if(!(text.matches("(?s).*(tender|bid|procurement|enquiry|rfq|nit|gem/\\d{4}/b/).*"))){stats.blocked++;continue;}
   JSONObject record=new JSONObject();record.put("title",title);record.put("url",url);record.put("snippet",snippet);record.put("status","DISCOVERED — deadline unverified");record.put("source",serper?"Serper":"Tavily");found.put(url,record);
  }
 }
 public static final class Stats{public int requests,blocked;public final ArrayList<String> errors=new ArrayList<>();}
 public static final class Result{public final ArrayList<JSONObject> tenders;public final Stats stats;Result(ArrayList<JSONObject> a,Stats s){tenders=a;stats=s;}}
 public static Result scan(String tavily,String serper)throws Exception{
  Stats stats=new Stats();LinkedHashMap<String,JSONObject> found=new LinkedHashMap<>();
  if(tavily.isEmpty()&&serper.isEmpty())throw new IllegalArgumentException("Enter a Tavily or Serper API key.");
  for(String batch:BATCHES){
   if(!tavily.isEmpty())try{JSONObject p=new JSONObject().put("api_key",tavily).put("query",batch).put("search_depth","basic").put("max_results",8).put("include_domains",new JSONArray(HOSTS));stats.requests++;JSONObject response=new JSONObject(post("https://api.tavily.com/search",p,null,null));accept(response.optJSONArray("results")==null?new JSONArray():response.getJSONArray("results"),false,found,stats);}catch(Exception e){stats.errors.add("Tavily: "+e.getMessage());}
   if(!serper.isEmpty())try{StringBuilder sites=new StringBuilder("(");for(int j=0;j<HOSTS.length;j++){if(j>0)sites.append(" OR ");sites.append("site:").append(HOSTS[j]);}sites.append(")");JSONObject p=new JSONObject().put("q",batch+" "+sites).put("gl","in").put("num",10);stats.requests++;JSONObject response=new JSONObject(post("https://google.serper.dev/search",p,"X-API-KEY",serper));accept(response.optJSONArray("organic")==null?new JSONArray():response.getJSONArray("organic"),true,found,stats);}catch(Exception e){stats.errors.add("Serper: "+e.getMessage());}
  }
  return new Result(new ArrayList<>(found.values()),stats);
 }
}
