package com.tenderintelligence.ai;
import org.json.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;

public final class TenderDetails {
 private static final Pattern BID=Pattern.compile("GEM/\\d{4}/[A-Z]/\\d+",Pattern.CASE_INSENSITIVE);
 private static final Pattern DUE=Pattern.compile("(?:closing|submission|last|end|due)\\s*(?:date|time)?\\s*[:\\-]?\\s*(\\d{1,2}[-/.]\\d{1,2}[-/.]20\\d{2}|20\\d{2}[-/.]\\d{1,2}[-/.]\\d{1,2})",Pattern.CASE_INSENSITIVE);
 public static JSONObject enrich(JSONObject original)throws JSONException{
  JSONObject j=new JSONObject(original.toString());String text=j.optString("title")+"\n"+j.optString("snippet");
  Matcher bid=BID.matcher(text);j.put("bidId",bid.find()?bid.group().toUpperCase(Locale.ROOT):"");
  Matcher date=DUE.matcher(text);String due="";String state="UNKNOWN";if(date.find()){Date parsed=parse(date.group(1));if(parsed!=null){due=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(parsed);Calendar today=Calendar.getInstance();today.set(Calendar.HOUR_OF_DAY,0);today.set(Calendar.MINUTE,0);today.set(Calendar.SECOND,0);today.set(Calendar.MILLISECOND,0);state=parsed.before(today.getTime())?"EXPIRED":"DATE_FOUND_UNVERIFIED";}}
  j.put("deadline",due);j.put("deadlineStatus",state);j.put("verification","SEARCH_RESULT_ONLY");return j;
 }
 private static Date parse(String raw){String[] formats=raw.matches("^20\\d{2}.*")?new String[]{"yyyy-MM-dd","yyyy/MM/dd","yyyy.MM.dd"}:new String[]{"dd-MM-yyyy","dd/MM/yyyy","dd.MM.yyyy"};for(String f:formats)try{SimpleDateFormat df=new SimpleDateFormat(f,Locale.US);df.setLenient(false);return df.parse(raw);}catch(ParseException ignored){}return null;}
}
