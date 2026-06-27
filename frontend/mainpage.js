//       MM      MM       AA   TTTTTT
//      MM MM  MM  MM    AAAA    TT
//     MM    MM     MM  AA  AA   TT
const API = 'http://localhost:8085/api';

const state = {
    username: null,
    activeChatId: null,
    activeChatName: null,
    activeIsGroup: false,
    editingMsgId: null,
    contextMsgId: null,
    contextMsgSender: null,
    actionTargetChat: null,
    pollInterval: null,
    chatPollInterval: null,
    chatsData: [],
    messagesData: [],
};

document.addEventListener('DOMContentLoaded', () => {
    state.username = localStorage.getItem('username');
    if (!state.username) {
        window.location.href = 'index.html';
        return;
    }
    
    document.getElementById('userName').textContent = state.username;
    const currentLoggedInUser = localStorage.getItem('userUsername') || localStorage.getItem('userName') || 'default';
    const savedPic = localStorage.getItem('userAvatar');
    const avatarUrl = savedPic || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + currentLoggedInUser;
    document.getElementById('userAvatar').style.backgroundImage = `url('${avatarUrl}')`;

    loadChatList();
    state.chatPollInterval = setInterval(loadChatList, 5000);
    initEvents();
});

function initEvents() {
    document.getElementById('menuBtn').addEventListener('click', (e) => {
        e.stopPropagation();
        document.getElementById('dropdownMenu').classList.toggle('open');
    });

    document.getElementById('searchToggleBtn').addEventListener('click', () => {
        openSearchOverlay();
    });

    document.getElementById('closeSearchBtn').addEventListener('click', closeSearchOverlay);

    document.getElementById('searchInput').addEventListener('input', (e) => {
        doSearchChats(e.target.value.trim());
    });

    document.getElementById('logoutBtn').addEventListener('click', (e) => {
        e.preventDefault();
        localStorage.removeItem('username');
        window.location.href = 'index.html';
    });

    document.getElementById('archiveRow').addEventListener('click', openArchiveModal);

    document.getElementById('savedMessagesItem').addEventListener('click', () => {
        openChat('saved_messages', 'پیام‌های ذخیره شده', false);
    });

    document.getElementById('actionPin').addEventListener('click', doTogglePin);
    document.getElementById('actionArchive').addEventListener('click', doToggleArchive);
    document.getElementById('actionBlock').addEventListener('click', doToggleBlock);
    document.getElementById('actionDelete').addEventListener('click', doDeleteChat);

    document.getElementById('msgSearchToggle').addEventListener('click', toggleMsgSearch);
    document.getElementById('clearMsgSearch').addEventListener('click', clearMsgSearch);
    document.getElementById('msgSearchInput').addEventListener('input', (e) => {
        loadMessages(e.target.value.trim());
    });

    document.getElementById('cwhMenuBtn').addEventListener('click', (e) => {
        e.stopPropagation();
        window.location.href = `ChatInfo.html?chatId=${state.activeChatId}&username=${state.username}`;
    });

    document.getElementById('sendBtn').addEventListener('click', sendMessage);
    document.getElementById('messageInput').addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
    });
    document.getElementById('messageInput').addEventListener('input', function() {
        autoResize(this);
        updateCharCount(this.value.length);
    });

    document.querySelector('.attach-btn').addEventListener('click', () => {
        document.getElementById('fileInput').click();
    });
    document.getElementById('fileInput').addEventListener('change', handleFileAttach);

    document.querySelector('#editBar button').addEventListener('click', cancelEdit);

    document.getElementById('mcmEdit').addEventListener('click', startEditMessage);
    document.getElementById('mcmDelete').addEventListener('click', deleteMessage);
    document.getElementById('mcmReport').addEventListener('click', reportMessage);

    document.getElementById('closeArchiveModal').addEventListener('click', closeArchiveModal);
    document.getElementById('archiveModal').addEventListener('click', (e) => {
        if (e.target === document.getElementById('archiveModal')) closeArchiveModal();
    });

    document.addEventListener('click', (e) => {
        if (!e.target.closest('#menuBtn') && !e.target.closest('#dropdownMenu')) {
            document.getElementById('dropdownMenu').classList.remove('open');
        }
        if (!e.target.closest('#chatActionBox') && !e.target.closest('.chat-item')) {
            hideChatActionBox();
        }
        if (!e.target.closest('#msgContextMenu')) {
            hideMsgContextMenu();
        }
    });
}

