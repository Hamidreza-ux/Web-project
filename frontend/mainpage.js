// ============================================
// داده‌های نمونه
// ============================================
const ME = 'علی';

const CHATS = [
    {
        id: 'sara',
        name: 'سارا احمدی',
        isGroup: false,
        isPinned: true,
        lastMessage: 'باشه، فردا می‌بینمت 👍',
        time: '۱۴:۳۲',
        unread: 2,
        avatarColor: '#e67e22',
        status: 'آخرین بازدید: ۱۰ دقیقه پیش',
        messages: [
            { id: 1, sender: ME,    content: 'سلام سارا! چطوری؟', time: '۱۴:۱۰', isFile: false },
            { id: 2, sender: 'سارا احمدی', content: 'سلام علی! خوبم ممنون، تو چطوری؟', time: '۱۴:۱۲', isFile: false },
            { id: 3, sender: ME,    content: 'منم خوبم. راستی پروژه رو دیدی؟', time: '۱۴:۱۵', isFile: false },
            { id: 4, sender: 'سارا احمدی', content: 'آره! خیلی قشنگ شده. آفرین بهت 🎉', time: '۱۴:۲۰', isFile: false },
            { id: 5, sender: ME,    content: 'ممنون، یه فایل هم برات فرستادم', time: '۱۴:۲۵', isFile: true, fileName: 'project_v2.pdf' },
            { id: 6, sender: 'سارا احمدی', content: 'باشه، فردا می‌بینمت 👍', time: '۱۴:۳۲', isFile: false },
        ]
    },
    {
        id: 'reza',
        name: 'رضا کریمی',
        isGroup: false,
        isPinned: false,
        lastMessage: 'کد رو چک کردم، یه باگ داره',
        time: '۱۱:۰۵',
        unread: 0,
        avatarColor: '#27ae60',
        status: 'آنلاین',
        messages: [
            { id: 1, sender: 'رضا کریمی', content: 'سلام علی، کد جدید رو پوش کردم', time: '۱۰:۳۰', isFile: false },
            { id: 2, sender: ME,    content: 'دیدم، ممنون. ریویو می‌کنم', time: '۱۰:۳۵', isFile: false },
            { id: 3, sender: 'رضا کریمی', content: 'فایل تست رو هم اضافه کردم', time: '۱۰:۴۰', isFile: true, fileName: 'test_cases.zip' },
            { id: 4, sender: ME,    content: 'عالیه. پول ریکوئست رو مرج می‌کنم', time: '۱۰:۵۰', isFile: false },
            { id: 5, sender: 'رضا کریمی', content: 'صبر کن، کد رو چک کردم، یه باگ داره', time: '۱۱:۰۵', isFile: false, isEdited: true },
        ]
    },
    {
        id: 'team',
        name: 'گروه تیم توسعه 🚀',
        isGroup: true,
        isPinned: false,
        lastMessage: 'نیلوفر: جلسه رو به چهارشنبه موکول کردیم',
        time: 'دیروز',
        unread: 5,
        avatarColor: '#8e44ad',
        status: '۴ عضو',
        members: ['علی', 'رضا کریمی', 'نیلوفر حسینی', 'امیر طاهری'],
        messages: [
            { id: 1, sender: 'امیر طاهری',    content: 'سلام بچه‌ها، اسپرینت جدید شروع شد!', time: 'دیروز ۰۹:۰۰', isFile: false },
            { id: 2, sender: ME,              content: 'سلام امیر. تسک‌های من رو آپدیت کردم', time: 'دیروز ۰۹:۱۵', isFile: false },
            { id: 3, sender: 'رضا کریمی',     content: 'منم آماده‌ام. مستندات رو فرستادم', time: 'دیروز ۰۹:۳۰', isFile: true, fileName: 'sprint_docs.pdf' },
            { id: 4, sender: 'نیلوفر حسینی', content: 'عالی! من روی API کار می‌کنم', time: 'دیروز ۱۰:۰۰', isFile: false },
            { id: 5, sender: ME,              content: 'نیلوفر، مشکل auth رو حل کردی؟', time: 'دیروز ۱۱:۲۰', isFile: false },
            { id: 6, sender: 'نیلوفر حسینی', content: 'آره، تاکن JWT رو درست کردم 🎯', time: 'دیروز ۱۱:۴۵', isFile: false },
            { id: 7, sender: 'امیر طاهری',    content: 'بچه‌ها، جلسه رو به چهارشنبه موکول کردیم', time: 'دیروز ۱۴:۰۰', isFile: false },
            { id: 8, sender: 'نیلوفر حسینی', content: 'جلسه رو به چهارشنبه موکول کردیم', time: 'دیروز ۱۴:۰۵', isFile: false },
        ]
    },
    {
        id: 'saved',
        name: 'پیام‌های ذخیره شده',
        isGroup: false,
        isSaved: true,
        lastMessage: 'یادداشت: جلسه فردا ساعت ۱۰',
        time: 'دیروز',
        unread: 0,
        avatarColor: '#20c997',
        status: '',
        messages: [
            { id: 1, sender: ME, content: 'یادداشت: جلسه فردا ساعت ۱۰', time: 'دیروز ۱۸:۰۰', isFile: false },
            { id: 2, sender: ME, content: 'لیست خرید: شیر، نان، پنیر، تخم‌مرغ', time: 'دیروز ۱۹:۳۰', isFile: false },
            { id: 3, sender: ME, content: 'لینک مهم: docs.anthropic.com', time: 'دیروز ۲۰:۰۰', isFile: false },
        ]
    }
];

