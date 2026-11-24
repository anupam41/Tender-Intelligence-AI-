package com.tenderintelligence.ai;

import java.util.*;

public final class EquipmentKeywords {

 public static final String[][] GROUPS = {
  {"Heat Exchanger", "Heat Exchanger", "heat exchanger"},
  {"Condenser", "Condenser", "condenser", "Vent condenser", "vent condenser", "AC condensers", "AC condenser", "ac condenser", "Surface Condenser", "surface condenser", "Gland steam Condenser", "gland steam condenser"},
  {"Cooler", "Cooler", "cooler", "Air-Fin-Fan cooler", "air fin fan cooler", "air-fin-fan cooler", "MOT cooler", "mot cooler", "Turbine oil cooler", "turbine oil cooler", "Turbine air cooler", "turbine air cooler", "Inter Stage Cooler", "inter stage cooler", "Inter Cooler", "inter cooler", "After cooler", "after cooler", "SWAS Cooler", "swas cooler", "BFP Lub oil cooler", "bfp lub oil cooler", "Working oil cooler", "working oil cooler", "Oil cooler", "oil cooler", "Stator water cooler", "stator water cooler", "Generator Air Cooler", "generator air cooler", "Stator air cooler", "stator air cooler", "Seal Oil cooler", "seal oil cooler", "Gas cooler", "gas cooler", "Guide bearing & Thrust bearing oil cooler", "guide bearing", "thrust bearing", "Furnace Oil cooler", "furnace oil cooler", "Gland steam cooler", "gland steam cooler", "Sampling cooler", "sampling cooler", "Voith cooler", "voith cooler", "Vacuum pump water cooler", "vacuum pump water cooler", "Water cooler", "water cooler", "ID-FD-PA-FA Fan Air & Oil cooler", "ID FD PA FA"},
  {"Chiller", "Chiller", "chiller"},
  {"Radiator", "Radiator", "radiator"},
  {"Dryer", "Dryer", "dryer"},
  {"Heater", "Heater", "heater", "LP heater", "lp heater", "HP heater", "hp heater", "Air-pre heater", "air pre heater", "air-pre heater"},
  {"Economizer", "Economizer", "economizer"},
  {"Ejector", "Ejector", "ejector"},
  {"Evaporator", "Evaporator", "evaporator"},
  {"U Tube Bundle", "“U” Tube bundle", "U Tube bundle", "u tube bundle", "tube bundle"},
  {"Cooling Coil", "Cooling coil", "cooling coil"},
  {"Tank", "Tank", "tank"},
  {"Vessels", "Vessels", "vessels", "Vessel", "vessel"}
 };

 public static final String[] ALL_KEYWORDS = {
  "Heat Exchanger",
  "Condenser",
  "Vent condenser",
  "AC condensers",
  "Surface Condenser",
  "Gland steam Condenser",
  "Cooler",
  "Air-Fin-Fan cooler",
  "MOT cooler",
  "Turbine oil cooler",
  "Turbine air cooler",
  "Inter Stage Cooler",
  "Inter Cooler",
  "After cooler",
  "SWAS Cooler",
  "BFP Lub oil cooler",
  "Working oil cooler",
  "Oil cooler",
  "Stator water cooler",
  "Generator Air Cooler",
  "Stator air cooler",
  "Seal Oil cooler",
  "Gas cooler",
  "Guide bearing & Thrust bearing oil cooler",
  "Furnace Oil cooler",
  "Gland steam cooler",
  "Sampling cooler",
  "Voith cooler",
  "Vacuum pump water cooler",
  "Water cooler",
  "ID-FD-PA-FA Fan Air & Oil cooler",
  "Chiller",
  "Radiator",
  "Dryer",
  "Heater",
  "LP heater",
  "HP heater",
  "Air-pre heater",
  "Economizer",
  "Ejector",
  "Evaporator",
  "“U” Tube bundle",
  "Cooling coil",
  "Tank",
  "Vessels"
 };

 public static final String[] SEARCH_QUERIES = {
  "heat exchanger",
  "condenser",
  "cooler",
  "oil cooler",
  "air cooler",
  "water cooler",
  "chiller",
  "radiator",
  "dryer",
  "heater",
  "economizer",
  "ejector",
  "evaporator",
  "tube bundle",
  "cooling coil",
  "tank",
  "vessel"
 };

 public static String[] getActiveQueries() {
  return SEARCH_QUERIES;
 }

 public static String list() {
  StringBuilder b = new StringBuilder();
  b.append("TARGET EQUIPMENT KEYWORDS (").append(ALL_KEYWORDS.length).append(" items):\n\n");
  for (String[] group : GROUPS) {
   b.append("▪ ").append(group[0]).append(":\n");
   Set<String> unique = new LinkedHashSet<>();
   for (int i = 1; i < group.length; i++) {
    String val = group[i].trim();
    if (!val.equalsIgnoreCase(group[0])) {
     unique.add(val);
    }
   }
   if (unique.isEmpty()) {
    b.append("  ").append(group[0]).append("\n\n");
   } else {
    b.append("  ").append(String.join(", ", unique)).append("\n\n");
   }
  }
  return b.toString();
 }

 public static boolean matches(String text) {
  if (text == null || text.isEmpty()) return false;
  String t = text.toLowerCase(Locale.ROOT).replace("-", " ").replaceAll("\\s+", " ");
  for (String[] group : GROUPS) {
   for (int i = 1; i < group.length; i++) {
    String term = group[i].toLowerCase(Locale.ROOT).replace("-", " ").replaceAll("\\s+", " ");
    if (t.contains(term)) return true;
   }
  }
  return false;
 }

 public static ArrayList<String> getMatchedKeywords(String text) {
  ArrayList<String> matched = new ArrayList<>();
  if (text == null || text.isEmpty()) return matched;
  String lower = text.toLowerCase(Locale.ROOT).replace("-", " ").replaceAll("\\s+", " ");
  for (String kw : ALL_KEYWORDS) {
   String norm = kw.toLowerCase(Locale.ROOT).replace("-", " ").replaceAll("“|”|\"", "").replaceAll("\\s+", " ");
   if (lower.contains(norm) && !matched.contains(kw)) {
    matched.add(kw);
   }
  }
  return matched;
 }
}
