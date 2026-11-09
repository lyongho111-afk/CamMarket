// Original teammate dialog below; adapters connect the real login/CSRF and current page.
(()=>{
 const baseApi=window.api;
 async function api(path,method='GET',body){
  if(path!=='reviews'||method!=='POST')return baseApi(path,method,body);
  const page=await baseApi('trades/'+encodeURIComponent(body.orderId)+'/rating');
  if(!page.rating.canRate)throw Error(page.rating.message);
  const response=await fetch('/api/reviews',{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-Token':page.csrfToken},body:JSON.stringify({...body,reviewerId:actorKey()})});
  const data=await response.json();if(!response.ok)throw Error(data.message||'평가를 저장하지 못했습니다.');return data;
 }
 function renderIntegration(){window.render();}
 const safeText=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function mannerReviewDialog(p) {
    const modal = $('#modal');
    const body = $('#modal-body') || $('#modalBody');
    if (!modal || !body) return;

    const orderId = p.order ? p.order.orderId : '';
    const targetId = p.seller || 'demo-seller';

    body.innerHTML = `
        <div style="padding: 10px 0;">
            <span class="eyebrow" style="color: var(--green, #24694f); font-weight: 700; font-size: 11px; letter-spacing: 1px;">CAMPUS RE:MARKET · MANNER</span>
            <h2 style="font-size: 21px; margin: 8px 0 12px; letter-spacing: -0.5px;">거래 상대방 매너 평가</h2>
            <p style="font-size: 13px; color: #666; margin-bottom: 20px; line-height: 1.6;">
                <b>'${p.title}'</b> 거래는 만족스러우셨나요?<br>
                따뜻한 평가가 더 신뢰받는 캠퍼스 마켓을 만듭니다.
            </p>
            <form id="mannerReviewForm">
                <label style="display: block; font-weight: 700; margin-bottom: 6px; font-size: 12px;">거래 매너 만족도</label>
                <select id="mannerScore" style="width: 100%; padding: 11px; border: 1px solid #dfe6d8; border-radius: 7px; margin-bottom: 14px; font-size: 13px;">
                    <option value="5" selected>⭐⭐⭐⭐⭐ 매우 친절하고 시간 약속을 잘 지켰어요 (+0.5℃)</option>
                    <option value="4">⭐⭐⭐⭐ 친절하고 기분 좋은 거래였어요 (+0.2℃)</option>
                    <option value="3">⭐⭐⭐ 보통이었어요 (온도 변동 없음)</option>
                    <option value="2">⭐⭐ 약속에 늦거나 응답이 느렸어요 (-0.3℃)</option>
                    <option value="1">⭐ 불친절하고 매너가 좋지 않았어요 (-0.8℃)</option>
                </select>

                <label style="display: block; font-weight: 700; margin-bottom: 6px; font-size: 12px;">거래 태도 키워드 (선택)</label>
                <input type="text" id="mannerAttitude" value="시간엄수, 친절매너" 
                       style="width: 100%; padding: 10px; border: 1px solid #dfe6d8; border-radius: 7px; margin-bottom: 14px; font-size: 13px;">

                <label style="display: block; font-weight: 700; margin-bottom: 6px; font-size: 12px;">상세 후기 남기기</label>
                <textarea id="mannerContent" style="width: 100%; min-height: 80px; padding: 10px; border: 1px solid #dfe6d8; border-radius: 7px; margin-bottom: 18px; font-size: 13px;">약속 장소에 시간 맞춰 나와주셨고 상품 상태도 너무 깨끗했습니다!</textarea>

                <button type="submit" class="primary" style="width: 100%; padding: 13px; font-size: 14px; border-radius: 8px;">
                    매너온도 평가 제출하기 ↗
                </button>
            </form>
        </div>
    `;

    if (typeof modal.showModal === 'function') modal.showModal();
    else modal.removeAttribute('hidden');

    const closeBtn = modal.querySelector('.close');
    if (closeBtn) closeBtn.onclick = () => modal.close ? modal.close() : modal.setAttribute('hidden', '');

    const form = body.querySelector('#mannerReviewForm');
    form.onsubmit = async (e) => {
        e.preventDefault();
        const score = parseInt(form.querySelector('#mannerScore').value, 10);
        const attitude = form.querySelector('#mannerAttitude').value;
        const content = form.querySelector('#mannerContent').value;
        const submitBtn = form.querySelector('button[type="submit"]');
        submitBtn.disabled = true;

        try {
            // YS 조원의 api() 공통 함수를 사용해 POST /api/reviews 호출!
            const result = await api('reviews', 'POST', {
                orderId: orderId,
                reviewerId: 'demo-buyer',
                score: score,
                attitude: attitude,
                content: content
            });

            if (modal.close) modal.close();
            else modal.setAttribute('hidden', '');

            const msg = `🎉 상대방의 매너온도가 ${result.currentTemperature}℃가 되었습니다! (${result.appliedDelta >= 0 ? '+' : ''}${result.appliedDelta}℃)`;
            if (typeof toast === 'function') toast(msg);
            else alert(msg);

            // 데이터 리로드 및 화면 재렌더링
            if (typeof loadData === 'function') await loadData();
            renderIntegration();
        } catch (err) {
            submitBtn.disabled = false;
            alert(err.message || '평가 제출 중 오류가 발생했습니다.');
        }
    };
}


 window.renderTradeRating=async function(root,orderId){
  const section=el('section','manner-rating');section.style.cssText='margin-top:20px;padding-top:16px;border-top:1px solid #dce6dd';section.append(el('h3','','거래 상대 매너온도 평가'));root.append(section);
  try{
   const page=await baseApi('trades/'+encodeURIComponent(orderId)+'/rating');if(!section.isConnected)return;const state=page.rating;
   section.append(el('p','fine-print',state.message));if(state.temp!=null)section.append(el('p','fine-print','상대방 현재 매너온도 '+(state.temp/10).toFixed(1)+'°'));
   if(!state.canRate)return;
   section.append(actionButton('⭐ 거래 상대방 매너온도 평가하기',()=>{const product=products.find(p=>p.order?.orderId===orderId);if(!product)return toast('거래 상품 정보를 다시 불러와 주세요.');mannerReviewDialog({...product,title:safeText(product.title)});},false,'primary wide'));
  }catch(error){section.append(el('p','fine-print',error.message));}
 };
})();
