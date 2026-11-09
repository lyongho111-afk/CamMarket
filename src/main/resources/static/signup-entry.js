// 원본 signup.html/member.js는 유지하고, 캡처에 해당하는 사장님 가게 정보부터 표시합니다.
(()=>{
 function init(){
  const query=new URLSearchParams(location.search);
  if(query.get('type')==='student')return;
  const form=document.querySelector('#ownerForm');
  const ownerTab=document.querySelector('[data-type="owner"]');
  const first=document.querySelector('#ownerStep1');
  const second=document.querySelector('#ownerStep2');
  if(!form||!ownerTab||!first||!second)return;
  ownerTab.click();
  if(query.get('step')==='1')return;
  first.hidden=true;second.hidden=false;
  document.querySelector('#ownerStepLabel').textContent='2 / 2 · 가게 정보';
  // 캡처 화면에서 시작해도 필수 계정 정보를 생략한 가입은 허용하지 않습니다.
  form.noValidate=true;
  form.addEventListener('submit',event=>{
   const invalid=[...first.querySelectorAll('input')].find(input=>!input.checkValidity());
   const mismatch=form.elements.password.value!==form.elements.password2.value;
   if(first.hidden&&(invalid||mismatch)){
    event.preventDefault();event.stopImmediatePropagation();
    document.querySelector('#ownerPrev').click();
    if(typeof toast==='function')toast(mismatch?'비밀번호가 서로 달라요.':'가입을 완료하려면 계정 정보를 먼저 입력해 주세요. 가게 정보는 유지됩니다.');
    if(invalid){invalid.focus();invalid.reportValidity();}
    return;
   }
   if(!first.hidden){
    event.preventDefault();event.stopImmediatePropagation();
    document.querySelector('#ownerNext').click();
   }
  },true);
 }
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init,{once:true});else init();
})();