async function loadChatList(searchQuery = '') {
    let chats;
    try {
        const headers = { 'X-Username': state.username };
        if (searchQuery) headers['X-Search'] = searchQuery;
        const res = await fetch(`${API}/message`, { headers });
        if (!res.ok) return;
        chats = await res.json();
    } catch (err) {
        console.error('loadChatList:', err);
        return;
    }
    state.chatsData = chats;
    renderChatList(chats);
    updateArchiveRow();
}

function renderChatList(chats) {
    const container = document.getElementById('dynamicChatList');
    container.innerHTML = '';

    const sorted = [...chats].sort((a, b) => {
        if (a.isPinned && !b.isPinned) return -1;
        if (!a.isPinned && b.isPinned) return 1;
        return 0;
    });

    sorted.forEach(chat => {
        container.appendChild(buildChatItem(chat));
    });
}

function buildChatItem(chat) {
    const div = document.createElement('div');
    div.className = 'chat-item' +
        (chat.isPinned ? ' pinned' : '') +
        (chat.id === state.activeChatId ? ' active' : '');
    div.dataset.chatId = chat.id;

    const av = document.createElement('div');
    av.className = 'chat-avatar';
    av.textContent = chat.name.charAt(0).toUpperCase();
    av.style.background = stringToColor(chat.name);

    const info = document.createElement('div');
    info.className = 'chat-info';

    const top = document.createElement('div');
    top.className = 'chat-top-row';

    const name = document.createElement('span');
    name.className = 'chat-name';
    name.textContent = chat.name;

    top.appendChild(name);

    const bot = document.createElement('div');
    bot.className = 'chat-bottom-row';

    const lm = document.createElement('span');
    lm.className = 'chat-last-msg';
    lm.textContent = chat.lastMessage || '';
    bot.appendChild(lm);

    if (chat.unreadCount > 0) {
        const badge = document.createElement('span');
        badge.className = 'unread-badge';
        badge.textContent = chat.unreadCount;
        bot.appendChild(badge);
    }

    info.appendChild(top);
    info.appendChild(bot);
    div.appendChild(av);
    div.appendChild(info);

    div.addEventListener('click', () => openChat(chat.id, chat.name, chat.isGroup));

    let holdTimer;
    div.addEventListener('mousedown', (e) => {
        holdTimer = setTimeout(() => {
            showChatActionBox(e, chat);
        }, 500);
    });
    div.addEventListener('mouseup', () => clearTimeout(holdTimer));
    div.addEventListener('mouseleave', () => clearTimeout(holdTimer));

    div.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        showChatActionBox(e, chat);
    });

    return div;
}

async function updateArchiveRow() {
    let archivedChats;
    try {
        const res = await fetch(`${API}/message`, {
            headers: { 'X-Username': state.username, 'X-Get-Archive': 'true' }
        });
        if (!res.ok) return;
        archivedChats = await res.json();
    } catch { return; }
    
    const row = document.getElementById('archiveRow');
    if (archivedChats.length > 0) {
        row.style.display = 'flex';
        document.getElementById('archiveBadge').textContent = archivedChats.length;
    } else {
        row.style.display = 'none';
    }
}

// این بخش اصلاح شد تا چت درون همان صفحه باز شود
async function openChat(chatId, chatName, isGroup) {
    state.activeChatId = chatId;
    state.activeChatName = chatName;
    state.activeIsGroup = isGroup;

    document.getElementById('welcomeScreen').style.display = 'none';
    document.getElementById('messageInputArea').style.display = 'flex';
    
    document.getElementById('cwhName').textContent = chatName;
    document.getElementById('cwhStatus').textContent = isGroup ? 'گروه' : 'آنلاین';
    
    const avatar = document.getElementById('cwhAvatar');
    avatar.textContent = chatName.charAt(0).toUpperCase();
    avatar.style.background = stringToColor(chatName);

    document.querySelectorAll('.chat-item').forEach(el => el.classList.remove('active'));
    const activeItem = document.querySelector(`.chat-item[data-chat-id="${chatId}"]`);
    if (activeItem) activeItem.classList.add('active');

    await loadMessages();
}

