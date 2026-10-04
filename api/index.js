const crypto = require('crypto');
const { loadDb, saveDb, nextId } = require('./db');

function calculateDistanceMeters(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

function getTodayStr() {
    return new Date().toISOString().split('T')[0];
}

function getTimeStr() {
    return new Date().toTimeString().split(' ')[0];
}

function logEvent(db, eventType, email, details, ip) {
    db.system_logs.unshift({
        log_id: nextId(db.system_logs, 'log_id'),
        event_type: eventType,
        user_email: email,
        details: details,
        ip_address: ip || '127.0.0.1',
        timestamp: new Date().toISOString()
    });
    if (db.system_logs.length > 200) db.system_logs.pop();
    saveDb();
}

function enrichAttendanceRecord(db, record) {
    const student = db.student.find(s => s.student_id === record.student_id) || {};
    const cls = db.class.find(c => c.class_id === record.class_id) || {};
    const sub = db.subject.find(s => s.subject_id === record.subject_id) || {};

    return {
        attendanceId: record.attendance_id,
        studentId: record.student_id,
        qrId: record.qr_id,
        classId: record.class_id,
        subjectId: record.subject_id,
        date: record.date,
        timeIn: record.time_in,
        status: record.status,
        latitude: record.latitude,
        longitude: record.longitude,
        ipAddress: record.ip_address,
        deviceInfo: record.device_info,
        studentName: student.name || 'Unknown',
        rollNo: student.roll_no || 'N/A',
        className: cls.class_name || 'N/A',
        subjectName: sub.subject_name || 'N/A',
        subjectCode: sub.code || 'N/A'
    };
}

function enrichClass(db, c) {
    const teacher = db.teacher.find(t => t.teacher_id === c.teacher_id) || {};
    const subject = db.subject.find(s => s.subject_id === c.subject_id) || {};

    return {
        classId: c.class_id,
        className: c.class_name,
        section: c.section,
        semester: c.semester,
        teacherId: c.teacher_id,
        subjectId: c.subject_id,
        roomNo: c.room_no,
        teacherName: teacher.name || 'Unknown',
        subjectName: subject.subject_name || 'Unknown',
        subjectCode: subject.code || 'N/A'
    };
}

module.exports = async function handler(req, res) {
    // CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        res.statusCode = 204;
        return res.end();
    }

    // Resolve URL and Query
    const host = req.headers.host || 'localhost';
    const rawUrl = req.headers['x-forwarded-url'] || req.headers['x-matched-path'] || req.url;
    const parsedUrl = new URL(rawUrl, `http://${host}`);
    let pathname = parsedUrl.pathname;

    const queryParams = {};
    parsedUrl.searchParams.forEach((v, k) => { queryParams[k] = v; });
    if (req.query) {
        Object.assign(queryParams, req.query);
    }

    // Accurately resolve original API pathname if rewritten by Vercel
    if (queryParams.__api_path) {
        pathname = '/api/' + queryParams.__api_path.replace(/^\/+/, '');
    } else if (pathname === '/api/index.js' || pathname === '/api') {
        if (queryParams.match) {
            pathname = '/api/' + queryParams.match.replace(/^\/+/, '');
        } else if (queryParams.route) {
            const routeSegments = Array.isArray(queryParams.route) ? queryParams.route : [queryParams.route];
            pathname = '/api/' + routeSegments.join('/');
        }
    }

    // Parse JSON body
    let body = req.body;
    if (typeof body === 'string') {
        try { body = JSON.parse(body); } catch (e) { body = {}; }
    } else if (!body && (req.method === 'POST' || req.method === 'PUT')) {
        body = await new Promise((resolve) => {
            let data = '';
            req.on('data', chunk => data += chunk);
            req.on('end', () => {
                try { resolve(JSON.parse(data)); } catch (e) { resolve({}); }
            });
        });
    }
    body = body || {};

    const db = loadDb();
    const method = req.method.toUpperCase();
    const clientIp = req.headers['x-forwarded-for'] || req.connection?.remoteAddress || '127.0.0.1';

    let response = {};
    let statusCode = 200;

    try {
        // ==================== AUTHENTICATION ====================
        if (pathname === '/api/auth/login' && method === 'POST') {
            const { email, password, role } = body;
            const cleanEmail = (email || '').trim().toLowerCase();
            const cleanPass = (password || '').trim();

            let foundUser = null;
            let userRole = (role || '').toUpperCase();

            if (userRole === 'ADMIN' || !role) {
                const admin = db.admin.find(a => a.email.toLowerCase() === cleanEmail && a.password === cleanPass);
                if (admin) {
                    foundUser = { id: admin.admin_id, name: admin.name, email: admin.email, role: admin.role || 'SUPER_ADMIN' };
                }
            }
            if (!foundUser && (userRole === 'TEACHER' || !role)) {
                const teacher = db.teacher.find(t => t.email.toLowerCase() === cleanEmail && t.password === cleanPass);
                if (teacher) {
                    foundUser = { id: teacher.teacher_id, name: teacher.name, email: teacher.email, role: 'TEACHER' };
                    response.teacherDetails = teacher;
                }
            }
            if (!foundUser && (userRole === 'STUDENT' || !role)) {
                const student = db.student.find(s => s.email.toLowerCase() === cleanEmail && s.password === cleanPass);
                if (student) {
                    foundUser = { id: student.student_id, name: student.name, email: student.email, role: 'STUDENT' };
                    response.studentDetails = student;
                }
            }

            if (foundUser) {
                response.success = true;
                response.message = 'Login successful';
                response.user = foundUser;
            } else {
                response.success = false;
                response.message = 'Invalid email, password, or role selection';
                statusCode = 401;
            }

        } else if (pathname === '/api/auth/register' && method === 'POST') {
            const { name, email, password, rollNo, department, semester, phone } = body;
            const emailExists = db.student.some(s => s.email.toLowerCase() === (email || '').trim().toLowerCase());
            const rollExists = db.student.some(s => s.roll_no.toLowerCase() === (rollNo || '').trim().toLowerCase());

            if (emailExists || rollExists) {
                response.success = false;
                response.message = 'Registration failed. Email or Roll Number may already exist.';
            } else {
                const newId = nextId(db.student, 'student_id');
                const newStudent = {
                    student_id: newId,
                    name: (name || '').trim(),
                    email: (email || '').trim(),
                    password: (password || '').trim(),
                    roll_no: (rollNo || '').trim(),
                    department: (department || 'Computer Science').trim(),
                    semester: parseInt(semester, 10) || 1,
                    phone: (phone || '').trim(),
                    status: 'ACTIVE',
                    created_at: new Date().toISOString()
                };
                db.student.push(newStudent);
                saveDb();
                logEvent(db, 'STUDENT_REGISTER', newStudent.email, `Student self-registered with Roll: ${newStudent.roll_no}`, clientIp);
                response.success = true;
                response.message = 'Student registered successfully! You can now log in.';
            }

        } else if (pathname === '/api/auth/forgot-password' && method === 'POST') {
            const { email, role, newPassword } = body;
            const r = (role || '').toUpperCase();
            const cleanEmail = (email || '').trim().toLowerCase();
            let updated = false;

            if (r === 'ADMIN') {
                const u = db.admin.find(a => a.email.toLowerCase() === cleanEmail);
                if (u) { u.password = newPassword; updated = true; }
            } else if (r === 'TEACHER') {
                const u = db.teacher.find(t => t.email.toLowerCase() === cleanEmail);
                if (u) { u.password = newPassword; updated = true; }
            } else if (r === 'STUDENT') {
                const u = db.student.find(s => s.email.toLowerCase() === cleanEmail);
                if (u) { u.password = newPassword; updated = true; }
            }

            if (updated) {
                saveDb();
                logEvent(db, 'PASSWORD_RESET', cleanEmail, `Password reset for role: ${r}`, clientIp);
                response.success = true;
                response.message = 'Password has been successfully reset! You can now log in.';
            } else {
                response.success = false;
                response.message = 'No user found with the provided email and role.';
            }

        } else if (pathname === '/api/auth/change-password' && method === 'POST') {
            const { userId, role, oldPassword, newPassword } = body;
            const uid = parseInt(userId, 10);
            const r = (role || '').toUpperCase();
            let user = null;

            if (r === 'ADMIN') user = db.admin.find(a => a.admin_id === uid);
            else if (r === 'TEACHER') user = db.teacher.find(t => t.teacher_id === uid);
            else if (r === 'STUDENT') user = db.student.find(s => s.student_id === uid);

            if (user && user.password === oldPassword) {
                user.password = newPassword;
                saveDb();
                response.success = true;
                response.message = 'Password updated successfully!';
            } else {
                response.success = false;
                response.message = 'Current password was incorrect.';
            }

        // ==================== STUDENT APIS ====================
        } else if (pathname === '/api/student/profile' && method === 'GET') {
            const studentId = parseInt(queryParams.studentId, 10);
            const s = db.student.find(st => st.student_id === studentId);
            if (s) {
                const sanitized = { ...s, password: '' };
                response.success = true;
                response.profile = sanitized;
            } else {
                response.success = false;
                response.message = 'Student not found';
            }

        } else if (pathname === '/api/student/profile' && method === 'POST') {
            const { studentId, name, phone, department, semester } = body;
            const sid = parseInt(studentId, 10);
            const s = db.student.find(st => st.student_id === sid);
            if (s) {
                if (name) s.name = name;
                if (phone !== undefined) s.phone = phone;
                if (department) s.department = department;
                if (semester) s.semester = parseInt(semester, 10);
                saveDb();
                response.success = true;
                response.message = 'Profile updated successfully!';
            } else {
                response.success = false;
                response.message = 'Failed to update profile.';
            }

        } else if (pathname === '/api/student/delete-profile' && method === 'POST') {
            const studentId = parseInt(body.studentId, 10);
            const idx = db.student.findIndex(st => st.student_id === studentId);
            if (idx !== -1) {
                db.student.splice(idx, 1);
                db.attendance = db.attendance.filter(a => a.student_id !== studentId);
                saveDb();
                response.success = true;
                response.message = 'Profile deleted successfully.';
            } else {
                response.success = false;
                response.message = 'Failed to delete profile.';
            }

        } else if (pathname === '/api/student/attendance' && method === 'GET') {
            const studentId = parseInt(queryParams.studentId, 10);
            const records = db.attendance
                .filter(a => a.student_id === studentId)
                .sort((a, b) => (b.date + ' ' + b.time_in).localeCompare(a.date + ' ' + a.time_in))
                .map(r => enrichAttendanceRecord(db, r));
            response.success = true;
            response.records = records;

        } else if (pathname === '/api/student/stats' && method === 'GET') {
            const studentId = parseInt(queryParams.studentId, 10);
            const myRecords = db.attendance.filter(a => a.student_id === studentId);

            let presentCount = 0;
            let lateCount = 0;
            for (const r of myRecords) {
                if (r.status === 'PRESENT') presentCount++;
                else if (r.status === 'LATE') lateCount++;
            }
            const totalAttended = presentCount + lateCount;

            const uniqueLectures = new Set(db.attendance.map(a => `${a.date}-${a.class_id}`));
            let totalLecturesConducted = uniqueLectures.size;
            if (totalLecturesConducted < totalAttended) totalLecturesConducted = totalAttended;
            if (totalLecturesConducted === 0) totalLecturesConducted = 1;

            const overallPercentage = Math.round(((totalAttended / totalLecturesConducted) * 100) * 10) / 10;

            const subjectStats = db.subject.map(sub => {
                const attended = myRecords.filter(a => a.subject_id === sub.subject_id).length;
                const subLectures = new Set(db.attendance.filter(a => a.subject_id === sub.subject_id).map(a => `${a.date}-${a.class_id}`));
                let totalSub = subLectures.size;
                if (totalSub < attended) totalSub = attended;
                if (totalSub === 0) totalSub = Math.max(attended, 4);

                const subPct = Math.round(((attended / totalSub) * 100) * 10) / 10;
                const statusBadge = subPct >= 75 ? 'Safe' : (subPct >= 65 ? 'Warning' : 'Critical');

                return {
                    subjectId: sub.subject_id,
                    subjectName: sub.subject_name,
                    subjectCode: sub.code,
                    attended: attended,
                    total: totalSub,
                    percentage: subPct,
                    statusBadge: statusBadge
                };
            });

            response.success = true;
            response.stats = {
                presentCount,
                lateCount,
                totalAttended,
                totalLecturesConducted,
                overallPercentage,
                isEligible: overallPercentage >= 75.0,
                subjectStats
            };

        } else if (pathname === '/api/student/scan-qr' && method === 'POST') {
            const { studentId, qrPayload, latitude, longitude, deviceInfo } = body;
            const sid = parseInt(studentId, 10);

            if (!qrPayload || !qrPayload.trim()) {
                response.success = false;
                response.message = 'Invalid QR code data. Please scan a valid class QR code.';
            } else {
                const qr = db.qr_code.slice().reverse().find(q => q.qr_data === qrPayload.trim());
                const nowMs = Date.now();
                const isExpired = !qr || qr.status === 'EXPIRED' || (qr.expiry_ms ? nowMs > qr.expiry_ms : (new Date(qr.expiry_time).getTime() || 0) < nowMs);

                if (!qr) {
                    logEvent(db, 'PROXY_ATTEMPT', `Student ID: ${sid}`, 'Scan rejected: QR code not found or forged', clientIp);
                    response.success = false;
                    response.message = 'Unrecognized or fake QR code!';
                } else if (isExpired) {
                    qr.status = 'EXPIRED';
                    saveDb();
                    logEvent(db, 'EXPIRED_QR_SCAN', `Student ID: ${sid}`, `Attempted scan of expired QR code ID: ${qr.qr_id}`, clientIp);
                    response.success = false;
                    response.message = 'QR Code has expired! Please ask your teacher for the refreshed QR code.';
                } else {
                    const geofenceEnabled = db.system_settings['geofence_enabled'] === 'true';
                    let passedGeofence = true;

                    if (geofenceEnabled) {
                        const campusLat = parseFloat(db.system_settings['campus_latitude'] || '12.9716');
                        const campusLon = parseFloat(db.system_settings['campus_longitude'] || '77.5946');
                        const maxRadius = parseFloat(db.system_settings['geofence_radius_meters'] || '500');

                        if (latitude === undefined || longitude === undefined || latitude === null || longitude === null) {
                            passedGeofence = false;
                            logEvent(db, 'PROXY_ATTEMPT', `Student ID: ${sid}`, 'Location disabled while geofencing is enforced', clientIp);
                            response.success = false;
                            response.message = 'Location access is required by institutional anti-proxy policy. Please enable GPS.';
                        } else {
                            const distance = calculateDistanceMeters(latitude, longitude, campusLat, campusLon);
                            if (distance > maxRadius) {
                                passedGeofence = false;
                                logEvent(db, 'PROXY_ATTEMPT', `Student ID: ${sid}`, `Location out of bounds: ${distance.toFixed(1)}m away`, clientIp);
                                response.success = false;
                                response.message = `Anti-Proxy Alert: You appear to be ${distance.toFixed(1)} meters outside the campus classroom boundary.`;
                            }
                        }
                    }

                    if (passedGeofence) {
                        const todayStr = getTodayStr();
                        const timeStr = getTimeStr();

                        const alreadyMarked = db.attendance.find(a => a.student_id === sid && a.class_id === qr.class_id && a.date === todayStr);
                        if (alreadyMarked) {
                            response.success = false;
                            response.message = `You have already marked your attendance for this class today at ${alreadyMarked.time_in}`;
                        } else {
                            const newAttId = nextId(db.attendance, 'attendance_id');
                            const newRecord = {
                                attendance_id: newAttId,
                                student_id: sid,
                                qr_id: qr.qr_id,
                                class_id: qr.class_id,
                                subject_id: qr.subject_id,
                                date: todayStr,
                                time_in: timeStr,
                                status: 'PRESENT',
                                latitude: latitude || null,
                                longitude: longitude || null,
                                ip_address: clientIp,
                                device_info: deviceInfo || 'Vercel Web App',
                                created_at: new Date().toISOString()
                            };
                            db.attendance.push(newRecord);
                            saveDb();
                            logEvent(db, 'ATTENDANCE_MARKED', `Student ID: ${sid}`, `Marked attendance successfully for class ID: ${qr.class_id}`, clientIp);

                            const enriched = enrichAttendanceRecord(db, newRecord);
                            response.success = true;
                            response.message = 'Attendance recorded successfully!';
                            response.attendanceId = newAttId;
                            response.time = timeStr;
                            response.date = todayStr;
                            response.details = enriched;
                        }
                    }
                }
            }

        // ==================== TEACHER APIS ====================
        } else if (pathname === '/api/teacher/classes' && method === 'GET') {
            const teacherId = parseInt(queryParams.teacherId, 10);
            const classes = db.class
                .filter(c => c.teacher_id === teacherId)
                .map(c => enrichClass(db, c));
            response.success = true;
            response.classes = classes;

        } else if (pathname === '/api/teacher/start-session' && method === 'POST') {
            const { classId, subjectId, teacherId } = body;
            const cid = parseInt(classId, 10);
            const sid = parseInt(subjectId, 10);
            const tid = parseInt(teacherId, 10);

            // Expire old active QR codes for this class
            db.qr_code.forEach(q => {
                if (q.class_id === cid && q.status === 'ACTIVE') q.status = 'EXPIRED';
            });

            const expirySeconds = parseInt(db.system_settings['qr_expiry_seconds'] || '30', 10);
            const now = new Date();
            const expiry = new Date(now.getTime() + expirySeconds * 1000);

            const token = crypto.randomBytes(6).toString('hex');
            const qrPayload = `ATTEND::CLASS:${cid}::SUB:${sid}::TEACH:${tid}::TS:${now.getTime()}::TOK:${token}`;

            const newQrId = nextId(db.qr_code, 'qr_id');
            const newQr = {
                qr_id: newQrId,
                class_id: cid,
                subject_id: sid,
                generated_by: tid,
                qr_data: qrPayload,
                generated_time: now.toISOString().replace('T', ' ').substring(0, 19),
                expiry_time: expiry.toISOString().replace('T', ' ').substring(0, 19),
                expiry_ms: expiry.getTime(),
                status: 'ACTIVE'
            };
            db.qr_code.push(newQr);
            saveDb();

            response.success = true;
            response.qr = {
                qrId: newQr.qr_id,
                classId: newQr.class_id,
                subjectId: newQr.subject_id,
                generatedBy: newQr.generated_by,
                qrData: newQr.qr_data,
                generatedTime: newQr.generated_time,
                expiryTime: newQr.expiry_time,
                status: newQr.status
            };
            response.expirySeconds = expirySeconds;

        } else if (pathname === '/api/teacher/active-qr' && method === 'GET') {
            const classId = parseInt(queryParams.classId, 10);
            const qr = db.qr_code.slice().reverse().find(q => q.class_id === classId && q.status === 'ACTIVE');

            if (qr) {
                const nowMs = Date.now();
                const isExpired = qr.status === 'EXPIRED' || (qr.expiry_ms ? nowMs > qr.expiry_ms : (new Date(qr.expiry_time).getTime() || 0) < nowMs);
                if (isExpired) {
                    qr.status = 'EXPIRED';
                    saveDb();
                    response.success = false;
                    response.message = 'No active QR session found for this class.';
                } else {
                    response.success = true;
                    response.qr = {
                        qrId: qr.qr_id,
                        classId: qr.class_id,
                        subjectId: qr.subject_id,
                        generatedBy: qr.generated_by,
                        qrData: qr.qr_data,
                        generatedTime: qr.generated_time,
                        expiryTime: qr.expiry_time,
                        status: qr.status
                    };
                }
            } else {
                response.success = false;
                response.message = 'No active QR session found for this class.';
            }

        } else if (pathname === '/api/teacher/live-attendance' && method === 'GET') {
            const classId = parseInt(queryParams.classId, 10);
            const date = queryParams.date || getTodayStr();

            const records = db.attendance
                .filter(a => a.class_id === classId && a.date === date)
                .sort((a, b) => b.time_in.localeCompare(a.time_in))
                .map(r => enrichAttendanceRecord(db, r));

            response.success = true;
            response.records = records;

        } else if (pathname === '/api/teacher/manual-mark' && method === 'POST') {
            const { studentId, classId, subjectId, date, status } = body;
            const newAttId = nextId(db.attendance, 'attendance_id');
            const newRecord = {
                attendance_id: newAttId,
                student_id: parseInt(studentId, 10),
                qr_id: null,
                class_id: parseInt(classId, 10),
                subject_id: parseInt(subjectId, 10),
                date: date || getTodayStr(),
                time_in: getTimeStr(),
                status: status || 'PRESENT',
                latitude: null,
                longitude: null,
                ip_address: clientIp,
                device_info: 'MANUAL_OVERRIDE',
                created_at: new Date().toISOString()
            };
            db.attendance.push(newRecord);
            saveDb();
            response.success = true;
            response.message = 'Manual attendance recorded successfully!';

        // ==================== ADMIN APIS ====================
        } else if (pathname === '/api/admin/stats' && method === 'GET') {
            const today = getTodayStr();
            const now = new Date();
            const activeQRs = db.qr_code.filter(q => q.status === 'ACTIVE' && new Date(q.expiry_time) > now).length;

            response.success = true;
            response.stats = {
                totalStudents: db.student.length,
                totalTeachers: db.teacher.length,
                totalClasses: db.class.length,
                totalSubjects: db.subject.length,
                todayAttendanceCount: db.attendance.filter(a => a.date === today).length,
                activeQRSessions: activeQRs
            };

        } else if (pathname === '/api/admin/students' && method === 'GET') {
            const students = db.student.map(s => ({
                id: s.student_id,
                name: s.name,
                email: s.email,
                password: s.password,
                rollNo: s.roll_no,
                department: s.department,
                semester: s.semester,
                phone: s.phone,
                status: s.status
            }));
            response.success = true;
            response.students = students;

        } else if (pathname === '/api/admin/students' && method === 'POST') {
            const id = parseInt(body.id, 10) || 0;
            if (id > 0) {
                const s = db.student.find(st => st.student_id === id);
                if (s) {
                    if (body.name) s.name = body.name;
                    if (body.email) s.email = body.email;
                    if (body.password) s.password = body.password;
                    if (body.rollNo) s.roll_no = body.rollNo;
                    if (body.department) s.department = body.department;
                    if (body.semester) s.semester = parseInt(body.semester, 10);
                    if (body.phone !== undefined) s.phone = body.phone;
                    if (body.status) s.status = body.status;
                }
            } else {
                const newId = nextId(db.student, 'student_id');
                db.student.push({
                    student_id: newId,
                    name: body.name || '',
                    email: body.email || '',
                    password: body.password || '',
                    roll_no: body.rollNo || '',
                    department: body.department || 'Computer Science',
                    semester: parseInt(body.semester, 10) || 1,
                    phone: body.phone || '',
                    status: body.status || 'ACTIVE',
                    created_at: new Date().toISOString()
                });
            }
            saveDb();
            response.success = true;
            response.message = 'Student record saved successfully!';

        } else if (pathname === '/api/admin/students' && method === 'DELETE') {
            const id = parseInt(queryParams.id, 10);
            db.student = db.student.filter(s => s.student_id !== id);
            db.attendance = db.attendance.filter(a => a.student_id !== id);
            saveDb();
            response.success = true;
            response.message = 'Student deleted successfully.';

        } else if (pathname === '/api/admin/teachers' && method === 'GET') {
            const teachers = db.teacher.map(t => ({
                id: t.teacher_id,
                name: t.name,
                email: t.email,
                password: t.password,
                department: t.department,
                phone: t.phone
            }));
            response.success = true;
            response.teachers = teachers;

        } else if (pathname === '/api/admin/teachers' && method === 'POST') {
            const id = parseInt(body.id, 10) || 0;
            if (id > 0) {
                const t = db.teacher.find(tc => tc.teacher_id === id);
                if (t) {
                    if (body.name) t.name = body.name;
                    if (body.email) t.email = body.email;
                    if (body.password) t.password = body.password;
                    if (body.department) t.department = body.department;
                    if (body.phone !== undefined) t.phone = body.phone;
                }
            } else {
                const newId = nextId(db.teacher, 'teacher_id');
                db.teacher.push({
                    teacher_id: newId,
                    name: body.name || '',
                    email: body.email || '',
                    password: body.password || '',
                    department: body.department || 'Computer Science',
                    phone: body.phone || '',
                    created_at: new Date().toISOString()
                });
            }
            saveDb();
            response.success = true;
            response.message = 'Teacher record saved successfully!';

        } else if (pathname === '/api/admin/teachers' && method === 'DELETE') {
            const id = parseInt(queryParams.id, 10);
            db.teacher = db.teacher.filter(t => t.teacher_id !== id);
            saveDb();
            response.success = true;
            response.message = 'Teacher deleted successfully.';

        } else if (pathname === '/api/admin/subjects' && method === 'GET') {
            const subjects = db.subject.map(s => ({
                subjectId: s.subject_id,
                subjectName: s.subject_name,
                code: s.code,
                department: s.department
            }));
            response.success = true;
            response.subjects = subjects;

        } else if (pathname === '/api/admin/subjects' && method === 'POST') {
            const id = parseInt(body.id, 10) || 0;
            if (id > 0) {
                const s = db.subject.find(sub => sub.subject_id === id);
                if (s) {
                    if (body.subjectName) s.subject_name = body.subjectName;
                    if (body.code) s.code = body.code;
                    if (body.department) s.department = body.department;
                }
            } else {
                const newId = nextId(db.subject, 'subject_id');
                db.subject.push({
                    subject_id: newId,
                    subject_name: body.subjectName || '',
                    code: body.code || '',
                    department: body.department || 'Computer Science',
                    created_at: new Date().toISOString()
                });
            }
            saveDb();
            response.success = true;
            response.message = 'Subject saved successfully!';

        } else if (pathname === '/api/admin/subjects' && method === 'DELETE') {
            const id = parseInt(queryParams.id, 10);
            db.subject = db.subject.filter(s => s.subject_id !== id);
            saveDb();
            response.success = true;
            response.message = 'Subject deleted successfully.';

        } else if (pathname === '/api/admin/classes' && method === 'GET') {
            const classes = db.class.map(c => enrichClass(db, c));
            response.success = true;
            response.classes = classes;

        } else if (pathname === '/api/admin/classes' && method === 'POST') {
            const id = parseInt(body.id, 10) || 0;
            if (id > 0) {
                const c = db.class.find(cl => cl.class_id === id);
                if (c) {
                    if (body.className) c.class_name = body.className;
                    if (body.section) c.section = body.section;
                    if (body.semester) c.semester = parseInt(body.semester, 10);
                    if (body.teacherId) c.teacher_id = parseInt(body.teacherId, 10);
                    if (body.subjectId) c.subject_id = parseInt(body.subjectId, 10);
                    if (body.roomNo) c.room_no = body.roomNo;
                }
            } else {
                const newId = nextId(db.class, 'class_id');
                db.class.push({
                    class_id: newId,
                    class_name: body.className || '',
                    section: body.section || 'Sec-A',
                    semester: parseInt(body.semester, 10) || 1,
                    teacher_id: parseInt(body.teacherId, 10) || 1,
                    subject_id: parseInt(body.subjectId, 10) || 1,
                    room_no: body.roomNo || 'Room 101',
                    created_at: new Date().toISOString()
                });
            }
            saveDb();
            response.success = true;
            response.message = 'Class saved successfully!';

        } else if (pathname === '/api/admin/classes' && method === 'DELETE') {
            const id = parseInt(queryParams.id, 10);
            db.class = db.class.filter(c => c.class_id !== id);
            saveDb();
            response.success = true;
            response.message = 'Class deleted successfully.';

        } else if (pathname === '/api/admin/attendance' && method === 'GET') {
            const classId = queryParams.classId ? parseInt(queryParams.classId, 10) : null;
            const subjectId = queryParams.subjectId ? parseInt(queryParams.subjectId, 10) : null;
            const date = queryParams.date ? queryParams.date.trim() : null;

            let records = db.attendance.slice();
            if (classId) records = records.filter(a => a.class_id === classId);
            if (subjectId) records = records.filter(a => a.subject_id === subjectId);
            if (date) records = records.filter(a => a.date === date);

            records = records
                .sort((a, b) => (b.date + ' ' + b.time_in).localeCompare(a.date + ' ' + a.time_in))
                .slice(0, 200)
                .map(r => enrichAttendanceRecord(db, r));

            response.success = true;
            response.records = records;

        } else if (pathname === '/api/admin/attendance' && method === 'PUT') {
            const attId = parseInt(body.attendanceId, 10);
            const { status } = body;
            const record = db.attendance.find(a => a.attendance_id === attId);
            if (record) {
                record.status = status || 'PRESENT';
                saveDb();
                response.success = true;
                response.message = 'Attendance status updated.';
            } else {
                response.success = false;
                response.message = 'Failed to update attendance status.';
            }

        } else if (pathname === '/api/admin/attendance' && method === 'DELETE') {
            const id = parseInt(queryParams.id, 10);
            const before = db.attendance.length;
            db.attendance = db.attendance.filter(a => a.attendance_id !== id);
            saveDb();
            response.success = db.attendance.length < before;
            response.message = response.success ? 'Attendance record deleted.' : 'Failed to delete attendance record.';

        } else if (pathname === '/api/admin/settings' && method === 'GET') {
            response.success = true;
            response.settings = { ...db.system_settings };

        } else if (pathname === '/api/admin/settings' && method === 'POST') {
            for (const [k, v] of Object.entries(body)) {
                db.system_settings[k] = String(v);
            }
            saveDb();
            response.success = true;
            response.message = 'System anti-proxy & QR settings saved successfully.';

        } else if (pathname === '/api/admin/logs' && method === 'GET') {
            response.success = true;
            response.logs = (db.system_logs || []).slice(0, 100).map(l => ({
                logId: l.log_id,
                eventType: l.event_type,
                userEmail: l.user_email,
                details: l.details,
                ipAddress: l.ip_address,
                timestamp: l.timestamp
            }));

        } else {
            response.error = 'Not Found';
            response.path = pathname;
            response.method = method;
            statusCode = 404;
        }

    } catch (err) {
        console.error('API Serverless Error:', err);
        response.success = false;
        response.error = err.message;
        statusCode = 500;
    }

    res.statusCode = statusCode;
    res.setHeader('Content-Type', 'application/json; charset=UTF-8');
    res.end(JSON.stringify(response));
};
