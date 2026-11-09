const SITE='owner';
function publicLabel(s){return !s?'미등록':s.approved?'공개 중':state.publication?.status==='비공개'?'공개 중지':'승인 대기'}
const statusKinds={'영업중':'','혼잡':'pending','영업종료':'neutral','휴무':'red'};
async function refresh(){state=await api('owner/dashboard');if(!['OWNER','MANAGER'].includes(state.me.role))throw Error('사장님·매니저 계정으로 로그인해 주세요.');page=location.hash.slice(1)||'overview';render()}
function inputFor(s){return {address:s?.address||'',latitude:s?.latitude??null,longitude:s?.longitude??null,version:s?.version??null,shopname:s?.name||'',shopphone:s?.phone||'',busiKind:s?.category||'카페',bmenu:s?.menu||'',reservtime:s?.reservation||'',busitime:s?.hours||'09:00~21:00',offday:s?.offday||'',specialevent:s?.event||'',sizeandtype:s?.spaceType||'',status:s?.status||'영업종료',availablespace:s?.space||''}}
function render(){
 if(!['overview','shop','operations','managers','history'].includes(page))page='overview';showApp();
 $('#workspace-name').textContent=state.shop?.name||'아직 등록한 가게가 없어요';
 document.querySelector('[data-page="managers"]').hidden=state.me.role!=='OWNER';
 const root=$('#page');root.replaceChildren();
 if(page==='overview')overview(root);else if(page==='shop')shopPage(root);else if(page==='operations')operationsPage(root);else if(page==='history')historyPage(root);else managersPage(root);
}
function emptyShop(root){const box=panel('우리 가게를 등록해 주세요','등록한 가게는 관리자 승인 후 학생 사이트에 공개됩니다.'),empty=el('div','empty');empty.append(icon('shop'),el('p','','아직 연결된 가게가 없습니다.'));if(state.me.role==='OWNER')empty.append(btn('가게 등록하기',()=>go('shop'),'primary'));else empty.append(el('p','','사장님에게 가게 등록을 요청해 주세요.'));box.append(empty);root.append(box)}
function preview(s){const box=el('div','shop-preview'),art=el('div','shop-preview-art'),mark=el('b','','good<br>coffee.');mark.replaceChildren(document.createTextNode('our shop.'));if(s.photo){const photo=el('img');photo.src='/api/shops/photo/'+encodeURIComponent(s.photo);photo.alt=s.name+' 상점 사진';photo.style.cssText='width:100%;height:100%;object-fit:cover';art.append(photo)}else art.append(icon('shop'),mark);const content=el('div','shop-preview-body');content.append(badge(s.status,statusKinds[s.status]),el('h3','',s.name),el('p','',s.category+' · '+(s.hours||'영업시간 문의')),el('p','',s.menu||'대표 메뉴를 등록해 주세요.'));box.append(art,content);return box}
function overview(root){
 const s=state.shop,refreshButton=btn('새로고침',()=>busy(refreshButton,refresh));root.append(heading('BUSINESS OVERVIEW',state.me.name+'님, 반가워요.','우리 가게의 오늘을 확인하고, 학생들에게 새로운 소식을 전하세요.',refreshButton));
 const welcome=el('section','welcome'),text=el('div');text.append(el('span','eyebrow','GOOD DAYS, GOOD BUSINESS'),el('h2','',s?'우리 가게의 소식, 캠퍼스에 닿도록.':'캠퍼스와 만나는 첫걸음.'),el('p','',s?'영업 상태와 여유 공간을 최신 정보로 유지해 주세요.':'가게를 등록하면 학생들에게 영업 정보와 메뉴를 소개할 수 있어요.'));const mark=el('div','welcome-mark','open.');mark.append(el('small','','HELLO, CAMPUS.'));welcome.append(text,mark);root.append(welcome);
 const stats=el('div','stats');stats.append(stat('현재 영업 상태',s?.status||'미등록',s?'학생 화면에 표시되는 영업 상태':'먼저 가게를 등록해 주세요','clock'),stat('학생 사이트 공개',publicLabel(s),s?.approved?'학생들이 가게를 볼 수 있어요':'관리자 승인 후 공개됩니다','shop'),stat('여유 공간',s?.space||'정보 없음','매장 상황에 맞게 업데이트하세요','grid'),stat('등록 매니저',state.me.role==='OWNER'?state.managers.length+' / 3':'매니저 계정',state.me.role==='OWNER'?'최대 3명까지 함께 관리할 수 있어요':'영업 정보를 함께 관리할 수 있어요','users'));root.append(stats);
 if(!s)return emptyShop(root);
 const grid=el('div','grid-two'),left=panel('지금 우리 가게는','한 번의 선택으로 학생 사이트의 영업 상태를 변경합니다.'),right=panel('빠른 메뉴','자주 사용하는 가게 관리 기능을 모았어요.');left.append(statusButtons(s),el('div','info-strip',s.approved?'공개 중인 가게입니다. 저장하면 학생 사이트에 반영됩니다.':state.publication?.status==='비공개'?'공개 중지 상태입니다. 가게 정보의 관리자 안내를 확인해 주세요.':'승인 대기 중입니다. 운영 정보는 저장되며, 관리자 승인 후 학생 사이트에 공개됩니다.'));const details=el('div','detail-list');details.append(detailRow('영업시간',s.hours),detailRow('오늘의 공간',s.space),detailRow('휴무일',s.offday));left.append(details);const quick=el('div','quick-links');for(const [key,title,sub,ico]of [['shop','가게 정보 수정','가게 소개와 대표 메뉴를 관리해요.','shop'],['operations','운영 정보 관리','영업시간·여유 공간·이벤트를 알려요.','clock'],...(state.me.role==='OWNER'?[['managers','매니저 관리','함께 관리하는 매니저를 등록해요.','users']]:[])]){const b=btn('',()=>go(key),'quick-link'),t=el('span');t.append(el('strong','',title),el('small','',sub));b.append(icon(ico),t,icon('arrow'));quick.append(b)}right.append(quick);grid.append(left,right);root.append(grid);
 const readiness=panel('학생에게 소개할 준비','등록된 정보를 확인하고 부족한 안내를 채워 주세요.'),checks=el('div','checklist');for(const [label,ok]of [['관리자 승인',s.approved],['대표 메뉴 등록',!!s.menu],['전화 또는 예약 안내',!!(s.phone||s.reservation)],['여유 공간 안내',!!s.space],['학생 혜택 등록',!!s.event]]){const item=el('div','check-item'+(ok?'':' missing'));item.append(icon(ok?'check':'clock'),el('span','',label),badge(ok?'완료':'확인 필요',ok?'':'pending'));checks.append(item)}readiness.append(checks);root.append(readiness);
 const lower=el('div','grid-two'),info=panel('학생 화면에 보이는 정보','실제 저장된 가게 정보입니다.'),notice=panel('사장센터 이용 안내','우리 가게를 함께 관리하는 방법');info.append(preview(s));notice.append(detailRow('가게 정보 변경','사장님 계정에서 변경할 수 있어요.'),detailRow('영업 관리','사장님·매니저 모두 사용할 수 있어요.'),detailRow('매니저 등록','사장님만 최대 3명까지 등록할 수 있어요.'),el('div','notice','비밀번호를 공유하는 대신, 매니저별 계정을 등록해 주세요. 등록한 매니저는 우리 가게의 영업 정보만 관리합니다.'));lower.append(info,notice);root.append(lower);
}
function statusButtons(s){const row=el('div','status-row');for(const status of ['영업중','혼잡','영업종료','휴무']){const b=btn('',()=>{if(activeWork||s.status===status||!canLeave())return;busy(b,async()=>{await api('owner/shop/status','PUT',{status,version:s.version});await refresh();toast('영업 상태를 '+status+'으로 변경했어요.')})},'status-option'+(s.status===status?' active':''));b.dataset.status=status;b.append(el('i','status-dot'),el('span','',status));b.setAttribute('aria-pressed',String(s.status===status));row.append(b)}return row}
function shopPage(root){
 const s=state.shop;const reload=btn('최신 정보 불러오기',()=>{if(canLeave())busy(reload,refresh)});root.append(heading('SHOP PROFILE',s?'우리 가게 정보':'새로운 가게 등록','학생들에게 보여 줄 가게의 기본 정보와 소개를 관리합니다.',reload));
 if(state.me.role!=='OWNER'){if(s){const p=panel('가게 정보','기본 정보 변경은 사장님 계정에서 할 수 있습니다.');p.append(preview(s),detailRow('사업자번호',s.businessNo),detailRow('대표자명',s.ownerName));root.append(p)}else emptyShop(root);return}
 const grid=el('div','grid-two'),box=panel(s?'기본 정보':'가게 등록 정보',s?'사업자 정보는 가입한 사장 계정에 연결되어 있습니다.':'등록 후 관리자 승인을 기다려 주세요.'),form=el('form'),fields=el('div','form-grid'),data=inputFor(s);
 field(fields,'학교','ownerSchool',state.me.schoolName||'학교 정보 미등록',{readonly:true,full:true});field(fields,'가게명','shopname',data.shopname,{required:true,max:50});field(fields,'업종','busiKind',data.busiKind,{required:true,max:50});field(fields,'대표자명','ceoname',s?.ownerName||state.me.name,{readonly:true});field(fields,'사업자번호','busino',s?.businessNo||state.me.businessNo,{readonly:true});
 field(fields,'가게 주소','address',data.address,{max:200,full:true});field(fields,'지도 위도 (-90~90)','latitude',data.latitude??'',{});field(fields,'지도 경도 (-180~180)','longitude',data.longitude??'',{});fields.querySelectorAll('[name=latitude],[name=longitude]').forEach(n=>{n.type='number';n.step='any'});field(fields,'가게 전화번호','shopphone',data.shopphone,{max:50});field(fields,'영업시간','busitime',data.busitime,{required:true,max:100});field(fields,'휴무일','offday',data.offday,{max:20});field(fields,'예약 안내','reservtime',data.reservtime,{max:100});field(fields,'공간 소개','sizeandtype',data.sizeandtype,{max:200,full:true});field(fields,'대표 메뉴','bmenu',data.bmenu,{area:true,max:400,full:true});
 const foot=el('div','form-footer'),submit=el('button','primary',s?'변경 사항 저장':'가게 등록 요청');submit.type='submit';foot.append(el('small','',s?.approved?'변경한 공개 정보는 학생 사이트에 반영됩니다.':'등록한 가게는 관리자 승인 후 학생 사이트에 공개됩니다.'),submit);form.append(fields,foot);form.onsubmit=e=>{e.preventDefault();busy(submit,async()=>{const f=Object.fromEntries(new FormData(form));const payload={...data,...f,latitude:f.latitude===''?null:Number(f.latitude),longitude:f.longitude===''?null:Number(f.longitude)};delete payload.ceoname;delete payload.busino;await api('owner/shop',s?'PUT':'POST',payload);await refresh();toast(s?'가게 정보를 저장했어요.':'등록 요청을 보냈어요. 관리자 승인 후 공개됩니다.')})};box.append(watchForm(form));
 const side=panel('가게 공개 상태','현재 학생 사이트 공개 여부');side.append(badge(s?publicLabel(s):'등록 전',s?.approved?'':'pending'),el('div','notice','대표자명과 사업자번호는 회원가입한 계정의 정보를 사용합니다. 다른 가게의 소유권을 변경하려면 관리자에게 문의해 주세요.'));if(state.publication)side.append(el('div','approval-note','관리자 안내 · '+dateTime(state.publication.changedAt)+'\n'+state.publication.status+'\n'+(state.publication.note||'처리 메모가 없습니다.')));if(s)side.append(preview(s));grid.append(box,side);root.append(grid);
}
function operationsPage(root){
 const s=state.shop;const reload=btn('최신 정보 불러오기',()=>{if(canLeave())busy(reload,refresh)});root.append(heading('DAILY OPERATIONS','영업 관리','지금의 가게 상황을 알려, 학생들이 편하게 방문할 수 있도록 해 주세요.',reload));if(!s)return emptyShop(root);
 const statePanel=panel('현재 영업 상태','가게 상황에 맞는 상태를 선택해 주세요.');statePanel.append(statusButtons(s));root.append(statePanel);
 const grid=el('div','grid-two'),box=panel('오늘의 운영 정보','사장님과 등록된 매니저가 함께 관리합니다.'),form=el('form'),fields=el('div','form-grid'),data=inputFor(s);
 field(fields,'영업시간','busitime',data.busitime,{required:true,max:100});field(fields,'휴무일','offday',data.offday,{max:20});field(fields,'여유 공간','availablespace',data.availablespace,{max:50});field(fields,'가게 전화번호','shopphone',data.shopphone,{max:50});field(fields,'예약 안내','reservtime',data.reservtime,{max:100,full:true});field(fields,'이벤트·학생 혜택','specialevent',data.specialevent,{max:50,full:true});field(fields,'메뉴 안내','bmenu',data.bmenu,{area:true,max:400,full:true});
 const foot=el('div','form-footer'),submit=el('button','primary','운영 정보 저장');submit.type='submit';foot.append(el('small','','공개 중인 가게의 변경 사항은 학생 사이트에 반영됩니다.'),submit);form.append(fields,foot);form.onsubmit=e=>{e.preventDefault();busy(submit,async()=>{await api('owner/shop/operations','PUT',{...data,...Object.fromEntries(new FormData(form))});await refresh();toast('운영 정보를 저장했어요.')})};box.append(watchForm(form));const side=panel('방문 전, 학생들은 이런 정보를 확인해요','간단하고 정확하게 안내해 주세요.');side.append(detailRow('영업시간','예: 09:00~21:00'),detailRow('여유 공간','예: 4인석 2개 · 콘센트 자리 여유'),detailRow('이벤트','예: 학생증 지참 시 음료 10% 할인'),el('div','notice','혼잡도와 여유 공간은 실시간 방문 상황을 확인해 직접 업데이트해 주세요.'));grid.append(box,side);root.append(grid);
}
function managersPage(root){
 root.append(heading('TEAM MEMBERS','매니저 관리','함께 가게를 운영하는 매니저 계정을 관리합니다.',state.me.role==='OWNER'?btn('＋ 매니저 등록',managerForm,'primary'):null));
 if(state.me.role!=='OWNER'){root.append(el('div','empty','매니저 계정은 사장님이 관리합니다.'));return}
 const box=panel('등록된 매니저 '+state.managers.length+' / 3','각 매니저는 본인 계정으로 사장센터에 로그인할 수 있습니다.'),list=el('div','manager-list');
 for(const manager of state.managers){const row=el('article','manager-card'),text=el('div');text.append(el('b','',manager.name),el('small','','아이디 '+manager.loginId));row.append(el('span','avatar',manager.name.slice(0,1)),text,btn('삭제',()=>deleteManager(manager),'small-button danger'));list.append(row)}
 if(!state.managers.length)list.append(el('div','empty','등록된 매니저가 없습니다. 함께 관리할 계정을 추가해 주세요.'));box.append(list,el('div','notice','매니저는 영업 상태·운영 정보를 변경할 수 있습니다. 가게 기본 정보 변경, 새 매니저 등록·삭제는 사장님만 가능합니다.'));root.append(box);
}
function managerForm(){
 if(state.managers.length>=3)return toast('매니저는 최대 3명까지 등록할 수 있어요.');const root=dialog('매니저 등록');root.append(el('p','dialog-description','함께 가게를 관리할 매니저의 이름과 로그인 계정을 입력해 주세요.'));const form=el('form','dialog-form');field(form,'이름','name','',{required:true,max:20});const id=field(form,'아이디','loginId','',{required:true,max:20});id.pattern='[a-z0-9_]{4,20}';id.placeholder='영문 소문자·숫자·_ 4~20자';const pw=field(form,'비밀번호','password','',{required:true,type:'password'});pw.minLength=8;pw.maxLength=72;pw.autocomplete='new-password';const submit=el('button','primary','매니저 등록');submit.type='submit';form.append(submit);form.onsubmit=e=>{e.preventDefault();busy(submit,async()=>{await api('members/managers','POST',Object.fromEntries(new FormData(form)));$('#dialog').close();await refresh();toast('매니저를 등록했어요.')})};root.append(watchForm(form));
}
function deleteManager(manager){const root=dialog('매니저를 삭제할까요?');root.append(el('p','dialog-description',manager.name+'님은 사장센터에서 우리 가게를 관리할 수 없게 됩니다.'));const button=btn('매니저 삭제',()=>busy(button,async()=>{await api('members/managers/'+manager.id,'DELETE');$('#dialog').close();await refresh();toast('매니저를 삭제했어요.')}),'primary');root.append(button)}
let historyRequest=0;
async function historyPage(root){
 const request=++historyRequest,refreshButton=btn('새로고침',()=>render());
 root.append(heading('CHANGE HISTORY','변경 이력 조회','우리 가게의 변경자·변경 시각·변경 전후 내용을 확인합니다.',refreshButton));
 const box=panel('최근 변경 이력','최근 200개 변경 항목을 최신순으로 표시합니다. 같은 내용으로 저장하면 새 이력은 생기지 않습니다.');
 const loading=el('div','empty','변경 이력을 불러오는 중입니다.');box.append(loading);root.append(box);
 try{
  const rows=await api('owner/shop/history');if(page!=='history'||request!==historyRequest||!box.isConnected)return;loading.remove();
  const fields=[...new Map(rows.map(r=>[r.fieldKey,r.fieldLabel]))];
  table(box,rows,[
   {title:'변경 시각',value:r=>dateTime(r.changedAt),sort:r=>r.changedAt},
   {title:'변경자',value:r=>nameCell(r.actorName,r.actorRole==='OWNER'?'사장':'매니저')},
   {title:'변경 항목',value:r=>badge(r.fieldLabel,r.fieldKey==='status'?'':'neutral')},
   {title:'변경 전',value:r=>el('span','history-value history-before',r.beforeValue||'(미입력)')},
   {title:'변경 후',value:r=>el('span','history-value history-after',r.afterValue||'(미입력)')}
  ],{dateKey:'changedAt',exportName:'우리 가게 변경 이력',placeholder:'변경자·변경 내용 검색',filter:{options:[['','전체 변경 항목'],...fields],match:(r,v)=>!v||r.fieldKey===v},empty:'아직 변경 이력이 없습니다. 기능 추가 이후 저장한 변경부터 기록됩니다.'});
 }catch(e){if(page==='history'&&request===historyRequest&&box.isConnected){loading.textContent=e.message;toast(e.message)}}
}
init();
