// 로그인·회원가입·매니저 관리 화면 공통 스크립트
const $ = s => document.querySelector(s);
const escape = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); // 이름 등을 HTML에 넣을 때 안전하게

function toast(message) {                       // 화면 아래 알림
  $('#toast').textContent = message;
  $('#toast').style.display = 'block';
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => $('#toast').style.display = 'none', 3000);
}

// /api/members 로 요청. body가 있으면 POST(또는 지정한 method), 실패하면 서버 메시지를 에러로
async function api(path, body, method) {
  const options = body === undefined && !method ? {} : {
    method: method || 'POST',
    headers: {'Content-Type': 'application/json'},
    body: body === undefined ? undefined : JSON.stringify(body)
  };
  const r = await fetch('/api/members' + path, options);
  let data = null;
  try { data = await r.json(); } catch {}
  if (!r.ok) throw Error(data?.message || '서버 연결을 확인하고 다시 시도해 주세요.');
  return data;
}

async function loadSchools() {                  // 학교 목록을 select에 채우기
  const select = $('#schoolSelect');
  try {
    const schools = await api('/schools');
    const staff = select.querySelector('option[value="staff"]'); // 로그인 화면의 "사장님·매니저·관리자"는 맨 아래 유지
    schools.forEach(s => {
      const o = document.createElement('option');
      o.value = s.id;
      o.textContent = `${s.name} · ${s.region}`;
      o.dataset.domain = s.emailDomain;
      select.insertBefore(o, staff);
    });
  } catch (e) { toast(e.message); }
}

// ===================== 로그인 화면 =====================
if ($('#loginForm')) {
  loadSchools();
  $('#schoolSelect').onchange = e => {          // 사장·매니저·관리자를 고르면 "학번" → "아이디"
    const staff = e.target.value === 'staff';
    $('#loginIdLabel').textContent = staff ? '아이디' : '학번';
    $('#loginForm [name=loginId]').placeholder = staff ? '예: owner1' : '예: 20261234';
  };
  $('#loginForm').onsubmit = async e => {
    e.preventDefault();
    const b = e.target.querySelector('button[type=submit]');
    if (b.disabled) return;
    b.disabled = true;
    const f = Object.fromEntries(new FormData(e.target));
    try {
      const me = await api('/login', {
        schoolId: f.schoolId === 'staff' ? null : Number(f.schoolId), // 학생이 아니면 학교 없이
        loginId: f.loginId, password: f.password
      });
      toast(`${me.name}님, 반가워요!`);
      const next = me.role === 'OWNER' ? 'manager.html'   // 사장 → 매니저 관리 화면
                 : '/';                     // 연습용: 내 정보 확인 (통합본에선 '/')
      setTimeout(() => location.href = next, 700);
    } catch (err) { toast(err.message); b.disabled = false; }
  };
}

