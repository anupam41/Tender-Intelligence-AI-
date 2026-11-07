package com.tenderintelligence.ai;

import android.content.Context;
import android.net.Uri;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public final class PdfTenderReader {
 public static final class Result {
  public String text, bidId, deadline, deadlineStatus, keywords;
  public int pages;
 }
 public static Result read(Context context, Uri uri) throws Exception {
  PDFBoxResourceLoader.init(context.getApplicationContext());
  File file=File.createTempFile("tender_",".pdf",context.getCacheDir());
  try {
   try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(file)){
    if(in==null)throw new IOException("Cannot open PDF");
    byte[] buf=new byte[8192];int n;long total=0;
    while((n=in.read(buf))!=-1){total+=n;if(total>12*1024*1024)throw new IOException("PDF exceeds 12 MB limit");out.write(buf,0,n);}
   }
   try(PDDocument doc=PDDocument.load(file)){
    if(doc.isEncrypted())throw new IOException("Encrypted PDFs are not supported");
    Result result=new Result();result.pages=doc.getNumberOfPages();
    if(result.pages>100)throw new IOException("PDF exceeds 100 pages");
    PDFTextStripper stripper=new PDFTextStripper();
    StringBuilder all=new StringBuilder();
    for(int page=1;page<=result.pages;page++){
     stripper.setStartPage(page);stripper.setEndPage(page);
     String segment=stripper.getText(doc);
     if(all.length()+segment.length()>150000){all.append(segment,0,Math.max(0,150000-all.length()));break;}
     all.append(segment).append('\n');
    }
    result.text=all.toString().trim();
    if(result.text.length()<30)throw new IOException("No extractable text. This PDF may be scanned; OCR is not available yet.");
    JSONObject details=TenderDetails.enrich(new JSONObject().put("title","Imported PDF").put("snippet",result.text));
    result.bidId=details.optString("bidId");result.deadline=details.optString("deadline");result.deadlineStatus=details.optString("deadlineStatus");
    String lower=result.text.toLowerCase(Locale.ROOT);
    String[] terms={"sample cooler","swas cooler","heat exchanger","shell and tube","condenser","oil cooler","tube bundle","retubing","pressure vessel","air receiver","economizer","evaporator","reboiler","tube sheet","cooling coil","steam coil","gland steam","generator air cooler","stator water cooler","hp heater","lp heater"};
    ArrayList<String> matched=new ArrayList<>();for(String term:terms)if(lower.contains(term))matched.add(term);
    result.keywords=String.join(", ",matched);return result;
   }
  }finally{file.delete();}
 }
}
