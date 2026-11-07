package com.tenderintelligence.ai;

import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class TenderSearch {
 public static final String GEM_URL="https://bidplus.gem.gov.in/all-bids";
 private static final String[] HOSTS={"bidplus.gem.gov.in"};
 private static final String[] BATCHES=EquipmentKeywords.BATCHES;
 private static final String[] NEGATIVE={"drinking water cooler","water purifier","tube clamp","led lamp","plastic tube","split ac","window ac","research paper","annual report"};
 public static boolean allowed(String url){try{URI u=new URI(url);if(!"https".equalsIgnoreCase(u.getScheme())||u.getUserInfo()!=null)return false;String h=u.getHost();if(h==null)return false;h=h.toLowerCase(Locale.ROOT);for(String domain:HOSTS)if(h.equals(domain)||h.endsWith("."+domain))return true;return false;}catch(Exception e){return false;}}
 private static String post(String endpoint,JSONObject payload,String header,String secret)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");if(header!=null)c.setRequestProperty(header,secret);
  try(OutputStream out=c.getOutputStream()){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}
  int status=c.getResponseCode();InputStream in=status>=400?c.getErrorStream():c.getInputStream();if(in==null)throw new IOException("HTTP "+status);
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;try(InputStream input=in){while((n=input.read(buf))!=-1){if(out.size()+n>2000000)throw new IOException("Response too large");out.write(buf,0,n);}}
  c.disconnect();String data=new String(out.toByteArray(),StandardCharsets.UTF_8);if(status>=400)throw new IOException("HTTP "+status+" from search provider (response body hidden to protect API credentials)");return data;
 }
 private static void accept(JSONArray source,LinkedHashMap<String,JSONObject> found,Stats stats)throws Exception{
  for(int i=0;i<source.length();i++){JSONObject item=source.optJSONObject(i);if(item==null)continue;
   String url=item.optString("url"),title=item.optString("title"),snippet=item.optString("content");
   if(!allowed(url)){stats.blocked++;continue;}String text=(title+" "+snippet).toLowerCase(Locale.ROOT);
   boolean relevant=EquipmentKeywords.matches(text);for(String term:NEGATIVE)if(text.contains(term)){relevant=false;break;}
   if(!relevant){stats.blocked++;continue;}if(!(text.matches("(?s).*(tender|bid|procurement|enquiry|rfq|nit|gem/\\d{4}/b/).*"))){stats.blocked++;continue;}
   JSONObject record=new JSONObject();record.put("title",title);record.put("url",url);record.put("snippet",snippet);record.put("status","DISCOVERED — deadline unverified");record.put("source","Tavily");found.put(url,record);
  }
 }
 public static final class Stats{public int requests,blocked;public final ArrayList<String> errors=new ArrayList<>();}
 public static final class Result{public final ArrayList<JSONObject> tenders;public final Stats stats;Result(ArrayList<JSONObject> a,Stats s){tenders=a;stats=s;}}
 public static Result scan(String tavily)throws Exception{
  Stats stats=new Stats();LinkedHashMap<String,JSONObject> found=new LinkedHashMap<>();
  if(tavily.isEmpty())throw new IllegalArgumentException("Enter a Tavily API key.");
  for(String batch:BATCHES){
   if(!tavily.isEmpty())try{JSONObject p=new JSONObject().put("api_key",tavily).put("query","site:bidplus.gem.gov.in "+batch+" tender bid").put("search_depth","basic").put("max_results",8).put("include_domains",new JSONArray(HOSTS));stats.requests++;JSONObject response=new JSONObject(post("https://api.tavily.com/search",p,null,null));accept(response.optJSONArray("results")==null?new JSONArray():response.getJSONArray("results"),found,stats);}catch(Exception e){stats.errors.add("Tavily: "+e.getMessage());}

  }
  return new Result(new ArrayList<>(found.values()),stats);
 }
}
