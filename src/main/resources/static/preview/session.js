let currentSession={authenticated:false};
function actorKey(){return currentSession.actorKey||null}
function isMySale(p){return currentSession.authenticated&&(p.sellerMemberId!=null?Number(p.sellerMemberId)===Number(currentSession.member.id):p.seller===actorKey())}
function requireLogin(){if(currentSession.authenticated)return true;location.href='/login.html';return false}
function requireStudentLogin(){
  if(!requireLogin())return false;
  if(currentSession.member.role==='STUDENT')return true;
  toast('판매·구매·평가는 학생 계정으로 이용해 주세요.');return false;
}
async function loadSession(){
  const response=await fetch('/api/session',{cache:'no-store'});
  if(!response.ok)throw Error('로그인 상태를 확인할 수 없어요.');
  currentSession=await response.json();await loadCampus();
  const root=document.querySelector('#member-actions');
  if(!root)return;
  root.replaceChildren();
  function link(text,href){const a=document.createElement('a');a.textContent=text;a.href=href;root.append(a)}
  for(const id of ['location','profile']){const item=document.getElementById(id);if(item){item.hidden=!currentSession.authenticated;item.style.display=currentSession.authenticated?'':'none'}}
  if(!currentSession.authenticated){link('로그인','/login.html');link('회원가입','/signup.html');return}
  const me=currentSession.member, label=document.createElement('span');
  label.textContent=me.name+'님';root.append(label);link('내 프로필','/sales/selectAll#profile');link('계정 설정','/mypage.html');
  if(['OWNER','MANAGER'].includes(me.role)){const portals=await fetch('/api/portals').then(r=>r.json());link(me.role==='OWNER'?'사장센터로 이동':'매니저센터로 이동',portals.owner);}
  if(me.role==='ADMIN'){const portals=await fetch('/api/portals').then(r=>r.json());link('관리자 페이지로 이동',portals.admin);}
  const button=document.createElement('button');button.textContent='로그아웃';
  button.onclick=async()=>{const r=await fetch('/api/members/logout',{method:'POST'});if(r.ok)location.href='/';else alert('로그아웃에 실패했어요. 다시 시도해 주세요.')};root.append(button);
  const profile=document.querySelector('#profile');if(profile){profile.textContent=me.name.slice(0,1);profile.title=me.name+' · 내 프로필'}
}
