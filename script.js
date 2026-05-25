document.addEventListener('DOMContentLoaded', () => {

// ── WAVE ANIMATION ──
const phoneCanvas = document.getElementById('phoneWave');
const pctx = phoneCanvas.getContext('2d');

let t = 0;

function drawPhoneWave() {
    pctx.clearRect(0, 0, 200, 60);
    const bars = 28;
    const w = 200 / bars;
    for (let i = 0; i < bars; i++) {
        const h = Math.abs(Math.sin(t * 2 + i * 0.5)) * 30 + 8;
        const alpha = 0.4 + Math.abs(Math.sin(t + i * 0.3)) * 0.4;
        pctx.fillStyle = `rgba(59,130,246,${alpha})`;
        pctx.beginPath();
        pctx.roundRect(i * w + 1, 30 - h / 2, w - 3, h, 2);
        pctx.fill();
    }
    t += 0.04;
    requestAnimationFrame(drawPhoneWave);
}
drawPhoneWave();

// Phone mute counter
let muteNum = 47;
setInterval(() => {
    if (Math.random() > 0.7) {
        muteNum++;
        document.getElementById('muteCount').innerHTML = `${muteNum} <span>↑</span>`;
    }
}, 3000);

// ── DEMO WAVE ──
const demoCanvas = document.getElementById('waveCanvas');
const dctx = demoCanvas.getContext('2d');
let dt = 0;
let demoActive = 'music';

function resizeDemoCanvas() {
    demoCanvas.width = demoCanvas.offsetWidth;
    demoCanvas.height = demoCanvas.offsetHeight;
}
resizeDemoCanvas();
window.addEventListener('resize', resizeDemoCanvas);

function drawDemoWave() {
    const W = demoCanvas.width, H = demoCanvas.height;
    dctx.clearRect(0, 0, W, H);

    if (demoActive === 'muting') {
        dctx.fillStyle = 'rgba(239,68,68,0.08)';
        dctx.fillRect(0, 0, W, H);
    }

    const bars = Math.floor(W / 10);
    for (let i = 0; i < bars; i++) {
        let amp = 1;
        if (demoActive === 'ad') amp = 1.6;
        if (demoActive === 'muting') amp = 0.3 + Math.random() * 0.1;
        if (demoActive === 'resume') amp = 0.8;

        const h = Math.abs(Math.sin(dt * 2.5 + i * 0.4)) * (H * 0.35 * amp) + 4;
        let color = demoActive === 'ad' ? `rgba(239,68,68,0.6)` :
            demoActive === 'muting' ? `rgba(239,68,68,0.2)` :
                `rgba(49,130,206,${0.35 + Math.abs(Math.sin(dt + i * 0.25)) * 0.45})`;
        dctx.fillStyle = color;
        dctx.beginPath();
        dctx.roundRect(i * 10 + 1, H / 2 - h / 2, 7, h, 3);
        dctx.fill();
    }
    dt += 0.05;
    requestAnimationFrame(drawDemoWave);
}
drawDemoWave();

function setDemo(mode, btn) {
    demoActive = mode;
    console.log('Demo mode changed to:', mode); // Debug log
    document.querySelectorAll('.demo-btn').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');

    const dot = document.querySelector('.demo-status-dot');
    const label = document.getElementById('demoStatusText');
    const statuses = {
        music: ['playing', 'Music stream active'],
        ad: ['', 'Ad detected - intercepting…'],
        muting: ['muting', 'Muting via AudioManager'],
        resume: ['playing', 'Music resumed seamlessly']
    };

    if (dot && label) {
        dot.className = 'demo-status-dot ' + statuses[mode][0];
        label.textContent = statuses[mode][1];
    }
}

// Ensure globally accessible
window.setDemo = setDemo;

// ── ENGINE TOGGLE ──
let engineOn = true;
function toggleEngine() {
    engineOn = !engineOn;
    const dot = document.getElementById('engineDot');
    const label = document.getElementById('engineLabel');
    const btn = document.getElementById('toggleBtn');
    if (engineOn) {
        dot.style.background = '#22C55E';
        label.textContent = 'Monitoring active';
        btn.textContent = 'Simulate mute →';
    } else {
        dot.style.background = '#EF4444';
        dot.style.boxShadow = '0 0 8px rgba(239,68,68,0.4)';
        label.textContent = 'Muting ad…';
        btn.textContent = 'Simulate track →';
        setTimeout(() => {
            if (!engineOn) {
                dot.style.background = '#22C55E';
                dot.style.boxShadow = '';
                label.textContent = 'Resumed - track playing';
                engineOn = true;
                btn.textContent = 'Simulate mute →';
            }
        }, 2500);
    }
}

// ── SCROLL REVEAL & AUDIT ANIMATION ──
const auditObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('visible');
            if (entry.target.classList.contains('score-card')) {
                animateAuditCard(entry.target);
            }
            auditObserver.unobserve(entry.target);
        }
    });
}, { threshold: 0.15 });

