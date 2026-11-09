// 지도에는 공개 승인된 앱 등록 업체만 표시합니다.
let registeredShopMap=null;
const shopStateColors={'영업중':'#238657','혼잡':'#cf8218','영업종료':'#73808b','휴무':'#ba4650'};
function renderShops(){
 if(registeredShopMap){registeredShopMap.remove();registeredShopMap=null;}
 $('#section-kicker').textContent='AROUND CAMPUS';$('#section-title').textContent='학교 주변, 지금 열려 있는 곳';
 $('#section-sub').textContent='캠퍼스 리마켓에 등록된 추천 업체를 지도에서 만나보세요.';
 $('.controls').hidden=true;$('#write').hidden=true;
 const root=$('#results');root.className='shops-view';root.replaceChildren();
 const layout=el('div','shops-layout'),mapPanel=el('section','shops-panel'),listPanel=el('section','shops-panel');
 mapPanel.append(el('span','eyebrow','REGISTERED SHOPS'),el('h3','','캠퍼스 주변 지도'),el('p','shops-description','마커를 누르면 가게 정보와 현재 운영 상태를 볼 수 있어요.'));
 const legend=el('div','shop-map-legend');for(const [status,color]of Object.entries(shopStateColors)){const entry=el('span','',status),dot=el('i');dot.style.background=color;entry.prepend(dot);legend.append(entry)}
 const mapNode=el('div','registered-shop-map');mapNode.setAttribute('aria-label','등록 추천 업체 지도');mapPanel.append(campusInformation(),legend,mapNode);
 listPanel.append(el('span','eyebrow','CAMPUS RECOMMENDS'),el('h3','','앱 등록 추천 업체'));
 const q=$('#search').value.trim().toLowerCase(),visible=shops.filter(s=>(s.title+' '+(s.category||'')+' '+(s.menu||'')+' '+(s.address||'')).toLowerCase().includes(q));
 const list=el('div','campus-store-list');listPanel.append(list);layout.append(mapPanel,listPanel);root.append(layout);
 if(!loaded)list.append(el('p','shops-empty','등록된 가게를 불러오고 있어요…'));
 else if(!visible.length)list.append(el('p','shops-empty',q?'검색 결과가 없어요.':'아직 공개된 등록 업체가 없어요.'));
 if(window.L){
  const map=registeredShopMap=L.map(mapNode,{scrollWheelZoom:false}).setView(campusCenter(),16);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'}).addTo(map);
  const positions=[];
  for(const store of visible){
   if(store.latitude==null||store.longitude==null)continue;
   const position=[Number(store.latitude),Number(store.longitude)];positions.push(position);
   const box=el('div','shop-map-popup');box.append(el('strong','',store.title),el('p','',store.status+' · '+(store.hours||'영업시간 문의')),el('p','',store.address||''),actionButton('이용 정보 보기',()=>shopDetail(store),false,'secondary'));
   const marker=L.circleMarker(position,{radius:12,color:'#fff',weight:3,fillColor:shopStateColors[store.status]||'#73808b',fillOpacity:1}).addTo(map).bindPopup(box);
   marker.bindTooltip(store.title+' · '+store.status,{direction:'top'});store._mapMarker=marker;
  }
  if(positions.length)map.fitBounds(L.latLngBounds([campusCenter(),...positions]),{padding:[28,28],maxZoom:16});
  else map.setView(campusCenter(),16);
  requestAnimationFrame(()=>{if(mapNode.isConnected)map.invalidateSize()});
 }else mapNode.append(el('p','shops-empty','지도를 불러오지 못했습니다. 업체 목록에서 이용 정보를 확인해 주세요.'));
 let unlocated=0;
 for(const store of visible){
  const card=el('article','campus-store'),open=el('button','campus-store-open'),info=el('div','campus-store-info'),status=el('span','store-status',store.status);
  status.style.color=shopStateColors[store.status];status.style.background=(shopStateColors[store.status]||'#73808b')+'18';
  info.append(el('strong','campus-store-name',store.title),el('span','campus-store-hours',store.hours||'영업시간 문의'));
  open.append(shopPhoto(store),info,status);open.onclick=()=>shopDetail(store);card.append(open);
  if(store.event?.startsWith('운영 여부 확인 필요'))card.append(el('p','campus-store-menu',store.event));
  if(store.address)card.append(el('p','campus-store-menu',store.address));if(store.menu)card.append(el('p','campus-store-menu',store.menu));if(store.space)card.append(el('p','campus-store-space','여유 공간 · '+store.space));
  const actions=el('div','campus-store-actions');
  if(store.latitude!=null&&store.longitude!=null){const mapButton=actionButton('지도에서 보기',()=>{const m=store._mapMarker;if(registeredShopMap&&m){registeredShopMap.setView(m.getLatLng(),17);m.openPopup();mapNode.scrollIntoView({behavior:'smooth',block:'center'})}},false,'shop-map-button');const pin=document.createElementNS('http://www.w3.org/2000/svg','svg');pin.setAttribute('viewBox','0 0 24 24');pin.setAttribute('aria-hidden','true');pin.innerHTML='<path d="M20 10c0 6-8 11-8 11S4 16 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/>';mapButton.prepend(pin);actions.append(mapButton)}
  else{unlocated++;card.append(el('p','fine-print','위치 등록 준비 중 · 목록에서 정보 확인 가능'))}actions.append(engagementControls('SHOP',store.id,store.title));card.append(actions);list.append(card);
 }
 if(unlocated)mapPanel.append(el('p','fine-print','좌표가 없는 '+unlocated+'개 업체는 목록에만 표시됩니다.'));
 mapPanel.append(el('p','fine-print','업체가 직접 입력한 운영 상태입니다. 방문 전 확인해 주세요.'));
}

function shopPhoto(s){if(!s.photo)return el('span','campus-store-icon',s.category?.includes('카페')?'☕':'🍽');const i=el('img','store-real-photo');i.src='/api/shops/photo/'+encodeURIComponent(s.photo);i.alt=s.title+' 가게 사진';i.onerror=()=>{const placeholder=el('span','campus-store-icon','사진 없음');i.replaceWith(placeholder)};return i;}

// Refresh only changed shop data and retain the map position while this page is open.
let shopStatusRefreshing=false;
async function refreshShopStatuses(){
 if(shopStatusRefreshing||tab!=='life'||document.hidden||!loaded)return;
 shopStatusRefreshing=true;
 try{const next=await api('shops');const previous=shops.map(({_mapMarker,...s})=>s);
  if(JSON.stringify(previous)!==JSON.stringify(next)&&tab==='life'){
   const view=registeredShopMap?{center:registeredShopMap.getCenter(),zoom:registeredShopMap.getZoom()}:null;
   shops=next;renderShops();if(view&&registeredShopMap)registeredShopMap.setView(view.center,view.zoom);
  }
 }catch{/* Keep the last loaded shops if the connection is temporarily unavailable. */}
 finally{shopStatusRefreshing=false;}
}
document.addEventListener('DOMContentLoaded',()=>{setInterval(refreshShopStatuses,15000);document.addEventListener('visibilitychange',()=>{if(!document.hidden)refreshShopStatuses()})});
