/* CaptionForge mobile-first editor. No external JavaScript dependencies. */
'use strict';
const $=s=>document.querySelector(s);
const $$=s=>Array.from(document.querySelectorAll(s));
const E=(s)=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let project=null, activeTrack=null, activeCue=null, panel='Style', colorTarget='textColor';
let undoStack=[], redoStack=[], saveTimer=null, selectedLang='Persian (Afghanistan)', translateMode='Simultaneous';
let runningJob=null, lastTick=-1, busy=false, lastPreviewKey="";
const formats=[
 ['Style','▧'],['Font','Aa'],['Color','◉'],['Animation','✧'],['Follow','◎'],
 ['Translate','文'],['SFX','♫'],['Position','⌖'],['Timing','◷'],['Text flow','▤'],
 ['Overlay','▦'],['Tracks','☰'],['Hide','◉'],['Lock','♧'],['Duplicate','⧉'],['Filters','◐'],
 ['Clear voice','≋'],['Trim','✂'],['Text fix','✎'],['Delete track','♜'] ];
const languages=['Persian (Afghanistan)','Pashto','English','Arabic','Urdu','Hindi','Spanish','French','German','Italian','Portuguese','Russian','Japanese','Korean','Chinese','Turkish'];
const colors=['#ffffff','#050505','#34483e','#708f81','#a2c2a2','#364c67','#4a6984','#b8d0f5','#e2b693','#b58a80','#c5a9a8','#ada4ca','#ffd56c','#ff6a9e','#5a34ff','#22c3bd','#eb6435','#b2ff74'];
const presets=['Bold','Minimal','Story','Cards','Karaoke','Outline','Highlight','Neon','Soft'];
const animations=['None','Fade','Pop','Slide up','Slide down','Bounce','Zoom'];
function toast(msg,duration=3300){const e=$('#toast');e.textContent=msg;e.classList.remove('hidden');clearTimeout(e._timer);e._timer=setTimeout(()=>e.classList.add('hidden'),duration)}
function loading(v,title='Processing',msg=''){busy=v;$('#loading').classList.toggle('hidden',!v);$('#loadingTitle').textContent=title;$('#loadingText').textContent=msg}
function fmt(s){s=Math.max(0,Number(s)||0);return `${String(Math.floor(s/60)).padStart(2,'0')}:${String(Math.floor(s%60)).padStart(2,'0')}`}
function fmtPrec(s){s=Math.max(0,Number(s)||0);return `${fmt(s)}.${Math.floor(s*10)%10}`}
function trimLength(){return project?project.trim.end-project.trim.start:0}
function track(){return project?.tracks.find(t=>t.id===activeTrack)||project?.tracks[0]}
function style(){return track()?.style||{}}
function cue(){return track()?.captions.find(c=>c.id===activeCue)||null}
function copy(o){return JSON.parse(JSON.stringify(o))}
async function request(url,opts={}){
 const r=await fetch(url,opts);let data;const ct=r.headers.get('content-type')||'';
 try{data=ct.includes('application/json')?await r.json():await r.text()}catch{data=null}
 if(!r.ok)throw new Error(typeof data?.detail==='string'?data.detail:typeof data==='string'?data:`Server error ${r.status}`);
 return data;
}
async function listProjects(){try{const all=await request('/api/projects');$('#projectsList').innerHTML=all.length?all.map(p=>`<div class="project-item" data-open="${p.id}"><span class="project-thumb">▣</span><div class="details"><b>${E(p.name)}</b><small>${fmt(p.duration)} · ${new Date(p.updatedAt).toLocaleDateString()}</small></div><button class="del" data-remove="${p.id}" title="Delete project">×</button></div>`).join(''):'<div class="muted">Your first project will appear here</div>'}catch(e){toast(e.message)}}
async function uploadVideo(file){if(!file)return;loading(true,'Importing video','Preparing your editable project...');try{const fd=new FormData();fd.append('file',file);const p=await request('/api/projects',{method:'POST',body:fd});openProject(p)}catch(e){toast(e.message,6500)}finally{loading(false);$('#videoInput').value=''}}
async function openProject(p){
 if(typeof p==='string'){loading(true,'Loading project');try{p=await request('/api/projects/'+p)}catch(e){toast(e.message);return}finally{loading(false)}}
 project=p;activeTrack=project.tracks[0]?.id;activeCue=project.tracks[0]?.captions[0]?.id||null;undoStack=[];redoStack=[];
 $('#home').classList.add('hidden');$('#editor').classList.remove('hidden');
 $('#projectName').textContent=project.name;$('#resolution').value=String(project.resolution||720);
 const v=$('#video');v.src=`/api/projects/${project.id}/video`;v.load();v.onloadedmetadata=()=>{v.currentTime=project.trim.start;};
 v.ontimeupdate=onTimeUpdate;v.onended=()=>{$('#playBtn').textContent='▶'};
 v.onerror=()=>toast('Your browser cannot preview this video codec. Export can still work.',7000);
 panel='Style';draw();
}
function goHome(){$('#video').pause();$('#video').removeAttribute('src');$('#video').load();$('#editor').classList.add('hidden');$('#home').classList.remove('hidden');project=null;listProjects()}
function snapshot(){if(!project||busy)return;undoStack.push(copy({tracks:project.tracks,trim:project.trim,effects:project.effects,resolution:project.resolution}));if(undoStack.length>35)undoStack.shift();redoStack=[]}
function restore(x){if(!project)return;project.tracks=x.tracks;project.trim=x.trim;project.effects=x.effects;project.resolution=x.resolution;activeTrack=project.tracks.some(t=>t.id===activeTrack)?activeTrack:project.tracks[0]?.id;activeCue=track()?.captions[0]?.id||null;saveSoon();draw()}
function undo(){if(!undoStack.length)return;redoStack.push(copy({tracks:project.tracks,trim:project.trim,effects:project.effects,resolution:project.resolution}));restore(undoStack.pop())}
function redo(){if(!redoStack.length)return;undoStack.push(copy({tracks:project.tracks,trim:project.trim,effects:project.effects,resolution:project.resolution}));restore(redoStack.pop())}
function saveSoon(){if(!project)return;clearTimeout(saveTimer);saveTimer=setTimeout(async()=>{try{project=await request(`/api/projects/${project.id}`,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({tracks:project.tracks,trim:project.trim,effects:project.effects,resolution:project.resolution,name:project.name})})}catch(e){toast('Save failed: '+e.message,5000)}},550)}
function changed(){saveSoon();draw()}
function draw(){if(!project)return;$('#totalTime').textContent=fmt(trimLength());$('#sizeRange').value=style().fontSize||25;$('#sizeValue').textContent=style().fontSize||25;drawToolbar();drawPanel();drawTimeline();drawPreview(true);}
function drawToolbar(){ $('#toolbar').innerHTML=formats.map(([name,icon])=>`<button class="tool-btn ${panel===name?'active':''} ${name==='Delete track'?'danger':''}" data-panel="${E(name)}"><span class="icon">${icon}</span><span>${E(name)}</span></button>`).join('');}
function drawTimeline(){
 const root=$('#timelineTracks');let scale=52, width=Math.max(560,Math.ceil(trimLength()*scale)+45);
 $('#rulerMarks').innerHTML=Array.from({length:Math.ceil(trimLength()/2)+1},(_,i)=>`<span>${fmt(i*2)}</span>`).join('');$('#rulerMarks').style.gap=(2*scale-35)+'px';
 root.style.width=width+'px';root.innerHTML=project.tracks.map((tr,i)=>{
  let cues=tr.captions.map(c=>`<button class="cue ${tr.id===activeTrack&&c.id===activeCue?'selected':''}" style="left:${Math.round(c.start*scale)+25}px;width:${Math.max(39,Math.round((c.end-c.start)*scale)-3)}px;--track:${tr.color||'#6545fd'};opacity:${tr.visible===false?.45:1}" data-cue="${E(c.id)}" data-track="${E(tr.id)}" title="${E(c.text)}">${E(c.text)}</button>`).join('');
  return `<div class="track-row" style="width:${width}px"><button class="track-badge" style="background:${tr.color||'#6545fd'}" data-selecttrack="${E(tr.id)}" title="Select ${E(tr.name)}">${i+1}</button>${cues||'<span class="empty-track">No captions — generate or import</span>'}</div>`}).join('');
}
function onTimeUpdate(){if(!project)return;const v=$('#video');if(v.currentTime>=project.trim.end-.05){v.pause();$('#playBtn').textContent='▶';}const rel=Math.max(0,v.currentTime-project.trim.start);$('#currentTime').textContent=fmtPrec(rel);drawPreview();if(Math.round(rel*2)!==lastTick){lastTick=Math.round(rel*2);const x=rel*52;const sc=$('#tracksScroll');if(!v.paused&&x>sc.scrollLeft+sc.clientWidth*.68)sc.scrollLeft=x-sc.clientWidth*.34;}}
function drawPreview(force=false){if(!project)return;const v=$('#video');let sec=Math.max(0,(v.currentTime||0)-project.trim.start);
 const fx=project.effects||{};v.style.filter=`brightness(${1+Number(fx.brightness||0)}) contrast(${Number(fx.contrast??1)}) saturate(${Number(fx.saturation??1)})`;
 const previewKey=project.tracks.map(t=>t.id+':'+(t.visible!==false?t.captions.find(c=>sec>=c.start-(Number(t.style.early)||0)&&sec<=c.end)?.id:'hidden')).join('|');
 if(!force&&previewKey===lastPreviewKey)return;lastPreviewKey=previewKey;
 $('#captionLayer').innerHTML=project.tracks.filter(t=>t.visible!==false).map(tr=>{
  let c=tr.captions.find(a=>sec>=a.start-(Number(tr.style.early)||0)&&sec<=a.end);
  if(!c)return '';
  let st=tr.style||{}, text=c.text||'';let animation=st.animation||'';
  let bg=st.bgEnabled===false?'transparent':st.bgColor||'#181124';let fc=st.textColor||'#ffffff';
  let size=Number(st.fontSize)||25; let cssfont=fontCss(st.font);
  let ani='';let className='anim-'+animation.toLowerCase().replace(/[^a-z]+/g,'-');
  return `<div class="preview-caption ${className}" style="top:${Math.max(10,Math.min(91,(st.yPos??.76)*100))}%;font-size:${Math.max(9,size*.56)}px;font-family:${E(cssfont)};justify-content:${st.align==='left'?'flex-start':st.align==='right'?'flex-end':'center'};${ani}"><span dir="auto" style="color:${fc};background:${bg};opacity:${(st.opacity??100)/100};${st.oneLine?'white-space:nowrap;':''}">${E(text)}</span></div>`}).join('')}