async function loadMessages(searchQuery = '') {
    if (!state.activeChatId) return;
    let messages;
    try {
        const headers = {
            'X-Username': state.username,
            'X-Chat-Id': state.activeChatId
        };
        if (searchQuery) headers['X-Search-Msg'] = searchQuery;
        const res = await fetch(`${API}/message`, { headers });
        if (!res.ok) return;
        messages = await res.json();
    } catch (err) {
        console.error('loadMessages:', err);
        return;
    }
    state.messagesData = messages;
    renderMessages(messages);
}

function renderMessages(messages) {
    const list = document.getElementById('messagesList');
    const area = document.getElementById('messagesArea');
    const atBottom = area.scrollHeight - area.scrollTop - area.clientHeight < 60;

    list.innerHTML = '';

    messages.forEach(msg => {
        const isOut = msg.sender === state.username;
        const wrap = document.createElement('div');
        wrap.className = `msg-bubble ${isOut ? 'out' : 'in'}`;
        wrap.dataset.msgId = msg.id;
        wrap.dataset.sender = msg.sender;

        const body = document.createElement('div');
        body.className = 'bubble-body';

        if (state.activeIsGroup && !isOut) {
            const snd = document.createElement('div');
            snd.className = 'bubble-sender';
            snd.textContent = msg.sender;
            body.appendChild(snd);
        }

        const text = document.createElement('div');
        text.textContent = msg.isFile ? `📎 ${msg.content}` : msg.content;
        body.appendChild(text);

        const footer = document.createElement('div');
        footer.className = 'bubble-footer';

        const tags = document.createElement('div');
        tags.className = 'bubble-tags';
        if (msg.isEdited) tags.innerHTML += '<span>ویرایش شده</span>';
        if (msg.isReported) tags.innerHTML += '<span style="color:#f15c6d">!</span>';
        footer.appendChild(tags);

        body.appendChild(footer);
        wrap.appendChild(body);

        wrap.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            showMsgContextMenu(e, msg.id, msg.sender, isOut);
        });

        list.appendChild(wrap);
    });

    if (atBottom) area.scrollTop = area.scrollHeight;
}

let lastSendTime = 0;

async function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    if (!content || !state.activeChatId) return;

    if (content.length > 1000) {
        alert('پیام نباید از ۱۰۰۰ کاراکتر بیشتر باشد');
        return;
    }

    const now = Date.now();
    if (now - lastSendTime < 200) return;
    lastSendTime = now;

    if (state.editingMsgId) {
        await doEditMessage(content);
        return;
    }
        
    try {
        const res = await fetch(`${API}/message`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Username': state.username,
                'X-Chat-Id': state.activeChatId
            },
            body: JSON.stringify({ content, isFile: 'false' })
        });

        if (res.status === 201) {
            input.value = '';
            autoResize(input);
            updateCharCount(0);
            await loadMessages();
        } else if (res.status === 429) {
            alert('تکرار پیام بیش از حد مجاز');
        } else {
            const d = await res.json();
            alert(d.message || 'خطا در ارسال');
        }
    } catch (err) {
        console.error('sendMessage:', err);
    }
}

async function handleFileAttach(e) {
    const file = e.target.files[0];
    if (!file || !state.activeChatId) return;

    try {
        const res = await fetch(`${API}/message`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Username': state.username,
                'X-Chat-Id': state.activeChatId
            },
            body: JSON.stringify({ content: file.name, isFile: 'true' })
        });
        if (res.status === 201) await loadMessages();
    } catch (err) { console.error('handleFileAttach:', err); }
    e.target.value = '';
}

