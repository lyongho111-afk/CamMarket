// Google Maps + Toss Payments TEST + [조장] 매너온도 평가 시스템 연동
let runtimePromise, googlePromise, tossPromise;

function runtimeConfig() {
    return runtimePromise ??= api('integrations/config').catch(e => {
        runtimePromise = null;
        throw e
    })
}

function externalScript(src) {
    return new Promise((resolve, reject) => {
        const s = document.createElement('script');
        s.src = src;
        s.async = true;
        s.onload = resolve;
        s.onerror = () => reject(Error('외부 연결을 불러오지 못했습니다.'));
        document.head.append(s)
    })
}

async function loadGoogle(key) {
    if (window.google?.maps?.importLibrary) return;
    if (!googlePromise) googlePromise = new Promise((resolve, reject) => {
        window.campusGoogleMapsReady = resolve;
        externalScript('https://maps.googleapis.com/maps/api/js?key=' + encodeURIComponent(key) + '&loading=async&callback=campusGoogleMapsReady&language=ko&region=KR').catch(reject)
    });
    return googlePromise
}

function loadToss() {
    if (window.TossPayments) return Promise.resolve();
    return tossPromise ??= externalScript('https://js.tosspayments.com/v2/standard')
}

function renderIntegration() {
    $('#section-kicker').textContent = 'INHA CAMPUS · SAFE TRADE';
    $('#section-title').textContent = '가까운 곳에서, 안심하고 만나요.';
    $('#section-sub').textContent = '거래 장소와 약속한 가격을 확인하세요.';
    const root = $('#results');
    root.className = 'integration-view';
    root.replaceChildren();
    const p = products.find(x => x.id === tradeProductId) || products[0];
    if (!p) {
        root.append(el('p', 'empty', '상품을 먼저 선택해 주세요.'));
        return
    }
    tradeProductId = p.id;
    const select = el('select', 'trade-select');
    select.setAttribute('aria-label', '거래할 상품');
    products.forEach(x => select.append(new Option(x.title + ' · ' + statusText(x), x.id)));
    select.value = p.id;
    select.onchange = () => {
        tradeProductId = Number(select.value);
        history.replaceState(null, '', '#trade/' + tradeProductId);
        renderIntegration()
    };
    root.append(select);
    const cols = el('div', 'integration-grid'), mapPanel = el('article', 'integration-panel'),
        payPanel = el('article', 'integration-panel');
    cols.append(mapPanel, payPanel);
    root.append(cols);
    const saved = p.order?.latitude != null ? {
        name: p.order.meetingPlace,
        lat: Number(p.order.latitude),
        lng: Number(p.order.longitude)
    } : null;
    let selected = p.customPlace || saved || previewPlaces[p.place || 0];
    mapPanel.append(el('span', 'eyebrow', 'MEETING POINT'), el('h3', '', '거래 장소 지정'));
    const mapNode = el('div', 'campus-map live-map'), placeSelect = el('select');
    placeSelect.setAttribute('aria-label', '거래 장소');
    previewPlaces.forEach((x, i) => placeSelect.append(new Option(x.name, i)));
    placeSelect.value = p.place || 0;
    const chosen = el('p', 'chosen-place', '📍 ' + selected.name), link = el('a', 'map-link', 'Google 지도에서 위치 보기 ↗');
    link.target = '_blank';
    link.rel = 'noopener';

    function updateLink() {
        link.href = 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(selected.lat + ',' + selected.lng);
        chosen.textContent = '📍 ' + selected.name
    }

    updateLink();

    function embed() {
        mapNode.replaceChildren();
        const frame = el('iframe');
        frame.title = '인하대학교 거래 장소 Google 지도';
        frame.loading = 'lazy';
        frame.referrerPolicy = 'no-referrer-when-downgrade';
        frame.src = 'https://maps.google.com/maps?q=' + encodeURIComponent(selected.lat + ',' + selected.lng) + '&z=16&output=embed';
        mapNode.append(frame)
    }

    placeSelect.onchange = () => {
        p.place = Number(placeSelect.value);
        p.customPlace = previewPlaces[p.place];
        renderIntegration()
    };
    mapPanel.append(mapNode, placeSelect, chosen);
    const save = actionButton(p.order ? '이 장소로 약속 저장' : '이 장소에서 약속 잡기', async () => {
        if (!p.order) {
            appointmentDialog(p);
            const form = $('#modal-body form'), select = form.querySelector('[name="meetingPlace"]');
            select.replaceChildren(new Option(selected.name, selected.name));
            form.onsubmit = async e => {
                e.preventDefault();
                const b = form.querySelector('button[type="submit"]');
                b.disabled = true;
                const ok = await perform(() => api('trades', 'POST', {
                    selno: p.id,
                    meetingPlace: selected.name,
                    latitude: selected.lat,
                    longitude: selected.lng,
                    meetingTime: form.querySelector('[name="meetingTime"]').value || null
                }), '거래 약속을 저장했어요.');
                if (!ok) b.disabled = false
            };
            return
        }
        save.disabled = true;
        const ok = await perform(() => api('trades/' + p.order.orderId + '/meeting', 'PUT', {
            meetingPlace: selected.name,
            latitude: selected.lat,
            longitude: selected.lng,
            meetingTime: p.order.meetingTime
        }), '거래 장소를 저장했어요.', false);
        if (!ok) save.disabled = false
    }, p.status === 'done' || Boolean(p.order && p.order.status !== 'RESERVED'));
    mapPanel.append(save, link, el('p', 'fine-print', '인하대학교 주변의 Google 지도입니다. 거래 장소를 선택하고 약속을 저장해 주세요.'));
    embed();
    runtimeConfig().then(async cfg => {
        if (!cfg.mapsKey) return;
        await loadGoogle(cfg.mapsKey);
        if (!mapNode.isConnected) return;
        const {Map} = await google.maps.importLibrary('maps');
        mapNode.replaceChildren();
        const map = new Map(mapNode, {
            center: selected,
            zoom: 16,
            mapId: 'DEMO_MAP_ID',
            mapTypeControl: false,
            streetViewControl: false
        });
        const {AdvancedMarkerElement} = await google.maps.importLibrary('marker');
        const marker = new AdvancedMarkerElement({map, position: selected, title: selected.name});
        map.addListener('click', event => {
            selected = {
                name: '지도에서 선택한 장소 (' + event.latLng.lat().toFixed(5) + ', ' + event.latLng.lng().toFixed(5) + ')',
                lat: event.latLng.lat(),
                lng: event.latLng.lng()
            };
            p.customPlace = selected;
            marker.position = selected;
            updateLink()
        })
    }).catch(() => {
        if (mapNode.isConnected) embed()
    });
    payPanel.append(el('span', 'eyebrow', 'TOSS PAYMENTS'), el('h3', '', '거래 · 결제'), el('p', 'payment-product', p.title), el('strong', 'payment-amount', formatMoney(displayPrice(p))));
    if (p.order) {
        payPanel.append(el('p', 'locked-price', '약속한 거래 가격 유지'), el('p', 'fine-print', '주문번호 ' + p.order.orderId));
    }

    // ★ [핵심] 거래 완료 시 당근 앱처럼 [⭐ 거래 상대방 매너온도 평가하기] 버튼을 표시합니다!
    if (p.status === 'done') {
        payPanel.append(
            el('p', 'locked-price', '✓ 거래 완료'),
            actionButton('⭐ 거래 상대방 매너온도 평가하기', () => mannerReviewDialog(p), false, 'primary wide')
        );
        return
    }

    if (!p.order) {
        payPanel.append(el('p', 'fine-print', '먼저 약속을 확정하면 약속한 가격으로 결제할 수 있어요.'), actionButton('거래 약속 잡기', () => appointmentDialog(p)));
        return
    }
    if (p.order.status === 'PAID') payPanel.append(el('p', 'locked-price', '토스 테스트 승인 확인 · 실제 출금 없음'));
    else {
        const status = el('p', 'sdk-status', '토스 테스트 연결을 확인하는 중…'),
            button = actionButton('토스페이먼츠 테스트 결제창 열기', async () => {
                button.disabled = true;
                try {
                    const cfg = await runtimeConfig();
                    const order = await api('payments/prepare', 'POST', {orderId: p.order.orderId});
                    await loadToss();
                    await TossPayments(cfg.tossClientKey).payment({customerKey: 'campus-demo-buyer'}).requestPayment({
                        method: 'CARD',
                        amount: {currency: 'KRW', value: order.amount},
                        orderId: order.orderId,
                        orderName: order.orderName,
                        successUrl: location.origin + '/preview/payment-result.html',
                        failUrl: location.origin + '/preview/payment-result.html?failed=1',
                        card: {flowMode: 'DEFAULT'}
                    })
                } catch (e) {
                    toast(e.message || '결제창을 닫았어요.')
                } finally {
                    button.disabled = false;
                    await loadData()
                }
            }, true);
        payPanel.append(button, status);
        runtimeConfig().then(cfg => {
            button.disabled = !cfg.tossReady;
            status.textContent = cfg.tossReady ? 'TEST · 실제 출금 없이 결제창을 확인하세요.' : '토스페이먼츠 테스트 키가 아직 없어 결제를 시작할 수 없어요.'
        }).catch(e => status.textContent = e.message)
    }
    if (p.order.status === 'RESERVED') payPanel.append(actionButton('예약 취소', () => cancelReservation(p), false, 'secondary wide'));
    if (['RESERVED', 'PAID'].includes(p.order.status)) payPanel.append(actionButton('물건 전달 · 거래 완료', () => completeTrade(p), false, 'secondary wide'));
    payPanel.append(el('p', 'fine-print', '대면 거래 완료와 토스 결제 승인은 별도로 기록해요. 실제 송금·정산은 지원하지 않습니다.'));
}

