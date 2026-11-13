package com.tenderintelligence.ai;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistent progress checkpoint shared by UI and future service-owned scan engine. */
public final class ScanCheckpoint {
 private static final String PREFS="scan_checkpoint";
 private static final String INDEX="keyword_index";
 private static final String PAGE="last_observed_page";
 private static final String ACTIVE="active";
 private ScanCheckpoint(){}
 private static SharedPreferences prefs(Context context){return context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
 public static void save(Context context,int keywordIndex,int page){
  prefs(context).edit().putBoolean(ACTIVE,true).putInt(INDEX,Math.max(0,keywordIndex)).putInt(PAGE,Math.max(0,page)).apply();
 }
 public static boolean exists(Context context){return prefs(context).getBoolean(ACTIVE,false);}
 public static int keywordIndex(Context context){return prefs(context).getInt(INDEX,0);}
 /** Diagnostic only: page resumption requires restoring the GeM pagination state. */
 public static int lastObservedPage(Context context){return prefs(context).getInt(PAGE,0);}
 public static void clear(Context context){prefs(context).edit().clear().apply();}
}