function showChatActionBox(event, chat) {
    event.stopPropagation();
    state.actionTargetChat = chat;

    const box = document.getElementById('chatActionBox');

    const pinIcon = document.getElementById('actionPinIcon');
    const pinLabel = document.getElementById('actionPinLabel');
    if (chat.isPinned) {
        pinIcon.src = 'assets/pin-off.svg';
        pinLabel.textContent = 'برداشتن سنجاق';
    } else {
        pinIcon.src = 'assets/pin.svg';
        pinLabel.textContent = 'سنجاق کردن';
    }

    document.getElementById('actionArchiveLabel').textContent =
        chat.isArchived ? 'خارج از آرشیو' : 'آرشیو';

    const blockBtn = document.getElementById('actionBlock');
    if (!chat.isGroup) {
        blockBtn.style.display = 'flex';
        document.getElementById('actionBlockLabel').textContent =
            chat.isBlocked ? 'رفع مسدود کردن' : 'مسدود کردن';
    } else {
        blockBtn.style.display = 'none';
    }

    box.style.display = 'block';
    const x = event.clientX;
    const y = event.clientY;
    const bw = 200, bh = 180;
    box.style.top = `${Math.min(y, window.innerHeight - bh - 10)}px`;
    box.style.right = `${Math.max(10, window.innerWidth - x - bw)}px`;
    box.style.left = 'auto';
}

function hideChatActionBox() {
    document.getElementById('chatActionBox').style.display = 'none';
    state.actionTargetChat = null;
}

async function doTogglePin() {
    hideChatActionBox();
    const chat = state.actionTargetChat;
    if (!chat) return;
}

async function doToggleArchive() {
    hideChatActionBox();
    const chat = state.actionTargetChat;
    if (!chat) return;

    try {
        await fetch(`${API}/chat-info`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Username': state.username, 'X-Chat-Id': chat.id },
            body: JSON.stringify({ action: 'archive' })
        });
        await loadChatList();
        updateArchiveRow();
    } catch (err) { console.error(err); }
}

async function doToggleBlock() {
    hideChatActionBox();
    const chat = state.actionTargetChat;
    if (!chat || chat.isGroup) return;

    try {
        await fetch(`${API}/chat-info`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Username': state.username, 'X-Chat-Id': chat.id },
            body: JSON.stringify({ action: 'block' })
        });
    } catch (err) { console.error(err); }
}

async function doDeleteChat() {
    hideChatActionBox();
    const chat = state.actionTargetChat;
    if (!chat) return;
    if (!confirm(`آیا مطمئنید که می‌خواهید گفتگوی "${chat.name}" را حذف کنید؟`)) return;
}

function showMsgContextMenu(event, msgId, sender, isOut) {
    state.contextMsgId = msgId;
    state.contextMsgSender = sender;

    const menu = document.getElementById('msgContextMenu');
    document.getElementById('mcmEdit').style.display = isOut ? 'flex' : 'none';
    document.getElementById('mcmDelete').style.display = isOut ? 'flex' : 'none';

    menu.style.display = 'block';
    menu.style.top = `${Math.min(event.clientY, window.innerHeight - 140)}px`;
    menu.style.left = `${Math.min(event.clientX, window.innerWidth - 170)}px`;
    menu.style.right = 'auto';
}

function hideMsgContextMenu() {
    document.getElementById('msgContextMenu').style.display = 'none';
}

function startEditMessage() {
    hideMsgContextMenu();
    const msg = state.messagesData.find(m => m.id === state.contextMsgId);
    if (!msg) return;
    state.editingMsgId = msg.id;
    const input = document.getElementById('messageInput');
    input.value = msg.content;
    input.focus();
    document.getElementById('editBar').style.display = 'flex';
    autoResize(input);
}

async function doEditMessage(newContent) {
    try {
        const res = await fetch(`${API}/message`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'X-Username': state.username, 'X-Chat-Id': state.activeChatId },
            body: JSON.stringify({ messageId: state.editingMsgId, action: 'edit', newContent })
        });
        if (res.ok) { cancelEdit(); await loadMessages(); }
        else alert('خطا در ویرایش');
    } catch (err) { console.error(err); }
}

function cancelEdit() {
    state.editingMsgId = null;
    document.getElementById('messageInput').value = '';
    document.getElementById('editBar').style.display = 'none';
    updateCharCount(0);
    autoResize(document.getElementById('messageInput'));
}

