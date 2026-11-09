// 계정 설정 화면 스크립트 ($, toast, api 는 member.js에 있는 것을 같이 씀)

const statusNames = {ENROLLED: '재학생', GRADUATED: '졸업생'}; // 재학 상태 한글 이름
const roleNames = {STUDENT: '학생 회원', OWNER: '사장님', MANAGER: '매니저', ADMIN: '관리자'}; // 화면에 보여 줄 한글 이름

function showMe(me) {                              // 서버에서 받은 내 정보를 화면에 채우기
  $('#myRole').textContent = roleNames[me.role] || me.role;
  $('#myName').textContent = me.name;
  $('#myAvatar').textContent = (me.name || '?').trim().charAt(0); // 프로필 동그라미에는 이름 첫 글자
  $('#myEmail').textContent = me.email || '없음';  // 매니저는 이메일이 없음
  $('#myPhone').textContent = me.phone || '없음';
  if (me.schoolName) $('#mySchool').textContent = me.schoolName + ' / ' + me.loginId; // 학생: 학교 + 학번
  else $('#mySchool').textContent = '아이디: ' + me.loginId;                          // 그 외: 아이디
  $('#phoneForm [name=phone]').value = me.phone || '';  // 지금 전화번호를 입력칸에 미리 넣어 둠
  $('#nameForm [name=name]').value = me.name;           // 지금 이름을 입력칸에 미리 넣어 둠
  const isStudent = me.role === 'STUDENT';              // 닉네임 수정·탈퇴는 학생만 (사장 대표자명 등은 가게 정보와 연결됨)
  $('#myStatusRow').hidden = !isStudent;                // 재학 상태 줄도 학생만
  $('#myStatus').textContent = statusNames[me.studentStatus] || '';
  $('#myStatusChip').hidden = !isStudent;               // 프로필 카드의 재학 상태 칩
  $('#myStatusChip').textContent = statusNames[me.studentStatus] || '';
  $('#statusPanel').hidden = !isStudent;
  const radio = $('#statusForm [value=' + me.studentStatus + ']'); // 지금 상태 라디오를 미리 선택
  if (radio) radio.checked = true;
  $('#namePanel').hidden = !isStudent;
  $('#withdrawPanel').hidden = !isStudent;
  $('#manageTab').hidden = !isStudent;                  // 계정 관리 탭도 학생만
}

function openTab(name) {                           // 탭 버튼을 누르면 해당 구역만 보이게
  document.querySelectorAll('.acc-tabs button').forEach(b => b.classList.toggle('on', b.dataset.tab === name));
  document.querySelectorAll('.acc-sec').forEach(s => s.classList.toggle('on', s.id === 'sec-' + name));
}
document.querySelectorAll('.acc-tabs button').forEach(b => b.onclick = () => {openTab(b.dataset.tab);history.replaceState(null,'','#'+b.dataset.tab)});
if(['profile','security','manage'].includes(location.hash.slice(1)))openTab(location.hash.slice(1));

async function loadMe() {                          // 페이지가 열리면 내 정보 불러오기
  try {
    showMe(await api('/me'));                      // GET /api/members/me
  } catch (e) {
    location.href = '/login.html';                 // 로그인 안 했으면(401) 로그인 화면으로
  }
}

$('#phoneForm').onsubmit = async e => {            // 전화번호 저장
  e.preventDefault();                              // 폼이 페이지를 새로고침하지 않게
  const phone = e.target.phone.value.trim();
  try {
    showMe(await api('/me', {phone: phone}, 'PUT')); // PUT /api/members/me → 바뀐 정보로 다시 표시
    toast('전화번호를 저장했어요.');
  } catch (err) { toast(err.message); }            // 형식이 틀리면 서버 메시지 표시
};

$('#nameForm').onsubmit = async e => {             // 닉네임 저장
  e.preventDefault();
  try {
    showMe(await api('/me/name', {name: e.target.name.value.trim()}, 'PUT')); // PUT /api/members/me/name
    toast('닉네임을 저장했어요.');
  } catch (err) { toast(err.message); }
};

$('#statusForm').onsubmit = async e => {           // 재학/졸업 저장
  e.preventDefault();
  const picked = e.target.studentStatus.value;     // 선택된 라디오 값
  try {
    showMe(await api('/me/status', {studentStatus: picked}, 'PUT')); // PUT /api/members/me/status
    toast('재학 상태를 저장했어요.');
  } catch (err) { toast(err.message); }
};

$('#withdrawForm').onsubmit = async e => {         // 회원 탈퇴
  e.preventDefault();
  try {
    const r = await api('/me/withdraw', {password: e.target.password.value}); // POST /api/members/me/withdraw
    toast(r.message);
    setTimeout(() => location.href = '/', 1500);   // 서버가 로그아웃시켰으니 첫 화면으로
  } catch (err) { toast(err.message); }
};

$('#passwordForm').onsubmit = async e => {         // 비밀번호 변경
  e.preventDefault();
  const f = e.target;
  if (f.newPassword.value !== f.newPassword2.value) { // 새 비밀번호 두 칸이 같은지 먼저 화면에서 확인
    toast('새 비밀번호가 서로 달라요.');
    return;
  }
  try {
    // 조원이 만든 "로그인 상태 비밀번호 변경" API(/api/account/password)를 그대로 사용 (oldPassword, password)
    const r = await fetch('/api/account/password', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({oldPassword: f.currentPassword.value, password: f.newPassword.value})
    });
    let data = null;
    try { data = await r.json(); } catch {}        // 실패해도 서버 메시지를 읽기 위해
    if (!r.ok) throw Error(data?.message || '비밀번호를 바꾸지 못했어요.');
    toast(data.message);                           // "비밀번호를 변경했습니다. 다시 로그인하세요."
    setTimeout(() => location.href = '/login.html', 1500); // 서버가 로그아웃시키니까 로그인 화면으로
  } catch (err) { toast(err.message); }
};

loadMe();
