const BASE_URL = 'http://localhost:8085/api'; 

const urlParams = new URLSearchParams(window.location.search);
const CHAT_ID = urlParams.get('chatId'); 
const USERNAME = localStorage.getItem('username'); 

async function loadMessages() {
    try {
        const response = await fetch(`${BASE_URL}/message`, {
            method: 'GET',
            headers: { 
                'X-Username': USERNAME, 
                'X-Chat-Id': CHAT_ID 
            }
        });
    } catch (e) { 
        console.error(e); 
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (!CHAT_ID) {
        console.error("آیدی چت یافت نشد، بازگشت به صفحه اصلی...");
        window.location.href = 'MainPage.html';
        return;
    }
    
    loadChatInfo();
});

async function loadChatInfo() {
    try {
        const response = await fetch(`${BASE_URL}/chat-info`, {
            method: 'GET',
            headers: { 
                'X-Username': USERNAME, 
                'X-Chat-Id': CHAT_ID 
            }
        });

        if (response.ok) {
            const chatRoom = await response.json();
            
            // مقداردهی متغیر برای استفاده در تمام توابع
            isGroupChat = chatRoom.isGroup;
            
            document.getElementById('chat-name').innerText = chatRoom.name;
            document.getElementById('chat-avatar').src = chatRoom.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${chatRoom.name}`;
            
            if (isGroupChat) {
                document.getElementById('chat-status').innerText = `${chatRoom.memberCount || 0} عضو`;
            } else {
                document.getElementById('chat-status').innerText = chatRoom.isBlocked ? "بلاک شده" : "آنلاین";
            }
            
            setupThreeDotMenu();
        } else {
            console.error("خطا در دریافت اطلاعات چت از سرور");
        }
    } catch (error) {
        console.error("خطای شبکه:", error);
    }
}

function setupThreeDotMenu() {
    const menuContainer = document.getElementById('chat-options-menu');
    menuContainer.innerHTML = '';

    if (isGroupChat) {
        menuContainer.innerHTML = `
            <a href="#" onclick="triggerAction('leave')">ترک گروه</a>
            <a href="#" onclick="promptAddMember()">افزودن عضو</a>
            <a href="#" onclick="promptEditGroup()">ویرایش اطلاعات گروه</a>
            <a href="#" onclick="triggerAction('archive')">افزودن به آرشیو</a>
            <a href="#" onclick="loadGroupHistory()">تاریخچه تغییرات</a>
        `;
    } else {
        menuContainer.innerHTML = `
            <a href="#" onclick="triggerAction('block')">بلاک کردن کاربر</a>
            <a href="#" onclick="triggerAction('add_contact')">اضافه کردن به مخاطبین</a>
            <a href="#" onclick="triggerAction('archive')">افزودن به آرشیو</a>
        `;
    }
}

function toggleMenu(e) {
    e.stopPropagation();
    document.getElementById("chat-options-menu").classList.toggle("show");
}

window.onclick = () => document.getElementById("chat-options-menu").classList.remove("show");

async function loadMessages() {
    const searchVal = document.getElementById('search-input')?.value || '';
    try {
        const headers = { 'X-Username': USERNAME, 'X-Chat-Id': CHAT_ID };
        if (searchVal) headers['X-Search-Msg'] = searchVal;

        const response = await fetch(`${BASE_URL}/chat-page`, { method: 'GET', headers: headers });
        if (response.ok) {
            const messages = await response.json();
            const chatBox = document.getElementById('chat-box');
            chatBox.innerHTML = '';

            messages.forEach(msg => {
                const msgDiv = document.createElement('div');
                msgDiv.classList.add('message', msg.sender === USERNAME ? 'msg-outbound' : 'msg-inbound');

                let senderHeader = (isGroupChat && msg.sender !== USERNAME) ? `<span class="msg-sender-name">${msg.sender}</span>` : '';
                let fileTag = msg.isFile ? '📁 [فایل رسانه] ' : '';

                let actionButtons = '';
                if (msg.sender === USERNAME) {
                    actionButtons = `
                        <button class="btn-delete" onclick="deleteMessage('${msg.id}')">حذف</button>
                        <button class="btn-edit" onclick="editMessage('${msg.id}', '${msg.content}')">ویرایش</button>
                    `;
                } else {
                    actionButtons = `<button class="btn-report" onclick="reportMessage('${msg.id}')">گزارش پیام</button>`;
                }

                msgDiv.innerHTML = `
                    ${senderHeader}
                    <span>${fileTag}${msg.content} ${msg.isEdited ? '<small style="color:gray">(ویرایش شده)</small>' : ''}</span>
                    <div class="msg-actions">${actionButtons}</div>
                `;
                chatBox.appendChild(msgDiv);
            });
            chatBox.scrollTop = chatBox.scrollHeight;
        }
    } catch (error) {
        console.error(error);
    }
}

const fileAttachBtn = document.getElementById('fileAttachBtn');
const fileInput = document.getElementById('fileInput');

fileAttachBtn.addEventListener('click', () => {
    fileInput.click();
});

fileInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
        // تغییر نام متغیر از chatBody به chatBox جهت همخوانی با کدهای شما
        const chatBox = document.getElementById('chat-box');
        const msg = document.createElement('div');
        msg.className = 'message msg-outbound';
        msg.innerHTML = `<span>📁 فایل پیوست: ${file.name}</span>`;
        chatBox.appendChild(msg);
        chatBox.scrollTop = chatBox.scrollHeight;
    }
});

async function sendMessage() {
    const input = document.getElementById('msg-input');
    const isFileChecked = document.getElementById('file-checkbox').checked;
    const content = input.value.trim();
    if (!content) return;

    try {
        const response = await fetch(`${BASE_URL}/chat-page`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json; charset=utf-8', 'X-Username': USERNAME, 'X-Chat-Id': CHAT_ID },
            body: JSON.stringify({ content: content, isFile: isFileChecked ? "true" : "false" })
        });

        if (response.status === 201) {
            input.value = '';
            document.getElementById('file-checkbox').checked = false;
            loadMessages();
        } else {
            const err = await response.json();
            alert(err.message);
        }
    } catch (error) {
        console.error(error);
    }
}

async function deleteMessage(messageId) {
    if (!confirm("آیا مایل به حذف پیام هستید؟")) return;
    const response = await fetch(`${BASE_URL}/chat-page`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json', 'X-Username': USERNAME, 'X-Chat-Id': CHAT_ID },
        body: JSON.stringify({ messageId: messageId })
    });
    if (response.ok) loadMessages();
}

async function editMessage(messageId, oldContent) {
    const newContent = prompt("متن جدید پیام را وارد کنید:", oldContent);
    if (!newContent || newContent.trim() === oldContent) return;

    const response = await fetch(`${BASE_URL}/chat-page`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-Username': USERNAME, 'X-Chat-Id': CHAT_ID },
        body: JSON.stringify({ messageId: messageId, action: "edit", newContent: newContent })
    });
    if (response.ok) loadMessages();
}

async function reportMessage(messageId) {
    const response = await fetch(`${BASE_URL}/chat-page`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-Username': USERNAME, 'X-Chat-Id': CHAT_ID },
        body: JSON.stringify({ messageId: messageId, action: "report" })
    });
    const res = await response.json();
    alert(res.message);
}

async function triggerAction(actionName, extraData = {}) {
    try {
        const response = await fetch(`${BASE_URL}/chat-info`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Username': USERNAME, 'X-Chat-Id': CHAT_ID },
            body: JSON.stringify({ action: actionName, ...extraData })
        });
        const result = await response.json();
        alert(result.message);
        loadChatInfo();
    } catch (error) {
        console.error(error);
    }
}

function promptAddMember() {
    const member = prompt("نام کاربری عضو جدید گروه را وارد کنید:");
    if (member) triggerAction('add_member', { newMember: member });
}

function promptEditGroup() {
    const newName = prompt("نام جدید گروه:");
    const newAvatar = prompt("لینک عکس جدید گروه:");
    if (newName && newAvatar) triggerAction('edit_group', { newName: newName, newAvatar: newAvatar });
}

async function openInfoModal() {
    const modal = document.getElementById('info-modal');
    const detailsDiv = document.getElementById('modal-details');
    document.getElementById('modal-history-section').style.display = 'none';

    try {
        const response = await fetch(`${BASE_URL}/chat-info`, {
            method: 'GET',
            headers: { 'X-Username': USERNAME, 'X-Chat-Id': CHAT_ID }
        });
        if (response.ok) {
            const data = await response.json();
            document.getElementById('modal-title').innerText = data.name;
            
            if (data.isGroup) {
                detailsDiv.innerHTML = `
                    <p><b>نوع گفتگو:</b> گروه چت</p>
                    <p><b>آیدی گروه:</b> ${data.id}</p>
                    <p><b>تعداد کل اعضا:</b> ${data.memberCount} نفر</p>
                `;
            } else {
                detailsDiv.innerHTML = `
                    <p><b>نام کاربری:</b> @${data.username}</p>
                    <p><b>وضعیت بلاک:</b> ${data.isBlocked ? 'شما این کاربر را بلاک کرده‌اید' : 'بلاک نشده'}</p>
                    <p><b>گروه‌های مشترک شما:</b> ${data.commonGroups.length > 0 ? data.commonGroups.join(', ') : 'هیچ گروه مشترکی ندارید.'}</p>
                `;
            }
            modal.style.display = 'flex';
        }
    } catch (e) { console.error(e); }
}

async function loadGroupHistory() {
    openInfoModal();
    const historySection = document.getElementById('modal-history-section');
    const listDiv = document.getElementById('history-lists');
    listDiv.innerHTML = 'در حال بارگذاری تاریخچه...';
    historySection.style.display = 'block';

    try {
        const response = await fetch(`${BASE_URL}/chat-info`, {
            method: 'GET',
            headers: { 'X-Username': USERNAME, 'X-Chat-Id': CHAT_ID, 'X-Get-History': 'true' }
        });
        if (response.ok) {
            const historyData = await response.json();
            listDiv.innerHTML = '';

            listDiv.innerHTML += '<h5>پیام‌های حذف شده:</h5>';
            if (historyData.deletedMessages.length === 0) listDiv.innerHTML += '<p>پیامی حذف نشده است.</p>';
            historyData.deletedMessages.forEach(m => {
                listDiv.innerHTML += `<div class="history-item"><b>${m.sender}:</b> ${m.content}</div>`;
            });

            listDiv.innerHTML += '<h5 style="margin-top:15px;">📝 پیام‌های ویرایش شده (نسخه قبل):</h5>';
            if (historyData.editedMessages.length === 0) listDiv.innerHTML += '<p>پیامی ویرایش نشده است.</p>';
            historyData.editedMessages.forEach(m => {
                listDiv.innerHTML += `
                <div class="history-item">
                    <div class="current-text"><b>متن فعلی:</b> ${m.currentContent}</div>
                    <div class="old-text" style="color:red"><b>نسخه اولیه:</b> ${m.previousVersion}</div>
                </div>`;
            });
        }
    } catch (e) { console.error(e); }
}

function closeInfoModal() { document.getElementById('info-modal').style.display = 'none'; }
function goBack() { alert("بازگشت به صفحه اصلی چت‌ها "); }

window.onload = () => {
    loadChatInfo();
    loadMessages();
};