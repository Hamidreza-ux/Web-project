const API_BASE = 'http://localhost:8085/api';

// اگر مسیر فولدر assets تغییر کرد اینجا آپدیت کنید
const ASSETS = 'assets/';
const DEFAULT_AVATAR = ASSETS + 'default_avatar.png';

// ---- وضعیت سراسری برنامه ----
const state = {
    username: null,          // نام کاربر لاگین شده
    activeChatId: null,      // آیدی چت باز شده
    activeChatName: null,    // نام چت باز شده
    isGroup: false,          // آیا چت باز شده گروه است
    editingMsgId: null,      // آیدی پیامی که در حال ویرایش است
    contextMsgId: null,      // آیدی پیامی که روی آن راست‌کلیک شده
    contextMsgSender: null,  // فرستنده پیام انتخاب شده
    pollInterval: null,      // تایمر polling پیام‌ها
    chatPollInterval: null,  // تایمر polling لیست چت‌ها
    isDarkMode: false,       // وضعیت حالت شب
};
 
// راه‌اندازی اولیه صفحه
document.addEventListener('DOMContentLoaded', () => {
    // بررسی لاگین بودن کاربر
    state.username = localStorage.getItem('username');
    if (!state.username) {
        // اگر لاگین نیست، برگرد به صفحه ورود
        window.location.href = 'Login.html';
        return;
    }

    // بررسی حالت شب از localStorage
    state.isDarkMode = localStorage.getItem('darkMode') === 'true';
    if (state.isDarkMode) {
        document.body.classList.add('dark-mode');
    }

    // بارگذاری اولیه لیست چت‌ها
    loadChatList();

    // polling لیست چت‌ها هر ۵ ثانیه
    state.chatPollInterval = setInterval(loadChatList, 5000);

    // راه‌اندازی جستجو
    initSearchInput();

    // بستن منوی کانتکست با کلیک خارج از آن
    document.addEventListener('click', hideContextMenu);
});

// ==========================================
// بارگذاری لیست چت‌ها از بک‌اند
// ==========================================
async function loadChatList(searchQuery = '') {
    try {
        const headers = { 'X-Username': state.username };
        if (searchQuery) headers['X-Search'] = searchQuery;

        const res = await fetch(`${API_BASE}/chats`, { headers });
        if (!res.ok) throw new Error('خطا در دریافت لیست چت‌ها');

        const chats = await res.json();
        renderChatList(chats);

        // بارگذاری تعداد آرشیوها
        loadArchiveCount();

    } catch (err) {
        console.error('loadChatList error:', err);
    }
}

// ==========================================
// رندر لیست چت‌ها در سایدبار
// ==========================================
function renderChatList(chats) {
    const container = document.getElementById('dynamicChats');
    const emptyState = document.getElementById('emptyState');
    container.innerHTML = '';

    // فیلتر کردن چت‌های آرشیو شده (آنها در جای دیگر جداگانه نمایش داده می‌شوند)
    const activeChats = chats.filter(c => !c.isArchived && c.id !== 'saved_messages');

    if (activeChats.length === 0) {
        emptyState.style.display = 'block';
        return;
    }
    emptyState.style.display = 'none';

    activeChats.forEach(chat => {
        const item = createChatItem(chat);
        container.appendChild(item);
    });

    // آپدیت پیش‌نمایش Saved Messages
    const savedChat = chats.find(c => c.id === 'saved_messages');
    if (savedChat) {
        document.getElementById('savedMsgPreview').textContent =
            savedChat.lastMessage || 'بدون پیام';
    }
}