const ARCHIVED_CHATS = [
    {
        id: 'archived1',
        name: 'محمد رضایی',
        lastMessage: 'ممنون از همکاریت',
        avatarColor: '#c0392b',
        messages: [
            { id: 1, sender: 'محمد رضایی', content: 'پروژه تموم شد، ممنون از همکاریت', time: 'ماه پیش', isFile: false },
            { id: 2, sender: ME, content: 'خواهش می‌کنم، موفق باشی', time: 'ماه پیش', isFile: false },
        ]
    }
];

// ============================================
// وضعیت برنامه
// ============================================
const state = {
    activeChatId: null,
    editingMsgId: null,
    contextMsgId: null,
    contextMsgIsOutgoing: false,
};

// ============================================
// راه‌اندازی
// ============================================
document.addEventListener('DOMContentLoaded', () => {
    renderChatList(CHATS.filter(c => c.id !== 'saved'));
    document.addEventListener('click', (e) => {
        if (!e.target.closest('#msgContextMenu')) hideContextMenu();
    });
});

// ============================================
// رندر لیست چت‌ها
// ============================================
function renderChatList(chats) {
    const container = document.getElementById('dynamicChats');
    container.innerHTML = '';

    chats.forEach(chat => {
        if (chat.id === 'saved') return;
        const item = createChatItem(chat);
        container.appendChild(item);
    });
}

function createChatItem(chat) {
    const div = document.createElement('div');
    div.className = 'chat-item' + (chat.isPinned ? ' pinned' : '') + (chat.id === state.activeChatId ? ' active' : '');
    div.dataset.chatId = chat.id;

    const avatarEl = document.createElement('div');
    avatarEl.className = 'avatar';
    avatarEl.style.background = chat.avatarColor || 'var(--avatar-bg)';
    avatarEl.textContent = chat.name.charAt(0);

    const info = document.createElement('div');
    info.className = 'chat-item-info';
    info.innerHTML = `
        <div class="chat-item-top">
            <span class="chat-item-name">${chat.name}</span>
            <span class="chat-item-time">${chat.time || ''}</span>
        </div>
        <div class="chat-item-bottom">
            <span class="chat-item-last-msg">${chat.lastMessage || ''}</span>
            ${chat.unread > 0 ? `<span class="unread-badge">${chat.unread}</span>` : ''}
        </div>
    `;

    div.appendChild(avatarEl);
    div.appendChild(info);
    div.addEventListener('click', () => openChat(chat.id));
    return div;
}

