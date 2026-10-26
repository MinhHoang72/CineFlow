(() => {
    'use strict';
    const $ = id => document.getElementById(id);
    const search = $('st-search'), date = $('st-date'), hall = $('st-hall'), status = $('st-status');
    if (!search || !date || !hall || !status) return;
    let rows = Array.from(document.querySelectorAll('[data-showtime]'));
    let page = 1, view = 'table', refreshing = false;
    const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLocaleLowerCase('vi');
    const localDate = value => `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
    const midnight = value => new Date(`${value}T00:00:00`);
    const nextDay = value => { const copy = new Date(value); copy.setDate(copy.getDate() + 1); return copy; };
    const parse = value => new Date(value.replace(' ', 'T'));
    const clock = value => value.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    const fullTime = value => value.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' });
    const today = () => localDate(new Date());
    $('st-timeline-date').value = today();
    const hallNames = new Map();
    Array.from($('hallId').options).filter(option => option.value).forEach(option => hallNames.set(option.value, option.textContent.trim()));
    rows.forEach(row => hallNames.set(row.dataset.hallId, row.dataset.hall));
    for (const [id, name] of [...hallNames].sort((a, b) => a[1].localeCompare(b[1], 'vi'))) {
        const option = document.createElement('option'); option.value = id; option.textContent = name; hall.append(option);
    }
    const bounds = row => {
        const start = parse(row.dataset.start), end = parse(row.dataset.end);
        return { start, end, busy: new Date(end.getTime() + Number(row.dataset.buffer || 0) * 60000) };
    };
    const matches = (row, withDate = true) => {
        if (!normalize(row.dataset.title).includes(normalize(search.value.trim()))
            || (hall.value && row.dataset.hallId !== hall.value)
            || (status.value && row.dataset.status !== status.value)) return false;
        if (!withDate) return true;
        const range = date.value ? [midnight(date.value), nextDay(midnight(date.value))] : null;
        if (!range) return true;
        const { start, end } = bounds(row);
        return start < range[1] && end > range[0];
    };
    const element = (tag, cls, text) => { const node = document.createElement(tag); if (cls) node.className = cls; if (text !== undefined) node.textContent = text; return node; };
    function renderTimeline() {
        const grid = $('st-timeline-grid'); grid.replaceChildren();
        const day = midnight($('st-timeline-date').value || today()), tomorrow = nextDay(day), span = tomorrow - day;
        const ruler = element('div', 'st-time-ruler'); ruler.append(element('div', 'st-lane-name', 'Phòng / Giờ'));
        const ticks = element('div', 'st-time-ticks');
        for (let hour = 0; hour <= 24; hour++) { const tick = element('span', '', `${String(hour).padStart(2, '0')}:00`); tick.style.left = `${hour / 24 * 100}%`; ticks.append(tick); }
        ruler.append(ticks); grid.append(ruler);
        let count = 0, conflictCount = 0;
        const matching = rows.filter(row => matches(row, false));
        for (const [id, name] of hallNames) {
            if (hall.value && id !== hall.value) continue;
            const slots = matching.filter(row => row.dataset.hallId === id).map(row => ({ row, ...bounds(row) }))
                .filter(slot => slot.start < tomorrow && slot.busy > day).sort((a, b) => a.start - b.start);
            const line = element('div', 'st-time-lane'); line.append(element('div', 'st-lane-name', name));
            const track = element('div', 'st-lane-track');
            const levels = [];
            slots.forEach(slot => {
                count++;
                const active = slot.row.dataset.status !== 'CANCELLED';
                const conflict = active && slots.some(other => other !== slot && other.row.dataset.status !== 'CANCELLED' && slot.start < other.busy && slot.busy > other.start);
                if (conflict) conflictCount++;
                let level = levels.findIndex(end => end <= slot.start); if (level < 0) level = levels.length; levels[level] = slot.busy;
                const block = element('a', `st-time-block st-time-${slot.row.dataset.status}${conflict ? ' is-conflict' : ''}`);
                block.href = slot.row.querySelector('a').href;
                block.style.left = `${Math.max(0, (slot.start - day) / span * 100)}%`;
                block.style.width = `${Math.min(100, (Math.min(slot.busy, tomorrow) - Math.max(slot.start, day)) / span * 100)}%`;
                block.style.top = `${level * 58 + 9}px`;
                block.title = `${slot.row.dataset.title}\n${fullTime(slot.start)} – ${fullTime(slot.end)}\nDọn phòng đến ${fullTime(slot.busy)}${conflict ? '\nCảnh báo: trùng lịch hoặc thời gian dọn phòng' : ''}`;
                block.setAttribute('aria-label', `${name}: ${block.title}`);
                const movie = element('span', 'st-time-movie', slot.row.dataset.title);
                const time = element('small', '', `${clock(slot.start)} – ${clock(slot.end)}`);
                block.append(movie, time);
                const buffer = element('span', 'st-time-buffer');
                buffer.style.width = `${Math.max(0, Math.min(100, (Math.min(slot.busy, tomorrow) - Math.max(slot.end, day)) / (Math.min(slot.busy, tomorrow) - Math.max(slot.start, day)) * 100))}%`;
                block.append(buffer); track.append(block);
            });
            track.style.height = `${Math.max(1, levels.length) * 58 + 18}px`;
            if (!slots.length) track.append(element('span', 'st-free-room', 'Chưa có suất chiếu trong ngày'));
            line.append(track); grid.append(line);
        }
        $('st-timeline-summary').textContent = `${count} suất chiếu trong ngày${conflictCount ? ` · ${conflictCount} suất có xung đột (gồm thời gian dọn phòng)` : ''}. Bấm vào suất chiếu để xem ghế.`;
    }
    function render() {
        const filtered = rows.filter(row => matches(row));
        const size = Number($('st-page-size').value), pages = Math.max(1, Math.ceil(filtered.length / size)); page = Math.min(page, pages);
        const start = (page - 1) * size, visible = new Set(filtered.slice(start, start + size));
        rows.forEach(row => { row.hidden = !visible.has(row); });
        $('st-empty').hidden = view !== 'table' || filtered.length !== 0;
        $('st-result-count').textContent = view === 'table' ? `Hiển thị ${filtered.length ? start + 1 : 0}–${Math.min(start + size, filtered.length)} / ${filtered.length} suất chiếu` : 'Lịch theo phòng · gồm thời gian dọn phòng';
        $('st-page-info').textContent = `Trang ${page} / ${pages}`;
        $('st-prev-page').disabled = page <= 1; $('st-next-page').disabled = page >= pages;
        $('st-pagination').hidden = view !== 'table';
        const displayDate = value => value.toLocaleDateString('vi-VN');
        $('st-range-label').textContent = view === 'timeline'
            ? `Lịch phòng ngày ${displayDate(midnight($('st-timeline-date').value))}`
            : `${date.value ? `Ngày chiếu: ${displayDate(midnight(date.value))}` : 'Tất cả ngày chiếu'} · ${filtered.length} suất chiếu`;
        document.querySelectorAll('[data-quick]').forEach(button => {
            const tomorrow = localDate(nextDay(midnight(today())));
            const selected = date.value === (button.dataset.quick === 'today' ? today() : tomorrow);
            button.setAttribute('aria-pressed', String(selected));
        });
        $('st-table-view').hidden = view !== 'table'; $('st-timeline').hidden = view !== 'timeline';
        document.querySelectorAll('[data-view]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.view === view)));
        if (view === 'timeline') renderTimeline();
    }
    const filter = () => { page = 1; render(); };
    search.addEventListener('input', filter); [hall, status].forEach(control => control.addEventListener('change', filter));
    date.addEventListener('change', () => { if (date.value) $('st-timeline-date').value = date.value; document.querySelectorAll('[data-quick]').forEach(button => button.setAttribute('aria-pressed', 'false')); filter(); });
    $('st-reset').addEventListener('click', () => { [search, date, hall, status].forEach(control => { control.value = ''; }); $('st-timeline-date').value = today(); document.querySelectorAll('[data-quick]').forEach(button => button.setAttribute('aria-pressed', 'false')); filter(); search.focus(); });
    document.querySelectorAll('[data-quick]').forEach(button => {
        button.setAttribute('aria-pressed', 'false');
        button.addEventListener('click', () => {
            const selected = midnight(today());
            if (button.dataset.quick === 'tomorrow') selected.setDate(selected.getDate() + 1);
            date.value = localDate(selected);
            $('st-timeline-date').value = localDate(selected);
            document.querySelectorAll('[data-quick]').forEach(item => item.setAttribute('aria-pressed', String(item === button))); filter();
        });
    });
    document.querySelectorAll('[data-view]').forEach(button => button.addEventListener('click', () => { view = button.dataset.view; render(); }));
    $('st-timeline-date').addEventListener('change', () => { if (!$('st-timeline-date').value) $('st-timeline-date').value = today(); date.value = $('st-timeline-date').value; filter(); });
    $('st-page-size').addEventListener('change', filter);
    $('st-prev-page').addEventListener('click', () => { page--; render(); }); $('st-next-page').addEventListener('click', () => { page++; render(); });
    document.addEventListener('submit', event => { if (event.target.matches('[data-cancel-showtime]') && !window.confirm('Bạn có chắc muốn hủy suất chiếu này?')) event.preventDefault(); });

    const dialog = $('create-showtime'), form = $('st-create-form'), submit = $('st-create-submit'), feedback = $('st-availability');
    let checkController, checkVersion = 0, checkTimer;
    const formatInput = value => fullTime(parse(value));
    function feedbackState(message, state) { feedback.textContent = message; feedback.className = `st-availability ${state || ''}`; }
    async function checkAvailability() {
        const version = ++checkVersion; if (checkController) checkController.abort();
        const movie = $('movieId').value, room = $('hallId').value, start = $('startTime').value;
        if (!movie || !room || !start) { submit.disabled = movieId.options.length <= 1 || hallId.options.length <= 1; feedbackState('Chọn phim, phòng và giờ bắt đầu để kiểm tra lịch trống.'); return false; }
        checkController = new AbortController(); submit.disabled = true; feedbackState('Đang kiểm tra lịch phòng…', 'is-pending');
        try {
            const url = new URL(window.location.href); url.searchParams.set('action', 'availability'); url.searchParams.set('movieId', movie); url.searchParams.set('hallId', room); url.searchParams.set('startTime', start);
            const response = await fetch(url, { credentials: 'same-origin', cache: 'no-store', signal: checkController.signal });
            if (response.redirected || !response.headers.get('content-type')?.includes('application/json')) throw Error('Session unavailable');
            const result = await response.json(); if (version !== checkVersion) return false;
            if (!response.ok) { feedbackState(result.error || 'Không kiểm tra được lịch phòng.', 'is-error'); return false; }
            if (result.available) { feedbackState(`Phòng trống trong khung giờ đã chọn. Kết thúc ${formatInput(result.endTime)}, dọn phòng đến ${formatInput(result.cleaningUntil)}.`, 'is-free'); submit.disabled = false; return true; }
            const first = result.conflicts[0]; feedbackState(`Trùng với “${first.title}” (${formatInput(first.start)} – ${formatInput(first.end)}), phòng bận đến ${formatInput(first.busyUntil)}. Vui lòng đổi phòng hoặc thời gian.`, 'is-error'); return false;
        } catch (error) { if (error.name !== 'AbortError' && version === checkVersion) feedbackState('Chưa kiểm tra được lịch phòng. Hãy thử lại hoặc đăng nhập lại.', 'is-error'); return false; }
    }
    $('st-open-create').addEventListener('click', () => { dialog.showModal(); $('movieId').focus(); checkAvailability(); });
    document.querySelectorAll('[data-close-create]').forEach(button => button.addEventListener('click', () => dialog.close()));
    dialog.addEventListener('close', () => { ++checkVersion; if (checkController) checkController.abort(); clearTimeout(checkTimer); $('st-open-create').focus(); });
    ['movieId', 'hallId', 'startTime'].forEach(id => $(id).addEventListener('input', () => { clearTimeout(checkTimer); ++checkVersion; if (checkController) checkController.abort(); submit.disabled = true; checkTimer = window.setTimeout(checkAvailability, 300); }));
    form.addEventListener('submit', async event => { event.preventDefault(); clearTimeout(checkTimer); if (await checkAvailability()) { submit.disabled = true; HTMLFormElement.prototype.submit.call(form); } });
    render();

    async function refreshStatuses() {
        if (document.hidden || refreshing) return; refreshing = true;
        try {
            const response = await fetch(window.location.href, { credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'text/html' } });
            if (!response.ok || response.redirected) return;
            const documentCopy = new DOMParser().parseFromString(await response.text(), 'text/html');
            const latest = new Map(Array.from(documentCopy.querySelectorAll('[data-showtime]')).map(row => [row.dataset.showtimeId, row]));
            rows.forEach(row => {
                const updated = latest.get(row.dataset.showtimeId);
                if (!updated || row.contains(document.activeElement)) return;
                if (updated.dataset.status !== row.dataset.status || updated.dataset.start !== row.dataset.start || updated.dataset.end !== row.dataset.end) row.replaceWith(updated);
            });
            rows = Array.from(document.querySelectorAll('[data-showtime]')); render();
            if (dialog.open) checkAvailability();
        } catch (error) { console.warn('Không thể cập nhật lịch chiếu.', error); }
        finally { refreshing = false; }
    }
    window.setInterval(refreshStatuses, 30000);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) refreshStatuses(); });
})();
