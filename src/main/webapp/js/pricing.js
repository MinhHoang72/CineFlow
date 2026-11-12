(() => {
    'use strict';
    const centreSearch = document.getElementById('cp-search');
    if (centreSearch) {
        const city = document.getElementById('cp-city'), priceState = document.getElementById('cp-price-state');
        const rows = [...document.querySelectorAll('tr[data-centre-id]')];
        const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').toLocaleLowerCase('vi').trim();
        [...new Set(rows.map(row => row.dataset.city).filter(Boolean))].sort((a,b) => a.localeCompare(b,'vi')).forEach(name => city.append(new Option(name,name)));
        const filterCentres = () => {
            let count = 0;
            rows.forEach(row => {
                const data = row.dataset;
                row.hidden = !normalize(data.centreName).includes(normalize(centreSearch.value))
                    || Boolean(city.value && data.city !== city.value)
                    || (priceState.value === 'set' && data.priceSet !== 'true')
                    || (priceState.value === 'unset' && data.priceSet !== 'false')
                    || (priceState.value === 'scheduled' && data.priceScheduled !== 'true');
                if (!row.hidden) count++;
            });
            document.getElementById('cp-result-count').textContent = count === rows.length ? count : `${count} / ${rows.length}`;
            document.getElementById('cp-empty').hidden = count !== 0;
        };
        centreSearch.addEventListener('input',filterCentres);
        [city,priceState].forEach(input => input.addEventListener('change',filterCentres));
        document.getElementById('cp-reset').addEventListener('click', () => {
            centreSearch.value = ''; city.value = ''; priceState.value = ''; filterCentres(); centreSearch.focus();
        });
    }
    const historyFrom = document.getElementById('bp-history-from');
    if (historyFrom) {
        const historyTo = document.getElementById('bp-history-to');
        const updater = document.getElementById('bp-history-updater');
        const rows = [...document.querySelectorAll('[data-price-history]')];
        let historyPage = 1, filteredHistory = rows;
        const previous = document.getElementById('bp-history-prev'), next = document.getElementById('bp-history-next');
        const renderHistory = () => {
            const count = filteredHistory.length, pages = Math.max(1,Math.ceil(count / 5));
            historyPage = Math.max(1,Math.min(historyPage,pages));
            const start = (historyPage - 1) * 5, visible = new Set(filteredHistory.slice(start,start + 5));
            rows.forEach(row => { row.hidden = !visible.has(row); });
            document.getElementById('bp-history-count').textContent = count === rows.length ? count : `${count} / ${rows.length}`;
            document.getElementById('bp-history-empty').hidden = count !== 0;
            document.getElementById('bp-history-range').textContent = `Hiển thị ${count ? start + 1 : 0}–${Math.min(start + 5,count)} / ${count}`;
            document.getElementById('bp-history-page').textContent = `Trang ${historyPage} / ${pages}`;
            previous.disabled = historyPage <= 1; next.disabled = historyPage >= pages;
        };
        [...new Set(rows.map(row => row.dataset.updater))].sort((a,b) => a.localeCompare(b,'vi')).forEach(name => updater.append(new Option(name,name)));
        const filterHistory = () => {
            const invalid = Boolean(historyFrom.value && historyTo.value && historyFrom.value > historyTo.value);
            document.getElementById('bp-history-filter-error').hidden = !invalid;
            historyTo.setAttribute('aria-invalid', String(invalid));
            if (invalid) return;
            filteredHistory = rows.filter(row => !((historyFrom.value && row.dataset.effectiveDate < historyFrom.value)
                    || (historyTo.value && row.dataset.effectiveDate > historyTo.value)
                    || (updater.value && row.dataset.updater !== updater.value)));
            historyPage = 1; renderHistory();
        };
        [historyFrom,historyTo,updater].forEach(input => input.addEventListener('change',filterHistory));
        document.getElementById('bp-history-reset').addEventListener('click', () => {
            historyFrom.value = ''; historyTo.value = ''; updater.value = ''; filterHistory(); historyFrom.focus();
        });
        previous.addEventListener('click', () => { historyPage--; renderHistory(); });
        next.addEventListener('click', () => { historyPage++; renderHistory(); });
        renderHistory();
    }
    const policyEditor = document.getElementById('policy-editor');
    const policyAdd = document.getElementById('policy-add');
    if (policyEditor && policyAdd) {
        const focusEditor = () => document.getElementById('policy-editor-title').focus({preventScroll:true});
        policyAdd.addEventListener('click', event => {
            event.preventDefault();
            policyEditor.hidden = false;
            policyAdd.hidden = true;
            policyAdd.setAttribute('aria-expanded', 'true');
            policyEditor.scrollIntoView({block:'start', behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'});
            focusEditor();
        });
        if (!policyEditor.hidden) focusEditor();
    }
    const moneyInputs = [...document.querySelectorAll('[data-money]')];
    const digits = value => value.replace(/\./g, '');
    function formatMoney(input, preserveCaret = true) {
        const raw = input.value;
        if (!/^[0-9.]*$/.test(raw)) {
            input.setCustomValidity('Chỉ nhập số tiền VNĐ không âm.'); return;
        }
        const position = input.selectionStart || 0;
        const before = raw.slice(0, position).replace(/[^0-9]/g, '').length;
        let number = digits(raw).replace(/^0+(?=\d)/, '');
        input.setCustomValidity(number.length > 10 ? 'Số tiền tối đa 9.999.999.999 đ.' : '');
        input.value = number.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
        if (preserveCaret && document.activeElement === input) {
            let caret = 0, count = 0;
            while (caret < input.value.length && count < before) {
                if (/\d/.test(input.value[caret])) count++;
                caret++;
            }
            input.setSelectionRange(caret, caret);
        }
    }
    moneyInputs.forEach(input => {
        const wrapper = document.createElement('div'); wrapper.className = 'pr-money-input';
        input.before(wrapper); wrapper.append(input);
        const unit = document.createElement('span'); unit.textContent = 'đ'; unit.setAttribute('aria-hidden', 'true'); wrapper.append(unit);
        input.addEventListener('input', () => formatMoney(input));
        input.addEventListener('beforeinput', event => {
            const at = input.selectionStart;
            if (input.selectionEnd !== at) return;
            if (event.inputType === 'deleteContentBackward' && at > 1 && input.value[at - 1] === '.') {
                event.preventDefault(); input.value = input.value.slice(0, at - 2) + input.value.slice(at);
                input.setSelectionRange(at - 2, at - 2); formatMoney(input);
            } else if (event.inputType === 'deleteContentForward' && input.value[at] === '.') {
                event.preventDefault(); input.value = input.value.slice(0, at) + input.value.slice(at + 2);
                input.setSelectionRange(at, at); formatMoney(input);
            }
        });
        formatMoney(input, false);
    });
    document.querySelectorAll('form').forEach(form => {
        form.addEventListener('formdata', event => {
            form.querySelectorAll('[data-money][name]').forEach(input => {
                if (/^[0-9.]+$/.test(input.value)) event.formData.set(input.name, digits(input.value));
            });
        });
        if (form.dataset.confirm) form.addEventListener('submit', event => {
            if (!window.confirm(form.dataset.confirm)) event.preventDefault();
        });
    });
    const scheduled = document.getElementById('pr-effective-from');
    if (scheduled) {
        const modes = [...document.querySelectorAll('[name="effectiveMode"]')];
        const updateMode = () => {
            const isScheduled = modes.some(input => (input.tagName === 'SELECT' || input.checked) && input.value === 'scheduled');
            modes.forEach(input => input.setAttribute('aria-expanded', String(isScheduled)));
            document.getElementById('pr-effective-field').hidden = !isScheduled;
            scheduled.disabled = !isScheduled; scheduled.required = isScheduled;
            document.getElementById('pr-save-price').textContent = isScheduled ? 'Lên lịch áp dụng' : 'Cập nhật giá';
            scheduled.min = new Intl.DateTimeFormat('sv-SE', {timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).format(new Date(Date.now() + 60000)).replace(' ', 'T');
        };
        modes.forEach(input => input.addEventListener('change', updateMode)); updateMode();
    }
    const dialog = document.getElementById('pr-surcharge-dialog');
    if (dialog) {
        const open = document.getElementById('pr-open-surcharge');
        const type = document.getElementById('pr-rule-type');
        const target = document.getElementById('pr-rule-target');
        const targetOptions = [...target.querySelectorAll('option[data-target-type]')];
        const initialTarget = target.value;
        const updateTargets = (selectedValue = '') => {
            const options = targetOptions.filter(option => option.dataset.targetType === type.value);
            const placeholder = new Option(options.length ? 'Chọn loại ' + (type.value === 'HALL' ? 'phòng' : 'ghế') : 'Chưa có loại được cấu hình', '');
            target.replaceChildren(placeholder, ...options.map(option => option.cloneNode(true)));
            target.value = options.some(option => option.value === selectedValue) ? selectedValue : '';
            target.disabled = options.length === 0;
            dialog.querySelector('[type="submit"]').disabled = options.length === 0;
            document.getElementById('pr-target-help').textContent = options.length
                ? 'Danh sách lấy từ cấu hình ' + (type.value === 'HALL' ? 'phòng chiếu.' : 'ghế; không gồm STANDARD.')
                : 'Chưa có loại phù hợp. Vui lòng cấu hình ghế / phòng trước khi thêm phụ thu.';
        };
        type.addEventListener('change', () => updateTargets()); updateTargets(initialTarget);
        open.addEventListener('click', () => dialog.showModal());
        dialog.querySelectorAll('[data-close-surcharge]').forEach(button => button.addEventListener('click', () => dialog.close()));
        dialog.addEventListener('close', () => open.focus());
        if (dialog.dataset.reopen === 'true') dialog.showModal();
    }
    const policyMode=document.getElementById('policy-mode'),policyFrom=document.getElementById('policy-from');
    if(policyMode && policyFrom){
        const policyField=document.getElementById('policy-effective-field');
        const sync=()=>{
            const isScheduled=policyMode.value==='scheduled';
            policyField.hidden=!isScheduled;
            policyMode.setAttribute('aria-expanded',String(isScheduled));
            policyFrom.disabled=!isScheduled;
            policyFrom.required=isScheduled;
            if(isScheduled)policyFrom.min=new Intl.DateTimeFormat('sv-SE',{timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).format(new Date(Date.now()+60000)).replace(' ','T');
        };
        policyMode.addEventListener('change',sync);sync();
    }
    // A single HH:mm field; hour/minute choices live in an accessible native dialog.
    document.querySelectorAll('.pr-page input[type="datetime-local"], .pr-page input[type="time"], input[data-time-picker]').forEach(source=>{
        const hasDate=source.type==='datetime-local',hasSeconds=!hasDate&&source.step==='1';
        const wrapper=document.createElement('div');wrapper.className=hasDate?'pr-datetime24':'pr-time-picker';
        let date=null;
        if(hasDate){date=document.createElement('input');date.type='date';date.id=source.id+'-date';date.setAttribute('aria-label',source.dataset.dateLabel||'Ngày áp dụng');date.dataset.calendarPicker='';wrapper.append(date);}
        const picker=hasDate?document.createElement('div'):wrapper;picker.classList.add('pr-time-picker');
        const clock=document.createElement('input');clock.type='text';clock.id=source.id+'-clock';clock.placeholder='HH:mm';clock.inputMode='none';clock.className='pr-time-value';clock.pattern='(?:[01][0-9]|2[0-3]):[0-5][0-9]';clock.setAttribute('aria-label','Thời gian, định dạng 24 giờ');clock.setAttribute('aria-haspopup','dialog');
        const icon=document.createElement('span');icon.className='pr-clock-icon';icon.textContent='◷';icon.setAttribute('aria-hidden','true');picker.append(clock,icon);
        const dialog=document.createElement('dialog');dialog.className='pr-clock-dialog';dialog.id=source.id+'-picker';clock.setAttribute('aria-controls',dialog.id);
        const heading=document.createElement('div');heading.className='pr-clock-heading';
        const title=document.createElement('h2');title.textContent='Chọn thời gian';title.id=dialog.id+'-title';dialog.setAttribute('aria-labelledby',title.id);
        const close=document.createElement('button');close.type='button';close.className='pr-clock-close';close.textContent='×';close.setAttribute('aria-label','Đóng chọn thời gian');heading.append(title,close);
        const choices=document.createElement('div');choices.className='pr-clock-choices';
        const makeSelect=(suffix,label,count)=>{
            const cell=document.createElement('div'),caption=document.createElement('label'),select=document.createElement('select');
            select.id=source.id+'-'+suffix;caption.htmlFor=select.id;caption.textContent=label;
            for(let n=0;n<count;n++){const value=String(n).padStart(2,'0');select.append(new Option(value,value));}
            cell.append(caption,select);choices.append(cell);return select;
        };
        const hour=makeSelect('hour','Giờ (00–23)',24),minute=makeSelect('minute','Phút',60);
        const actions=document.createElement('div');actions.className='pr-clock-actions';
        const cancel=document.createElement('button');cancel.type='button';cancel.className='st-button st-button-muted';cancel.textContent='Hủy';
        const done=document.createElement('button');done.type='button';done.className='st-button st-button-primary';done.textContent='Chọn';actions.append(cancel,done);dialog.append(heading,choices,actions);picker.append(dialog);
        if(hasDate)wrapper.append(picker);
        const initialValue=source.value;let writingValue=false;
        source.type='hidden';source.after(wrapper);
        const controls=[date,clock].filter(Boolean),label=document.querySelector(`label[for="${source.id}"]`);if(label)label.htmlFor=controls[0].id;
        const readValue=()=>{
            const parts=source.value.split('T');if(date)date.value=parts[0]||'';
            clock.value=((hasDate?parts[1]:parts[0])||'').slice(0,5);
        };
        const validate=()=>{
            const control=date||clock;control.setCustomValidity('');
            if(!source.disabled&&source.value&&source.min&&source.value<source.min)control.setCustomValidity('Chọn thời gian sau '+source.min.replace('T',' ')+'.');
            if(!source.disabled&&source.value&&source.max&&source.value>source.max)control.setCustomValidity('Thời gian vượt giới hạn cho phép.');
        };
        const writeValue=()=>{
            const previousClock=(hasDate?source.value.split('T')[1]:source.value)||'';
            // Keep legacy seconds when HH:mm is unchanged; newly chosen minutes start at :00.
            const seconds=hasSeconds?(previousClock.slice(0,5)===clock.value&&previousClock.length>5?previousClock.slice(5):':00'):'';
            source.value=controls.every(control=>control.value!=='')?(hasDate?date.value+'T':'')+clock.value+seconds:'';
            validate();writingValue=true;
            try{source.dispatchEvent(new Event('input',{bubbles:true}));source.dispatchEvent(new Event('change',{bubbles:true}));}finally{writingValue=false;}
        };
        const syncState=()=>{
            controls.forEach(control=>{control.disabled=source.disabled;control.required=source.required&&!source.disabled;});
            if(date){date.min=source.min?source.min.split('T')[0]:'';date.max=source.max?source.max.split('T')[0]:'';}
            if(source.disabled&&dialog.open)dialog.close();validate();
        };
        const open=()=>{
            if(clock.disabled)return;hour.value=clock.value.slice(0,2)||'00';minute.value=clock.value.slice(3,5)||'00';
            if(!dialog.open){dialog.showModal();hour.focus();}
        };
        const apply=()=>{clock.value=hour.value+':'+minute.value;writeValue();dialog.close();};
        clock.addEventListener('click',open);
        clock.addEventListener('keydown',event=>{
            if(['Enter',' ','ArrowDown'].includes(event.key)){event.preventDefault();open();}
            else if(event.key.length===1||['Backspace','Delete','ArrowUp','ArrowLeft','ArrowRight'].includes(event.key))event.preventDefault();
        });
        ['beforeinput','paste','drop'].forEach(name=>clock.addEventListener(name,event=>event.preventDefault()));
        done.addEventListener('click',apply);close.addEventListener('click',()=>dialog.close());cancel.addEventListener('click',()=>dialog.close());
        dialog.addEventListener('close',()=>{if(!clock.disabled)clock.focus({preventScroll:true});});
        dialog.addEventListener('keydown',event=>{if(event.key==='Enter'&&event.target.tagName==='SELECT'){event.preventDefault();apply();}});
        dialog.addEventListener('click',event=>{if(event.target===dialog){const r=dialog.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)dialog.close();}});
        if(date)date.addEventListener('change',writeValue);
        source.addEventListener('change',()=>{if(!writingValue){readValue();syncState();}});
        new MutationObserver(syncState).observe(source,{attributes:true,attributeFilter:['disabled','required','min','max']});
        source.value=initialValue;readValue();syncState();
        if(source.form)source.form.addEventListener('reset',()=>setTimeout(()=>{readValue();syncState();},0));
    });
    // Keep the native calendar accessible; unsupported browsers retain their normal input.
    document.querySelectorAll('.pr-page input[type="date"], .pr-page input[type="time"], .pr-page input[type="datetime-local"], input[data-calendar-picker]').forEach(input=>{
        if(typeof input.showPicker!=='function')return;
        let pickerAvailable=true;
        input.inputMode='none';
        const openPicker=event=>{
            if(input.disabled||!pickerAvailable)return;
            try{input.showPicker();event.preventDefault();}catch(error){pickerAvailable=false;input.inputMode='';}
        };
        input.addEventListener('click',openPicker);
        input.addEventListener('keydown',event=>{
            if(!pickerAvailable)return;
            if(['Enter',' ','ArrowDown'].includes(event.key)){openPicker(event);return;}
            if(event.key==='Tab'||event.key==='Escape'||((event.ctrlKey||event.metaKey)&&['a','c'].includes(event.key.toLowerCase())))return;
            if(event.key.length===1||['Backspace','Delete','ArrowUp','ArrowLeft','ArrowRight'].includes(event.key))event.preventDefault();
        });
        ['beforeinput','paste','drop'].forEach(name=>input.addEventListener(name,event=>{if(pickerAvailable)event.preventDefault();}));
    });
    const inspectButtons = [...document.querySelectorAll('[data-seat-inspect]')];
    if (inspectButtons.length) {
        const byId = id => document.getElementById(id);
        const currency = value => value === '' || value === undefined ? 'Chưa có giá' : new Intl.NumberFormat('vi-VN').format(Number(value)) + ' đ';
        const types = {STANDARD:'Ghế thường', VIP:'VIP', SWEETBOX:'Sweetbox'};
        inspectButtons.forEach(button => button.addEventListener('click', () => {
            inspectButtons.forEach(item => item.setAttribute('aria-pressed', String(item === button)));
            const seat = button.dataset;
            byId('st-seat-price-hint').hidden = true;
            byId('st-seat-price-detail').hidden = false;
            byId('st-inspected-seat').textContent = `Ghế ${seat.label} · ${types[seat.type] || seat.type}`;
            byId('st-inspected-status').textContent = seat.statusLabel;
            for (const key of ['base','centre','movie','time','hall','seat','total']) byId(`st-price-${key}`).textContent = currency(seat[key]);
            byId('st-price-unavailable').hidden = seat.base !== '';
            byId('st-saved-price').hidden = seat.stored === '';
            byId('st-saved-label').textContent = seat.status === 'HELD' ? 'Giá đã lưu khi giữ ghế' : 'Giá đã lưu lúc đặt';
            byId('st-price-stored').textContent = currency(seat.stored);
            byId('st-seat-booking-note').textContent = seat.status === 'MAINTENANCE' ? 'Ghế đang bảo trì / không phục vụ, không thể đặt.'
                : seat.status === 'BOOKED' ? (seat.stored === '' ? 'Ghế đã đặt. Không tìm thấy giá lưu trên đơn.' : 'Ghế đã có người đặt.')
                : seat.status === 'HELD' ? 'Ghế đang được giữ trên một đơn đặt vé.' : 'Ghế còn trống.';
            byId('st-seat-price-announcement').textContent = `Ghế ${seat.label}, ${seat.statusLabel}. Giá cấu hình: ${currency(seat.total)}${seat.stored === '' ? '' : '. Giá lưu trên đơn: ' + currency(seat.stored)}`;
            if (matchMedia('(max-width: 900px)').matches) {
                byId('st-seat-price-title').focus({preventScroll:true});
                byId('st-seat-price-panel').scrollIntoView({block:'nearest',behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'});
            }
        }));
    }
})();
