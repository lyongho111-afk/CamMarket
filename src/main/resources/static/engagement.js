// One server-backed like and one private likes list.
const engagementState={items:new Map(),pending:new Set(),revision:new Map(),csrf:null,member:null,page:0,type:'ALL',sequence:0};
const engagementTypeNames={SALE:'판매글',POST:'게시글',SHOP:'가게',COMMENT:'댓글'};
const engagementKey=(type,id)=>type+':'+id;
function likeIcon(){const icon=document.createElementNS('http://www.w3.org/2000/svg','svg');icon.setAttribute('viewBox','0 0 24 24');icon.setAttribute('aria-hidden','true');icon.innerHTML='<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z"/>';return icon;}
function engagementControls(type,id,title,image=false){
 const bar=el('div','engagement-controls'+(image?' image-heart':''));bar.dataset.reactionKey=engagementKey(type,id);bar.dataset.title=title;
 const b=el('button','reaction-button like');b.type='button';b.setAttribute('aria-label',title+' 좋아요');b.setAttribute('aria-pressed','false');b.append(likeIcon());
 if(!image)b.append(el('span','like-label','좋아요 …'));
 b.disabled=true;b.onclick=e=>{e.preventDefault();e.stopPropagation();engagementSet(type,id)};bar.append(b);return bar;
}
function paintEngagement(){
 document.querySelectorAll('[data-reaction-key]').forEach(bar=>{const key=bar.dataset.reactionKey,s=engagementState.items.get(key),b=bar.querySelector('.reaction-button');if(!b)return;
  const active=!!s?.liked,label=b.querySelector('.like-label');if(label){const text='좋아요 '+(s?s.likeCount:'…');if(label.textContent!==text)label.textContent=text;}
  b.setAttribute('aria-pressed',String(active));b.disabled=!s||!s.available||engagementState.pending.has(key);const description=bar.dataset.title+' 좋아요'+(s?' '+s.likeCount+'개':'')+(active?' 취소':' 등록');b.setAttribute('aria-label',description);b.title=s&&!s.available?'삭제되었거나 공개되지 않은 콘텐츠':active?'좋아요 취소':'좋아요 등록';
 });
}
async function refreshEngagement(force=false){
 const member=currentSession.member?.id||null;if(engagementState.member!==member){engagementState.member=member;engagementState.csrf=null;engagementState.items.clear();}
 const keys=[...new Set([...document.querySelectorAll('[data-reaction-key]')].map(n=>n.dataset.reactionKey))].filter(key=>!engagementState.pending.has(key)&&(force||!engagementState.items.has(key)||Date.now()-engagementState.items.get(key).fetchedAt>20000));
 paintEngagement();for(let start=0;start<keys.length;start+=100){const batch=keys.slice(start,start+100),versions=new Map(batch.map(k=>[k,engagementState.revision.get(k)||0]));try{const rows=await api('engagement/state?targets='+encodeURIComponent(batch.join(',')));if(member!==(currentSession.member?.id||null))return;for(const row of rows){const key=engagementKey(row.type,row.id);if(versions.get(key)===(engagementState.revision.get(key)||0)&&!engagementState.pending.has(key))engagementState.items.set(key,{...row,fetchedAt:Date.now()});}paintEngagement();}catch(error){toast(error.message);return;}}
}
async function engagementSet(type,id,desired){
 if(!requireLogin())return false;const key=engagementKey(type,id),old=engagementState.items.get(key);if(engagementState.pending.has(key))return false;
 const active=desired??!old?.liked;engagementState.pending.add(key);engagementState.revision.set(key,(engagementState.revision.get(key)||0)+1);paintEngagement();
 try{if(!engagementState.csrf)engagementState.csrf=(await api('engagement/session')).csrfToken;const response=await fetch('/api/engagement/'+type+'/'+id+'/like',{method:'PUT',headers:{'Content-Type':'application/json','X-CSRF-Token':engagementState.csrf},body:JSON.stringify({active})});const row=await response.json();if(!response.ok){if(response.status===401)engagementState.csrf=null;throw Error(row.message||'좋아요를 저장하지 못했어요.');}engagementState.items.set(key,{...row,fetchedAt:Date.now()});toast(active?'좋아요 목록에 담았어요.':'좋아요를 취소했어요.');if(!active&&tab==='likes')queueMicrotask(renderLikes);return true;}catch(error){toast(error.message);return false;}finally{engagementState.pending.delete(key);paintEngagement();}
}
async function openLikedItem(item){
 if(item.type==='POST'||item.type==='COMMENT'){communityOpen(item.parentId);return;}
 if(item.type==='SALE'){if(!products.some(p=>p.id===item.id))await loadData();productDetail(item.id);return;}
 const store=shops.find(s=>s.id===item.id);if(store)shopDetail(store);else toast('공개된 가게 정보를 찾을 수 없어요.');
}
async function renderLikes(){
 document.body.dataset.page='likes';$('.section-head').hidden=false;$('.controls').hidden=true;$('#market-tools').hidden=true;$('#exposure-guide').hidden=true;$('#write').hidden=true;$('#page-return').hidden=false;$('#page-return').onclick=()=>switchTab('market');$('#section-kicker').textContent='MY LIKES';$('#section-title').textContent='내 좋아요';$('#section-sub').textContent='하트를 누른 물건과 캠퍼스 이야기를 한곳에서 확인해요.';
 const root=$('#results'),sequence=++engagementState.sequence,member=currentSession.member?.id;root.className='likes-view';root.replaceChildren();if(!currentSession.authenticated){root.append(el('p','empty','로그인하면 내 좋아요 목록을 볼 수 있어요.'),actionButton('로그인하기',()=>requireLogin()));return;}
 const filters=el('div','interest-filters');for(const [type,name]of Object.entries({ALL:'전체',...engagementTypeNames})){const b=actionButton(name,()=>{engagementState.type=type;engagementState.page=0;renderLikes()},false,type===engagementState.type?'active':'');b.setAttribute('aria-pressed',String(type===engagementState.type));filters.append(b)}root.append(filters,el('p','fine-print','내가 좋아요한 목록은 나만 볼 수 있어요. 하트를 다시 누르면 목록에서 빠져요.'));
 const loading=el('p','empty','좋아요 목록을 불러오는 중…');root.append(loading);try{const data=await api('engagement/likes?page='+engagementState.page+'&type='+engagementState.type);if(sequence!==engagementState.sequence||tab!=='likes'||member!==currentSession.member?.id)return;loading.remove();if(data.total>0&&!data.items.length&&engagementState.page>0){engagementState.page--;return renderLikes();}root.append(el('p','interest-count','좋아요 '+data.total+'개'));
 if(!data.items.length)root.append(el('p','empty','아직 좋아요한 항목이 없어요. 마음에 드는 콘텐츠의 하트를 눌러 보세요.'));
 const list=el('div','interest-list');for(const item of data.items){const product=item.type==='SALE'?products.find(p=>p.id===item.id):null;if(product){list.append(marketCard(product));continue;}
  const card=el('article','interest-card'),open=actionButton('',()=>openLikedItem(item),false,'interest-open');open.append(el('small','eyebrow',engagementTypeNames[item.type]),el('h3','',item.title),el('p','',item.subtitle||''));if(item.price!=null)open.append(el('strong','',formatMoney(item.price)));card.append(open,engagementControls(item.type,item.id,item.title));list.append(card)}root.append(list);
 const pager=el('div','interest-pagination');if(data.page>0)pager.append(actionButton('이전',()=>{engagementState.page--;renderLikes()},false,'secondary'));pager.append(el('span','',(data.page+1)+' / '+Math.max(1,Math.ceil(data.total/data.pageSize))));if((data.page+1)*data.pageSize<data.total)pager.append(actionButton('다음',()=>{engagementState.page++;renderLikes()},false,'secondary'));root.append(pager);refreshEngagement();
 }catch(error){if(sequence!==engagementState.sequence)return;loading.textContent=error.message;root.append(actionButton('다시 불러오기',renderLikes,false,'secondary'));}
}
document.addEventListener('DOMContentLoaded',()=>{
 const nav=document.querySelector('.main-nav');if(!nav)return;const b=actionButton('♡ 좋아요',()=>{engagementState.page=0;switchTab('likes')},false,'');b.dataset.tab='likes';nav.insertBefore(b,nav.lastElementChild);
 let timer;new MutationObserver(()=>{clearTimeout(timer);timer=setTimeout(()=>refreshEngagement(),60)}).observe(document.querySelector('main'),{childList:true,subtree:true});
 refreshEngagement();document.addEventListener('visibilitychange',()=>{if(!document.hidden)refreshEngagement(true)});
});
