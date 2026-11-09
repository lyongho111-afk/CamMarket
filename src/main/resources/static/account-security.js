/* Login/account CSRF is separate from the existing board, rating and staff tokens. */
(() => {
  if (window.campusAccountSecurity) return;
  window.campusAccountSecurity = true;
  const originalFetch = window.fetch.bind(window);
  window.fetch = async function(input, options) {
    const request = new Request(input, options);
    const url = new URL(request.url);
    const mutation = !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(request.method);
    if (url.origin !== location.origin || !mutation ||
        !/^\/api\/(members|account)\//.test(url.pathname)) return originalFetch(request);
    // Retrieve afresh: signing in/out rotates the session and invalidates the previous token.
    const response = await originalFetch('/api/security/csrf', {credentials: 'same-origin', cache: 'no-store'});
    if (!response.ok) throw new Error('로그인 인증 정보를 확인하지 못했습니다. 새로고침해 주세요.');
    const csrf = await response.json();
    const headers = new Headers(request.headers);
    headers.set(csrf.headerName, csrf.token);
    return originalFetch(new Request(request, {headers}));
  };
})();
