// Target Equipment Keywords - Tender Intelligence AI

const GROUPS = [
  {
    name: "Heat Exchanger",
    terms: ["Heat Exchanger", "heat exchanger"]
  },
  {
    name: "Condenser",
    terms: ["Condenser", "condenser", "Vent condenser", "vent condenser", "AC condensers", "AC condenser", "Surface Condenser", "surface condenser", "Gland steam Condenser", "gland steam condenser"]
  },
  {
    name: "Cooler",
    terms: [
      "Cooler", "cooler", "Air-Fin-Fan cooler", "air fin fan cooler", "air-fin-fan cooler",
      "MOT cooler", "mot cooler", "Turbine oil cooler", "turbine oil cooler", "Turbine air cooler", "turbine air cooler",
      "Inter Stage Cooler", "inter stage cooler", "Inter Cooler", "inter cooler", "After cooler", "after cooler",
      "SWAS Cooler", "swas cooler", "BFP Lub oil cooler", "bfp lub oil cooler", "Working oil cooler", "working oil cooler",
      "Oil cooler", "oil cooler", "Stator water cooler", "stator water cooler", "Generator Air Cooler", "generator air cooler",
      "Stator air cooler", "stator air cooler", "Seal Oil cooler", "seal oil cooler", "Gas cooler", "gas cooler",
      "Guide bearing & Thrust bearing oil cooler", "guide bearing", "thrust bearing", "Furnace Oil cooler", "furnace oil cooler",
      "Gland steam cooler", "gland steam cooler", "Sampling cooler", "sampling cooler", "Voith cooler", "voith cooler",
      "Vacuum pump water cooler", "vacuum pump water cooler", "Water cooler", "water cooler",
      "ID-FD-PA-FA Fan Air & Oil cooler", "ID FD PA FA"
    ]
  },
  {
    name: "Chiller",
    terms: ["Chiller", "chiller"]
  },
  {
    name: "Radiator",
    terms: ["Radiator", "radiator"]
  },
  {
    name: "Dryer",
    terms: ["Dryer", "dryer"]
  },
  {
    name: "Heater",
    terms: ["Heater", "heater", "LP heater", "lp heater", "HP heater", "hp heater", "Air-pre heater", "air pre heater", "air-pre heater"]
  },
  {
    name: "Economizer",
    terms: ["Economizer", "economizer"]
  },
  {
    name: "Ejector",
    terms: ["Ejector", "ejector"]
  },
  {
    name: "Evaporator",
    terms: ["Evaporator", "evaporator"]
  },
  {
    name: "U Tube Bundle",
    terms: ["“U” Tube bundle", "U Tube bundle", "u tube bundle", "tube bundle"]
  },
  {
    name: "Cooling Coil",
    terms: ["Cooling coil", "cooling coil"]
  },
  {
    name: "Tank",
    terms: ["Tank", "tank"]
  },
  {
    name: "Vessels",
    terms: ["Vessels", "vessels", "Vessel", "vessel"]
  }
];

const ALL_KEYWORDS = [
  "Heat Exchanger",
  "Condenser", "Vent condenser", "AC condensers", "Surface Condenser", "Gland steam Condenser",
  "Cooler", "Air-Fin-Fan cooler", "MOT cooler", "Turbine oil cooler", "Turbine air cooler",
  "Inter Stage Cooler", "Inter Cooler", "After cooler", "SWAS Cooler", "BFP Lub oil cooler",
  "Working oil cooler", "Oil cooler", "Stator water cooler", "Generator Air Cooler",
  "Stator air cooler", "Seal Oil cooler", "Gas cooler", "Guide bearing & Thrust bearing oil cooler",
  "Furnace Oil cooler", "Gland steam cooler", "Sampling cooler", "Voith cooler",
  "Vacuum pump water cooler", "Water cooler", "ID-FD-PA-FA Fan Air & Oil cooler",
  "Chiller", "Radiator", "Dryer",
  "Heater", "LP heater", "HP heater", "Air-pre heater",
  "Economizer", "Ejector", "Evaporator",
  "“U” Tube bundle", "Cooling coil", "Tank", "Vessels"
];

const SEARCH_QUERIES = [
  "heat exchanger", "condenser", "cooler", "oil cooler", "air cooler",
  "water cooler", "chiller", "radiator", "dryer", "heater",
  "economizer", "ejector", "evaporator", "tube bundle", "cooling coil",
  "tank", "vessel"
];

function normalize(text) {
  if (text === null || text === undefined) return "";
  let str = (typeof text === 'string') ? text : (Array.isArray(text) ? text.join(' ') : String(text));
  return str.toLowerCase().replace(/[-_]/g, " ").replace(/[“”"]/g, "").replace(/\s+/g, " ").trim();
}

function matches(text) {
  if (!text) return false;
  const t = normalize(text);
  for (const group of GROUPS) {
    for (const term of group.terms) {
      if (t.includes(normalize(term))) return true;
    }
  }
  return false;
}

function getMatchedKeywords(text) {
  if (!text) return [];
  const t = normalize(text);
  const matched = [];
  for (const kw of ALL_KEYWORDS) {
    if (t.includes(normalize(kw)) && !matched.includes(kw)) {
      matched.push(kw);
    }
  }
  return matched;
}

function calculateScore(text) {
  const matched = getMatchedKeywords(text);
  return Math.min(100, matched.length * 25);
}

module.exports = {
  GROUPS,
  ALL_KEYWORDS,
  SEARCH_QUERIES,
  matches,
  getMatchedKeywords,
  calculateScore
};