// ===================== 회원가입 화면 =====================
if ($('#signupForm')) {
  loadSchools();

  // ----- 가입 유형 선택: 일반회원 / 사장님 -----
  document.querySelectorAll('[data-type]').forEach(btn => btn.onclick = () => {
    document.querySelectorAll('[data-type]').forEach(x => x.classList.toggle('selected', x === btn)); // 고른 버튼만 초록색
    $('#signupForm').hidden = btn.dataset.type !== 'student';   // 학생 폼 보이기/숨기기
    $('#ownerForm').hidden = btn.dataset.type !== 'owner';      // 사장 폼 보이기/숨기기
  });

  // ----- 일반회원(학생) -----
  const form = $('#signupForm');
  let verifiedEmail = null;                     // 인증을 마친 메일 (메일을 바꾸면 다시 인증)

  $('#schoolSelect').onchange = e => {
    const domain = e.target.selectedOptions[0]?.dataset.domain?.split(',')[0]; // 도메인이 여러 개면 첫 번째를 예시로
    form.email.placeholder = domain ? `학번@${domain}` : '학교 웹메일';
  };

  form.email.oninput = () => {
    verifiedEmail = null;
    $('#verified').hidden = true;
    $('#signupButton').disabled = true;
  };

  $('#sendCode').onclick = async () => {
    const b = $('#sendCode');
    b.disabled = true;
    try {
      const res = await api('/email/code', { schoolId: Number(form.schoolId.value) || null, email: form.email.value });
      $('#codeBox').hidden = false;
      $('#devCode').hidden = !res.devCode;
      $('#devCode').textContent = `시연 모드 인증번호: ${res.devCode ?? ''} (5분 안에 입력)`;
      toast(res.message);
      form.code.focus();
    } catch (err) { toast(err.message); }
    finally { b.disabled = false; }
  };

  $('#verifyCode').onclick = async () => {
    try {
      const res = await api('/email/verify', { email: form.email.value, code: form.code.value });
      verifiedEmail = form.email.value;
      $('#codeBox').hidden = true;
      $('#verified').hidden = false;
      $('#signupButton').disabled = false;
      toast(res.message);
    } catch (err) { toast(err.message); }
  };

  form.onsubmit = async e => {
    e.preventDefault();
    if (!verifiedEmail) return toast('이메일 인증을 먼저 해 주세요.');
    if (form.password.value !== form.password2.value) return toast('비밀번호가 서로 달라요.');
    const b = $('#signupButton');
    if (b.disabled) return;
    b.disabled = true;
    const f = Object.fromEntries(new FormData(form));
    try {
      await api('/signup', {
        schoolId: Number(f.schoolId), studentNo: f.studentNo, password: f.password, name: f.name,
        email: verifiedEmail, phone: f.phone, studentStatus: f.studentStatus
      });
      toast('가입을 환영해요! 로그인해 주세요.');
      setTimeout(() => location.href = 'login.html', 900);
    } catch (err) { toast(err.message); b.disabled = false; }
  };

  // ----- 사장님 (2단계: 계정 → 가게) -----
  const owner = $('#ownerForm');

  function showOwnerStep(step) {                         // 1단계 ↔ 2단계 전환
    $('#ownerStep1').hidden = step !== 1;
    $('#ownerStep2').hidden = step !== 2;
    $('#ownerStepLabel').textContent = step === 1 ? '1 / 2 · 계정 정보' : '2 / 2 · 가게 정보';
    owner.scrollIntoView({ behavior: 'smooth' });
  }

  $('#ownerNext').onclick = () => {                      // [다음]: 1단계 칸부터 확인
    for (const input of $('#ownerStep1').querySelectorAll('input')) {
      if (!input.reportValidity()) return;               // 빈 칸·형식 오류면 브라우저 말풍선 띄우고 멈춤
    }
    if (owner.password.value !== owner.password2.value) return toast('비밀번호가 서로 달라요.');
    showOwnerStep(2);
  };
  $('#ownerPrev').onclick = () => showOwnerStep(1);      // [이전]

  $('#addressSearch').onclick = () => {                  // [주소 검색]: 카카오 우편번호 창
    if (typeof daum === 'undefined' || !daum.Postcode)   // 인터넷이 안 되면 스크립트를 못 불러옴
      return toast('주소 검색을 불러오지 못했어요. 주소를 직접 입력해 주세요.');
    new daum.Postcode({
      oncomplete: data => {                              // 주소를 고르면
        owner.address.value = data.roadAddress || data.jibunAddress;  // 도로명 주소 우선
        owner.addressDetail.focus();                     // 상세 주소 칸으로 이동
      }
    }).open();
  };

  owner.photo.onchange = () => {                         // 사진을 고르면 미리보기
    const file = owner.photo.files[0];
    const img = $('#photoPreview');
    if (!file) { img.hidden = true; return; }
    if (file.size > 5 * 1024 * 1024) {                   // 5MB 넘으면 서버에 보내기 전에 막기
      owner.photo.value = '';
      img.hidden = true;
      return toast('사진은 5MB 이하만 올릴 수 있어요.');
    }
    img.src = URL.createObjectURL(file);                 // 내 컴퓨터 파일을 잠깐 화면에 보여 주는 주소
    img.hidden = false;
  };

  owner.onsubmit = async e => {                          // [사장님으로 가입하기]
    e.preventDefault();
    if (!owner.shopname.value.trim()) return toast('상호를 입력해 주세요.');
    if (!owner.busiKind.value) return toast('업종을 선택해 주세요.');
    if (!owner.address.value.trim()) return toast('가게 주소를 입력해 주세요.');
    const b = owner.querySelector('button[type=submit]');
    if (b.disabled) return;
    b.disabled = true;
    const data = new FormData(owner);                    // 글자 + 사진을 한 번에 담기 (start16과 같음)
    data.delete('password2');                            // 확인용 칸은 서버로 안 보냄
    try {
      const r = await fetch('/api/members/signup/owner', { method: 'POST', body: data }); // ⚠️ Content-Type 직접 쓰지 않기
      let res = null;
      try { res = await r.json(); } catch {}
      if (!r.ok) throw Error(res?.message || '서버 연결을 확인하고 다시 시도해 주세요.');
      toast('가입 완료! 관리자 승인 후 가게가 공개돼요. 로그인해 주세요.');
      setTimeout(() => location.href = 'login.html', 1300);
    } catch (err) {
      toast(err.message);
      if (/아이디|비밀번호|이름|전화번호를|이메일|사업자/.test(err.message) && !/가게 전화번호/.test(err.message))
        showOwnerStep(1);                                // 1단계 칸 문제면 1단계로 되돌리기
      b.disabled = false;
    }
  };
}

