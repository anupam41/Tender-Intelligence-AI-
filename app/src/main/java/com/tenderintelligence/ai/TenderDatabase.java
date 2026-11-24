package com.tenderintelligence.ai;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public final class TenderDatabase extends SQLiteOpenHelper {
 private static final String DB_NAME = "tender_intelligence.db";
 private static final int DB_VERSION = 1;

 public static final String TABLE_TENDERS = "saved_tenders";
 public static final String COL_ID = "_id";
 public static final String COL_BID_ID = "bid_id";
 public static final String COL_TITLE = "title";
 public static final String COL_DEADLINE = "deadline";
 public static final String COL_SCORE = "score";
 public static final String COL_VERDICT = "verdict";
 public static final String COL_TEXT = "body_text";
 public static final String COL_CREATED_AT = "created_at";

 private static TenderDatabase instance;

 public static synchronized TenderDatabase getInstance(Context context) {
  if (instance == null) {
   instance = new TenderDatabase(context.getApplicationContext());
  }
  return instance;
 }

 private TenderDatabase(Context context) {
  super(context, DB_NAME, null, DB_VERSION);
 }

 @Override
 public void onCreate(SQLiteDatabase db) {
  db.execSQL("CREATE TABLE " + TABLE_TENDERS + " ("
   + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
   + COL_BID_ID + " TEXT UNIQUE, "
   + COL_TITLE + " TEXT NOT NULL, "
   + COL_DEADLINE + " TEXT, "
   + COL_SCORE + " INTEGER DEFAULT 0, "
   + COL_VERDICT + " TEXT, "
   + COL_TEXT + " TEXT, "
   + COL_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
   + ");");
 }

 @Override
 public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  db.execSQL("DROP TABLE IF EXISTS " + TABLE_TENDERS);
  onCreate(db);
 }

 public long insertOrUpdate(String bidId, String title, String deadline, int score, String verdict, String body) {
  SQLiteDatabase db = getWritableDatabase();
  ContentValues cv = new ContentValues();
  cv.put(COL_BID_ID, (bidId != null && !bidId.isEmpty()) ? bidId : "MANUAL_" + System.currentTimeMillis());
  cv.put(COL_TITLE, title);
  cv.put(COL_DEADLINE, deadline);
  cv.put(COL_SCORE, score);
  cv.put(COL_VERDICT, verdict);
  cv.put(COL_TEXT, body);

  return db.insertWithOnConflict(TABLE_TENDERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
 }

 public ArrayList<JSONObject> getAllTenders() {
  ArrayList<JSONObject> list = new ArrayList<>();
  SQLiteDatabase db = getReadableDatabase();
  try (Cursor c = db.query(TABLE_TENDERS, null, null, null, null, null, COL_ID + " DESC")) {
   while (c.moveToNext()) {
    JSONObject j = new JSONObject();
    j.put("id", c.getLong(c.getColumnIndexOrThrow(COL_ID)));
    j.put("bidId", c.getString(c.getColumnIndexOrThrow(COL_BID_ID)));
    j.put("title", c.getString(c.getColumnIndexOrThrow(COL_TITLE)));
    j.put("deadline", c.getString(c.getColumnIndexOrThrow(COL_DEADLINE)));
    j.put("score", c.getInt(c.getColumnIndexOrThrow(COL_SCORE)));
    j.put("verdict", c.getString(c.getColumnIndexOrThrow(COL_VERDICT)));
    j.put("text", c.getString(c.getColumnIndexOrThrow(COL_TEXT)));
    list.add(j);
   }
  } catch (Exception ignored) {}
  return list;
 }

 public boolean deleteTender(long id) {
  SQLiteDatabase db = getWritableDatabase();
  return db.delete(TABLE_TENDERS, COL_ID + "=?", new String[]{String.valueOf(id)}) > 0;
 }

 public ArrayList<JSONObject> getUpcomingDeadlines(int daysAhead) {
  ArrayList<JSONObject> list = new ArrayList<>();
  SQLiteDatabase db = getReadableDatabase();
  String query = "SELECT * FROM " + TABLE_TENDERS 
   + " WHERE deadline IS NOT NULL AND deadline != '' "
   + " AND date(deadline) >= date('now') "
   + " AND date(deadline) <= date('now', '+" + daysAhead + " days') "
   + " ORDER BY date(deadline) ASC";

  try (Cursor c = db.rawQuery(query, null)) {
   while (c.moveToNext()) {
    JSONObject j = new JSONObject();
    j.put("id", c.getLong(c.getColumnIndexOrThrow(COL_ID)));
    j.put("title", c.getString(c.getColumnIndexOrThrow(COL_TITLE)));
    j.put("deadline", c.getString(c.getColumnIndexOrThrow(COL_DEADLINE)));
    j.put("verdict", c.getString(c.getColumnIndexOrThrow(COL_VERDICT)));
    list.add(j);
   }
  } catch (Exception ignored) {}
  return list;
 }
}
