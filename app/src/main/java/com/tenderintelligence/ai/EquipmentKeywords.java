package com.tenderintelligence.ai;
import java.util.*;
public final class EquipmentKeywords {
 public static final String[][] GROUPS={
 {"Heat Exchanger","heat exchanger","heatExchanger"},
 {"Condenser","condenser","vent condenser","AC condenser","surface condenser","gland steam condenser"},
 {"Cooler","cooler","air fin fan cooler","MOT cooler","turbine oil cooler","turbine air cooler","inter stage cooler","inter cooler","after cooler","SWAS cooler","BFP lub oil cooler","working oil cooler","oil cooler","stator water cooler","generator air cooler","stator air cooler","seal oil cooler","gas cooler","guide bearing thrust bearing oil cooler","furnace oil cooler","gland steam cooler","sampling cooler","Voith cooler","vacuum pump water cooler","water cooler","ID FD PA FA fan air oil cooler"},
 {"Chiller","chiller"},{"Radiator","radiator"},{"Dryer","dryer"},
 {"Heater","heater","LP heater","HP heater","air pre heater"},
 {"Economizer","economizer"},{"Ejector","ejector"},{"Evaporator","evaporator"},
 {"U Tube Bundle","U tube bundle","tube bundle"},{"Cooling Coil","cooling coil"},
 {"Tank","tank"},{"Vessels","vessel","vessels"}
 };
 public static final String[] BATCHES={
 "\"heat exchanger\" OR \"HeatExchanger\"",
 "\"vent condenser\" OR \"AC condenser\" OR \"surface condenser\" OR \"gland steam condenser\"",
 "\"air fin fan cooler\" OR \"MOT cooler\" OR \"turbine oil cooler\" OR \"turbine air cooler\"",
 "\"inter stage cooler\" OR \"inter cooler\" OR \"after cooler\" OR \"SWAS cooler\"",
 "\"BFP lub oil cooler\" OR \"working oil cooler\" OR \"oil cooler\"",
 "\"stator water cooler\" OR \"generator air cooler\" OR \"stator air cooler\" OR \"seal oil cooler\"",
 "\"gas cooler\" OR \"guide bearing\" OR \"thrust bearing oil cooler\" OR \"furnace oil cooler\"",
 "\"gland steam cooler\" OR \"sampling cooler\" OR \"Voith cooler\" OR \"vacuum pump water cooler\"",
 "\"water cooler\" OR \"ID FD PA FA fan\"",
 "\"chiller\" OR \"radiator\" OR \"dryer\"",
 "\"LP heater\" OR \"HP heater\" OR \"air pre heater\"",
 "\"economizer\" OR \"ejector\" OR \"evaporator\"",
 "\"U tube bundle\" OR \"cooling coil\"",
 "\"tank\" OR \"vessel\""
 };
 public static String list(){StringBuilder b=new StringBuilder();for(String[] group:GROUPS){b.append(group[0]).append(": ");for(int i=1;i<group.length;i++){if(i>1)b.append(", ");b.append(group[i]);}b.append("\n\n");}return b.toString();}
 public static boolean matches(String text){String t=text.toLowerCase(Locale.ROOT).replace("-"," ").replaceAll("\\s+"," ");for(String[] group:GROUPS)for(int i=1;i<group.length;i++)if(t.contains(group[i].toLowerCase(Locale.ROOT).replace("-"," ")))return true;return false;}
}
