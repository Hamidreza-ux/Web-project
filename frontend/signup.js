document.getElementById('SignupForm').addEventListener('submit', function (event) {
    event.preventDefault();

    const usernameInput = document.getElementById('username').value;
    const passwordInput = document.getElementById('password').value;
    const confirmPasswordInput = document.getElementById('confirmPassword').value;
    const userIdInput = document.getElementById('userId').value;
    const messageBox = document.getElementById('messageBox');

    messageBox.className = 'message-box';
    messageBox.style.display = 'none';

    const requestData = {
        username: usernameInput,
        password: passwordInput,
        confirmPassword: confirmPasswordInput,
        userId: userIdInput
    };

    fetch('http://localhost:8085/api/auth/signup', {
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
            messageBox.style.display = 'block';

            if (result.status === 201) {
                messageBox.innerText = result.data.message;
                messageBox.classList.add('success');

                setTimeout(() => {
                    alert("انتقال به داشبورد اصلی سایت...");
                    window.location.href = 'index.html';
                }, 2000);
            } else {
                messageBox.innerText = result.data.message;
                messageBox.classList.add('error');
            }
        })
        .catch(error => {
            messageBox.style.display = 'block';
            messageBox.innerText = "خطا در برقراری ارتباط با سرور";
            messageBox.classList.add('error');
            console.error("Error:", error);
    });
});