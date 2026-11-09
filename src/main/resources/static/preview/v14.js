// 세 장의 캠퍼스 광고: 자동 전환, 직접 선택, 이전·다음, 일시 정지.
(()=>{
 const hero=document.querySelector('.hero'),copy=hero?.firstElementChild;if(!hero||!copy)return;
 copy.classList.add('ad-copy');hero.setAttribute('aria-roledescription','캐러셀');hero.setAttribute('aria-label','캠퍼스 소식 광고 배너');
 const slides=[{tag:'BACK TO CAMPUS · RE:START',title:'필요했던 그 물건,\n같은 캠퍼스에 있어요.',body:'전공책부터 실습 준비물까지.\n잘 쓰던 물건에 다음 학기를 선물하세요.',button:'우리 학교 물건 둘러보기　↗',tab:'market',theme:''},{tag:'CAMPUS ESSENTIALS · RE:USE',title:'실습의 시작을,\n조금 더 가볍게.',body:'조리복 · 제도 도구 · 노트북.\n선배가 아끼던 준비물을 만나보세요.',button:'실습 준비물 찾아보기　↗',tab:'market',theme:'ad-campus'},{tag:'AROUND CAMPUS · TOGETHER',title:'공강 한 시간도,\n학교 주변과 함께.',body:'지금 열린 가게와 학생 혜택.\n캠퍼스 주변의 새로운 곳을 발견하세요.',button:'학교 주변 업체 보기　↗',tab:'life',theme:'ad-life'}];
 let index=0,paused=false,timer;const controls=el('div','ad-controls');
 const prev=actionButton('‹',()=>{show(index-1);restart()},false,''),next=actionButton('›',()=>{show(index+1);restart()},false,'');prev.setAttribute('aria-label','이전 광고');next.setAttribute('aria-label','다음 광고');controls.append(prev);
 const dots=slides.map((_,i)=>{const b=actionButton('',()=>{show(i);restart()},false,'ad-dot');b.setAttribute('aria-label',(i+1)+'번 광고 보기');controls.append(b);return b});
 const count=el('span','fine-print'),pause=actionButton('일시 정지',()=>{paused=!paused;pause.textContent=paused?'자동 재생':'일시 정지';pause.setAttribute('aria-pressed',String(paused));restart()},false,'ad-pause');controls.append(next,count,pause);hero.append(controls);
 function show(n){index=(n+slides.length)%slides.length;const s=slides[index];hero.classList.remove('ad-campus','ad-life','ad-school','ad-food');if(s.theme)hero.classList.add(s.theme);copy.querySelector('.eyebrow').textContent=s.tag;copy.querySelector('h1').replaceChildren(...s.title.split('\n').flatMap((t,i)=>i?[document.createElement('br'),document.createTextNode(t)]:[document.createTextNode(t)]));copy.querySelector('p').textContent=s.body;copy.querySelector('p').style.whiteSpace='pre-line';const c=copy.querySelector('button');c.textContent=s.button;c.onclick=()=>switchTab(s.tab);dots.forEach((b,i)=>{b.classList.toggle('active',i===index);b.setAttribute('aria-current',String(i===index))});count.textContent=(index+1)+' / '+slides.length;showSchoolFrame(s);showFoodFrame(s);showDeco(s)}
 function restart(){clearInterval(timer);if(!paused&&!document.hidden)timer=setInterval(()=>show(index+1),6000)}
 hero.addEventListener('mouseenter',()=>clearInterval(timer));hero.addEventListener('mouseleave',restart);hero.addEventListener('focusin',()=>clearInterval(timer));hero.addEventListener('focusout',restart);document.addEventListener('visibilitychange',restart);
 // ===== 마지막 4번째 슬라이드: 로그인한 학생의 학교 소개 (왼쪽 글 + 오른쪽 학교 사진 액자) =====
 // 그림 위치: static/school-photos/학교이름.jpg, 학교이름-약도.jpg (약도가 있으면 "캠퍼스 약도 보기" 버튼)
 const schoolStyle=document.createElement('style');
 schoolStyle.textContent='.hero.ad-school{background:#e6eee2}.hero.ad-school .hero-art,.hero.ad-food .hero-art{visibility:hidden}.hero.ad-food{background:#f7ebdd}.ad-food-frame{position:absolute;right:48px;top:24px;bottom:24px;width:min(46%,470px);display:flex;gap:12px;z-index:2}.ad-food-frame button{flex:1;min-width:0;padding:0;border:5px solid #fff;border-radius:16px;overflow:hidden;background:#fff;box-shadow:0 10px 26px #4a2e1a22;cursor:zoom-in;transition:transform .2s}.ad-food-frame button:hover{transform:translateY(-4px)}.ad-food-frame img{width:100%;height:100%;display:block;object-fit:cover}@media(max-width:800px){.ad-food-frame{display:none}}.ad-deco{position:absolute;inset:0;pointer-events:none;z-index:3;overflow:hidden}.ad-deco[hidden]{display:none!important}.ad-orbit{position:absolute;left:-110px;top:-150px;width:300px;height:300px;border:2px dashed #24694f55;border-radius:50%}.ad-orbit:after{content:\"\";position:absolute;left:38px;top:30px;width:16px;height:16px;border-radius:50%;background:#f0a24a}.ad-star{position:absolute;font-size:26px;line-height:1;color:#24694f66}.ad-star.s1{left:46%;top:16px}.ad-star.s2{left:41%;bottom:34px;font-size:18px;color:#e08a2e99}.ad-sticker{position:absolute;right:26px;top:16px;transform:rotate(6deg);background:#ffd96b;color:#4a3410;font-weight:800;font-size:14px;padding:8px 14px;border-radius:10px;box-shadow:0 4px 10px #4a341022;z-index:3;white-space:nowrap}.ad-chips{position:absolute;right:48px;bottom:46px;display:flex;flex-direction:column;align-items:flex-end;gap:8px;z-index:3}.ad-chips span{background:#ffffffd9;border:1px solid #cfdcd2;border-radius:18px;padding:5px 12px;font-size:12.5px;font-weight:700;color:#244e3d}@media(max-width:800px){.ad-deco{display:none}}.ad-school-frame{position:absolute;right:48px;top:24px;bottom:24px;aspect-ratio:4/3;border:6px solid #fff;border-radius:18px;overflow:hidden;background:#fff;box-shadow:0 12px 30px #1e39201f;z-index:2}.ad-school-frame img{width:100%;height:100%;display:block;object-fit:cover}.ad-school-frame img.is-map{object-fit:contain;cursor:zoom-in}.ad-school-toggle{position:absolute;right:8px;bottom:8px;white-space:nowrap;background:#ffffffeb;border:1px solid #b3c3b5;border-radius:18px;padding:6px 12px;font-size:12px;font-weight:700;color:#244e3d;cursor:pointer}@media(max-width:800px){.ad-school-frame{display:none}}';
 document.head.append(schoolStyle);
 let schoolFrame=null,schoolImg,schoolToggle,showingMap=false;
 function renderSchoolImage(s){const useMap=showingMap&&s.map;schoolImg.src=useMap?s.map:s.photo;schoolImg.classList.toggle('is-map',!!useMap);schoolImg.alt=s.school+(useMap?' 캠퍼스 약도':' 사진');schoolToggle.hidden=!s.map;schoolToggle.textContent=useMap?'학교 사진 보기':'캠퍼스 약도 보기'}
 function showSchoolFrame(s){                         // 학교 슬라이드면 액자를 보이고, 아니면 숨김
  if(!s.school){if(schoolFrame)schoolFrame.hidden=true;return}
  if(!schoolFrame){
   schoolFrame=document.createElement('div');schoolFrame.className='ad-school-frame';
   schoolImg=document.createElement('img');schoolToggle=document.createElement('button');schoolToggle.type='button';schoolToggle.className='ad-school-toggle';
   schoolToggle.onclick=()=>{showingMap=!showingMap;renderSchoolImage(slides[index])};
   schoolImg.onclick=()=>{if(schoolImg.classList.contains('is-map'))window.open(schoolImg.src,'_blank','noopener')}; // 약도를 누르면 크게 보기
   schoolFrame.append(schoolImg,schoolToggle);hero.append(schoolFrame);
  }
  schoolFrame.hidden=false;showingMap=false;renderSchoolImage(s);
 }
 function imageExists(url){return new Promise(r=>{const i=new Image();i.onload=()=>r(true);i.onerror=()=>r(false);i.src=url})}
 async function addSchoolSlide(){
  for(let t=0;t<25&&(!currentSession.authenticated||!currentCampus.id);t++)await new Promise(r=>setTimeout(r,300)); // 로그인 정보가 올 때까지 잠깐 기다림(최대 약 7초)
  const m=currentSession.authenticated&&currentSession.member;if(!m||!m.schoolName)return; // 학교가 없는 계정(사장·관리자)·비로그인은 슬라이드 추가 안 함
  const photo=currentCampus.photo?'/api/school-images/'+encodeURIComponent(currentCampus.photo):'/school-photos/'+encodeURIComponent(m.schoolName)+'.jpg',map=currentCampus.mapImage?'/api/school-images/'+encodeURIComponent(currentCampus.mapImage):'/school-photos/'+encodeURIComponent(m.schoolName+'-약도')+'.jpg';
  const [hasPhoto,hasMap]=await Promise.all([imageExists(photo),imageExists(map)]);
  if(!hasPhoto&&!hasMap)return;
  slides.push({tag:'OUR CAMPUS · '+m.schoolName,title:'우리 학교,\n'+m.schoolName+'.',body:'우리 학교 이웃들이 올린 물건을\n가장 먼저 만나보세요.',button:'우리 학교 물건 둘러보기　↗',tab:'market',theme:'ad-school',school:m.schoolName,photo:hasPhoto?photo:map,map:hasPhoto&&hasMap?map:''}); // 사진이 없으면 약도를 대신 사용
  const i=slides.length-1,b=actionButton('',()=>{show(i);restart()},false,'ad-dot');b.setAttribute('aria-label',(i+1)+'번 광고 보기');
  controls.insertBefore(b,next);dots.push(b);count.textContent=(index+1)+' / '+slides.length;
 }
 // ===== 5번째 슬라이드: 우리 학교 근처 맛집 추천 (학교별 사진 1~3장) =====
 // 그림 위치: static/school-food/학교이름-1.jpg, -2.jpg, -3.jpg
 let foodFrame=null;
 function showFoodFrame(s){                           // 맛집 슬라이드면 사진 카드를 보이고, 아니면 숨김
  if(!s.food){if(foodFrame)foodFrame.hidden=true;return}
  if(!foodFrame){foodFrame=document.createElement('div');foodFrame.className='ad-food-frame';hero.append(foodFrame)}
  foodFrame.replaceChildren(...s.food.map(url=>{const b=document.createElement('button');b.type='button';b.setAttribute('aria-label',s.foodSchool+' 근처 맛집 사진 크게 보기');const img=document.createElement('img');img.src=url;img.alt=s.foodSchool+' 근처 맛집';b.append(img);b.onclick=()=>window.open(url,'_blank','noopener');return b})); // 누르면 크게 보기
  foodFrame.hidden=false;
 }

 let deco,regionMap=null;                              // 장식 묶음, 학교이름→지역
 function showDeco(s){                                // 학교·맛집 슬라이드에만 장식 도형, 스티커, 정보 칩을 보여줌
  const kind=s.school?'school':s.food?'food':'';
  if(!kind){if(deco)deco.hidden=true;return}
  if(!deco){
   deco=document.createElement('div');deco.className='ad-deco';deco.setAttribute('aria-hidden','true');
   deco.innerHTML='<span class="ad-orbit"></span><span class="ad-star s1">✳</span><span class="ad-star s2">✳</span><span class="ad-sticker"></span><div class="ad-chips"></div>';
   hero.append(deco);
  }
  deco.hidden=false;
  const name=s.school||s.foodSchool,chips=deco.querySelector('.ad-chips');
  deco.querySelector('.ad-sticker').textContent=kind==='school'?'우리 학교 장터 OPEN!':'오늘의 맛집 PICK!';
  const fill=()=>{                                    // 칩 내용을 현재 슬라이드에 맞게 다시 채움
   if(!deco||deco.hidden)return;
   const list=[],region=regionMap&&regionMap[name],n=(document.getElementById('result-count')?.textContent||'').replace(/[^0-9]/g,'');
   if(kind==='school'){if(region)list.push('📍 '+region);if(n)list.push('이웃이 올린 물건 '+n+'개')}
   else{list.push('📍 '+name+' 근처');if(s.food)list.push('🍽 맛집 사진 '+s.food.length+'장')}
   const fr=kind==='school'?schoolFrame:foodFrame;if(fr)chips.style.right=Math.max(8,hero.clientWidth-fr.offsetLeft-22)+'px';   // 사진 왼쪽 가장자리에 살짝 걸치게 배치
   chips.replaceChildren(...list.map(x=>{const c=document.createElement('span');c.textContent=x;return c}));
  };
  fill();
  if(!regionMap){regionMap={};fetch('/api/members/schools').then(r=>r.json()).then(rows=>{rows.forEach(r=>regionMap[r.name]=r.region);fill()}).catch(()=>{})}
 }
 async function addFoodSlide(){
  const m=currentSession.authenticated&&currentSession.member;if(!m||!m.schoolName)return;
  const found=[];
  for(let n=1;n<=3;n++){const url='/school-food/'+encodeURIComponent(m.schoolName+'-'+n)+'.jpg';if(await imageExists(url))found.push(url);else break} // -1, -2, -3 순서로 있는 것까지
  if(!found.length)return;
  slides.push({tag:'TASTY PICKS · 맛집 추천',title:'우리 학교 앞,\n오늘은 뭐 먹지?',body:m.schoolName+' 근처 맛집을\n모아봤어요.',button:'학교 주변 업체 보기　↗',tab:'life',theme:'ad-food',food:found,foodSchool:m.schoolName});
  const i=slides.length-1,b=actionButton('',()=>{show(i);restart()},false,'ad-dot');b.setAttribute('aria-label',(i+1)+'번 광고 보기');
  controls.insertBefore(b,next);dots.push(b);count.textContent=(index+1)+' / '+slides.length;
 }
 addSchoolSlide().then(addFoodSlide); // 학교 사진 슬라이드 다음에 맛집 슬라이드
 show(0);if(matchMedia('(prefers-reduced-motion: reduce)').matches){paused=true;pause.textContent='자동 재생'}restart();
})();
let lastNotificationCount=null;
async function refreshSellerNotifications(){
 if(!currentSession.authenticated||currentSession.member.role!=='STUDENT')return;
 try{const list=await api('notifications'),unread=list.filter(n=>!n.readAt).length;let b=$('#seller-notifications');if(!b){b=actionButton('알림',openSellerNotifications,false,'');b.id='seller-notifications';$('#member-actions').append(b)}b.textContent='알림'+(unread?' '+unread:'');b.setAttribute('aria-label','거래 알림 '+unread+'개 미확인');if(lastNotificationCount!==null&&unread>lastNotificationCount)toast('새 거래 약속 알림이 도착했어요.');lastNotificationCount=unread}catch{}
}
async function openSellerNotifications(){
 try{
  const [list,prefs]=await Promise.all([api('notifications'),api('notifications/settings')]);const root=modal('거래 약속 알림','거래 약속이 잡히면 판매자와 구매자 모두에게 알려 드립니다.');
  const settings=el('form','notification-settings');settings.append(el('strong','','외부 알림 수신 설정'));
  const email=addField(settings,'수신 이메일','email',prefs.email||'','input',false);email.type='email';email.maxLength=100;
  const phone=addField(settings,'수신 전화번호 (문자·카카오)','phone',prefs.phone||'','input',false);phone.type='tel';phone.maxLength=20;
  for(const [key,label]of [['smsEnabled','문자 알림'],['emailEnabled','이메일 알림'],['kakaoEnabled','카카오 알림톡']]){const l=el('label'),i=el('input');i.type='checkbox';i.name=key;i.checked=prefs[key];l.append(i,document.createTextNode(' '+label));settings.append(l)}
  settings.append(el('p','fine-print',prefs.deliveryEnabled?'외부 발송이 활성화되어 있습니다. 채널별 연동 설정에 따라 발송됩니다.':'현재 외부 발송이 꺼져 있습니다. 운영자가 서비스를 연결하면 발송할 수 있습니다.'));
  const save=el('button','primary','수신 설정 저장');save.type='submit';settings.append(save);settings.onsubmit=async e=>{e.preventDefault();save.disabled=true;try{await api('notifications/settings','PUT',{email:email.value,phone:phone.value,smsEnabled:settings.elements.smsEnabled.checked,emailEnabled:settings.elements.emailEnabled.checked,kakaoEnabled:settings.elements.kakaoEnabled.checked});toast('알림 수신 설정을 저장했어요.')}catch(err){toast(err.message)}finally{save.disabled=false}};root.append(settings);
  if(!list.length)root.append(el('p','empty','아직 받은 거래 약속이 없어요.'));
  const labels={PENDING:'발송 대기',SENDING:'발송 처리 중',ACCEPTED:'서비스 접수 완료',FAILED:'발송 실패',SETUP_REQUIRED:'서비스 설정 필요',DISABLED:'수신 꺼짐',CONTACT_REQUIRED:'연락처 필요'};
  for(const n of list){const row=el('article','notification-row');row.append(el('strong','',n.title),el('p','',n.message),el('small','',dateTime(timestamp(n.createdAt))));for(const d of n.deliveries)row.append(el('p','delivery-label',({SMS:'문자',EMAIL:'이메일',KAKAO:'카카오 알림톡'}[d.channel])+' · '+(labels[d.status]||d.status)));row.append(actionButton('거래 보기',()=>{$('#modal').close();tradeProductId=n.saleId;switchTab('integrations')},false,'secondary'));if(!n.readAt)row.append(actionButton('읽음 표시',async()=>{await api('notifications/'+n.id+'/read','POST');await refreshSellerNotifications();openSellerNotifications()},false,'secondary'));if(n.deliveries.some(d=>['FAILED','SETUP_REQUIRED','CONTACT_REQUIRED','DISABLED'].includes(d.status)))row.append(actionButton('외부 알림 다시 시도',async()=>{await api('notifications/'+n.id+'/retry','POST');toast('발송 가능한 채널을 다시 대기열에 넣었어요.');openSellerNotifications()},false,'secondary'));root.append(row)}
 }catch(e){toast(e.message)}
}
document.addEventListener('DOMContentLoaded',()=>{setTimeout(refreshSellerNotifications,1000);setInterval(refreshSellerNotifications,15000)});
