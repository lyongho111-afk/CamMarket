// Campus community: real post routes and server-backed comments.
const communityState={sort:'latest',commentSort:'oldest',csrf:null,csrfMember:null,sequence:0};

function communityIcon(name){
  const icon=el('span','c-icon');icon.setAttribute('aria-hidden','true');
  const paths={comment:'<path d="M20 11.5a7.5 7.5 0 0 1-7.5 7.5H5l-3 3V11.5A7.5 7.5 0 0 1 9.5 4h3A7.5 7.5 0 0 1 20 11.5Z"/>',search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',pen:'<path d="m15 4 5 5M4 20l5-1L21 7a2 2 0 0 0-5-5L4 14Z"/>'};
  icon.innerHTML='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">'+paths[name]+'</svg>';return icon;
}
function communityTime(value){
  const time=timestamp(value),minutes=Math.max(0,Math.floor((Date.now()-time)/60000));
  if(!time)return '방금 전';if(minutes<1)return '방금 전';if(minutes<60)return minutes+'분 전';if(minutes<1440)return Math.floor(minutes/60)+'시간 전';if(minutes<10080)return Math.floor(minutes/1440)+'일 전';return new Date(time).toLocaleDateString('ko-KR');
}
function communityName(p){return p.authorName||'캠퍼스 이웃'}
function communitySchool(p){return p.schoolName||currentCampus.name}
function communityOwn(p){return currentSession.authenticated&&(p.authorMemberId!=null?Number(p.authorMemberId)===Number(currentSession.member.id):p.writer===actorKey())}
function communityNavigate(path){history.pushState(null,'','#'+path);render();$('#content').scrollIntoView({behavior:'instant'});requestAnimationFrame(()=>{const heading=$('#results h2');if(heading){heading.tabIndex=-1;heading.focus({preventScroll:true})}})}
function communityOpen(id){communityNavigate('community/'+id)}
function communityForm(p=null){if(!requireLogin())return;communityNavigate(p?'community/edit/'+(p.postId||p.id):'community/new')}
async function communityApi(path,method='GET',body){
  if(method==='GET')return api('boards'+path);
  if(!communityState.csrf||communityState.csrfMember!==currentSession.member?.id){const session=await api('boards/session');communityState.csrf=session.csrfToken;communityState.csrfMember=currentSession.member?.id}
  const response=await fetch('/api/boards'+path,{method,headers:{'Content-Type':'application/json','X-CSRF-Token':communityState.csrf},body:body===undefined?undefined:JSON.stringify(body)});
  const raw=await response.text();let data;try{data=raw?JSON.parse(raw):null}catch{}
  if(!response.ok)throw Error(data?.message||'저장하지 못했어요. 잠시 후 다시 시도해 주세요.');return data;
}
function communityAvatar(name,small=false){const avatar=el('span','c-avatar'+(small?' small':''),(name||'캠').slice(0,1));avatar.setAttribute('aria-hidden','true');return avatar}
function communityCategory(next){category=next;communityNavigate('community')}
function communitySidebar(){
  const aside=el('aside','c-sidebar'),heading=el('h3','','이야기 카테고리'),nav=el('nav');nav.setAttribute('aria-label','커뮤니티 카테고리');
  ['전체',...boardCategories].forEach(name=>{const button=actionButton('',()=>communityCategory(name),false,'c-category'+(category===name?' active':''));button.append(el('span','',name));const count=posts.filter(p=>name==='전체'||p.category===name).length;button.append(el('small','',String(count)));button.setAttribute('aria-pressed',String(category===name));nav.append(button)});
  aside.append(heading,nav,el('div','c-sidebar-note','같은 캠퍼스라 더 가까운 이야기.\n서로를 존중하는 말을 나눠요.'));return aside;
}
function communityRail(){
  const aside=el('aside','c-rail'),card=el('div','c-rail-card');card.append(el('span','c-mini-label','OUR CAMPUS'),el('h3','','함께 채우는\n우리 학교 이야기'),el('p','','점심 메뉴부터 수업 고민까지,\n가까운 이웃에게 물어보세요.'));
  const write=actionButton('글쓰기',()=>communityForm(),false,'c-write');write.prepend(communityIcon('pen'));card.append(write);
  const topics=el('div','c-rail-topics');topics.append(el('h3','','어떤 이야기를 찾으세요?'));const chips=el('div','c-topic-chips');['맛집 추천','가성비','취업팁','고민상담','자유게시판'].forEach(name=>chips.append(actionButton(name,()=>communityCategory(name),false,'')));topics.append(chips,el('p','','궁금한 주제를 선택해 보세요.'));
  aside.append(card,topics);return aside;
}
function renderCommunity(){
  const sequence=++communityState.sequence;document.body.dataset.page='community';$('#search').placeholder='캠퍼스 이야기를 검색해 보세요';
  $('.section-head').hidden=true;$('.controls').hidden=true;$('#page-return').hidden=true;
  const root=$('#results');root.className='community-view';root.replaceChildren();
  const breadcrumb=el('div','c-breadcrumb');breadcrumb.append(actionButton('홈',()=>switchTab('market'),false,''),el('span','','›'),actionButton('커뮤니티',()=>communityNavigate('community'),false,''),el('span','c-school-pill','● '+(currentSession.member?.schoolName||currentCampus.name)));
  const layout=el('div','c-layout'),main=el('div','c-main');layout.append(communitySidebar(),main);root.append(breadcrumb,layout);
  const route=location.hash.slice(1).split('/');
  if(route[1]==='new'||route[1]==='edit'){layout.classList.add('c-layout-detail');renderCommunityEditor(main,route[1]==='edit'?Number(route[2]):null,sequence);return}
  if(route[1]){layout.classList.add('c-layout-detail');renderCommunityDetail(main,Number(route[1]),sequence);return}
  layout.append(communityRail());renderCommunityFeed(main);
}
function communityHeading(main,title,subtitle){const intro=el('div','c-intro');intro.append(el('span','c-mini-label','CAMPUS TALK'),el('h2','',title),el('p','',subtitle));main.append(intro)}
function renderCommunityFeed(main){
  communityHeading(main,'우리 캠퍼스의 오늘 이야기','궁금한 것부터 소소한 일상까지, 이웃과 나눠요.');
  const toolbar=el('div','c-feed-toolbar'),search=el('form','c-search');search.setAttribute('role','search');const input=el('input');input.type='search';input.setAttribute('aria-label','커뮤니티 검색');input.placeholder='어떤 이야기가 궁금하세요?';input.value=$('#search').value;const find=el('button');find.type='submit';find.setAttribute('aria-label','검색');find.append(communityIcon('search'));search.append(input,find);
  let searchTimer;input.oninput=()=>{clearTimeout(searchTimer);searchTimer=setTimeout(()=>{$('#search').value=input.value;updateList()},150)};search.onsubmit=e=>{e.preventDefault();clearTimeout(searchTimer);$('#search').value=input.value;updateList()};
  const write=actionButton('글쓰기',()=>communityForm(),false,'c-write');write.prepend(communityIcon('pen'));toolbar.append(search,write);main.append(toolbar);
  const filterbar=el('div','c-feed-filter'),count=el('span'),sort=el('select');sort.setAttribute('aria-label','게시글 정렬');sort.append(new Option('최신순','latest'),new Option('댓글 많은 순','comments'));sort.value=communityState.sort;sort.onchange=()=>{communityState.sort=sort.value;updateList()};filterbar.append(count,sort);const list=el('div','c-feed');main.append(filterbar,list);
  function updateList(){
    const q=$('#search').value.trim().toLowerCase();let data=posts.filter(p=>(category==='전체'||p.category===category)&&[p.title,p.body,p.authorName].join(' ').toLowerCase().includes(q));
    data.sort((a,b)=>(communityState.sort==='comments'?(b.commentCount||0)-(a.commentCount||0):0)||b.id-a.id);
    count.replaceChildren(el('strong','',category==='전체'?'전체 이야기':category),document.createTextNode(' '+data.length+'개'));list.replaceChildren();
    if(!loaded){list.append(el('p','c-empty','이야기를 불러오고 있어요…'));return}
    if(!data.length){const empty=el('div','c-empty');empty.append(el('strong','',q?'검색 결과가 없어요':'아직 올라온 이야기가 없어요'),el('p','',q?'다른 검색어나 카테고리로 찾아보세요.':'첫 번째 이야기를 들려주세요.'),actionButton('글쓰기',()=>communityForm(),false,'c-text-button'));list.append(empty);return}
    data.forEach(p=>list.append(communityPostRow(p)));
  }updateList();
}
function communityPostRow(p){
  const row=el('a','c-post');row.href='#community/'+p.id;row.onclick=e=>{if(e.ctrlKey||e.metaKey||e.shiftKey||e.altKey)return;e.preventDefault();communityOpen(p.id)};
  const top=el('div','c-post-top');top.append(el('span','c-category-badge',p.category),el('span','c-post-time',communityTime(p.regtime)));
  row.append(top,el('h3','',p.title),el('p','c-post-excerpt',p.body));const bottom=el('div','c-post-bottom'),by=el('span','c-post-by');by.append(communityAvatar(communityName(p),true),document.createTextNode(communityName(p)+' · '+communitySchool(p)));const comments=actionButton('',()=>communityOpen(p.id),false,'c-comment-count');comments.setAttribute('aria-label',p.title+' 댓글 '+(p.commentCount||0)+'개 보기');comments.append(communityIcon('comment'),document.createTextNode('댓글 '+(p.commentCount||0)));const actions=el('div','c-post-actions');actions.append(comments,engagementControls('POST',p.id,p.title));bottom.append(by,actions);const wrapper=el('article','engagement-post');wrapper.append(row,bottom);return wrapper;
}
function communityBack(main){const row=el('div','c-back-row');row.append(actionButton('← 목록으로',()=>communityNavigate('community'),false,'c-text-button'),actionButton('새 글쓰기',()=>communityForm(),false,'c-text-button'));main.append(row)}
async function renderCommunityDetail(main,id,sequence){
  communityBack(main);const loading=el('p','c-empty','이야기를 불러오고 있어요…');main.append(loading);
  try{
    if(!Number.isInteger(id)||id<1)throw Error('게시글을 찾을 수 없어요.');
    const [post,comments]=await Promise.all([communityApi('/'+id),communityApi('/'+id+'/comments')]);
    if(sequence!==communityState.sequence||!main.isConnected)return;loading.remove();
    const article=el('article','c-article'),author=el('div','c-author'),authorInfo=el('div');authorInfo.append(el('strong','',communityName(post)),el('p','',communitySchool(post)+' · '+communityTime(post.regtime)));author.append(communityAvatar(communityName(post)),authorInfo);
    const categoryLink=actionButton(post.categoryName,()=>communityCategory(post.categoryName),false,'c-category-badge');article.append(categoryLink,author,el('h2','c-article-title',post.title),el('div','c-article-body',post.content));
    const foot=el('div','c-article-footer'),commentCount=el('span','c-comment-count');commentCount.append(communityIcon('comment'),document.createTextNode('댓글 '+comments.length));foot.append(commentCount,el('time','',dateTime(timestamp(post.regtime))));article.append(foot);
    if(communityOwn(post)){const actions=el('div','c-author-actions');actions.append(actionButton('수정하기',()=>communityForm(post),false,'c-text-button'),actionButton('삭제하기',()=>communityDeletePost(post),false,'c-text-button danger'));article.append(actions)}article.append(engagementControls('POST',id,post.title));main.append(article);
    renderCommunityComments(main,post,comments);
    const related=posts.filter(p=>p.id!==id&&(category==='전체'||p.category===category)).slice(0,3);if(related.length){const more=el('section','c-related');more.append(el('h3','','캠퍼스의 다른 이야기'));related.forEach(p=>more.append(communityPostRow(p)));main.append(more)}
  }catch(error){if(sequence===communityState.sequence&&main.isConnected){loading.className='c-empty c-error';loading.textContent=error.message}}
}
function renderCommunityComments(main,post,comments){
  const section=el('section','c-comments');section.setAttribute('aria-label','댓글');const header=el('div','c-comments-head'),sort=el('select');sort.setAttribute('aria-label','댓글 정렬');sort.append(new Option('등록순','oldest'),new Option('최신순','latest'));sort.value=communityState.commentSort;header.append(el('h3','','댓글 '+comments.length),sort);section.append(header);
  const list=el('div','c-comment-list');section.append(list);sort.onchange=()=>{communityState.commentSort=sort.value;drawComments()};
  function drawComments(){list.replaceChildren();if(!comments.length){const empty=el('div','c-comment-empty');empty.append(communityIcon('comment'),el('strong','','아직 댓글이 없어요'),el('p','','가장 먼저 따뜻한 한마디를 남겨보세요.'));list.append(empty);return}
    const data=communityState.commentSort==='latest'?[...comments].reverse():comments;data.forEach(comment=>{
      const row=el('article','c-comment'),body=el('div','c-comment-body'),head=el('div','c-comment-author');head.append(el('strong','',comment.authorName));if(comment.memberId===post.authorMemberId)head.append(el('span','c-writer-label','작성자'));head.append(el('small','',communityTime(comment.regtime)));body.append(head,el('p','',comment.content));
      if(currentSession.authenticated&&Number(comment.memberId)===Number(currentSession.member.id))body.append(actionButton('삭제',()=>communityDeleteComment(post.postId,comment.commentId),false,'c-comment-delete'));
      body.append(engagementControls('COMMENT',comment.commentId,comment.content.slice(0,35)));row.append(communityAvatar(comment.authorName,true),body);list.append(row);
    });
  }drawComments();
  if(!currentSession.authenticated){const gate=el('div','c-login-prompt');gate.append(el('p','','로그인하고 캠퍼스 이웃과 이야기를 나눠보세요.'),actionButton('로그인하고 댓글 쓰기',()=>requireLogin(),false,'c-write'));section.append(gate);main.append(section);return}
  const form=el('form','c-comment-form'),label=el('label','','댓글 남기기'),textarea=el('textarea');textarea.name='content';textarea.required=true;textarea.maxLength=1000;textarea.placeholder='이웃에게 도움이 되는 댓글을 남겨주세요.';label.append(textarea);const bottom=el('div','c-form-bottom'),counter=el('span','c-character-count','0 / 1,000'),error=el('p','c-form-error');error.setAttribute('role','alert');const submit=el('button','c-write','댓글 등록');submit.type='submit';textarea.oninput=()=>counter.textContent=textarea.value.length.toLocaleString()+' / 1,000';bottom.append(counter,submit);form.append(label,error,bottom);
  form.onsubmit=async e=>{e.preventDefault();if(!textarea.value.trim()){error.textContent='댓글 내용을 입력해 주세요.';textarea.focus();return}submit.disabled=true;error.textContent='';try{await communityApi('/'+post.postId+'/comments','POST',{content:textarea.value.trim()});await loadData();toast('댓글을 등록했어요.');requestAnimationFrame(()=>$('.c-comments')?.scrollIntoView({block:'start'}))}catch(err){error.textContent=err.message;submit.disabled=false}};section.append(form);main.append(section);
}
function communityDeletePost(post){confirmAction('게시글을 삭제할까요?','게시글과 댓글이 함께 삭제됩니다.',async()=>{await communityApi('/'+post.postId,'DELETE');communityNavigate('community')},'삭제하기')}
function communityDeleteComment(postId,commentId){confirmAction('내 댓글을 삭제할까요?','삭제한 댓글은 되돌릴 수 없어요.',()=>communityApi('/'+postId+'/comments/'+commentId,'DELETE'),'삭제하기')}
async function renderCommunityEditor(main,id,sequence){
  communityBack(main);if(!currentSession.authenticated){communityHeading(main,'함께 나누고 싶은 이야기가 있나요?','로그인 후 글을 작성할 수 있어요.');main.append(actionButton('로그인하고 글쓰기',()=>requireLogin(),false,'c-write'));return}
  let post=null;
  if(id){const loading=el('p','c-empty','게시글을 불러오고 있어요…');main.append(loading);try{post=await communityApi('/'+id);if(sequence!==communityState.sequence||!main.isConnected)return;loading.remove();if(!communityOwn(post))throw Error('내가 쓴 게시글만 수정할 수 있어요.')}catch(error){loading.remove();main.append(el('p','c-empty c-error',error.message));return}}
  communityHeading(main,id?'이야기 수정하기':'우리 캠퍼스에 이야기 나누기','정보도, 질문도, 소소한 일상도 좋아요.');
  const form=el('form','c-editor'),categoryLabel=el('label','','카테고리'),select=el('select');select.name='categoryId';boardCategories.forEach((name,i)=>select.append(new Option(name,boardCategoryIds[i])));select.value=post?.categoryId||(category!=='전체'?boardCategoryIds[boardCategories.indexOf(category)]:10);categoryLabel.append(select);
  const titleLabel=el('label','','제목'),title=el('input');title.name='title';title.required=true;title.maxLength=150;title.placeholder='이야기의 제목을 적어주세요';title.value=post?.title||'';titleLabel.append(title);
  const bodyLabel=el('label','','내용'),body=el('textarea');body.name='content';body.required=true;body.maxLength=3000;body.placeholder='이웃과 나누고 싶은 이야기를 들려주세요.\n서로를 존중하는 따뜻한 말을 사용해 주세요.';body.value=post?.content||'';bodyLabel.append(body);
  const counter=el('span','c-character-count'),error=el('p','c-form-error');error.setAttribute('role','alert');const bottom=el('div','c-editor-bottom'),cancel=actionButton('취소',()=>communityNavigate(id?'community/'+id:'community'),false,'c-cancel'),submit=el('button','c-write',id?'수정 저장':'게시글 등록');submit.type='submit';body.oninput=()=>counter.textContent=body.value.length.toLocaleString()+' / 3,000';body.oninput();bottom.append(cancel,submit);form.append(categoryLabel,titleLabel,bodyLabel,counter,el('p','c-editor-note','개인 연락처 등 민감한 정보는 게시글에 남기지 마세요.'),error,bottom);
  form.onsubmit=async e=>{e.preventDefault();if(!title.value.trim()||!body.value.trim()){error.textContent='제목과 내용을 모두 입력해 주세요.';return}submit.disabled=true;error.textContent='';try{const saved=await communityApi(id?'/'+id:'',id?'PUT':'POST',{categoryId:Number(select.value),title:title.value.trim(),content:body.value.trim()});await loadData();communityOpen(saved.postId);toast(id?'게시글을 수정했어요.':'새 이야기를 등록했어요.')}catch(err){error.textContent=err.message;submit.disabled=false}};main.append(form);
}
