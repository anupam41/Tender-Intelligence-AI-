package com.tenderintelligence.ai;

import android.content.Context;
import android.net.Uri;
import org.json.JSONObject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Minimal standards-based WordprocessingML .docx export without third-party dependencies. */
public final class TenderWordExport {
 private TenderWordExport(){}
 private static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
 private static String para(String text,boolean heading){
  StringBuilder b=new StringBuilder("<w:p><w:pPr>");
  if(heading)b.append("<w:pStyle w:val=\"Heading1\"/>");
  b.append("</w:pPr><w:r><w:t xml:space=\"preserve\">").append(esc(text)).append("</w:t></w:r></w:p>");
  return b.toString();
 }
 private static void entry(ZipOutputStream z,String name,String text)throws Exception{
  z.putNextEntry(new ZipEntry(name));z.write(text.getBytes(StandardCharsets.UTF_8));z.closeEntry();
 }
 public static void write(Context context,Uri destination,JSONObject bid,JSONObject review)throws Exception{
  String id=bid.optString("bidId","Unknown");
  String url=bid.optString("url","");
  StringBuilder body=new StringBuilder();
  body.append(para("Shortlisted Tender",true));
  body.append(para("GeM Bid: "+id,false));
  body.append(para("Title: "+bid.optString("title","Not provided"),false));
  body.append(para("Closing date: "+bid.optString("deadlineText","Not provided"),false));
  body.append(para("Original GeM listing: "+(url.isEmpty()?"Not available":url),false));
  body.append(para("AI assessment",true));
  if(review==null){body.append(para("No AI summary saved for this tender.",false));}
  else{
   body.append(para("Verdict: "+review.optString("verdict","Needs review"),false));
   body.append(para("Indicative relevance score: "+review.optInt("score",0)+"/100",false));
   body.append(para("Evidence: "+review.optString("source","Document source not recorded"),false));
   for(String line:review.optString("report","").split("\\r?\\n"))body.append(para(line,false));
  }
  body.append(para("Note: AI results are provisional. Verify full buyer technical specifications, drawings, eligibility and closing dates on GeM before quotation.",false));
  String xml="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"+body+"<w:sectPr/></w:body></w:document>";
  try(OutputStream output=context.getContentResolver().openOutputStream(destination)){
   if(output==null)throw new java.io.IOException("Cannot write selected file");
   try(ZipOutputStream z=new ZipOutputStream(output)){
    entry(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
    entry(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
    entry(z,"word/document.xml",xml);
   }
  }
 }

 private static String combinedXml(java.util.Collection<JSONObject> bids,java.util.Map<String,JSONObject> reviews){
  StringBuilder body=new StringBuilder();
  body.append(para("Tender Radar - Shortlisted Tenders",true));
  body.append(para("Saved tenders: "+bids.size(),false));
  int index=0;
  for(JSONObject bid:bids){
   String id=bid.optString("bidId","Unknown");
   JSONObject review=reviews.get(id);
   body.append(para((++index)+". "+bid.optString("title","Untitled tender"),true));
   body.append(para("GeM bid: "+id,false));
   body.append(para("Closing: "+bid.optString("deadlineText","Not provided"),false));
   String url=bid.optString("url","");
   if(url.isEmpty()){
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)^GEM/20\\\\d{2}/B/(\\\\d+)$").matcher(id);
    if(m.matches())url="https://bidplus.gem.gov.in/showbidDocument/"+m.group(1);
   }
   body.append(para("GeM document: "+(url.isEmpty()?"Not available":url),false));
   if(review==null)body.append(para("AI summary: Not yet reviewed.",false));
   else{
    body.append(para("Verdict: "+review.optString("verdict","Needs review")+"; score: "+review.optInt("score",0)+"/100",false));
    for(String line:review.optString("report","").split("\\r?\\n"))body.append(para(line,false));
   }
  }
  body.append(para("AI assessments are provisional. Verify specifications and deadlines on GeM.",false));
  return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"+body+"<w:sectPr/></w:body></w:document>";
 }
 private static void writeCombined(java.io.OutputStream output,java.util.Collection<JSONObject> bids,java.util.Map<String,JSONObject> reviews)throws Exception{
  try(ZipOutputStream z=new ZipOutputStream(output)){
   entry(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
   entry(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
   entry(z,"word/document.xml",combinedXml(bids,reviews));
  }
 }
 public static void writeAll(java.io.File destination,java.util.Collection<JSONObject> bids,java.util.Map<String,JSONObject> reviews)throws Exception{
  writeCombined(new java.io.FileOutputStream(destination),bids,reviews);
 }
 public static void writeAll(Context context,Uri destination,java.util.Collection<JSONObject> bids,java.util.Map<String,JSONObject> reviews)throws Exception{
  java.io.OutputStream out=context.getContentResolver().openOutputStream(destination);
  if(out==null)throw new java.io.IOException("Cannot open Word destination");
  writeCombined(out,bids,reviews);
 }
}