// ===================== 매니저 관리 화면 (사장만) =====================
if ($('#managerForm')) {
  const MAX = 3;                                // 서버 MemberService.MAX_MANAGERS와 같게

  async function loadManagers() {               // 내 매니저 목록 그리기
    const list = await api('/managers');
    $('#count').textContent = `(${list.length} / ${MAX})`;
    $('#addButton').disabled = list.length >= MAX;   // 꽉 차면 등록 버튼 끄기
    $('#managerList').innerHTML = list.length
      ? list.map(m => `<div class="store">👤<div><b>${escape(m.name)}</b><small>아이디 ${escape(m.loginId)}</small></div>
          <button type="button" class="tag" data-delete="${m.id}" style="background:#fdecec;color:#c04545">삭제</button></div>`).join('')
      : '<div class="empty" style="margin-top:14px">아직 등록한 매니저가 없어요.</div>';
  }

  (async () => {                                // 화면이 열리면: 로그인한 사람이 사장인지 먼저 확인
    try {
      const me = await api('/me');
      if (me.role !== 'OWNER') throw Error();
      $('#ownerArea').hidden = false;
      $('#ownerName').textContent = me.name;
      $('#avatar').textContent = me.name.slice(0, 1);
      $('#businessNo').textContent = me.businessNo ?? '-';
      $('#max').textContent = MAX;
      await loadManagers();
    } catch {
      $('#notOwner').hidden = false;            // 로그인 안 했거나 사장이 아니면 안내
    }
  })();

  $('#managerForm').onsubmit = async e => {     // 매니저 등록
    e.preventDefault();
    const b = $('#addButton');
    if (b.disabled) return;
    b.disabled = true;
    const f = Object.fromEntries(new FormData(e.target));
    try {
      const m = await api('/managers', { name: f.name, loginId: f.loginId, password: f.password });
      toast(`${m.name} 매니저를 등록했어요.`);
      e.target.reset();
    } catch (err) { toast(err.message); }
    finally { await loadManagers().catch(() => {}); }   // 목록과 버튼 상태 새로 고침
  };

  $('#managerList').onclick = async e => {      // 삭제 버튼 (이벤트 위임: 나중에 생긴 버튼도 처리)
    const btn = e.target.closest('[data-delete]');
    if (!btn || !confirm('이 매니저를 삭제할까요?')) return;
    try {
      const res = await api('/managers/' + btn.dataset.delete, undefined, 'DELETE');
      toast(res.message);
      await loadManagers();
    } catch (err) { toast(err.message); }
  };

  $('#logout').onclick = async () => {          // 로그아웃
    await fetch('/api/members/logout', { method: 'POST' });
    location.href = 'login.html';
  };
}
