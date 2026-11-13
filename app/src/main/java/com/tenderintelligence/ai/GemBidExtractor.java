package com.tenderintelligence.ai;

/** GeM public listing DOM extraction; independent of Activity UI. */
public final class GemBidExtractor {
 private GemBidExtractor() {}
 public static String listingScript(){return "(function(){try{var found=[],seen={};"+
   "var ids=Array.from(document.querySelectorAll('a,span,strong,b,div')).filter(e=>e.children.length<3&&/GEM\\/20\\d{2}\\/B\\/\\d+/i.test(e.textContent||''));"+
   "for(var node of ids){var match=(node.textContent||'').match(/GEM\\/20\\d{2}\\/B\\/\\d+/i);if(!match||seen[match[0]])continue;"+
   "var card=node;for(var n=0;n<7&&card.parentElement;n++){var t=card.innerText||'';if(/End Date\\s*:/i.test(t)&&/Items?\\s*:/i.test(t)&&t.length<3500)break;card=card.parentElement;}"+
   "var text=card.innerText||'';if(text.length>6000)continue;"+
   "var item=text.match(/Items?\\s*:\\s*([^\\n]+)/i),end=text.match(/End Date\\s*:\\s*(\\d{2}-\\d{2}-\\d{4}(?:\\s+\\d{1,2}:\\d{2}\\s*(?:AM|PM))?)/i);"+
   "var anchor=node.closest('a[href]')||Array.from(card.querySelectorAll('a[href]')).find(a=>(a.textContent||'').includes(match[0]));"+
   "var buyerMatch=text.match(/Department Name And Address\\s*:\\s*([^\\n]+)(?:\\n([^\\n]+))?/i);var buyer=buyerMatch?((buyerMatch[2]||buyerMatch[1]||'').trim()):'';"+
   "var docs=Array.from(card.querySelectorAll('a[href]')).filter(a=>/\\.pdf(?:[?#]|$)|showbidDocument\\/\\d+|download.*(bid|document|specification|atc)/i.test(a.href||'')).map(a=>a.href).filter(u=>/^https:\\/\\/([a-z0-9-]+\\.)*gem\\.gov\\.in(?:[:\\/]|$)/i.test(u)).slice(0,3);"+
   "seen[match[0]]=true;found.push({bidId:match[0],title:item?item[1].trim():'GeM bid',deadlineText:end?end[1].trim():'',buyer:buyer,documentLinks:docs,url:anchor?anchor.href:'',source:'GeM visible listing'});if(found.length>=100)break;}"+
   "return JSON.stringify({items:found,matchedNodes:ids.length});}catch(e){return JSON.stringify({error:e.message})}})()";}
}
