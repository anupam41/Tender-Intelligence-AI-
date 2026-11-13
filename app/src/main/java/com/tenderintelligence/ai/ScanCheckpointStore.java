package com.tenderintelligence.ai;

import android.content.Context;
import android.content.SharedPreferences;

/** Checkpoint storage shared by Activity and future scan service. */
public final class ScanCheckpointStore {
 private static final String KEY="scan_resume_index";
 private final SharedPreferences preferences;
 public ScanCheckpointStore(Context context){
  // Activity.getPreferences(0) uses the Activity's local-class-name as the file name.
  preferences=context.getSharedPreferences("GemBrowserActivity",Context.MODE_PRIVATE);
 }
 public boolean hasCheckpoint(){return preferences.contains(KEY);}
 public int load(){return Math.max(0,Math.min(preferences.getInt(KEY,0),ScanPlan.size()-1));}
 public void saveActiveCategory(int nextCategoryIndex){
  preferences.edit().putInt(KEY,Math.max(0,Math.min(nextCategoryIndex-1,ScanPlan.size()-1))).apply();
 }
 public void clear(){preferences.edit().remove(KEY).apply();}
}