// ============================================
// باز کردن چت
// ============================================
function openChat(chatId) {
    const chat = [...CHATS, ...ARCHIVED_CHATS].find(c => c.id === chatId);
    if (!chat) return;

    state.activeChatId = chatId;
    state.editingMsgId = null;

    document.querySelectorAll('.chat-item').forEach(el => {
        el.classList.toggle('active', el.dataset.chatId === chatId);
    });

    document.getElementById('welcomeScreen').style.display = 'none';
    document.getElementById('chatHeader').style.display = 'flex';
    document.getElementById('messagesBody').style.display = 'flex';
    document.getElementById('messageInputArea').style.display = 'block';
    document.getElementById('msgSearchBar').style.display = 'none';
    document.getElementById('editBar').style.display = 'none';

    const headerAvatar = document.getElementById('chatHeaderAvatar');
    headerAvatar.textContent = chat.name.charAt(0);
    headerAvatar.style.background = chat.avatarColor || 'var(--avatar-bg)';
    if (chat.isGroup) headerAvatar.style.background = '#8e44ad';
    if (chat.isSaved) headerAvatar.style.background = '#20c997';

    document.getElementById('chatHeaderName').textContent = chat.name;
    document.getElementById('chatHeaderStatus').textContent = chat.status || '';

    // پاک کردن badge خوانده نشده
    chat.unread = 0;
    renderChatList(CHATS.filter(c => c.id !== 'saved'));

    renderMessages(chat.messages, chat.isGroup);

    // scroll به آخر
    setTimeout(() => {
        const body = document.getElementById('messagesBody');
        body.scrollTop = body.scrollHeight;
    }, 50);
}

// ============================================
// رندر پیام‌ها
// ============================================
function renderMessages(messages, isGroup) {
    const list = document.getElementById('messagesList');
    list.innerHTML = '';

    messages.forEach(msg => {
        const bubble = createBubble(msg, isGroup);
        list.appendChild(bubble);
    });
}

function createBubble(msg, isGroup) {
    const isOutgoing = msg.sender === ME;
    const wrapper = document.createElement('div');
    wrapper.className = `message-bubble ${isOutgoing ? 'outgoing' : 'incoming'}`;
    wrapper.dataset.msgId = msg.id;

    const content = document.createElement('div');
    content.className = 'bubble-content';

    if (isGroup && !isOutgoing) {
        const senderEl = document.createElement('div');
        senderEl.className = 'bubble-sender';
        senderEl.textContent = msg.sender;
        content.appendChild(senderEl);
    }

    if (msg.isFile) {
        const fileDiv = document.createElement('div');
        fileDiv.className = 'file-bubble';
        fileDiv.innerHTML = `<svg viewBox="0 0 24 24"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11z"/></svg><span>${msg.fileName || msg.content}</span>`;
        content.appendChild(fileDiv);
    } else {
        const textEl = document.createElement('div');
        textEl.textContent = msg.content;
        textEl.dataset.originalText = msg.content;
        content.appendChild(textEl);
    }

    const meta = document.createElement('div');
    meta.className = 'bubble-meta';
    const timeSpan = document.createElement('span');
    timeSpan.textContent = msg.time || '';
    meta.appendChild(timeSpan);
    if (msg.isEdited) {
        const editedTag = document.createElement('span');
        editedTag.className = 'edited-tag';
        editedTag.textContent = '(ویرایش شده)';
        meta.appendChild(editedTag);
    }
    if (msg.isReported) {
        const rep = document.createElement('span');
        rep.className = 'reported-tag';
        rep.textContent = '🚩';
        meta.appendChild(rep);
    }
    content.appendChild(meta);
    wrapper.appendChild(content);

    content.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        showContextMenu(e, msg.id, isOutgoing);
    });

    return wrapper;
}

// ============================================
// بستن چت
// ============================================
function closeChat() {
    state.activeChatId = null;
    document.getElementById('welcomeScreen').style.display = 'flex';
    document.getElementById('chatHeader').style.display = 'none';
    document.getElementById('messagesBody').style.display = 'none';
    document.getElementById('messageInputArea').style.display = 'none';
    document.getElementById('msgSearchBar').style.display = 'none';
    document.querySelectorAll('.chat-item').forEach(el => el.classList.remove('active'));
}

// ============================================
// ارسال پیام
// ============================================
let lastSendTime = 0;

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    if (!content || !state.activeChatId) return;

    const now = Date.now();
    if (now - lastSendTime < 200) return;
    lastSendTime = now;

    if (state.editingMsgId) {
        doEditMessage(content);
        return;
    }

    const chat = CHATS.find(c => c.id === state.activeChatId);
    if (!chat) return;

    const newMsg = {
        id: Date.now(),
        sender: ME,
        content,
        time: getCurrentTime(),
        isFile: false
    };

    chat.messages.push(newMsg);
    chat.lastMessage = content;
    chat.time = getCurrentTime();

    input.value = '';
    updateCharCounter('');
    autoResizeTextarea(input);

    renderMessages(chat.messages, chat.isGroup);
    renderChatList(CHATS.filter(c => c.id !== 'saved'));

    setTimeout(() => {
        const body = document.getElementById('messagesBody');
        body.scrollTop = body.scrollHeight;
    }, 30);
}

