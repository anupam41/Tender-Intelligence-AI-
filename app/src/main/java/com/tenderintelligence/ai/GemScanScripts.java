package com.tenderintelligence.ai;

import org.json.JSONObject;

/** GeM browser script generation, shared independently of the Activity UI. */
public final class GemScanScripts {
 private GemScanScripts() {}
 public static String search(String keyword){return "(function(){try{var inputs=Array.from(document.querySelectorAll('input')).filter(e=>{var r=e.getBoundingClientRect();return r.width>100&&r.height>0&&!e.disabled});"+
   "var el=inputs.find(e=>/searchBid|search_bid|bid_search/i.test((e.id||'')+' '+(e.name||'')))||inputs.find(e=>{var p=e.parentElement;return p&&/contains/i.test((p.parentElement&&p.parentElement.innerText||'').slice(0,90))})||inputs.filter(e=>/text|search/i.test(e.type||'text')).slice(-1)[0];"+
   "if(!el)return 'NO_INPUT';var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;setter.call(el,"+JSONObject.quote(keyword)+");el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));"+
   "var r=el.getBoundingClientRect();var buttons=Array.from(document.querySelectorAll('button,input[type=submit],a,[role=button]')).filter(e=>{var b=e.getBoundingClientRect();return b.width>18&&b.height>18&&b.left>=r.right-12&&b.left<r.right+145&&Math.abs(b.top-r.top)<38&&!e.disabled});"+
   "buttons.sort((a,b)=>Math.abs(a.getBoundingClientRect().left-r.right)-Math.abs(b.getBoundingClientRect().left-r.right));if(!buttons.length)return 'NO_BUTTON';el.blur();buttons[0].click();return 'OK';}catch(e){return 'ERROR'}})()";}
 public static String nextPage(){return "(function(){var a=Array.from(document.querySelectorAll('a,button')).find(e=>{var r=e.getBoundingClientRect();return r.width>0&&r.height>0&&!e.disabled&&e.getAttribute('aria-disabled')!=='true'&&!e.classList.contains('disabled')&&/^(next|next page|›|»|>)$/i.test((e.innerText||'').trim())});if(!a)return 'END';a.click();return 'NEXT'})()";}
}