/**
 * ★ [조장 기능] 당근마켓 스타일 매너 평가 팝업 모달
 */
function mannerReviewDialog(p) {
    const modal = $('#modal');
    const body = $('#modal-body') || $('#modalBody');
    if (!modal || !body) return;

    const orderId = p.order ? p.order.orderId : '';
    const targetId = p.seller || 'demo-seller';

    body.innerHTML = `
        <div style="padding: 10px 0;">
            <span class="eyebrow" style="color: var(--green, #24694f); font-weight: 700; font-size: 11px; letter-spacing: 1px;">CAMPUS RE:MARKET · MANNER</span>
            <h2 style="font-size: 21px; margin: 8px 0 12px; letter-spacing: -0.5px;">거래 상대방 매너 평가</h2>
            <p style="font-size: 13px; color: #666; margin-bottom: 20px; line-height: 1.6;">
                <b>'${p.title}'</b> 거래는 만족스러우셨나요?<br>
                따뜻한 평가가 더 신뢰받는 캠퍼스 마켓을 만듭니다.
            </p>
            <form id="mannerReviewForm">
                <label style="display: block; font-weight: 700; margin-bottom: 6px; font-size: 12px;">거래 매너 만족도</label>
                <select id="mannerScore" style="width: 100%; padding: 11px; border: 1px solid #dfe6d8; border-radius: 7px; margin-bottom: 14px; font-size: 13px;">
                    <option value="5" selected>⭐⭐⭐⭐⭐ 매우 친절하고 시간 약속을 잘 지켰어요 (+0.5℃)</option>
                    <option value="4">⭐⭐⭐⭐ 친절하고 기분 좋은 거래였어요 (+0.2℃)</option>
                    <option value="3">⭐⭐⭐ 보통이었어요 (온도 변동 없음)</option>
                    <option value="2">⭐⭐ 약속에 늦거나 응답이 느렸어요 (-0.3℃)</option>
                    <option value="1">⭐ 불친절하고 매너가 좋지 않았어요 (-0.8℃)</option>
                </select>

                <label style="display: block; font-weight: 700; margin-bottom: 6px; font-size: 12px;">거래 태도 키워드 (선택)</label>
                <input type="text" id="mannerAttitude" value="시간엄수, 친절매너" 
                       style="width: 100%; padding: 10px; border: 1px solid #dfe6d8; border-radius: 7px; margin-bottom: 14px; font-size: 13px;">

                <label style="display: block; font-weight: 700; margin-bottom: 6px; font-size: 12px;">상세 후기 남기기</label>
                <textarea id="mannerContent" style="width: 100%; min-height: 80px; padding: 10px; border: 1px solid #dfe6d8; border-radius: 7px; margin-bottom: 18px; font-size: 13px;">약속 장소에 시간 맞춰 나와주셨고 상품 상태도 너무 깨끗했습니다!</textarea>

                <button type="submit" class="primary" style="width: 100%; padding: 13px; font-size: 14px; border-radius: 8px;">
                    매너온도 평가 제출하기 ↗
                </button>
            </form>
        </div>
    `;

    if (typeof modal.showModal === 'function') modal.showModal();
    else modal.removeAttribute('hidden');

    const closeBtn = modal.querySelector('.close');
    if (closeBtn) closeBtn.onclick = () => modal.close ? modal.close() : modal.setAttribute('hidden', '');

    const form = body.querySelector('#mannerReviewForm');
    form.onsubmit = async (e) => {
        e.preventDefault();
        const score = parseInt(form.querySelector('#mannerScore').value, 10);
        const attitude = form.querySelector('#mannerAttitude').value;
        const content = form.querySelector('#mannerContent').value;
        const submitBtn = form.querySelector('button[type="submit"]');
        submitBtn.disabled = true;

        try {
            // YS 조원의 api() 공통 함수를 사용해 POST /api/reviews 호출!
            const result = await api('reviews', 'POST', {
                orderId: orderId,
                reviewerId: 'demo-buyer',
                score: score,
                attitude: attitude,
                content: content
            });

            if (modal.close) modal.close();
            else modal.setAttribute('hidden', '');

            const msg = `🎉 상대방의 매너온도가 ${result.currentTemperature}℃가 되었습니다! (${result.appliedDelta >= 0 ? '+' : ''}${result.appliedDelta}℃)`;
            if (typeof toast === 'function') toast(msg);
            else alert(msg);

            // 데이터 리로드 및 화면 재렌더링
            if (typeof loadData === 'function') await loadData();
            renderIntegration();
        } catch (err) {
            submitBtn.disabled = false;
            alert(err.message || '평가 제출 중 오류가 발생했습니다.');
        }
    };
}

window.gm_authFailure = () => {
    document.querySelectorAll('.live-map').forEach(n => {
        n.replaceChildren(el('p', 'integration-error', 'Google 지도 인증을 확인해 주세요. 아래 Google 지도 링크로 위치를 확인할 수 있어요.'))
    })
};