function getCurrentTime() {
    const d = new Date();
    return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}

// ============================================
// ارسال فایل
// ============================================
function handleFileSelect(event) {
    const file = event.target.files[0];
    if (!file || !state.activeChatId) return;

    const chat = CHATS.find(c => c.id === state.activeChatId);
    if (!chat) return;

    const newMsg = { id: Date.now(), sender: ME, content: file.name, time: getCurrentTime(), isFile: true, fileName: file.name };
    chat.messages.push(newMsg);
    chat.lastMessage = '📎 ' + file.name;
    chat.time = getCurrentTime();

    renderMessages(chat.messages, chat.isGroup);
    renderChatList(CHATS.filter(c => c.id !== 'saved'));

    setTimeout(() => { document.getElementById('messagesBody').scrollTop = document.getElementById('messagesBody').scrollHeight; }, 30);
    event.target.value = '';
}

// ============================================
// ویرایش پیام
// ============================================
function editSelectedMessage() {
    hideContextMenu();
    if (!state.contextMsgId) return;

    const chat = CHATS.find(c => c.id === state.activeChatId);
    const msg = chat?.messages.find(m => m.id === state.contextMsgId);
    if (!msg || msg.isFile) return;

    const input = document.getElementById('messageInput');
    input.value = msg.content;
    input.focus();
    updateCharCounter(msg.content);
    state.editingMsgId = state.contextMsgId;
    document.getElementById('editBar').style.display = 'flex';
}

function doEditMessage(newContent) {
    const chat = CHATS.find(c => c.id === state.activeChatId);
    const msg = chat?.messages.find(m => m.id === state.editingMsgId);
    if (msg) {
        msg.content = newContent;
        msg.isEdited = true;
        if (chat.lastMessage && !chat.lastMessage.startsWith('📎')) chat.lastMessage = newContent;
    }
    cancelEdit();
    renderMessages(chat.messages, chat.isGroup);
    renderChatList(CHATS.filter(c => c.id !== 'saved'));
}

function cancelEdit() {
    state.editingMsgId = null;
    document.getElementById('messageInput').value = '';
    document.getElementById('editBar').style.display = 'none';
    updateCharCounter('');
}

// ============================================
// حذف پیام
// ============================================
function deleteSelectedMessage() {
    hideContextMenu();
    if (!state.contextMsgId) return;
    if (!confirm('آیا می‌خواهید این پیام را حذف کنید؟')) return;

    const chat = CHATS.find(c => c.id === state.activeChatId);
    if (!chat) return;

    chat.messages = chat.messages.filter(m => m.id !== state.contextMsgId);
    const last = chat.messages[chat.messages.length - 1];
    chat.lastMessage = last ? (last.isFile ? '📎 ' + (last.fileName || last.content) : last.content) : '';

    renderMessages(chat.messages, chat.isGroup);
    renderChatList(CHATS.filter(c => c.id !== 'saved'));
}

// ============================================
// گزارش پیام
// ============================================
function reportSelectedMessage() {
    hideContextMenu();
    if (!state.contextMsgId) return;
    if (!confirm('آیا می‌خواهید این پیام را گزارش دهید؟')) return;

    const chat = CHATS.find(c => c.id === state.activeChatId);
    const msg = chat?.messages.find(m => m.id === state.contextMsgId);
    if (msg) msg.isReported = true;

    renderMessages(chat.messages, chat.isGroup);
    alert('✅ گزارش شما ثبت شد.');
}

// ============================================
// منوی راست‌کلیک
// ============================================
function showContextMenu(e, msgId, isOutgoing) {
    state.contextMsgId = msgId;
    state.contextMsgIsOutgoing = isOutgoing;

    const menu = document.getElementById('msgContextMenu');
    menu.style.display = 'block';
    menu.style.left = Math.min(e.clientX, window.innerWidth - 180) + 'px';
    menu.style.top = Math.min(e.clientY, window.innerHeight - 130) + 'px';

    document.getElementById('ctxEdit').style.display = isOutgoing ? 'block' : 'none';
    document.getElementById('ctxDelete').style.display = isOutgoing ? 'block' : 'none';
}