function fontCss(f){const fonts={'Classic':'Georgia,serif','Mono':'monospace','Comic':'cursive','Serif':'Georgia,serif','Noto Sans Arabic':'"Noto Sans Arabic",Tahoma,Arial,sans-serif','Vazirmatn':'Vazirmatn,Tahoma,sans-serif','Nastaliq':'Noto Nastaliq Urdu,serif','Poppins':'Poppins,Inter,sans-serif'};return fonts[f]||'Inter,"Noto Sans Arabic",sans-serif'}
function choosePanel(name){panel=name;drawToolbar();drawPanel()}
function pillGroup(values,current,key,kind='style'){return `<div class="panel-options">${values.map(v=>`<button class="pill ${v===current?'active':''}" data-${kind}="${E(key)}" data-value="${E(v)}">${E(v)}</button>`).join('')}</div>`}
function swatches(key){return `<div class="color-grid">${colors.map(c=>`<button class="color-swatch ${style()[key]===c?'active':''}" data-style="${key}" data-value="${c}" style="background:${c}" title="${c}"></button>`).join('')}</div>`}
function slider(key,label,min,max,step,value,type='style'){return `<label class="panel-label">${E(label)}: <b>${Number(value).toFixed(step<.1?2:step<1?1:0)}</b></label><div class="panel-row"><input type="range" min="${min}" max="${max}" step="${step}" value="${value}" data-${type}-range="${key}"><span style="width:28px;font-size:11px;text-align:right">${value}</span></div>`}
function button(label,action,extra=''){return `<button class="panel-button ${extra}" data-action="${action}">${label}</button>`}
function drawPanel(){if(!project)return;
 const st=style(),fx=project.effects,tr=track(),c=cue();let out='',heading=panel;
 switch(panel){
 case 'Style':out=`<div class="style-grid">${presets.map(p=>`<button class="style-card ${st.preset===p?'active':''}" data-preset="${p}">${p==='Karaoke'?'✨':p==='Neon'?'✦':p==='Outline'?'◇':'Aa'}<span>${p}</span></button>`).join('')}</div><div class="panel-row"><span>Word-by-word highlight</span><input class="switch" type="checkbox" data-toggle="karaoke" ${st.karaoke?'checked':''}></div>`;break;
 case 'Font':out=`<div class="font-grid">${['Noto Sans','Inter','Poppins','Classic','Serif','Mono','Vazirmatn','Noto Sans Arabic','Nastaliq','Comic'].map(f=>`<button class="font-option ${st.font===f?'active':''}" data-style="font" data-value="${f}" style="font-family:${fontCss(f)}">${f==='Noto Sans Arabic'?'سلام دنیا':f==='Vazirmatn'?'متن فارسی':f==='Nastaliq'?'اردو':'Caption Aa'}<small>${f}</small></button>`).join('')}</div><p class="tools-hint">A font must also be installed on the rendering server to appear in exports.</p>`;break;
 case 'Color':out=`${pillGroup(['textColor','bgColor'],colorTarget,'colorTarget','target')}<label class="panel-label">${colorTarget==='textColor'?'Text color':'Box background color'}</label>${swatches(colorTarget)}<div class="panel-row"><span>Custom color</span><input id="customColor" type="color" value="${st[colorTarget]||'#ffffff'}" data-style-color="${colorTarget}"></div><div class="panel-row"><span>Show background box</span><input class="switch" data-toggle="bgEnabled" type="checkbox" ${st.bgEnabled?'checked':''}></div>${slider('opacity','Opacity %',30,100,5,st.opacity??100)}`;break;
 case 'Animation':out=`<div class="style-grid">${animations.map(a=>`<button class="style-card ${st.animation===a?'active':''}" data-style="animation" data-value="${a}">✧<span>${a}</span></button>`).join('')}</div>`;break;
 case 'Follow':out=`<p class="tools-hint">On export, place captions near detected faces when the optional OpenCV engine is installed. Recognition may miss faces or shift between captions.</p><div class="panel-row"><span>Follow face</span><input class="switch" data-toggle="followFace" type="checkbox" ${st.followFace?'checked':''}></div>${button('Check backend capabilities','capabilities','secondary')}`;break;
 case 'Translate':out=`<p class="tools-hint">Generate a translated track with the original timing. Requires OPENAI_API_KEY on the backend.</p><label class="panel-label">How should translations show?</label>${pillGroup(['Replace','Simultaneous'],translateMode,'translateMode','mode')}<label class="panel-label">Translate into</label><div class="panel-options">${languages.map(l=>`<button class="pill ${selectedLang===l?'active':''}" data-language="${E(l)}">${E(l)}</button>`).join('')}</div>${button('✦ Translate captions','translate')}`;break;
 case 'SFX':out=`<label class="panel-label">Sound effect</label>${pillGroup(['None','Pop','Click','Soft'],fx.sfx||'None','sfx','eff')}<label class="panel-label">When to play</label>${pillGroup(['Word','Sentence'],fx.sfxMode||'Sentence','effMode')} ${slider('sfxVolume','Volume %',0,100,5,fx.sfxVolume??40,'eff')}<p class="tools-hint">Sound effects are mixed into the exported audio.</p>`;break;
 case 'Position':out=`${slider('yPos','Vertical position',.1,.92,.01,st.yPos??.76)}<label class="panel-label">Text alignment</label>${pillGroup(['left','center','right'],st.align||'center','align','style')}<div class="panel-row"><span>One line only</span><input class="switch" type="checkbox" data-toggle="oneLine" ${st.oneLine?'checked':''}></div>`;break;
 case 'Timing':out=`${slider('early','Show captions earlier (seconds)',0,1,.05,st.early||0)}<p class="tools-hint">Changes start time of captions without changing the spoken words.</p><div class="panel-row"><span>Word-by-word animation</span><input class="switch" data-toggle="karaoke" type="checkbox" ${st.karaoke?'checked':''}></div>`;break;
 case 'Text flow':out=`<div class="panel-row"><span>Always on one line</span><input class="switch" type="checkbox" data-toggle="oneLine" ${st.oneLine?'checked':''}></div><label class="panel-label">Alignment</label>${pillGroup(['left','center','right'],st.align||'center','align','style')}<p class="tools-hint">For long sentences, keeping multiple lines is easier to read on phones.</p>`;break;
 case 'Overlay':out=`<p class="tools-hint">Choose a subtitle overlay appearance. Some presets also adjust the position and background box.</p><div class="style-grid">${['None','Smart','Bottom','Deep','Full'].map(o=>`<button class="style-card ${st.overlay===o?'active':''}" data-overlay="${o}"><span style="border-radius:5px;background:#362b46;padding:7px 12px">Aa</span><span>${o}</span></button>`).join('')}</div>`;break;
 case 'Tracks':out=`<p class="tools-hint">Select and manage up to five independent subtitle tracks. Import a subtitle file, create a blank track, or generate automatic speech captions.</p><div class="panel-options">${project.tracks.map((t,i)=>`<button class="pill ${t.id===activeTrack?'active':''}" data-selecttrack="${t.id}">${i+1}. ${E(t.name)}</button>`).join('')}</div><div class="shortcut-row">${button('+ Add track','add-track')}${button('✦ Auto subtitles','generate')}${button('Import SRT','import-srt','secondary')}${button('Download SRT','download-srt','secondary')}</div>`;break;
 case 'Hide':out=`<div class="panel-row"><span>Show track in preview and export</span><input class="switch" type="checkbox" data-track-toggle="visible" ${tr.visible!==false?'checked':''}></div>`;break;
 case 'Lock':out=`<div class="panel-row"><span>Lock track to prevent edits</span><input class="switch" type="checkbox" data-track-toggle="locked" ${tr.locked?'checked':''}></div>`;break;
 case 'Duplicate':out=`<p class="tools-hint">Duplicate the current caption track, including timings, styles and text.</p>${button('⧉ Duplicate this track','duplicate')}`;break;
 case 'Filters':out=`<label class="panel-label">Color filter</label><div class="panel-options">${['Original','Vivid','Warm','Cool','Muted'].map(x=>`<button class="pill" data-filter="${x}">${x}</button>`).join('')}</div>${slider('brightness','Brightness',-.5,.5,.05,fx.brightness||0,'eff')}${slider('contrast','Contrast',.5,1.8,.05,fx.contrast??1,'eff')}${slider('saturation','Saturation',0,2,.05,fx.saturation??1,'eff')}${slider('sharpness','Sharpness',0,2,.1,fx.sharpness||0,'eff')}`;break;
 case 'Clear voice':out=`<p class="tools-hint">Remove some background noise from video speech during export. Strong settings may introduce artifacts.</p><label class="panel-label">Noise reduction</label>${pillGroup(['Off','Light','Balanced','Strong'],fx.clearVoice||'Off','clearVoice','eff')}`;break;
 case 'Trim':out=`<p class="tools-hint">Select the part of the video you want to caption and export. Subtitle times are relative to your selected range.</p><div class="range-row"><label>Start (seconds)<input type="number" id="trimStart" min="0" step="0.1" value="${project.trim.start}"></label><label>End (seconds)<input type="number" id="trimEnd" min="0.1" step="0.1" value="${project.trim.end}"></label></div>${button('Apply trim','apply-trim')} ${button('Use whole video','full-video','secondary')}`;break;
 case 'Text fix':out=c?`<p class="tools-hint">Edit the selected caption. To select a different caption, tap a block on the timeline.</p><textarea class="caption-text" id="captionText" placeholder="Edit caption text" ${tr.locked?'disabled':''}>${E(c.text)}</textarea><div class="range-row"><label>Start (seconds)<input id="cueStart" type="number" min="0" step="0.01" value="${c.start}" ${tr.locked?'disabled':''}></label><label>End (seconds)<input id="cueEnd" type="number" min="0.1" step="0.01" value="${c.end}" ${tr.locked?'disabled':''}></label></div><div class="shortcut-row">${button('Save caption','save-cue')} ${button('Delete caption','delete-cue','secondary')} ${button('Split at playhead','split-cue','secondary')}</div>`:`<p class="tools-hint">Select a caption on the timeline to edit its text, or tap + to create a caption.</p>${button('Add caption','add-cue')}`;break;
 case 'Delete track':out=`<p class="tools-hint">Permanently removes this track from the project. You can use Undo immediately afterwards.</p>${button('Delete ${E(tr.name)}','delete-track','danger-button')}`;break;
 }
 $('#panelTitle').textContent=heading;$('#optionsPanel').innerHTML=out;
}
function patchStyle(k,v){let t=track();if(t.locked&&k!=='locked')return toast('This track is locked');snapshot();t.style[k]=v;changed()}
function patchEffect(k,v){snapshot();project.effects[k]=v;changed()}
function activeTime(){return Math.max(0,Math.min(trimLength(),($('#video').currentTime||0)-project.trim.start))}
function addCue(){let t=track();if(t.locked)return toast('Unlock track to edit');snapshot();let now=activeTime(),c={id:'c'+Date.now(),start:Math.round(now*100)/100,end:Math.min(trimLength(),Math.max(now+.5,now+1.5)),text:'New caption',words:[]};t.captions.push(c);t.captions.sort((a,b)=>a.start-b.start);activeCue=c.id;choosePanel('Text fix');changed();}
function applyPreset(p){const data={Bold:{font:'Noto Sans',fontSize:26,bgEnabled:true,textColor:'#ffffff',bgColor:'#20152f',karaoke:false,outline:2},Minimal:{font:'Inter',fontSize:22,bgEnabled:false,textColor:'#ffffff',karaoke:false,outline:1},Story:{font:'Poppins',fontSize:27,bgEnabled:true,textColor:'#f7edc0',bgColor:'#25182c',karaoke:false},Cards:{font:'Inter',fontSize:24,bgEnabled:true,textColor:'#ffffff',bgColor:'#5c38f3'},Karaoke:{font:'Noto Sans',fontSize:27,bgEnabled:true,textColor:'#fff55b',bgColor:'#24132e',karaoke:true},Outline:{font:'Noto Sans',fontSize:26,bgEnabled:false,textColor:'#ffffff',outline:3,karaoke:false},Highlight:{font:'Inter',fontSize:27,bgEnabled:true,textColor:'#000000',bgColor:'#e3f362'},Neon:{font:'Inter',fontSize:29,bgEnabled:true,textColor:'#63ffff',bgColor:'#1d1041'},Soft:{font:'Serif',fontSize:23,bgEnabled:true,textColor:'#eae7ff',bgColor:'#4b4763'}};
 snapshot();Object.assign(track().style,data[p],{preset:p});changed()}