// ساختن المان آیتم چت
function createChatItem(chat) {
    const div = document.createElement('div');
    div.className = 'chat-item' +
        (chat.isPinned ? ' pinned' : '') +
        (chat.id === state.activeChatId ? ' active' : '');
    div.dataset.chatId = chat.id;

    // آواتار
    const avatarEl = document.createElement('div');
    avatarEl.className = 'avatar';
    if (chat.avatarUrl && chat.avatarUrl !== 'assets/default_avatar.png') {
        const img = document.createElement('img');
        img.src = chat.avatarUrl;
        img.alt = chat.name;
        img.onerror = () => { avatarEl.textContent = chat.name.charAt(0).toUpperCase(); };
        avatarEl.appendChild(img);
    } else {
        avatarEl.textContent = chat.name.charAt(0).toUpperCase();
    }

    // اطلاعات
    const info = document.createElement('div');
    info.className = 'chat-item-info';

    const topRow = document.createElement('div');
    topRow.className = 'chat-item-top';

    const nameEl = document.createElement('span');
    nameEl.className = 'chat-item-name';
    nameEl.textContent = chat.name;

    const timeEl = document.createElement('span');
    timeEl.className = 'chat-item-time';
    timeEl.textContent = ''; // TODO: بک‌اند timestamp پیام آخر را برگرداند

    topRow.appendChild(nameEl);
    topRow.appendChild(timeEl);

    const bottomRow = document.createElement('div');
    bottomRow.className = 'chat-item-bottom';

    const lastMsg = document.createElement('span');
    lastMsg.className = 'chat-item-last-msg';
    lastMsg.textContent = chat.lastMessage || '';

    bottomRow.appendChild(lastMsg);

    // نشانگر پیام خوانده نشده
    if (chat.unreadCount > 0) {
        const badge = document.createElement('span');
        badge.className = 'unread-badge';
        badge.textContent = chat.unreadCount;
        bottomRow.appendChild(badge);
    }

    info.appendChild(topRow);
    info.appendChild(bottomRow);
    div.appendChild(avatarEl);
    div.appendChild(info);

    // کلیک برای باز کردن چت
    div.addEventListener('click', () => openChat(chat.id, chat.name, chat.isGroup));

    return div;
}


// باز کردن یک چت
async function openChat(chatId, chatName, isGroup) {
    state.activeChatId = chatId;
    state.activeChatName = chatName;
    state.isGroup = isGroup;
    state.editingMsgId = null;

    // آپدیت کلاس active در لیست
    document.querySelectorAll('.chat-item').forEach(el => {
        el.classList.toggle('active', el.dataset.chatId === chatId);
    });

    // نمایش هدر و ناحیه پیام
    document.getElementById('welcomeScreen').style.display = 'none';
    document.getElementById('chatHeader').style.display = 'flex';
    document.getElementById('messagesBody').style.display = 'flex';
    document.getElementById('messageInputArea').style.display = 'block';

    // پر کردن هدر چت
    document.getElementById('chatHeaderName').textContent = chatName;
    const headerAvatar = document.getElementById('chatHeaderAvatar');
    headerAvatar.textContent = chatName.charAt(0).toUpperCase();

    // بارگذاری اطلاعات چت (تعداد اعضا یا وضعیت آنلاین)
    loadChatInfo(chatId, isGroup);

    // بارگذاری پیام‌ها
    await loadMessages();

    // شروع polling پیام‌ها هر ۳ ثانیه
    if (state.pollInterval) clearInterval(state.pollInterval);
    state.pollInterval = setInterval(loadMessages, 3000);
}

// باز کردن Saved Messages
function openSavedMessages() {
    openChat('saved_messages', 'پیام‌های ذخیره شده', false);
}

// بستن چت و نمایش صفحه خوش‌آمدگویی
function closeChat() {
    state.activeChatId = null;
    if (state.pollInterval) clearInterval(state.pollInterval);

    document.getElementById('welcomeScreen').style.display = 'flex';
    document.getElementById('chatHeader').style.display = 'none';
    document.getElementById('messagesBody').style.display = 'none';
    document.getElementById('messageInputArea').style.display = 'none';
    document.getElementById('msgSearchBar').style.display = 'none';

    document.querySelectorAll('.chat-item').forEach(el => el.classList.remove('active'));
}

// بارگذاری اطلاعات چت
async function loadChatInfo(chatId, isGroup) {
    try {
        const res = await fetch(`${API_BASE}/chat-info`, {
            headers: {
                'X-Username': state.username,
                'X-Chat-Id': chatId
            }
        });
        if (!res.ok) return;
        const info = await res.json();

        const statusEl = document.getElementById('chatHeaderStatus');
        if (isGroup) {
            statusEl.textContent = `${info.memberCount || 0} عضو`;
        } else {
            statusEl.textContent = info.isBlocked ? 'بلاک شده' : '';
        }
    } catch (err) {
        console.error('loadChatInfo error:', err);
    }
}


