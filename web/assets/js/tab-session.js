(function () {
    'use strict';

    const PARAM_NAME = '_tab';
    const HEADER_NAME = 'X-HRM-Tab-Id';
    const STORAGE_KEY = 'hrm.tab.id';
    const CHANNEL_NAME = 'hrm.tab.session';
    const TAB_PATTERN = /^[A-Za-z0-9_-]{8,80}$/;
    const instanceId = createId('inst');
    let tabId = readStoredTabId();
    let rotated = false;

    if (!tabId) {
        tabId = createId('tab');
        writeStoredTabId(tabId);
    }

    const urlTabId = new URL(window.location.href).searchParams.get(PARAM_NAME);
    if (isValidTabId(urlTabId) && urlTabId !== tabId && !isAuthPath()) {
        syncUrl(true);
    }
    startDuplicateTabGuard();
    bindNavigationHandlers();
    patchFetch();
    patchXhr();

    function createId(prefix) {
        if (window.crypto && typeof window.crypto.randomUUID === 'function') {
            return prefix + '_' + window.crypto.randomUUID();
        }
        return prefix + '_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 14);
    }

    function readStoredTabId() {
        try {
            const value = window.sessionStorage.getItem(STORAGE_KEY);
            return isValidTabId(value) ? value : null;
        } catch (error) {
            return null;
        }
    }

    function writeStoredTabId(value) {
        try {
            window.sessionStorage.setItem(STORAGE_KEY, value);
        } catch (error) {
            // If storage is unavailable, the in-memory tab id still works for this page.
        }
    }

    function isValidTabId(value) {
        return typeof value === 'string' && TAB_PATTERN.test(value);
    }

    function isAuthPath() {
        const path = window.location.pathname.toLowerCase();
        return path.endsWith('/login')
            || path.endsWith('/forgot-password')
            || path.endsWith('/auth/google/callback')
            || path === '/'
            || path.endsWith('/index.jsp');
    }

    function syncUrl(forceReload) {
        const url = new URL(window.location.href);
        if (url.searchParams.get(PARAM_NAME) === tabId) {
            return;
        }

        url.searchParams.set(PARAM_NAME, tabId);
        if (forceReload) {
            window.location.replace(url.toString());
        } else {
            window.history.replaceState(window.history.state, document.title, url.toString());
        }
    }

    function startDuplicateTabGuard() {
        if (!('BroadcastChannel' in window)) {
            return;
        }

        try {
            const channel = new BroadcastChannel(CHANNEL_NAME);
            channel.onmessage = function (event) {
                const data = event.data || {};
                if (data.tabId !== tabId || data.instanceId === instanceId) {
                    return;
                }
                if (data.type === 'hello') {
                    channel.postMessage({
                        type: 'occupied',
                        tabId: tabId,
                        to: data.instanceId,
                        instanceId: instanceId
                    });
                    return;
                }
                if (data.type === 'occupied' && data.to === instanceId) {
                    rotateTabId();
                }
            };

            setTimeout(function () {
                channel.postMessage({
                    type: 'hello',
                    tabId: tabId,
                    instanceId: instanceId
                });
            }, 0);
        } catch (error) {
            // BroadcastChannel is only a duplicate-tab refinement.
        }
    }

    function rotateTabId() {
        if (rotated) {
            return;
        }
        rotated = true;
        tabId = createId('tab');
        writeStoredTabId(tabId);
        syncUrl(!isAuthPath());
    }

    function bindNavigationHandlers() {
        document.addEventListener('click', function (event) {
            const link = event.target.closest && event.target.closest('a[href]');
            if (link) {
                decorateLink(link);
            }
        }, true);

        document.addEventListener('submit', function (event) {
            if (event.target instanceof HTMLFormElement) {
                decorateForm(event.target);
            }
        }, true);
    }

    function decorateLink(link) {
        const href = link.getAttribute('href');
        const decorated = withTabParam(href);
        if (decorated) {
            link.setAttribute('href', decorated);
        }
    }

    function decorateForm(form) {
        const action = form.getAttribute('action');
        if (action) {
            const decorated = withTabParam(action);
            if (decorated) {
                form.setAttribute('action', decorated);
            }
        }

        let input = form.querySelector('input[name="' + PARAM_NAME + '"]');
        if (!input) {
            input = document.createElement('input');
            input.type = 'hidden';
            input.name = PARAM_NAME;
            form.appendChild(input);
        }
        input.value = tabId;
    }

    function withTabParam(value) {
        if (!value || value.startsWith('#')) {
            return null;
        }

        const lower = value.trim().toLowerCase();
        if (lower.startsWith('javascript:')
                || lower.startsWith('mailto:')
                || lower.startsWith('tel:')
                || lower.startsWith('data:')) {
            return null;
        }

        try {
            const url = new URL(value, window.location.href);
            if (!isSameOriginHttpUrl(url)) {
                return null;
            }
            url.searchParams.set(PARAM_NAME, tabId);
            return formatLikeOriginal(value, url);
        } catch (error) {
            return null;
        }
    }

    function formatLikeOriginal(original, url) {
        const trimmed = original.trim();
        const lower = trimmed.toLowerCase();

        if (lower.startsWith('http://') || lower.startsWith('https://')) {
            return url.toString();
        }
        if (trimmed.startsWith('?')) {
            return url.search + url.hash;
        }
        if (trimmed.startsWith('/')) {
            return url.pathname + url.search + url.hash;
        }
        return url.pathname + url.search + url.hash;
    }

    function isSameOriginHttpUrl(url) {
        return (url.protocol === 'http:' || url.protocol === 'https:')
            && url.origin === window.location.origin;
    }

    function patchFetch() {
        if (typeof window.fetch !== 'function') {
            return;
        }

        const originalFetch = window.fetch;
        window.fetch = function (resource, init) {
            const requestUrl = typeof resource === 'string'
                ? resource
                : resource && resource.url;

            if (requestUrl && isSameOriginRequest(requestUrl)) {
                init = init || {};
                const headers = new Headers(init.headers
                    || (window.Request && resource instanceof Request ? resource.headers : {}));
                headers.set(HEADER_NAME, tabId);
                init.headers = headers;
            }

            return originalFetch.call(this, resource, init);
        };
    }

    function patchXhr() {
        if (!window.XMLHttpRequest) {
            return;
        }

        const originalOpen = XMLHttpRequest.prototype.open;
        const originalSend = XMLHttpRequest.prototype.send;

        XMLHttpRequest.prototype.open = function (method, url) {
            this.__hrmTabUrl = url;
            return originalOpen.apply(this, arguments);
        };

        XMLHttpRequest.prototype.send = function () {
            if (this.__hrmTabUrl && isSameOriginRequest(this.__hrmTabUrl)) {
                this.setRequestHeader(HEADER_NAME, tabId);
            }
            return originalSend.apply(this, arguments);
        };
    }

    function isSameOriginRequest(value) {
        try {
            return isSameOriginHttpUrl(new URL(value, window.location.href));
        } catch (error) {
            return false;
        }
    }
})();
