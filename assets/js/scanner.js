/**
 * QR Code Attendance System - Scanner Module
 * Integrates HTML5 Camera Scanner with Geolocation & Anti-Proxy Verification
 */

let html5QrCode = null;
let isScannerRunning = false;

async function initQRScanner(onSuccessCallback) {
    const scannerElement = document.getElementById("qr-reader");
    if (!scannerElement) return;

    try {
        html5QrCode = new Html5Qrcode("qr-reader");
        const cameras = await Html5Qrcode.getCameras();

        const cameraSelect = document.getElementById("camera-select");
        if (cameraSelect && cameras && cameras.length) {
            cameraSelect.innerHTML = "";
            cameras.forEach((cam, idx) => {
                const opt = document.createElement("option");
                opt.value = cam.id;
                opt.text = cam.label || `Camera ${idx + 1}`;
                cameraSelect.appendChild(opt);
            });
        }
    } catch (e) {
        console.warn("Unable to enumerate cameras:", e);
    }
}

async function startScanner(onSuccessCallback) {
    if (!html5QrCode) {
        await initQRScanner(onSuccessCallback);
    }
    if (!html5QrCode) {
        showToast("QR Scanner engine could not be initialized.", "error");
        return;
    }

    const cameraSelect = document.getElementById("camera-select");
    const cameraId = cameraSelect ? cameraSelect.value : null;
    const config = {
        fps: 10,
        qrbox: { width: 250, height: 250 },
        aspectRatio: 1.0
    };

    try {
        const cameraMode = cameraId ? cameraId : { facingMode: "environment" };
        await html5QrCode.start(
            cameraMode,
            config,
            (decodedText, decodedResult) => {
                handleScannedQRCode(decodedText, onSuccessCallback);
            },
            (errorMessage) => {
                // scanning frame error (ignore continuous scan attempts)
            }
        );
        isScannerRunning = true;
        const btnToggle = document.getElementById("btn-toggle-scan");
        if (btnToggle) btnToggle.innerHTML = "⏹️ Stop Camera";
        const laser = document.querySelector(".laser-scanner");
        if (laser) laser.style.display = "block";
    } catch (err) {
        console.error("Camera start error:", err);
        showToast("Camera access denied or unavailable. Use File Scan or Quick Simulator.", "warning");
    }
}

async function stopScanner() {
    if (html5QrCode && isScannerRunning) {
        try {
            await html5QrCode.stop();
            isScannerRunning = false;
            const btnToggle = document.getElementById("btn-toggle-scan");
            if (btnToggle) btnToggle.innerHTML = "📷 Start Camera Scanner";
            const laser = document.querySelector(".laser-scanner");
            if (laser) laser.style.display = "none";
        } catch (e) {
            console.error("Stop scanner error:", e);
        }
    }
}

async function handleScannedQRCode(qrPayload, onSuccessCallback) {
    // Temporarily pause scanner to avoid duplicate triggers
    if (isScannerRunning) {
        await stopScanner();
    }

    const user = getCurrentUser();
    if (!user) {
        showToast("Please log in as a student first.", "error");
        return;
    }

    showToast("Validating QR & verifying campus GPS...", "info");

    // Gather Anti-Proxy Metadata
    const geo = await getDeviceGeolocation();
    const deviceInfo = navigator.userAgent;

    const payload = {
        studentId: user.id,
        qrPayload: qrPayload,
        latitude: geo.lat,
        longitude: geo.lon,
        deviceInfo: deviceInfo
    };

    const res = await apiRequest("/api/student/scan-qr", "POST", payload);

    if (res.success) {
        showToast(res.message, "success");
        if (typeof onSuccessCallback === "function") {
            onSuccessCallback(res);
        }
    } else {
        showToast(res.message || "Failed to record attendance.", "error");
    }
}

// File scan handler
async function scanQRFromFile(file, onSuccessCallback) {
    if (!file) return;
    if (!html5QrCode) {
        html5QrCode = new Html5Qrcode("qr-reader");
    }
    try {
        const decodedText = await html5QrCode.scanFile(file, true);
        handleScannedQRCode(decodedText, onSuccessCallback);
    } catch (err) {
        showToast("Could not find a valid QR code in this image.", "error");
    }
}