// بارگذاری پیام‌ها
async function loadMessages(searchQuery = '') {
    if (!state.activeChatId) return;

    try {
        const headers = {
            'X-Username': state.username,
            'X-Chat-Id': state.activeChatId
        };
        if (searchQuery) headers['X-Search-Msg'] = searchQuery;

        const res = await fetch(`${API_BASE}/message`, { headers });
        if (!res.ok) throw new Error('خطا در دریافت پیام‌ها');

        const messages = await res.json();
        renderMessages(messages);

    } catch (err) {
        console.error('loadMessages error:', err);
    }
}

// ==========================================
// رندر کردن پیام‌ها
function renderMessages(messages) {
    const list = document.getElementById('messagesList');
    const body = document.getElementById('messagesBody');

    // نگه داشتن موقعیت اسکرول اگر کاربر بالاتر رفته
    const atBottom = body.scrollHeight - body.scrollTop - body.clientHeight < 60;

    list.innerHTML = '';
    let lastDate = null;

    messages.forEach(msg => {
        // تاریخ‌بند بین پیام‌ها (در صورت داشتن timestamp)
        // TODO: وقتی timestamp به پیام‌ها اضافه شد، این بخش را فعال کنید
        // const msgDate = formatDate(msg.timestamp);
        // if (msgDate !== lastDate) { ... }

        const bubble = createMessageBubble(msg);
        list.appendChild(bubble);
    });

    // اسکرول به پایین فقط اگر کاربر پایین بود
    if (atBottom) {
        body.scrollTop = body.scrollHeight;
    }
}

// ساختن حباب پیام
function createMessageBubble(msg) {
    const isOutgoing = msg.sender === state.username;

    const wrapper = document.createElement('div');
    wrapper.className = `message-bubble ${isOutgoing ? 'outgoing' : 'incoming'}`;
    wrapper.dataset.msgId = msg.id;
    wrapper.dataset.sender = msg.sender;

    const bubble = document.createElement('div');
    bubble.className = 'bubble-content';

    // نام فرستنده در گروه‌ها
    if (state.isGroup && !isOutgoing) {
        const senderEl = document.createElement('div');
        senderEl.className = 'bubble-sender';
        senderEl.textContent = msg.sender;
        bubble.appendChild(senderEl);
    }

    // محتوای پیام
    if (msg.isFile) {
        // پیام فایل
        const fileDiv = document.createElement('div');
        fileDiv.className = 'file-bubble';
        fileDiv.innerHTML = `
            <svg viewBox="0 0 24 24"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11z"/></svg>
            <span>${msg.content}</span>
        `;
        bubble.appendChild(fileDiv);
    } else {
        const textEl = document.createElement('div');
        textEl.textContent = msg.content;
        bubble.appendChild(textEl);
    }

    // متادیتا (ویرایش شده / گزارش شده)
    const meta = document.createElement('div');
    meta.className = 'bubble-meta';
    if (msg.isEdited) {
        const editedTag = document.createElement('span');
        editedTag.className = 'edited-tag';
        editedTag.textContent = '(ویرایش شده)';
        meta.appendChild(editedTag);
    }
    if (msg.isReported) {
        const reportedTag = document.createElement('span');
        reportedTag.className = 'reported-tag';
        reportedTag.textContent = '🚩';
        meta.appendChild(reportedTag);
    }
    bubble.appendChild(meta);

    wrapper.appendChild(bubble);

    // راست‌کلیک برای منوی عملیات
    bubble.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        showContextMenu(e, msg.id, msg.sender, isOutgoing);
    });

    return wrapper;
}


// ارسال پیام
let lastSendTime = 0; //  زمان آخرین ارسال برای جلوگیری از تکرار زیاد پیام (اسپم کردن)

