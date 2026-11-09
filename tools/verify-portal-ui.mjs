import fs from 'node:fs';
import vm from 'node:vm';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const base=fileURLToPath(new URL('../',import.meta.url));
let checks=0;
for(const site of ['owner','admin']){
 const element={classList:{toggle(){},add(){},remove(){}},replaceChildren(){},addEventListener(){},setAttribute(){},focus(){}};
 const calls=[];
 const context=vm.createContext({document:{querySelector:()=>element,querySelectorAll:()=>[],createElement:()=>({...element}),createTextNode:text=>({textContent:text})},window:{addEventListener(){}},SITE:site,setTimeout,clearTimeout,AbortController,fetch:async(url,options)=>{calls.push({url,...options});return {ok:true,status:200,json:async()=>({ok:true})}},console});
 vm.runInContext(fs.readFileSync(base+'src/main/resources/portal-sites/'+site+'/common.js','utf8'),context);
 vm.runInContext("state={csrfToken:'session-csrf'}",context);
 await vm.runInContext("api('members/managers/123','DELETE')",context);
 assert.equal(calls.at(-1).headers['X-CSRF-Token'],'session-csrf');assert.equal(calls.at(-1).body,undefined);checks++;
 await vm.runInContext("api('owner/dashboard')",context);assert.equal(calls.at(-1).headers['X-CSRF-Token'],undefined);checks++;
 await vm.runInContext("api('owner/shop/status','PUT',{status:'혼잡',version:3})",context);assert.deepEqual(JSON.parse(calls.at(-1).body),{status:'혼잡',version:3});assert.equal(calls.at(-1).headers['X-CSRF-Token'],'session-csrf');checks++;
 for(const [input,expected]of [['=1+1',"\"'=1+1\""],['  @SUM(A1)',"\"'  @SUM(A1)\""],['메뉴, "커피"','"메뉴, ""커피"""'],['일반 텍스트','"일반 텍스트"']]){context.testInput=input;assert.equal(vm.runInContext('csvCell(testInput)',context),expected);checks++}
 context.fetch=async()=>({ok:false,status:409,json:async()=>({message:'최신 정보 충돌'})});await assert.rejects(vm.runInContext("api('owner/shop/status','PUT',{})",context),error=>error.status===409&&error.message==='최신 정보 충돌');checks++;
 context.dirtyForm={getClientRects:()=>[{}],values:[['note','changed']]};context.FormData=class{constructor(form){return form.values}};context.document.querySelectorAll=()=>[context.dirtyForm];
 vm.runInContext("formSnapshots.set(dirtyForm,JSON.stringify([['note','initial']]))",context);assert.equal(vm.runInContext('hasChanges()',context),true);checks++;
 context.dirtyForm.getClientRects=()=>[];assert.equal(vm.runInContext('hasChanges()',context),false);checks++;
}
const result={checks,failed:0,scope:'Shared owner/admin helpers: bodyless DELETE CSRF, GET headers, versioned PUT, CSV quoting/formula neutralization, conflict errors, visible dirty forms and closed dialog exclusion'};
fs.writeFileSync(base+'docs/v13_프런트엔드_자동검증.json',JSON.stringify(result,null,2));console.log(result);