function doFilter(f){snapshot();let e=project.effects;const setting={Original:[0,1,1,0],Vivid:[.04,1.16,1.45,.5],Warm:[.04,1.06,1.20,.1],Cool:[-.04,1.08,.88,.2],Muted:[-.02,.98,.60,.05]}[f]||[0,1,1,0];[e.brightness,e.contrast,e.saturation,e.sharpness]=setting;changed()}
function doOverlay(o){snapshot();Object.assign(track().style,{overlay:o});if(o==='None'){track().style.bgEnabled=false}else if(o==='Bottom'){track().style.yPos=.86;track().style.bgEnabled=true}else if(o==='Deep'){track().style.yPos=.78;track().style.bgEnabled=true}else if(o==='Full'){track().style.yPos=.62;track().style.bgEnabled=true}else if(o==='Smart'){track().style.yPos=.72;track().style.bgEnabled=true}changed()}
function action(a){if(!project)return;const t=track();switch(a){
 case 'add-cue':addCue();break;
 case 'add-track':addTrack();break;
 case 'generate':transcribe();break;
 case 'import-srt':$('#srtInput').click();break;
 case 'download-srt':window.location.href=`/api/projects/${project.id}/srt?track=${activeTrack}`;break;
 case 'apply-trim':{let start=Number($('#trimStart').value),end=Number($('#trimEnd').value);if(!Number.isFinite(start)||!Number.isFinite(end)||end<=start||start<0||end>project.duration)return toast('Check the start and end of the selection');snapshot();let prev=project.trim.start;project.trim={start,end};if(t.captions.length&&prev!==start)toast('Existing captions keep their current relative timestamps. Re-transcribe after a major trim change.',6000);$('#video').currentTime=start;changed();break}
 case 'full-video':snapshot();project.trim={start:0,end:project.duration};$('#video').currentTime=0;changed();break;
 case 'save-cue':{let c=cue();if(!c||t.locked)return;let start=Number($('#cueStart').value),end=Number($('#cueEnd').value);if(start<0||end<=start)return toast('Invalid caption time');snapshot();c.start=start;c.end=end;c.text=$('#captionText').value;c.words=[];changed();break}
 case 'delete-cue':if(t.locked)return toast('Track locked');snapshot();t.captions=t.captions.filter(c=>c.id!==activeCue);activeCue=t.captions[0]?.id||null;changed();break;
 case 'split-cue':{let c=cue(),mid=activeTime();if(!c||t.locked||mid<=c.start+.05||mid>=c.end-.05)return toast('Playhead must be inside the selected caption');snapshot();let nc=copy(c);nc.id='c'+Date.now();nc.start=mid;c.end=mid;c.words=[];nc.words=[];t.captions.push(nc);activeCue=nc.id;changed();break}
 case 'duplicate':if(project.tracks.length>=5)return toast('Maximum five tracks');snapshot();let dup=copy(t);dup.id='t'+Date.now();dup.name=t.name+' copy';dup.color='#8d51f5';dup.captions=dup.captions.map((c,i)=>({...c,id:'d'+Date.now()+i}));project.tracks.push(dup);activeTrack=dup.id;changed();break;
 case 'delete-track':if(project.tracks.length===1)return toast('Keep at least one caption track');snapshot();project.tracks=project.tracks.filter(x=>x.id!==activeTrack);activeTrack=project.tracks[0].id;activeCue=track().captions[0]?.id;changed();break;
 case 'translate':doTranslate();break;
 case 'capabilities':checkCapabilities();break;
 }
}
async function checkCapabilities(){try{let c=await request('/api/config');toast(`Voice recognition: ${c.transcription}. Translation: ${c.translation}. Face tracking: ${c.faceTracking?'available':'install opencv-python-headless'}.`,6500)}catch(e){toast(e.message)}}
async function doTranslate(){if(!track().captions.length)return toast('Generate or import captions first');loading(true,'Translating captions',`Translating into ${selectedLang}...`);try{let r=await request(`/api/projects/${project.id}/translate`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({trackId:activeTrack,language:selectedLang,mode:translateMode})});pollJob(r.jobId,'translate')}catch(e){loading(false);toast(e.message,7000)}}
async function transcribe(){loading(true,'Generating subtitles','Speech recognition analyzes the audio and times every word.');try{const r=await request(`/api/projects/${project.id}/transcribe`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({model:'small',language:null})});pollJob(r.jobId,'transcribe')}catch(e){loading(false);toast(e.message,7000)}}
async function exportMovie(){if(!project)return;loading(true,'Exporting video','Rendering fonts, subtitles, animations and effects with FFmpeg.');try{await request(`/api/projects/${project.id}`,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({tracks:project.tracks,trim:project.trim,effects:project.effects,resolution:Number($('#resolution').value)})});let r=await request(`/api/projects/${project.id}/export`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({resolution:Number($('#resolution').value)})});pollJob(r.jobId,'export')}catch(e){loading(false);toast(e.message,8000)}}
async function pollJob(jid,type){runningJob=jid;let n=0;while(n++<1200){await new Promise(r=>setTimeout(r,1400));try{const res=await request(`/api/jobs/${jid}`);if(res.status==='running'){if(n%7===0)$('#loadingText').textContent=type==='export'?'Video rendering is in progress...':'Processing your subtitles...';continue}loading(false);runningJob=null;if(res.status==='error')throw new Error(res.message);if(type==='export'){toast('Export complete! Your MP4 is ready.',7000);window.location.href=`/api/projects/${project.id}/download`;}else{project=res.result.project;activeTrack=type==='translate'?res.result.trackId:project.tracks[0].id;activeCue=track().captions[0]?.id||null;draw();toast(type==='translate'?'Translation track created.':'Subtitles generated. You can edit every caption.',6000)}return}catch(e){loading(false);runningJob=null;toast(e.message,8000);return}}loading(false);toast('Job is taking too long. Check server logs.');}
async function importSrt(file){if(!file||!project)return;loading(true,'Importing subtitle track');try{const fd=new FormData();fd.append('file',file);let r=await request(`/api/projects/${project.id}/srt`,{method:'POST',body:fd});project=r.project;activeTrack=r.track.id;activeCue=r.track.captions[0]?.id;draw();toast(`Imported ${r.track.captions.length} captions.`)}catch(e){toast(e.message,5000)}finally{loading(false);$('#srtInput').value=''}}
function addTrack(){if(!project)return;if(project.tracks.length>=5)return toast('Maximum five subtitle tracks');snapshot();let id='t'+Date.now(),names=['Original','Translation','Additional'];let tr={id,name:names[Math.min(project.tracks.length,2)]+' '+project.tracks.length,language:'auto',color:['#8c52ff','#ff9f32','#30b6aa'][project.tracks.length%3],visible:true,locked:false,style:copy(track().style),captions:[]};tr.style.yPos=Math.min(.9,tr.style.yPos+.11);project.tracks.push(tr);activeTrack=id;activeCue=null;changed();}
function confirmAction(title,message,onOkay){$('#dialogTitle').textContent=title;$('#dialogText').textContent=message;$('#dialog').classList.remove('hidden');$('#dialogOk').onclick=()=>{$('#dialog').classList.add('hidden');onOkay()};$('#dialogCancel').onclick=()=>$('#dialog').classList.add('hidden')}
// Delegated click handlers, ensuring newly-rendered controls remain functional.
document.addEventListener('click',async ev=>{
 const el=ev.target.closest('button,[data-open],label.new-project');if(!el)return;
 if(el.matches('[data-remove]')){ev.stopPropagation();const id=el.dataset.remove;confirmAction('Delete project?','The original video and captions in this local workspace will be deleted.',async()=>{try{await request('/api/projects/'+id,{method:'DELETE'});listProjects()}catch(e){toast(e.message)}});return}
 if(el.closest('[data-open]')){openProject(el.closest('[data-open]').dataset.open);return}
 if(el.dataset.panel){choosePanel(el.dataset.panel);return}
 if(el.dataset.selecttrack){activeTrack=el.dataset.selecttrack;activeCue=track().captions[0]?.id||null;draw();return}
 if(el.dataset.cue){activeTrack=el.dataset.track;activeCue=el.dataset.cue;let c=cue();if(c)$('#video').currentTime=project.trim.start+c.start;choosePanel('Text fix');draw();return}
 if(el.dataset.preset){applyPreset(el.dataset.preset);return}
 if(el.dataset.target){colorTarget=el.dataset.value;drawPanel();return}
 if(el.dataset.language){selectedLang=el.dataset.language;drawPanel();return}
 if(el.dataset.mode){translateMode=el.dataset.value;drawPanel();return}
 if(el.dataset.filter){doFilter(el.dataset.filter);return}
 if(el.dataset.overlay){doOverlay(el.dataset.overlay);return}
 if(el.dataset.style){patchStyle(el.dataset.style,el.dataset.value);return}
 if(el.dataset.eff){patchEffect(el.dataset.eff,el.dataset.value);return}
 if(el.dataset.effMode){patchEffect('sfxMode',el.dataset.value);return}
 if(el.dataset.action){action(el.dataset.action);return}
 });
