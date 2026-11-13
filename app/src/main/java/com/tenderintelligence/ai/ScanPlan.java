package com.tenderintelligence.ai;

/** Single source of truth for the GeM equipment-category scan plan. */
public final class ScanPlan {
 private ScanPlan() {}
 public static final String[] TERMS={"Heat exchanger","Condenser","Vent condenser","Surface condenser","Gland steam condenser","AC condenser","Air fin fan cooler","MOT cooler","Turbine oil cooler","Turbine air cooler","Inter stage cooler","Inter cooler","After cooler","SWAS cooler","BFP lub oil cooler","Working oil cooler","Oil cooler","Stator water cooler","Generator air cooler","Stator air cooler","Seal oil cooler","Gas cooler","Guide bearing oil cooler","Thrust bearing oil cooler","Furnace oil cooler","Gland steam cooler","Sampling cooler","Voith cooler","Vacuum pump water cooler","Water cooler","Fan air oil cooler","Chiller","Radiator","Dryer","LP heater","HP heater","Air pre heater","Economizer","Ejector","Evaporator","U tube bundle","Cooling coil","Tank","Vessel","Retubing"};
 public static int size(){return TERMS.length;}
}
