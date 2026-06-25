(function applyThemeEarly() {
    const theme = localStorage.getItem('appTheme') || 'light';
    document.documentElement.setAttribute('data-theme', theme);
})();

// کدهای مربوط به هر صفحه، بعد از لود کامل DOM
document.addEventListener('DOMContentLoaded', function () {

    /* ---- صفحه تنظیمات ---- */

    const displayName     = document.getElementById('displayName');
    const displayUsername = document.getElementById('displayUsername');
    const displayTheme    = document.getElementById('displayTheme');

    if (displayName) {
        displayName.textContent = localStorage.getItem('userName') || 'نام کاربر';
    }
    if (displayUsername) {
        displayUsername.textContent = localStorage.getItem('userUsername') || 'شناسه کاربر';
    }
    if (displayTheme) {
        const theme = localStorage.getItem('appTheme') || 'light';
        displayTheme.textContent = theme === 'dark' ? 'تاریک' : 'روشن';
    }


    /* ---- ویرایش نام ---- */
    const editNameInput = document.getElementById('editNameInput');
    const saveNameBtn   = document.getElementById('saveNameBtn');

    if (editNameInput && saveNameBtn) {
        // پر کردن مقدار فعلی
        editNameInput.value = localStorage.getItem('userName') || '';

        // فعال / غیرفعال کردن دکمه ذخیره
        editNameInput.addEventListener('input', function () {
            const hasValue = this.value.trim().length > 0;
            saveNameBtn.disabled = !hasValue;
            saveNameBtn.className = 'btn ' + (hasValue ? 'btn-primary' : 'btn-disabled');
        });

        // اگر مقدار اولیه داشت، دکمه را از ابتدا فعال کن
        if (editNameInput.value.trim().length > 0) {
            saveNameBtn.disabled = false;
            saveNameBtn.className = 'btn btn-primary';
        }

        // ذخیره نام
        saveNameBtn.addEventListener('click', function () {
            const newName = editNameInput.value.trim();
            if (newName) {
                localStorage.setItem('userName', newName);
                window.location.href = 'Setting.html';
            }
        });
    }


    /* ---- ویرایش شناسه ---- */
    const editUsernameInput = document.getElementById('editUsernameInput');
    const saveUsernameBtn   = document.getElementById('saveUsernameBtn');

    if (editUsernameInput && saveUsernameBtn) {
        // پر کردن مقدار فعلی
        editUsernameInput.value = localStorage.getItem('userUsername') || '';

        // فعال / غیرفعال کردن دکمه ذخیره
        editUsernameInput.addEventListener('input', function () {
            const hasValue = this.value.trim().length > 0;
            saveUsernameBtn.disabled = !hasValue;
            saveUsernameBtn.className = 'btn ' + (hasValue ? 'btn-primary' : 'btn-disabled');
        });

        if (editUsernameInput.value.trim().length > 0) {
            saveUsernameBtn.disabled = false;
            saveUsernameBtn.className = 'btn btn-primary';
        }

        // ذخیره شناسه
        saveUsernameBtn.addEventListener('click', function () {
            const newUsername = editUsernameInput.value.trim();
            if (newUsername) {
                localStorage.setItem('userUsername', newUsername);
                window.location.href = 'Setting.html';
            }
        });
    }

    /* ---- ویرایش پروفایل ---- */
    const editProfilePic = document.getElementById('editProfilePic');
    if (editProfilePic) {
        const currentUser = localStorage.getItem('userUsername') || localStorage.getItem('userName') || 'default';
        const savedPic = localStorage.getItem('userAvatar');
        editProfilePic.src = savedPic || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + currentUser;
    }

    /* ---- نمایش عکس پروفایل در صفحه تنظیمات ---- */
    const userAvatarEl = document.getElementById('userAvatar');
    if (userAvatarEl) {
        const currentUser = localStorage.getItem('userUsername') || localStorage.getItem('userName') || 'default';
        const savedPic = localStorage.getItem('userAvatar');
        userAvatarEl.src = savedPic || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + currentUser;
    }

    /* ---- صفحه انتخاب تم ---- */
    const themeRadios = document.querySelectorAll('.theme-radio');
        if (themeRadios.length > 0) {
            const savedTheme = localStorage.getItem('appTheme') || 'light';
            const savedRadio = document.querySelector(`input[value="${savedTheme}"]`);
        if (savedRadio) savedRadio.checked = true;
            updateCardStyles(savedTheme);
            applyThemeToPage(savedTheme);

        themeRadios.forEach(radio => {
            radio.addEventListener('change', function () {
                updateCardStyles(this.value);
                applyThemeToPage(this.value);
            });
        });
    }
});

// تابع پیش‌نمایش عکس (فراخوانی از HTML)
function previewImage(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function (e) {
        const preview = document.getElementById('editProfilePic');
        if (preview) preview.src = e.target.result;

        // ذخیره در localStorage برای نمایش در بقیه صفحات
        localStorage.setItem('userAvatar', e.target.result);
    };
    reader.readAsDataURL(file);
}

function updateCardStyles(selected) {
    document.querySelectorAll('.theme-card').forEach(card => {
        card.classList.remove('theme-card-selected');
    });
    const selectedCard = document.getElementById(`card-${selected}`);
    if (selectedCard) selectedCard.classList.add('theme-card-selected');
}

function applyThemeToPage(theme) {
    document.documentElement.setAttribute('data-theme', theme);
}

function saveTheme() {
    const checked = document.querySelector('input[name="theme"]:checked');
    if (checked) {
        localStorage.setItem('appTheme', checked.value);
        window.location.href = 'Setting.html';
    }
}