document.getElementById('loginForm').addEventListener('submit', function (event) {
    event.preventDefault();

    const usernameInput = document.getElementById('username').value;
    const passwordInput = document.getElementById('password').value;
    const messageBox = document.getElementById('messageBox');

    messageBox.className = 'message-box';
    messageBox.style.display = 'none';

    const requestData = {
        username: usernameInput,
        password: passwordInput
    };

    fetch('http://localhost:8085/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
    })
        .then(response => {
            return response.json().then(function (data) {
                const myPack = {
                    status: response.status,
                    data: data
                };

                return myPack;
            });
        })
        .then(result => {
            if (result.status === 200) {
                messageBox.innerText = result.data.message;
                messageBox.classList.add('success');

                localStorage.setItem('username', usernameInput); // ذخیره نام کاربری

                setTimeout(() => {
                    window.location.href = 'MainPage.html'; // انتقال به صفحه اصلی
                }, 1500);
            } else {
                messageBox.innerText = result.data.message;
                messageBox.classList.add('error');
            }
        })
        .catch(error => {
            messageBox.innerText = "خطا در برقراری ارتباط با سرور";
            messageBox.classList.add('error');
            console.error("Error:", error);
        });
});