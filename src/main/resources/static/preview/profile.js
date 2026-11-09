// A member owns their listings and their cumulative manner temperature.
let profileSequence=0;
async function openProfileListing(id){try{if(!products.some(p=>p.id===id))await loadData();productDetail(id)}catch(error){toast(error.message)}}
function openOwnProfile(){if(!requireLogin())return;profileNavigate('profile')}
function openSellerProfile(id){if($('#modal').open)$('#modal').close();profileNavigate('seller/'+id)}
function profileNavigate(route){history.pushState(null,'','#'+route);tab='profile';render();$('#content').scrollIntoView({behavior:'instant'})}
function sellerProfileButton(p,compact=false){
  const button=actionButton('',()=>openSellerProfile(p.id),false,compact?'seller-on-card':'seller-profile-summary');
  const name=p.sellerName||'기존 시연 판매자',avatar=el('span','seller-avatar',name.slice(0,1)),info=el('span','seller-id');
  info.append(el('span','seller-small-label',compact?'판매자':'판매자 프로필'),el('strong','',name));
  if(!compact)info.append(el('small','',p.sellerSchoolName||'회원 연결 확인 필요'));
  const temperature=el('strong','seller-temperature',p.temperature.toFixed(1)+'℃');temperature.title='이 판매자의 누적 매너온도';
  button.append(avatar,info,temperature,el('span','seller-arrow','›'));button.setAttribute('aria-label',name+' 판매자 프로필 · 매너온도 '+p.temperature.toFixed(1)+'도');return button;
}
function sellerInformation(p){
  const card=el('section','seller-detail-card');card.setAttribute('aria-label','판매자 정보');
  const identity=actionButton('',()=>openSellerProfile(p.id),false,'seller-detail-identity'),name=p.sellerName||'판매자',avatar=el('span','seller-detail-avatar',name.match(/[ABC]$/)?.[0]||name.slice(0,1)),copy=el('span','seller-detail-name');
  copy.append(el('strong','',name),el('small','',p.sellerSchoolName||'캠퍼스 이웃'));identity.append(avatar,copy,el('span','seller-detail-temperature',p.temperature.toFixed(1)+'℃'),el('span','seller-arrow','›'));identity.setAttribute('aria-label',name+' 판매자 프로필 보기');card.append(identity);
  const content=el('div','seller-detail-content');content.append(el('p','seller-detail-empty','거래 후기를 불러오고 있어요.'));card.append(content,actionButton('판매자 정보 / 판매글 더보기 ›',()=>openSellerProfile(p.id),false,'seller-detail-more'));
  api('products/'+p.id+'/seller-profile').then(profile=>{
    if(!card.isConnected)return;content.replaceChildren();const s=profile.summary,meta=el('p','seller-detail-meta');meta.append(el('span','seller-detail-star',s.averageScore===null?'★ 평가 전':'★ '+s.averageScore.toFixed(1)),document.createTextNode(' · 후기 '+s.total+' · 거래내역 '+s.completedTrades));content.append(meta);
    const stats=el('div','seller-detail-stats');for(const [value,label]of [[s.averageScore===null?'–':s.averageScore.toFixed(1),'평균 별점'],[s.satisfactionRate===null?'–':Math.round(s.satisfactionRate)+'%','만족 후기']]){const stat=el('div');stat.append(el('strong','',value),el('small','',label));stats.append(stat)}content.append(stats);
    if(s.feedback?.length){const tags=el('div','seller-feedback-list');s.feedback.slice(0,3).forEach(item=>{const row=el('div');row.append(el('span','','♡ '+item.label),el('strong','',String(item.count)));tags.append(row)});content.append(tags)}else content.append(el('p','seller-detail-empty',s.total?'아직 모인 칭찬 키워드가 없어요.':'아직 받은 거래 후기가 없어요.'));
  }).catch(()=>{if(card.isConnected)content.replaceChildren(el('p','seller-detail-empty','후기를 불러오지 못했어요. 판매자 프로필에서 다시 확인해 주세요.'))});return card;
}
async function renderProfile(){
  const sequence=++profileSequence,route=location.hash.slice(1).split('/'),sellerId=route[0]==='seller'?Number(route[1]):null;
  $('.section-head').hidden=true;$('.controls').hidden=true;$('#page-return').hidden=true;
  const root=$('#results');root.className='member-profile-view';root.replaceChildren();
  const toolbar=el('div','profile-toolbar');toolbar.append(actionButton('← 중고거래',()=>switchTab('market'),false,'profile-back'),actionButton('새로고침',()=>render(),false,'profile-back'));root.append(toolbar);
  if(sellerId===null&&!currentSession.authenticated){root.append(el('h2','','내 프로필'),el('p','profile-empty','로그인하고 내 매너온도를 확인해 보세요.'),actionButton('로그인',()=>requireLogin(),false,'primary'));return}
  const loading=el('p','profile-empty','프로필을 불러오고 있어요…');root.append(loading);
  try{
    if(sellerId!==null&&(!Number.isInteger(sellerId)||sellerId<1))throw Error('판매자를 찾을 수 없어요.');
    const profile=await api(sellerId===null?'profiles/me':'products/'+sellerId+'/seller-profile');
    if(sequence!==profileSequence||tab!=='profile'||!root.isConnected)return;
    loading.remove();
    for(const sale of profile.listings){const cached=products.find(p=>p.id===sale.selno);if(cached){cached.temperature=Number(sale.sellerTemperature);cached.sellerMemberId=sale.sellerMemberId;cached.sellerName=sale.sellerName;cached.sellerSchoolName=sale.sellerSchoolName}}
    const intro=el('div','profile-intro');intro.append(el('span','eyebrow',profile.own?'MY CAMPUS PROFILE':'CAMPUS NEIGHBOR'),el('h2','',profile.own?'내 프로필':'판매자 프로필'));root.append(intro);
    const hero=el('section','profile-hero'),identity=el('div','profile-identity'),avatar=el('span','profile-avatar-large',profile.name.slice(0,1)),info=el('div');
    info.append(el('span','profile-member-type',profile.verifiedMember?'회원 프로필':'기존 시연 계정'),el('h3','',profile.name),el('p','',profile.schoolName||({OWNER:'사장님',MANAGER:'매니저',ADMIN:'관리자'}[profile.role])||'캠퍼스 이웃'));
    identity.append(avatar,info);hero.append(identity);
    if(profile.temperature!==null){
      const temperature=el('div','profile-temperature-panel'),line=el('div','profile-temperature-line');line.append(el('span','','매너온도'),el('strong','profile-temperature-value',profile.temperature.toFixed(1)+'℃'));
      const track=el('div','profile-temperature-track'),fill=el('span');fill.style.width=(Math.min(99,Math.max(0,profile.temperature))/99*100)+'%';track.append(fill);
      track.setAttribute('role','meter');track.setAttribute('aria-label','매너온도');track.setAttribute('aria-valuemin','0');track.setAttribute('aria-valuemax','99');track.setAttribute('aria-valuenow',String(profile.temperature));
      temperature.append(line,track,el('p','','기본 36.5℃ · 최고 99℃ · 완료 거래의 평가가 반영돼요.'));hero.append(temperature);
    }else hero.append(el('p','profile-staff-note','매너온도는 학생 회원의 중고거래 평가에 따라 쌓여요.'));
    root.append(hero);
    if(profile.temperature!==null){
      const summary=profile.summary,stats=el('div','profile-stats');
      [['받은 평가',summary.total+'건'],['좋아요',summary.good+'건'],['아쉬워요',summary.bad+'건'],['평균 별점',summary.averageScore===null?'–':summary.averageScore.toFixed(1)+' / 5']].forEach(([label,value])=>{const card=el('div');card.append(el('span','',label),el('strong','',value));stats.append(card)});root.append(stats,el('p','profile-explainer','매너온도는 회원별 누적 점수예요. 같은 판매자의 여러 글에는 같은 온도가 표시되고, 다른 회원의 평가에는 영향을 받지 않아요.'));
    }
    if(profile.own){const actions=el('div','profile-actions');actions.append(actionButton('계정 설정',()=>location.href='/mypage.html',false,'secondary'));if(profile.role==='STUDENT')actions.append(actionButton('내 판매글 관리',()=>switchTab('mine'),false,'primary'),actionButton('＋ 판매글 등록',()=>saleForm(),false,'secondary'));root.append(actions)}
    const listings=el('section','profile-section'),head=el('div','profile-section-head');head.append(el('h3','',profile.own?'내 판매글':'이 판매자의 판매글'),el('span','',profile.listings.length+'개'));listings.append(head);
    if(!profile.listings.length)listings.append(el('p','profile-empty',profile.own?'아직 등록한 판매글이 없어요.':'현재 등록된 판매글이 없어요.'));
    else{const list=el('div','profile-listings');profile.listings.forEach(sale=>{const button=actionButton('',()=>openProfileListing(sale.selno),false,'profile-listing'),copy=el('span');copy.append(el('small','',sale.category),el('strong','',sale.name),el('span','',sale.status+' · '+sale.location));button.append(copy,el('b','',Number(sale.currentPrice).toLocaleString('ko-KR')+'원'),el('span','seller-arrow','›'));list.append(button)});listings.append(list)}root.append(listings);
    if(profile.own&&profile.temperature!==null){
      const section=el('section','profile-section'),head=el('div','profile-section-head');head.append(el('h3','','내가 받은 평가'),el('span','',profile.reviews.length+'건'));section.append(head);
      if(!profile.reviews.length)section.append(el('p','profile-empty','완료한 거래에서 받은 평가가 여기에 표시돼요.'));
      profile.reviews.forEach(review=>{const card=el('article','profile-review'),top=el('div'),label=review.score?review.score+'점 · '+({GOOD:'좋아요',BAD:'아쉬워요',NEUTRAL:'보통'}[review.sentiment]):review.sentiment==='GOOD'?'좋아요':'아쉬워요';
        top.append(el('strong','',label),el('span','',review.delta>0?'+'+(review.delta/10).toFixed(1)+'℃':(review.delta/10).toFixed(1)+'℃'));card.append(top);if(review.attitude)card.append(el('small','',review.attitude));if(review.content)card.append(el('p','',review.content));if(review.createdAt)card.append(el('time','',dateTime(timestamp(review.createdAt))));section.append(card)});root.append(section);
    }
  }catch(error){if(sequence===profileSequence&&tab==='profile'){loading.textContent=error.message;loading.classList.add('profile-error')}}
}
