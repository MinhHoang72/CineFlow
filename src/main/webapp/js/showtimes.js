(() => {
    'use strict';
    const rows = Array.from(document.querySelectorAll('[data-showtime]'));
    const search = document.getElementById('st-search');
    const date = document.getElementById('st-date');
    const hall = document.getElementById('st-hall');
    const status = document.getElementById('st-status');
    if (!search || !date || !hall || !status) return;
    const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLocaleLowerCase('vi');
    const halls = new Map(rows.map(row => [row.dataset.hallId, row.dataset.hall]));
    for (const [id, name] of [...halls].sort((a, b) => a[1].localeCompare(b[1], 'vi'))) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = name;
        hall.append(option);
    }
    const filter = () => {
        const term = normalize(search.value.trim());
        let visible = 0;
        rows.forEach(row => {
            const match = normalize(row.dataset.title).includes(term)
                && (!date.value || row.dataset.date === date.value)
                && (!hall.value || row.dataset.hallId === hall.value)
                && (!status.value || row.dataset.status === status.value);
            row.hidden = !match;
            if (match) visible++;
        });
        document.getElementById('st-empty').hidden = visible !== 0;
        document.getElementById('st-result-count').textContent = `Hiển thị ${visible} / ${rows.length} suất chiếu`;
    };
    search.addEventListener('input', filter);
    [date, hall, status].forEach(control => control.addEventListener('change', filter));
    document.getElementById('st-reset').addEventListener('click', () => {
        [search, date, hall, status].forEach(control => { control.value = ''; });
        filter();
        search.focus();
    });
    document.querySelectorAll('[data-cancel-showtime]').forEach(form => {
        form.addEventListener('submit', event => {
            if (!window.confirm('Bạn có chắc muốn hủy suất chiếu này?')) event.preventDefault();
        });
    });
    filter();

    // Refresh only status badges/actions, leaving the create form and filters untouched.
    let refreshing = false;
    const refreshStatuses = async () => {
        if (document.hidden || refreshing) return;
        refreshing = true;
        try {
            const response = await fetch(window.location.href, {
                credentials: 'same-origin', cache: 'no-store',
                headers: { Accept: 'text/html' }
            });
            if (!response.ok || response.redirected) return;
            const page = new DOMParser().parseFromString(await response.text(), 'text/html');
            const latest = new Map(Array.from(page.querySelectorAll('[data-showtime]'))
                .map(row => [row.dataset.showtimeId, row]));
            rows.forEach(row => {
                const updated = latest.get(row.dataset.showtimeId);
                if (!updated || updated.dataset.status === row.dataset.status
                        || row.contains(document.activeElement)) return;
                const badge = updated.querySelector('.st-status');
                const actions = updated.querySelector('.st-row-actions');
                if (!badge || !actions) return;
                row.dataset.status = updated.dataset.status;
                row.querySelector('.st-status').replaceWith(badge);
                row.querySelector('.st-row-actions').replaceWith(actions);
            });
            filter();
        } catch (error) {
            // Retain the last displayed data and retry on the next interval.
            console.warn('Không thể cập nhật trạng thái suất chiếu.', error);
        } finally {
            refreshing = false;
        }
    };
    window.setInterval(refreshStatuses, 30000);
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) refreshStatuses();
    });
})();
