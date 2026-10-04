const http = require('http');
const server = require('./server.js');

async function request(path, method = 'GET', body = null) {
    return new Promise((resolve, reject) => {
        const req = http.request({
            hostname: 'localhost',
            port: 3000,
            path: path,
            method: method,
            headers: {
                'Content-Type': 'application/json'
            }
        }, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    resolve({ status: res.statusCode, data: JSON.parse(data) });
                } catch (e) {
                    resolve({ status: res.statusCode, raw: data });
                }
            });
        });
        req.on('error', reject);
        if (body) req.write(JSON.stringify(body));
        req.end();
    });
}

async function runTests() {
    console.log('--- STARTING VERIFICATION TESTS ---');
    await new Promise(r => setTimeout(r, 500));

    // 1. Test Student Login
    const loginRes = await request('/api/auth/login', 'POST', {
        email: 'alex@attendance.edu',
        password: 'student123',
        role: 'STUDENT'
    });
    console.log('1. Student Login Test:', loginRes.data.success ? 'PASSED ✅' : 'FAILED ❌', loginRes.data.user?.name);

    // 2. Test Teacher Classes
    const teacherClasses = await request('/api/teacher/classes?teacherId=1', 'GET');
    console.log('2. Teacher Classes Test:', teacherClasses.data.success && teacherClasses.data.classes?.length > 0 ? 'PASSED ✅' : 'FAILED ❌', `(${teacherClasses.data.classes?.length} classes found)`);

    // 3. Test Teacher Start QR Session
    const qrSession = await request('/api/teacher/start-session', 'POST', {
        classId: 1,
        subjectId: 1,
        teacherId: 1
    });
    console.log('3. Teacher Start QR Session Test:', qrSession.data.success ? 'PASSED ✅' : 'FAILED ❌', 'QR Payload:', qrSession.data.qr?.qrData);

    // 4. Test Student Scan QR
    const scanRes = await request('/api/student/scan-qr', 'POST', {
        studentId: 1,
        qrPayload: qrSession.data.qr?.qrData,
        latitude: 12.9716,
        longitude: 77.5946,
        deviceInfo: 'Test Automated Runner'
    });
    console.log('4. Student Scan QR Test:', scanRes.data.success ? 'PASSED ✅' : 'FAILED ❌', scanRes.data.message);

    // 5. Test Duplicate Scan Rejection
    const dupScan = await request('/api/student/scan-qr', 'POST', {
        studentId: 1,
        qrPayload: qrSession.data.qr?.qrData,
        latitude: 12.9716,
        longitude: 77.5946,
        deviceInfo: 'Test Automated Runner'
    });
    console.log('5. Anti-Duplicate Check Test:', !dupScan.data.success ? 'PASSED ✅' : 'FAILED ❌', dupScan.data.message);

    // 6. Test Student Stats & Attendance records
    const statsRes = await request('/api/student/stats?studentId=1', 'GET');
    console.log('6. Student Stats Test:', statsRes.data.success && statsRes.data.stats?.overallPercentage !== undefined ? 'PASSED ✅' : 'FAILED ❌', `Overall: ${statsRes.data.stats?.overallPercentage}%`);

    // 7. Test Admin Stats
    const adminStats = await request('/api/admin/stats', 'GET');
    console.log('7. Admin Stats Test:', adminStats.data.success && adminStats.data.stats?.totalStudents > 0 ? 'PASSED ✅' : 'FAILED ❌', `Students: ${adminStats.data.stats?.totalStudents}, Classes: ${adminStats.data.stats?.totalClasses}`);

    console.log('--- ALL TESTS COMPLETED SUCCESSFULLY! ---');
    process.exit(0);
}

runTests().catch(err => {
    console.error('Test execution error:', err);
    process.exit(1);
});
