// 평가 대상과 평가자 식별자는 클라이언트 입력을 받지 않고 서버가 결정합니다.
async function renderTradeRating(root,orderId){
 const section=el('section','manner-rating');section.style.cssText='margin-top:20px;padding-top:16px;border-top:1px solid #dce6dd';
 section.append(el('h3','','거래 상대 매너온도 평가'));root.append(section);
 try{
  const page=await api('trades/'+encodeURIComponent(orderId)+'/rating');if(!section.isConnected)return;
  const state=page.rating;
  if(page.demo)section.append(el('p','fine-print','demo-buyer 시연 계정으로 평가합니다. 실제 로그인 인증은 아직 연결 전입니다.'));
  section.append(el('p','fine-print',state.message));
  if(state.temp!=null)section.append(el('p','fine-print','상대방 현재 매너온도 '+(state.temp/10).toFixed(1)+'°'));
  if(!state.canRate)return;
  const form=el('form'),select=el('select');select.setAttribute('aria-label','매너온도 평가');
  select.append(new Option('좋아요','GOOD'),new Option('아쉬워요','BAD'));const submit=el('button','primary wide','평가 저장');submit.type='submit';form.append(select,submit);
  form.onsubmit=async event=>{
   event.preventDefault();submit.disabled=true;
   try{const r=await fetch('/api/trades/'+encodeURIComponent(orderId)+'/rating',{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-Token':page.csrfToken},body:JSON.stringify({feedback:select.value})});const data=await r.json();if(!r.ok)throw Error(data.message||'평가를 저장하지 못했습니다.');form.remove();section.append(el('p','locked-price','평가 완료 · 상대방 '+(data.rating.temp/10).toFixed(1)+'°'));await loadData();toast('매너온도 평가를 저장했어요.');}
   catch(error){toast(error.message);submit.disabled=false;}
  };
  section.append(form);
 }catch(error){if(section.isConnected)section.append(el('p','fine-print',error.message));}
}
