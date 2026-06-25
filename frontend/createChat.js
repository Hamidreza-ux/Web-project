document.addEventListener('DOMContentLoaded', () => {
    const addContactForm = document.getElementById('add-contact-form');
    const createGroupForm = document.getElementById('create-group-form');
    const contactsList = document.getElementById('contacts-list');

    // فرض می‌کنیم نام کاربری فرد لاگین‌شده را از هدر یا localStorage گرفته‌اید
    const currentUsername = "my_username"; 
    const API_URL = "http://localhost:8085/create-chat"; 
    
    // تنظیم هدرهای ثابت برای تمام درخواست‌ها
    const headers = {
        'X-Username': currentUsername,
        'Content-Type': 'application/json; charset=utf-8'
    };

    //  دریافت لیست مخاطبین از سرور
    async function loadContacts() {
        try {
            const response = await fetch(API_URL, { method: 'GET', headers: headers });
            if (!response.ok) throw new Error('خطا در دریافت اطلاعات از سرور');
            
            const contacts = await response.json(); // دریافت آرایه مخاطبین
            contactsList.innerHTML = ''; // پاک کردن لیست قدیمی

            contacts.forEach(contact => {
                const li = document.createElement('li');
                li.classList.add('contact-item');
                li.setAttribute('data-id', contact.uniqueId); // استفاده از آیدی منحصربه‌فرد برای هدایت به چت
                li.setAttribute('data-username', contact.username);

                li.innerHTML = `
                    <div style="display:flex; align-items:center; gap:10px;">
                        <img src="${contact.avatarUrl}" alt="avatar" style="width:35px; height:35px; border-radius:50%;">
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

    //  ارسال اطلاعات برای افزودن مخاطب جدید 
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
            alert(result.message);

            if (response.ok) {
                contactIdInput.value = '';
                loadContacts();
            }
        } catch (error) {
            alert('ارتباط با سرور برقرار نشد.');
        }
    });

    //                   ساخت گروه
    createGroupForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const groupNameInput = document.getElementById('group-name');
        const groupName = groupNameInput.value.trim();

        if (!groupName) return;

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(requestBody)
            });

            const result = await response.json();
            alert(result.message);

            if (response.status === 201 || response.ok) {
                groupNameInput.value = '';
            }
        } catch (error) {
            alert('ارتباط با سرور برقرار نشد.');
        }
    });

    // ۴. مدیریت کلیک روی هر مخاطب برای ورود به صفحه چت
    contactsList.addEventListener('click', (e) => {
        const clickedContact = e.target.closest('.contact-item');
        if (clickedContact) {
            const uniqueId = clickedContact.getAttribute('data-id');
            alert(`ورود به صفحه چت اختصاصی کاربر با آیدی: ${uniqueId}`);
            // هدایت به صفحه چت اختصاصی پروژه:
            // window.location.href = `chat.html?id=${uniqueId}`;
        }
    });

    // بارگذاری اولیه لیست مخاطبین هنگام باز شدن صفحه
    loadContacts();
});