document.addEventListener('change',ev=>{const el=ev.target;
 if(el.dataset.toggle){patchStyle(el.dataset.toggle,el.checked);return}
 if(el.dataset.trackToggle){snapshot();track()[el.dataset.trackToggle]=el.checked;changed();return}
 if(el.dataset.styleColor){patchStyle(el.dataset.styleColor,el.value);return}
 if(el.dataset.styleRange){patchStyle(el.dataset.styleRange,Number(el.value));return}
 if(el.dataset.effRange){patchEffect(el.dataset.effRange,Number(el.value));return}
});
$('#videoInput').addEventListener('change',e=>uploadVideo(e.target.files[0]));
$('#srtInput').addEventListener('change',e=>importSrt(e.target.files[0]));
$('#refreshProjects').onclick=listProjects;
$('#backBtn').onclick=goHome;
$('#undoBtn').onclick=undo;$('#redoBtn').onclick=redo;
$('#closePanel').onclick=()=>{panel='Style';drawToolbar();drawPanel()};
$('#settingsBtn').onclick=()=>toast('CaptionForge local workspace · Projects are stored on this server.');
$('#helpBtn').onclick=()=>confirmAction('How it works','Import a video, generate AI subtitles (or import SRT), edit captions and colors, translate, then export an MP4. Install faster-whisper for local AI captions. Use the tools at the bottom to edit.',()=>{});
$('#playBtn').onclick=()=>{const v=$('#video');if(v.paused){if(v.currentTime>=project.trim.end-.1)v.currentTime=project.trim.start;v.play().then(()=>$('#playBtn').textContent='Ⅱ').catch(()=>toast('Unable to play this video type in your browser'))}else{v.pause();$('#playBtn').textContent='▶'}};
$('#soundBtn').onclick=()=>{const v=$('#video');v.muted=!v.muted;$('#soundBtn').textContent=v.muted?'🔇':'🔊'};
$('#addBtn').onclick=addCue;
$('#sizeRange').onchange=e=>patchStyle('fontSize',Number(e.target.value));
$('#sizeRange').oninput=e=>{$('#sizeValue').textContent=e.target.value};
$('#resolution').onchange=e=>{snapshot();project.resolution=Number(e.target.value);saveSoon()};
$('#exportBtn').onclick=exportMovie;
$('#quickSettings').onclick=()=>choosePanel('Position');$('#quickVoice').onclick=()=>choosePanel('Clear voice');
$('#editorTip').onclick=()=>{confirmAction('Caption tools','Use Generate captions to transcribe, Import SRT for existing subtitles, or add a manual caption with +. The bottom toolbar provides the editors.',()=>{})};
$('#tracksScroll').onscroll=e=>{$('#rulerMarks').style.transform=`translateX(${-e.target.scrollLeft}px)`};
// Entry action bar: accessible from home and editor via keyboard or visible prompt.
window.addEventListener('keydown',e=>{if(!project||['INPUT','TEXTAREA','SELECT'].includes(document.activeElement.tagName))return;if(e.code==='Space'){e.preventDefault();$('#playBtn').click()}if((e.ctrlKey||e.metaKey)&&e.key==='z'){e.preventDefault();undo()}});
// Render an action strip above the timeline on opening, using the editor banner.
const tip=$('#editorTip');tip.innerHTML=`<span>✦ Edit subtitles on the timeline</span> <span style="float:right;display:flex;gap:9px"><button id="generateQuick" style="background:#673dff;border:0;color:white;font-weight:bold;border-radius:9px;padding:4px 8px;font-size:10px">Generate AI</button><button id="importQuick" style="background:#30234d;border:0;color:#fff;border-radius:9px;padding:4px 8px;font-size:10px">Import SRT</button></span>`;
$('#generateQuick').onclick=e=>{e.stopPropagation();transcribe()};$('#importQuick').onclick=e=>{e.stopPropagation();$('#srtInput').click()};
listProjects();

if('serviceWorker' in navigator && location.protocol !== 'file:') navigator.serviceWorker.register('/static/service-worker.js').catch(()=>{});
