document.addEventListener('DOMContentLoaded', () => {
    // ======================================================
    // LÓGICA DO MODAL DE AUTENTICAÇÃO
    // ======================================================
    const profileBtn = document.getElementById('profile-btn');
    const modal = document.getElementById('auth-modal');
    const closeBtn = document.querySelector('.close-btn');
    const tabBtns = document.querySelectorAll('.tab-btn');
    const authForms = document.querySelectorAll('.auth-form');

    if (profileBtn) {
        profileBtn.addEventListener('click', (e) => {
            e.preventDefault();
            modal.style.display = 'flex';
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            modal.style.display = 'none';
        });
    }

    window.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.style.display = 'none';
        }
    });

    tabBtns.forEach(button => {
        button.addEventListener('click', () => {
            tabBtns.forEach(btn => btn.classList.remove('active'));
            authForms.forEach(form => form.classList.remove('active'));
            const tabId = button.dataset.tab;
            button.classList.add('active');
            document.getElementById(tabId + '-form').classList.add('active');
        });
    });

    // ======================================================
    // LÓGICA PARA O FORMULÁRIO DE CADASTRO NO MODAL
    // ======================================================
    const signupFormModal = document.getElementById('signup-form-modal');

    if (signupFormModal) {
        signupFormModal.addEventListener('submit', (e) => {
            e.preventDefault();

            const username = document.getElementById('signup-name').value;
            const email = document.getElementById('signup-email').value;
            const password = document.getElementById('signup-password').value;

            const formData = new URLSearchParams();
            formData.append('username', username);
            formData.append('email', email);
            formData.append('password', password);

            // ALTERAÇÃO AQUI: Mudamos a URL para um caminho relativo
            fetch('/api/cadastrar', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData.toString()
            })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => Promise.reject(err));
                }
                return response.json();
            })
            .then(data => {
                alert(data.message);
                modal.style.display = 'none';
            })
            .catch(error => {
                console.error('Erro no cadastro:', error);
                alert(error.message || 'Ocorreu um erro ao tentar cadastrar.');
            });
        });
    }

    // ======================================================
    // LÓGICA PARA O FORMULÁRIO DE LOGIN NO MODAL
    // ======================================================
    const loginFormModal = document.getElementById('login-form-modal');
    const loginErrorMessage = document.getElementById('login-error-message');

    if (loginFormModal) {
        loginFormModal.addEventListener('submit', (e) => {
            e.preventDefault(); 

            loginErrorMessage.style.display = 'none';
            loginErrorMessage.textContent = '';

            const email = document.getElementById('login-email').value;
            const password = document.getElementById('login-password').value;

            const formData = new URLSearchParams();
            formData.append('email', email);
            formData.append('password', password);

            // ALTERAÇÃO AQUI TAMBÉM: Mudamos a URL para um caminho relativo
            fetch('/api/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData.toString()
            })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => Promise.reject(err));
                }
                return response.json();
            })
            .then(data => {
                alert(data.message); 
                modal.style.display = 'none';
            })
            .catch(error => {
                console.error('Erro no login:', error);
                loginErrorMessage.textContent = error.message;
                loginErrorMessage.style.display = 'block';
            });
        });
    }

    // O restante do seu código de carrossel continua aqui...
    // ======================================================
    // LÓGICA DO CARROSSEL DE PÔSTERES (JÁ EXISTENTE)
    // ======================================================
    const mainPosters = document.querySelectorAll('.main-poster');
    const sidePosters = document.querySelectorAll('.side-poster');
    let currentIndex = 0;
    let posterInterval;

    if (mainPosters.length > 0 && sidePosters.length > 0) {
        const changePoster = (index) => {
            mainPosters.forEach(poster => poster.classList.remove('active'));
            sidePosters.forEach(poster => poster.classList.remove('active'));
            mainPosters[index].classList.add('active');
            sidePosters[index].classList.add('active');
            currentIndex = index;
        };
        sidePosters.forEach(poster => {
            poster.addEventListener('click', () => {
                const index = parseInt(poster.dataset.index);
                changePoster(index);
                resetPosterInterval();
            });
        });
        const autoChangePoster = () => {
            let newIndex = (currentIndex + 1) % mainPosters.length;
            changePoster(newIndex);
        };
        const resetPosterInterval = () => {
            clearInterval(posterInterval);
            posterInterval = setInterval(autoChangePoster, 5000);
        };
        resetPosterInterval();
    }
    // ======================================================
    // LÓGICA DO CARROSSEL DE JOGOS (JÁ EXISTENTE)
    // ======================================================
    const gameSections = document.querySelectorAll('.games-section');
    gameSections.forEach(section => {
        const wrapper = section.querySelector('.carousel-wrapper');
        const prevBtn = section.querySelector('.nav-btn.prev');
        const nextBtn = section.querySelector('.nav-btn.next');
        if (wrapper && prevBtn && nextBtn) {
            nextBtn.addEventListener('click', () => {
                const scrollAmount = wrapper.querySelector('.game-card').offsetWidth + 30;
                wrapper.scrollBy({ left: scrollAmount, behavior: 'smooth' });
            });
            prevBtn.addEventListener('click', () => {
                const scrollAmount = wrapper.querySelector('.game-card').offsetWidth + 30;
                wrapper.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
            });
        }
    });
});