package com.tenderintelligence.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
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
  public boolean isOcr = false;
 }

 public static Result read(Context context, Uri uri) throws Exception {
  PDFBoxResourceLoader.init(context.getApplicationContext());
  File file = File.createTempFile("tender_", ".pdf", context.getCacheDir());
  try {
   try (InputStream in = context.getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(file)) {
    if (in == null) throw new IOException("Cannot open PDF");
    byte[] buf = new byte[8192];
    int n;
    long total = 0;
    while ((n = in.read(buf)) != -1) {
     total += n;
     if (total > 20 * 1024 * 1024) throw new IOException("PDF exceeds 20 MB limit");
     out.write(buf, 0, n);
    }
   }
   try (PDDocument doc = PDDocument.load(file)) {
    if (doc.isEncrypted()) throw new IOException("Encrypted PDFs are not supported");
    Result result = new Result();
    result.pages = doc.getNumberOfPages();
    if (result.pages > 150) throw new IOException("PDF exceeds 150 pages");

    // Attempt 1: Fast direct text extraction with PDFBox
    PDFTextStripper stripper = new PDFTextStripper();
    StringBuilder all = new StringBuilder();
    for (int page = 1; page <= result.pages; page++) {
     stripper.setStartPage(page);
     stripper.setEndPage(page);
     String segment = stripper.getText(doc);
     if (all.length() + segment.length() > 150000) {
      all.append(segment, 0, Math.max(0, 150000 - all.length()));
      break;
     }
     all.append(segment).append('\n');
    }
    result.text = all.toString().trim();

    // Attempt 2: Fallback to ML Kit On-Device OCR for scanned / image PDFs
    if (result.text.length() < 30) {
     result.text = extractOcr(file, Math.min(result.pages, 15));
     result.isOcr = true;
     if (result.text.length() < 30) {
      throw new IOException("No extractable text found. Document may be blank or too blurry for OCR.");
     }
    }

    JSONObject details = TenderDetails.enrich(new JSONObject().put("title", "Imported PDF").put("snippet", result.text));
    result.bidId = details.optString("bidId");
    result.deadline = details.optString("deadline");
    result.deadlineStatus = details.optString("deadlineStatus");

    ArrayList<String> matched = EquipmentKeywords.getMatchedKeywords(result.text);
    result.keywords = String.join(", ", matched);
    return result;
   }
  } finally {
   file.delete();
  }
 }

 private static String extractOcr(File file, int maxPages) throws Exception {
  try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
   PdfRenderer renderer = new PdfRenderer(pfd);
   int pagesToScan = Math.min(renderer.getPageCount(), maxPages);
   TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
   StringBuilder sb = new StringBuilder();
   try {
    for (int i = 0; i < pagesToScan; i++) {
     PdfRenderer.Page page = renderer.openPage(i);
     // Render at 2x resolution (capped at 2048px) for optimal OCR accuracy
     int width = Math.min(page.getWidth() * 2, 2048);
     int height = Math.min(page.getHeight() * 2, 2048);
     Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
     page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
     page.close();

     InputImage image = InputImage.fromBitmap(bitmap, 0);
     com.google.mlkit.vision.text.Text visionText = Tasks.await(recognizer.process(image));
     sb.append(visionText.getText()).append("\n");
     bitmap.recycle();
     if (sb.length() > 150000) break;
    }
   } finally {
    recognizer.close();
    renderer.close();
   }
   return sb.toString().trim();
  }
 }
}
