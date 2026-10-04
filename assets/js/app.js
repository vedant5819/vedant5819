/**
 * QR Code Attendance System - Core Application Logic & API Client
 */

const API_BASE = window.location.origin;

// ================== Auth State Management ==================
function getCurrentUser() {
    const userStr = localStorage.getItem('qr_attendance_user');
    if (!userStr) return null;
    try {
        return JSON.parse(userStr);
    } catch (e) {
        return null;
    }
}

function setCurrentUser(user) {
    localStorage.setItem('qr_attendance_user', JSON.stringify(user));
}

function logout() {
    localStorage.removeItem('qr_attendance_user');
    window.location.href = 'index.html';
}

function requireAuth(allowedRole) {
    const user = getCurrentUser();
    if (!user) {
        window.location.href = 'login.html';
        return null;
    }
    if (allowedRole && user.role !== allowedRole && user.role !== 'SUPER_ADMIN') {
        alert('Unauthorized access for role: ' + user.role);
        window.location.href = 'index.html';
        return null;
    }
    return user;
}

// ================== API Client Helper ==================
async function apiRequest(endpoint, method = 'GET', data = null) {
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json'
        }
    };
    if (data && (method === 'POST' || method === 'PUT')) {
        options.body = JSON.stringify(data);
    }
    try {
        const res = await fetch(API_BASE + endpoint, options);
        return await res.json();
    } catch (err) {
        console.error('API Error (' + endpoint + '):', err);
        return { success: false, error: err.message };
    }
}

// ================== Web Audio API Sound Synthesizer ==================
function playChime(isSuccess = true) {
    try {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (!AudioContext) return;
        const ctx = new AudioContext();

        if (isSuccess) {
            // Pleasant double beep: 587Hz (D5) -> 880Hz (A5)
            const osc1 = ctx.createOscillator();
            const gain1 = ctx.createGain();
            osc1.type = 'sine';
            osc1.frequency.setValueAtTime(587.33, ctx.currentTime);
            gain1.gain.setValueAtTime(0.15, ctx.currentTime);
            gain1.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.15);
            osc1.connect(gain1);
            gain1.connect(ctx.destination);
            osc1.start(ctx.currentTime);
            osc1.stop(ctx.currentTime + 0.15);

            const osc2 = ctx.createOscillator();
            const gain2 = ctx.createGain();
            osc2.type = 'sine';
            osc2.frequency.setValueAtTime(880.00, ctx.currentTime + 0.12);
            gain2.gain.setValueAtTime(0.2, ctx.currentTime + 0.12);
            gain2.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.35);
            osc2.connect(gain2);
            gain2.connect(ctx.destination);
            osc2.start(ctx.currentTime + 0.12);
            osc2.stop(ctx.currentTime + 0.35);
        } else {
            // Warning low buzz
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(220, ctx.currentTime);
            gain.gain.setValueAtTime(0.2, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.3);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start(ctx.currentTime);
            osc.stop(ctx.currentTime + 0.3);
        }
    } catch (e) {
        // Audio context may be blocked by browser policy until user gesture
    }
}

// ================== Toast Notification System ==================
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let icon = 'ℹ️';
    if (type === 'success') {
        icon = '✅';
        playChime(true);
    } else if (type === 'error') {
        icon = '❌';
        playChime(false);
    } else if (type === 'warning') {
        icon = '⚠️';
        playChime(false);
    }

    toast.innerHTML = `<span style="font-size:1.1rem">${icon}</span> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = 'all 0.3s ease';
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// ================== Geolocation Resolver ==================
function getDeviceGeolocation() {
    return new Promise((resolve) => {
        if (!navigator.geolocation) {
            resolve({ lat: 12.9716, lon: 77.5946, status: 'UNSUPPORTED' });
            return;
        }
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                resolve({
                    lat: pos.coords.latitude,
                    lon: pos.coords.longitude,
                    status: 'OK'
                });
            },
            (err) => {
                console.warn('Geolocation warning: ' + err.message + ' - using campus fallback coordinates');
                resolve({ lat: 12.9716, lon: 77.5946, status: 'FALLBACK' });
            },
            { timeout: 5000, enableHighAccuracy: true }
        );
    });
}

// ================== Tab Switching ==================
function initTabs(containerSelector = document) {
    const tabBtns = containerSelector.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-tab');
            const parent = btn.closest('.tabs-wrapper') || document;
            
            parent.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            parent.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            const targetContent = parent.querySelector('#' + targetId);
            if (targetContent) {
                targetContent.classList.add('active');
            }
        });
    });
}

// ================== Modal Helpers ==================
function openModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.add('active');
}

function closeModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.remove('active');
}

// ================== CSV Exporter ==================
function exportTableToCSV(tableId, filename = 'attendance_report.csv') {
    const table = document.getElementById(tableId);
    if (!table) return;

    let csv = [];
    const rows = table.querySelectorAll('tr');

    for (let i = 0; i < rows.length; i++) {
        let row = [], cols = rows[i].querySelectorAll('td, th');
        for (let j = 0; j < cols.length; j++) {
            // Strip buttons or action icons
            let text = cols[j].innerText.replace(/(\r\n|\n|\r)/gm, '').replace(/,/g, ';').trim();
            row.push('"' + text + '"');
        }
        csv.push(row.join(','));
    }

    const csvFile = new Blob([csv.join('\n')], { type: 'text/csv' });
    const downloadLink = document.createElement('a');
    downloadLink.download = filename;
    downloadLink.href = window.URL.createObjectURL(csvFile);
    downloadLink.style.display = 'none';
    document.body.appendChild(downloadLink);
    downloadLink.click();
    downloadLink.remove();
    showToast('Exported report: ' + filename, 'success');
}