async function deleteMessage() {
    hideMsgContextMenu();
    if (!confirm('پیام حذف شود؟')) return;

    try {
        const res = await fetch(`${API}/message`, {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json', 'X-Username': state.username, 'X-Chat-Id': state.activeChatId },
            body: JSON.stringify({ messageId: state.contextMsgId })
        });
        if (res.ok) await loadMessages();
        else alert('خطا در حذف');
    } catch (err) { console.error(err); }
}

async function reportMessage() {
    hideMsgContextMenu();
    if (!confirm('این پیام گزارش شود؟')) return;

    try {
        const res = await fetch(`${API}/message`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'X-Username': state.username, 'X-Chat-Id': state.activeChatId },
            body: JSON.stringify({ messageId: state.contextMsgId, action: 'report' })
        });
        if (res.ok) { await loadMessages(); alert('✅ گزارش ثبت شد'); }
    } catch (err) { console.error(err); }
}

function openSearchOverlay() {
    document.getElementById('searchOverlay').classList.add('open');
    document.getElementById('searchInput').focus();
    document.getElementById('searchResults').innerHTML = '';
    doSearchChats('');
}

function closeSearchOverlay() {
    document.getElementById('searchOverlay').classList.remove('open');
    document.getElementById('searchInput').value = '';
}

function doSearchChats(query) {
    const results = document.getElementById('searchResults');
    const source = state.chatsData.filter(c => c.name.includes(query));

    results.innerHTML = '';

    if (source.length === 0) {
        results.innerHTML = '<div style="padding:20px;text-align:center;color:#8696a0">نتیجه‌ای یافت نشد</div>';
        return;
    }

    source.forEach(chat => {
        const item = buildChatItem(chat);
        results.appendChild(item);
    });
}

function toggleMsgSearch() {
    const bar = document.getElementById('msgSearchBar');
    const visible = bar.style.display !== 'none';
    bar.style.display = visible ? 'none' : 'flex';
    if (!visible) document.getElementById('msgSearchInput').focus();
    else { document.getElementById('msgSearchInput').value = ''; loadMessages(); }
}

function clearMsgSearch() {
    document.getElementById('msgSearchInput').value = '';
    document.getElementById('msgSearchBar').style.display = 'none';
    loadMessages();
}

function openArchiveModal() {
    document.getElementById('archiveModal').style.display = 'flex';
    renderArchiveList();
}

function closeArchiveModal() {
    document.getElementById('archiveModal').style.display = 'none';
}

function renderArchiveList() {
    const list = document.getElementById('archiveList');
    const archived = state.chatsData.filter(c => c.isArchived);

    list.innerHTML = '';

    if (archived.length === 0) {
        list.innerHTML = '<div style="padding:24px;text-align:center;color:#8696a0">آرشیو خالی است</div>';
        return;
    }

    archived.forEach(chat => {
        const item = document.createElement('div');
        item.className = 'chat-item';
        item.style.padding = '10px 20px';

        const av = document.createElement('div');
        av.className = 'chat-avatar';
        av.textContent = chat.name.charAt(0).toUpperCase();
        av.style.background = stringToColor(chat.name);

        const info = document.createElement('div');
        info.className = 'chat-info';
        info.innerHTML = `<div class="chat-top-row"><span class="chat-name">${chat.name}</span></div>
                          <div class="chat-bottom-row"><span class="chat-last-msg">${chat.lastMessage || ''}</span></div>`;

        item.appendChild(av);
        item.appendChild(info);
        item.addEventListener('click', () => {
            closeArchiveModal();
            openChat(chat.id, chat.name, chat.isGroup);
        });
        list.appendChild(item);
    });
}

function autoResize(el) {
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
}

function updateCharCount(len) {
    const el = document.getElementById('charCount');
    el.textContent = `${len}/1000`;
    el.className = 'char-count' + (len > 900 ? ' over' : len > 700 ? ' warn' : '');
}

function stringToColor(str) {
    const colors = ['#2196f3','#e91e63','#9c27b0','#00bcd4','#ff5722','#607d8b','#4caf50','#ff9800'];
    let hash = 0;
    for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
}