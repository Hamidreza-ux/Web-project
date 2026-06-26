document.addEventListener('DOMContentLoaded', () => {
    const addContactForm = document.getElementById('add-contact-form');
    const createGroupForm = document.getElementById('create-group-form');
    const contactsList = document.getElementById('contacts-list');

    const currentUsername = localStorage.getItem('username'); 
    
    if (!currentUsername) {
        window.location.href = 'index.html';
        return;
    }

    const API_URL = "http://localhost:8085/create-chat"; 
    
    // تنظیم هدرهای ثابت برای تمام درخواست‌ها
    const headers = {
        'X-Username': currentUsername,
        'Content-Type': 'application/json; charset=utf-8'
    };

    // دریافت لیست مخاطبین از سرور
    async function loadContacts() {
        try {
            const response = await fetch(API_URL, { method: 'GET', headers: headers });
            if (!response.ok) throw new Error('خطا در دریافت اطلاعات از سرور');
            
            const contacts = await response.json(); // دریافت آرایه مخاطبین
            contactsList.innerHTML = ''; // پاک کردن لیست قدیمی

            contacts.forEach(contact => {
                const li = document.createElement('li');
                li.classList.add('contact-item');
                li.setAttribute('data-id', contact.uniqueId); 
                li.setAttribute('data-username', contact.username);

                li.innerHTML = `
                    <div style="display:flex; align-items:center; gap:10px;">
                        <img src="${contact.avatarUrl || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + contact.username}" alt="avatar" style="width:35px; height:35px; border-radius:50%;">
                        <span class="contact-name">${contact.username}</span>
                    </div>
                    <span class="contact-id">@${contact.uniqueId}</span>
                `;
                contactsList.appendChild(li);
            });
        } catch (error) {
            console.error('خطای شبکه:', error);
        }
    }

    // ارسال اطلاعات برای افزودن مخاطب جدید 
    addContactForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const contactIdInput = document.getElementById('contact-id');
        const targetId = contactIdInput.value.trim();

        if (!targetId) return;

        const requestBody = {
            action: "add_contact",
            targetId: targetId,
            members: ""
        };

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(requestBody)
            });

            const result = await response.json();
            alert(result.message || 'عملیات با موفقیت انجام شد');

            if (response.ok) {
                contactIdInput.value = '';
                loadContacts();
            }
        } catch (error) {
            alert('ارتباط با سرور برقرار نشد.');
        }
    });

    // ساخت گروه
    createGroupForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const groupNameInput = document.getElementById('group-name');
        const groupName = groupNameInput.value.trim();

        if (!groupName) return;

        const groupRequestBody = {
            action: "create_group",
            targetId: groupName,
            members: "" 
        };

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(groupRequestBody)
            });

            const result = await response.json();
            alert(result.message || 'گروه ساخته شد');

            if (response.status === 201 || response.ok) {
                groupNameInput.value = '';
            }
        } catch (error) {
            alert('ارتباط با سرور برقرار نشد.');
        }
    });

    // مدیریت کلیک روی هر مخاطب برای ورود به صفحه چت
    contactsList.addEventListener('click', (e) => {
        const clickedContact = e.target.closest('.contact-item');
        if (clickedContact) {
            const uniqueId = clickedContact.getAttribute('data-id');
            const targetUsername = clickedContact.getAttribute('data-username');
            
            alert(`ورود به صفحه چت اختصاصی کاربر با نام: ${targetUsername}`);
            
            // هدایت به صفحه چت اصلی (با فرستادن اطلاعات مورد نیاز در URL)
            window.location.href = `chat.html?chatId=${uniqueId}&name=${targetUsername}`;
        }
    });

    // بارگذاری اولیه لیست مخاطبین هنگام باز شدن صفحه
    loadContacts();
});