function hideContextMenu() {
    document.getElementById('msgContextMenu').style.display = 'none';
}

// ============================================
// جستجوی چت‌ها
// ============================================
function filterChats(query) {
    const filtered = CHATS.filter(c => c.id !== 'saved' && c.name.includes(query));
    renderChatList(filtered);
    document.getElementById('emptyState').style.display = filtered.length === 0 ? 'block' : 'none';
}

// ============================================
// جستجو در پیام‌ها
// ============================================
function toggleMsgSearch() {
    const bar = document.getElementById('msgSearchBar');
    const isVisible = bar.style.display !== 'none';
    bar.style.display = isVisible ? 'none' : 'flex';
    if (!isVisible) document.getElementById('msgSearchInput').focus();
    else {
        document.getElementById('msgSearchInput').value = '';
        const chat = CHATS.find(c => c.id === state.activeChatId);
        if (chat) renderMessages(chat.messages, chat.isGroup);
    }
}

function searchInMessages(query) {
    const chat = CHATS.find(c => c.id === state.activeChatId);
    if (!chat) return;
    if (!query) { renderMessages(chat.messages, chat.isGroup); return; }
    const filtered = chat.messages.filter(m => !m.isFile && m.content.includes(query));
    renderMessages(filtered, chat.isGroup);
}

function clearMsgSearch() {
    document.getElementById('msgSearchInput').value = '';
    document.getElementById('msgSearchBar').style.display = 'none';
    const chat = CHATS.find(c => c.id === state.activeChatId);
    if (chat) renderMessages(chat.messages, chat.isGroup);
}

// ============================================
// آرشیو
// ============================================
function toggleArchive() {
    const modal = document.getElementById('archiveModal');
    modal.style.display = 'flex';

    const list = document.getElementById('archiveList');
    list.innerHTML = '';

    if (ARCHIVED_CHATS.length === 0) {
        list.innerHTML = '<div style="padding:24px;text-align:center;color:#888;">هیچ گفتگوی آرشیو شده‌ای وجود ندارد</div>';
        return;
    }

    ARCHIVED_CHATS.forEach(chat => {
        const item = document.createElement('div');
        item.className = 'archive-chat-item';
        item.innerHTML = `
            <div class="avatar" style="background:${chat.avatarColor}">${chat.name.charAt(0)}</div>
            <div>
                <div style="font-weight:600;direction:rtl">${chat.name}</div>
                <div style="font-size:13px;color:#888;direction:rtl">${chat.lastMessage || ''}</div>
            </div>
        `;
        item.addEventListener('click', () => {
            closeArchiveModal();
            // نمایش چت آرشیو شده
            state.activeChatId = chat.id;
            document.getElementById('welcomeScreen').style.display = 'none';
            document.getElementById('chatHeader').style.display = 'flex';
            document.getElementById('messagesBody').style.display = 'flex';
            document.getElementById('messageInputArea').style.display = 'block';
            document.getElementById('chatHeaderName').textContent = chat.name;
            document.getElementById('chatHeaderStatus').textContent = 'آرشیو شده';
            const ha = document.getElementById('chatHeaderAvatar');
            ha.textContent = chat.name.charAt(0);
            ha.style.background = chat.avatarColor;
            renderMessages(chat.messages, false);
            setTimeout(() => { document.getElementById('messagesBody').scrollTop = document.getElementById('messagesBody').scrollHeight; }, 30);
        });
        list.appendChild(item);
    });
}

function closeArchiveModal(event) {
    if (!event || event.target === document.getElementById('archiveModal')) {
        document.getElementById('archiveModal').style.display = 'none';
    }
}

// ============================================
// کمکی‌ها
// ============================================
function autoResizeTextarea(el) {
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
    updateCharCounter(el.value);
}

function updateCharCounter(text) {
    const counter = document.getElementById('charCounter');
    const len = text.length;
    counter.textContent = `${len}/1000`;
    counter.className = 'char-counter' + (len > 900 ? ' danger' : len > 700 ? ' warn' : '');
}

function handleInputKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}