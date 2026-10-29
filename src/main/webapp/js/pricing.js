(() => {
    'use strict';
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
            const isScheduled = modes.some(input => input.checked && input.value === 'scheduled');
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
            for (const key of ['base','time','hall','seat','total']) byId(`st-price-${key}`).textContent = currency(seat[key]);
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
