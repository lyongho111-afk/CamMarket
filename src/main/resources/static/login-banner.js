// 로그인 화면: 학교를 고르면 그 학교 사진 배너를 보여 준다.
// 사진 위치: static/school-photos/학교이름.jpg      (예: 인하대학교.jpg)
// 약도 위치: static/school-photos/학교이름-약도.jpg  (있는 학교만. 있으면 "캠퍼스 약도 보기" 버튼이 생김)

let lastRequest = 0;       // 학교를 빠르게 바꿀 때 늦게 도착한 그림이 덮어쓰지 않게 구분하는 번호
let showingMap = false;    // 지금 약도를 보여 주는 중인지
let photoUrl = '';         // 현재 학교 사진 주소
let mapUrl = '';           // 현재 학교 약도 주소 (없으면 빈 글자)

function imageUrl(name, suffix) {                    // 파일 주소 만들기 (한글 이름은 주소용으로 변환)
  return 'school-photos/' + encodeURIComponent(name + suffix) + '.jpg';
}

function exists(url, done) {                         // 그 그림 파일이 실제로 있는지 확인
  const img = new Image();
  img.onload = () => done(true);                     // 불러와지면 있음
  img.onerror = () => done(false);                   // 안 되면 없음
  img.src = url;
}

function render() {                                  // 지금 상태(사진/약도)대로 배너 모양 바꾸기
  const img = document.getElementById('schoolImg');
  const toggle = document.getElementById('schoolImgToggle');
  if (showingMap && mapUrl) {                        // 약도 모드: 글씨가 읽히게 전체를 크게 보여 줌
    img.src = mapUrl;
    img.style.height = 'auto';
    img.style.maxHeight = '420px';
    img.style.objectFit = 'contain';
    toggle.textContent = '학교 사진 보기';
  } else {                                           // 사진 모드: 배너 높이에 꽉 채움
    img.src = photoUrl;
    img.style.height = '190px';
    img.style.maxHeight = 'none';
    img.style.objectFit = 'cover';
    toggle.textContent = '캠퍼스 약도 보기';
  }
  toggle.hidden = !mapUrl;                           // 약도 파일이 있는 학교만 버튼 보임
}

async function showSchoolBanner() {                        // 학교 선택이 바뀔 때마다 실행
  const select = document.getElementById('schoolSelect');
  const box = document.getElementById('schoolBanner');
  const option = select.options[select.selectedIndex];
  const request = ++lastRequest;                     // 이번 선택의 번호
  if (!option || !option.value || isNaN(Number(option.value))) { // 학교가 아니면(사장님·매니저·관리자·미선택)
    box.hidden = true;                               // 배너 숨김
    return;
  }
  const name = option.textContent.split(' · ')[0].trim(); // 목록 글자가 "학교 · 지역"이라 앞부분(학교 이름)만 사용
  const details=await fetch('/api/members/schools').then(r=>r.json()).then(rows=>rows.find(s=>String(s.id)===String(option.value))).catch(()=>null);
  if(request!==lastRequest)return;
  const photo = details?.photo?'/api/school-images/'+encodeURIComponent(details.photo):imageUrl(name, '');
  const map = details?.mapImage?'/api/school-images/'+encodeURIComponent(details.mapImage):imageUrl(name, '-약도');
  exists(photo, hasPhoto => {                        // 사진 파일 확인
    exists(map, hasMap => {                          // 약도 파일 확인
      if (request !== lastRequest) return;           // 그 사이 다른 학교를 골랐으면 무시
      if (!hasPhoto && !hasMap) { box.hidden = true; return; } // 둘 다 없으면 배너 숨김
      photoUrl = hasPhoto ? photo : map;             // 사진이 없으면 약도를 대신 보여 줌
      mapUrl = hasPhoto && hasMap ? map : '';        // 둘 다 있을 때만 전환 버튼
      showingMap = !hasPhoto;                        // 사진이 없으면 처음부터 약도 모드
      render();
      box.hidden = false;
    });
  });
}

document.getElementById('schoolSelect').addEventListener('change', showSchoolBanner); // 기존 onchange와 따로 동작
document.getElementById('schoolImgToggle').addEventListener('click', () => { // 버튼: 사진 ↔ 약도 전환
  showingMap = !showingMap;
  render();
});
