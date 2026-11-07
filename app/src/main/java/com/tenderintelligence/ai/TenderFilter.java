package com.tenderintelligence.ai;
import org.json.*;
import java.net.URI;
import java.util.*;
import java.util.regex.*;

public final class TenderFilter {
 private static final String[] REJECT={"drinking water cooler","water purifier","split ac","window ac","led lamp","tube clamp","plastic tube","research paper","annual report","product catalogue","product catalog","technical brochure","journal article","piston kit","civil construction"};
 private static final String[] STRONG={"heat exchanger","shell and tube","tube bundle","retubing","pressure vessel","air receiver","surface condenser","gland steam condenser","sample cooler","swas cooler","turbine oil cooler","seal oil cooler","stator water cooler","generator air cooler","cooling coil","steam coil","economizer","reboiler"};
 private static final Pattern BID=Pattern.compile("GEM/\\d{4}/[A-Z]/\\d+",Pattern.CASE_INSENSITIVE);
 public static String normalizedKey(JSONObject j){
  String bid=j.optString("bidId").trim().toUpperCase(Locale.ROOT);
  if(BID.matcher(bid).matches())return "gem:"+bid;
  String url=j.optString("url").trim();
  try{URI uri=new URI(url).normalize();String host=uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.ROOT);String path=uri.getPath()==null?"":uri.getPath().replaceAll("/+$","");String query=uri.getRawQuery();return "url:"+host+path+(query==null?"":"?"+query);}catch(Exception e){return "url:"+url;}
 }
 public static String reason(JSONObject j){
  if(!TenderSearch.allowed(j.optString("url")))return "Not on approved source host";
  String title=j.optString("title").toLowerCase(Locale.ROOT),snippet=j.optString("snippet").toLowerCase(Locale.ROOT),all=title+" "+snippet;
  for(String x:REJECT)if(title.contains(x))return "Unrelated item: "+x;
  if(all.contains("issn")||all.contains("literature review"))return "Research or journal content";
  boolean relevant=EquipmentKeywords.matches(all);
  if(!relevant)return "No strong equipment match";
  if(!Pattern.compile("\\b(tender|bid|nit|rfq|enquiry|procurement|supply of|retubing)\\b",Pattern.CASE_INSENSITIVE).matcher(all).find())return "No tender evidence";
  if("EXPIRED".equals(j.optString("deadlineStatus")))return "Closing date appears past; check for extensions";
  if(!"DATE_FOUND_UNVERIFIED".equals(j.optString("deadlineStatus")))return "Deadline unknown — hidden from open-bid results";
  return "";
 }
 public static final class Result{public final ArrayList<JSONObject> accepted=new ArrayList<>();public int blocked,duplicates,expired,unknown;public final ArrayList<String> reasons=new ArrayList<>();}
 public static Result apply(List<JSONObject> items){
  Result result=new Result();LinkedHashSet<String> seen=new LinkedHashSet<>();
  for(JSONObject j:items){
   String reason=reason(j);if(!reason.isEmpty()){if(reason.startsWith("Closing date"))result.expired++;else if(reason.startsWith("Deadline unknown"))result.unknown++;else result.blocked++;if(result.reasons.size()<8)result.reasons.add(reason);continue;}
   String key=normalizedKey(j);if(!seen.add(key)){result.duplicates++;continue;}
   result.accepted.add(j);
  }
  return result;
 }
}