function animateAuditCard(card) {
    const scoreText = card.querySelector('.score-val');
    const ringActive = card.querySelector('.ring-active');
    const targetScore = parseFloat(card.dataset.score);
    const duration = 1200;
    const startTime = performance.now();
    const circumference = 175.9;

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const ease = 1 - Math.pow(1 - progress, 4); // Quartic ease out

        // Animate Text
        const currentScore = (targetScore * ease).toFixed(1);
        scoreText.textContent = currentScore;

        // Animate Ring
        const offset = circumference - (circumference * (targetScore / 10) * ease);
        ringActive.style.strokeDashoffset = offset;

        if (progress < 1) {
            requestAnimationFrame(update);
        } else {
            scoreText.textContent = targetScore.toFixed(1);
            ringActive.style.strokeDashoffset = circumference - (circumference * (targetScore / 10));
        }
    }
    requestAnimationFrame(update);
}

document.querySelectorAll('.reveal').forEach(el => auditObserver.observe(el));
document.querySelectorAll('.score-card').forEach(el => auditObserver.observe(el));

// ── COUNTER ANIMATION ──
function animateCounters() {
    document.querySelectorAll('[data-target]').forEach(el => {
        const target = parseInt(el.dataset.target);
        const suffix = target === 18 ? ' MB' : target === 3 ? '' : target >= 98 ? '' : '';
        let start = 0;
        const step = target / 40;
        const interval = setInterval(() => {
            start = Math.min(start + step, target);
            el.textContent = Math.round(start) + suffix;
            if (start >= target) clearInterval(interval);
        }, 30);
    });
}

const statsObserver = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting) { animateCounters(); statsObserver.disconnect(); }
}, { threshold: 0.4 });
statsObserver.observe(document.querySelector('.stats-strip'));

// ── HAMBURGER MENU ──
const hamburgerBtn = document.getElementById('hamburgerBtn');
const navLinks = document.getElementById('navLinks');
const navOverlay = document.getElementById('navOverlay');

function openNav() {
    navLinks.classList.add('open');
    navOverlay.classList.add('visible');
    hamburgerBtn.setAttribute('aria-expanded', 'true');
    document.body.style.overflow = 'hidden';
}

function closeNav() {
    navLinks.classList.remove('open');
    navOverlay.classList.remove('visible');
    hamburgerBtn.setAttribute('aria-expanded', 'false');
    document.body.style.overflow = '';
}

hamburgerBtn.addEventListener('click', () => {
    const isOpen = navLinks.classList.contains('open');
    if (isOpen) closeNav(); else openNav();
});

// Close on overlay click
navOverlay.addEventListener('click', closeNav);

// Close on nav link click
document.querySelectorAll('.nav-link, .nav-cta').forEach(link => {
    link.addEventListener('click', closeNav);
});

// Close on Escape key
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && navLinks.classList.contains('open')) closeNav();
});

// ── NAV ACTIVE SECTION ──
function updateActiveNav() {
    const sections = document.querySelectorAll('section[id], div.stats-strip');
    let current = '';
    sections.forEach(s => {
        const top = s.getBoundingClientRect().top;
        if (top < 200) current = s.id || '';
    });
    document.querySelectorAll('.nav-link').forEach(link => {
        link.style.background = '';
        if (link.getAttribute('href') === '#' + current) {
            link.style.background = 'rgba(28,25,23,0.06)';
        }
    });
}

// ── NAV SCROLL SHADOW ──
window.addEventListener('scroll', () => {
    const nav = document.getElementById('navbar');
    if (window.scrollY > 40) {
        nav.style.boxShadow = '0 8px 32px rgba(28,25,23,0.1), 0 1px 0 rgba(255,255,255,0.8) inset';
    } else {
        nav.style.boxShadow = '0 4px 24px rgba(28,25,23,0.06), 0 1px 0 rgba(255,255,255,0.8) inset';
    }
    updateActiveNav();
});

updateActiveNav();

// ── FAQ TOGGLE (dynamic scrollHeight) ──
function setFaqHeight(item) {
    const answer = item.querySelector('.faq-answer');
    if (item.classList.contains('open')) {
        answer.style.maxHeight = answer.scrollHeight + 'px';
    } else {
        answer.style.maxHeight = '0';
    }
}

function toggleFaq(btn) {
    const item = btn.parentElement;
    const wasOpen = item.classList.contains('open');
    document.querySelectorAll('.faq-item.open').forEach(el => {
        el.classList.remove('open');
        el.querySelector('.faq-answer').style.maxHeight = '0';
    });
    if (!wasOpen) {
        item.classList.add('open');
        requestAnimationFrame(() => setFaqHeight(item));
    }
}

// Recalculate FAQ heights on resize
let resizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
        document.querySelectorAll('.faq-item.open').forEach(setFaqHeight);
    }, 150);
});

// ── BLOCK REVEAL OBSERVER ──
const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('is-visible');
            revealObserver.unobserve(entry.target);
        }
    });
}, { threshold: 0.15, rootMargin: '0px 0px -40px 0px' });

document.querySelectorAll('.reveal-on-scroll').forEach(el => revealObserver.observe(el));

}); // end DOMContentLoaded