async function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();

    if (!content || !state.activeChatId) return;

    // محدودیت طول پیام
    if (content.length > 1000) {
        alert('طول پیام نباید از ۱۰۰۰ کاراکتر بیشتر باشد');
        return;
    }

    // جلوگیری از اسپم: حداقل ۲۰۰ms بین ارسال‌ها در سمت فرانت
    const now = Date.now();
    if (now - lastSendTime < 200) return;
    lastSendTime = now;

    // اگر در حال ویرایش است
    if (state.editingMsgId) {
        await sendEditRequest(content);
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/message`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Username': state.username,
                'X-Chat-Id': state.activeChatId
            },
            body: JSON.stringify({ content, isFile: 'false' })
        });

        const data = await res.json();

        if (res.status === 201) {
            input.value = '';
            updateCharCounter('');
            autoResizeTextarea(input);
            await loadMessages();
        } else if (res.status === 429) {
            alert(' ارسال پیام بیش از حد مجاز ');
        } else {
            alert(data.message || 'خطا در ارسال پیام');
        }
    } catch (err) {
        console.error('sendMessage error:', err);
        alert('خطا در اتصال به سرور');
    }
}


// ارسال فایل
async function handleFileSelect(event) {
    const file = event.target.files[0];
    if (!file || !state.activeChatId) return;

    try {
        const res = await fetch(`${API_BASE}/message`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Username': state.username,
                'X-Chat-Id': state.activeChatId
            },
            body: JSON.stringify({ content: file.name, isFile: 'true' })
        });

        if (res.status === 201) {
            await loadMessages();
        } else {
            alert('خطا در ارسال فایل');
        }
    } catch (err) {
        console.error('handleFileSelect error:', err);
    }

    // ریست input فایل
    event.target.value = '';
}


// ویرایش پیام
function editSelectedMessage() {
    hideContextMenu();
    if (!state.contextMsgId) return;

    // پیدا کردن متن پیام
    const bubble = document.querySelector(`[data-msg-id="${state.contextMsgId}"] .bubble-content div`);
    if (!bubble) return;

    const currentText = bubble.textContent;
    const input = document.getElementById('messageInput');
    input.value = currentText;
    input.focus();

    state.editingMsgId = state.contextMsgId;
    document.getElementById('editBar').style.display = 'flex';
}

async function sendEditRequest(newContent) {
    try {
        const res = await fetch(`${API_BASE}/message`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-Username': state.username,
                'X-Chat-Id': state.activeChatId
            },
            body: JSON.stringify({
                messageId: state.editingMsgId,
                action: 'edit',
                newContent
            })
        });

        if (res.ok) {
            cancelEdit();
            await loadMessages();
        } else {
            alert('خطا در ویرایش پیام');
        }
    } catch (err) {
        console.error('sendEditRequest error:', err);
    }
}

function cancelEdit() {
    state.editingMsgId = null;
    document.getElementById('messageInput').value = '';
    document.getElementById('editBar').style.display = 'none';
    updateCharCounter('');
}


// حذف پیام
async function deleteSelectedMessage() {
    hideContextMenu();
    if (!state.contextMsgId) return;
    if (!confirm('آیا مطمئن هستید که می‌خواهید این پیام را حذف کنید؟')) return;

    try {
        const res = await fetch(`${API_BASE}/message`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'X-Username': state.username,
                'X-Chat-Id': state.activeChatId
            },
            body: JSON.stringify({ messageId: state.contextMsgId })
        });

        if (res.ok) {
            await loadMessages();
        } else {
            const data = await res.json();
            alert(data.message || 'خطا در حذف پیام');
        }
    } catch (err) {
        console.error('deleteSelectedMessage error:', err);
    }
}


// گزارش پیام
async function reportSelectedMessage() {
    hideContextMenu();
    if (!state.contextMsgId) return;
    if (!confirm('آیا می‌خواهید این پیام را به ادمین گزارش دهید؟')) return;

    try {
        const res = await fetch(`${API_BASE}/message`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-Username': state.username,
                'X-Chat-Id': state.activeChatId
            },
            body: JSON.stringify({
                messageId: state.contextMsgId,
                action: 'report'
            })
        });

        if (res.ok) {
            alert('✅ گزارش شما ثبت شد.');
            await loadMessages();
        } else {
            alert('خطا در ثبت گزارش');
        }
    } catch (err) {
        console.error('reportSelectedMessage error:', err);
    }
}

// منوی کانتکست پیام
function showContextMenu(event, msgId, sender, isOutgoing) {
    state.contextMsgId = msgId;
    state.contextMsgSender = sender;

    const menu = document.getElementById('msgContextMenu');
    menu.style.display = 'block';

    // تنظیم موقعیت
    const x = event.clientX;
    const y = event.clientY;
    menu.style.left = `${Math.min(x, window.innerWidth - 180)}px`;
    menu.style.top = `${Math.min(y, window.innerHeight - 130)}px`;

    // دکمه ویرایش و حذف فقط برای پیام‌های خود کاربر
    menu.querySelectorAll('button')[0].style.display = isOutgoing ? 'block' : 'none';
    menu.querySelectorAll('button')[1].style.display = isOutgoing ? 'block' : 'none';
}

function hideContextMenu() {
    document.getElementById('msgContextMenu').style.display = 'none';
}

// جستجوی چت‌ها
function initSearchInput() {
    const searchInput = document.getElementById('searchInput');
    let searchTimer = null;

    searchInput.addEventListener('input', () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => {
            loadChatList(searchInput.value.trim());
        }, 400);
    });
}

// جستجو در پیام‌ها
function toggleMsgSearch() {
    const bar = document.getElementById('msgSearchBar');
    const isVisible = bar.style.display !== 'none';
    bar.style.display = isVisible ? 'none' : 'flex';

    if (!isVisible) {
        document.getElementById('msgSearchInput').focus();
        document.getElementById('msgSearchInput').addEventListener('input', (e) => {
            loadMessages(e.target.value.trim());
        });
    } else {
        loadMessages(); // بارگذاری همه پیام‌ها
    }
}

function clearMsgSearch() {
    document.getElementById('msgSearchInput').value = '';
    document.getElementById('msgSearchBar').style.display = 'none';
    loadMessages();
}

// آرشیو
async function toggleArchive() {
    const modal = document.getElementById('archiveModal');
    modal.style.display = 'flex';
    await loadArchivedChats();
}

async function loadArchivedChats() {
    try {
        const res = await fetch(`${API_BASE}/chats`, {
            headers: {
                'X-Username': state.username,
                'X-Get-Archive': 'true'
            }
        });
        if (!res.ok) return;
        const chats = await res.json();

        const list = document.getElementById('archiveList');
        list.innerHTML = '';

        if (chats.length === 0) {
            list.innerHTML = '<div style="padding:24px;text-align:center;color:#888;">هیچ گفتگوی آرشیو شده‌ای وجود ندارد</div>';
            return;
        }

        chats.forEach(chat => {
            const item = document.createElement('div');
            item.className = 'archive-chat-item';

            const avatar = document.createElement('div');
            avatar.className = 'avatar';
            avatar.textContent = chat.name.charAt(0).toUpperCase();

            const info = document.createElement('div');
            info.innerHTML = `<div style="font-weight:600">${chat.name}</div>
                              <div style="font-size:13px;color:#888">${chat.lastMessage || ''}</div>`;

            item.appendChild(avatar);
            item.appendChild(info);
            item.addEventListener('click', () => {
                closeArchiveModal();
                openChat(chat.id, chat.name, chat.isGroup);
            });

            list.appendChild(item);
        });
    } catch (err) {
        console.error('loadArchivedChats error:', err);
    }
}

async function loadArchiveCount() {
    try {
        const res = await fetch(`${API_BASE}/chats`, {
            headers: { 'X-Username': state.username, 'X-Get-Archive': 'true' }
        });
        if (!res.ok) return;
        const chats = await res.json();
        const countEl = document.getElementById('archiveCount');
        if (chats.length > 0) {
            countEl.textContent = chats.length;
            countEl.style.display = 'inline';
        } else {
            countEl.style.display = 'none';
        }
    } catch (err) {}
}

function closeArchiveModal(event) {
    if (!event || event.target === document.getElementById('archiveModal')) {
        document.getElementById('archiveModal').style.display = 'none';
    }
}

// رفتن به صفحه اطلاعات چت
function openChatInfo() {
    if (!state.activeChatId) return;
    window.location.href = `ChatInfo.html?chatId=${state.activeChatId}&username=${state.username}`;
}

// تغییر اندازه خودکار textarea
function autoResizeTextarea(el) {
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
    updateCharCounter(el.value);
}

// آپدیت شمارنده کاراکتر
function updateCharCounter(text) {
    const counter = document.getElementById('charCounter');
    const len = text.length;
    counter.textContent = `${len}/1000`;
    counter.className = 'char-counter' + (len > 900 ? ' danger' : len > 700 ? ' warn' : '');
}

// ارسال با Enter (بدون Shift)
function handleInputKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

// فرمت‌کردن تاریخ برای نمایش
function formatDate(timestamp) {
    if (!timestamp) return '';
    const d = new Date(timestamp);
    return d.toLocaleDateString('fa-IR');
}
