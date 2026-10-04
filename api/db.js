// In-memory data store with /tmp/ JSON persistence for Vercel Serverless environment
const fs = require('fs');
const path = require('path');
const os = require('os');

const TMP_FILE = path.join(os.tmpdir(), 'qr_attendance_db.json');

function getPastDate(daysAgo) {
    const d = new Date();
    d.setDate(d.getDate() - daysAgo);
    return d.toISOString().split('T')[0];
}

function getInitialSeedData() {
    return {
        admin: [
            { admin_id: 1, name: 'System Administrator', email: 'admin@attendance.edu', password: 'admin123', role: 'SUPER_ADMIN', created_at: new Date().toISOString() }
        ],
        teacher: [
            { teacher_id: 1, name: 'Dr. Alan Turing', email: 'turing@attendance.edu', password: 'teacher123', department: 'Computer Science', phone: '+1 987-654-3210', created_at: new Date().toISOString() },
            { teacher_id: 2, name: 'Prof. Ada Lovelace', email: 'ada@attendance.edu', password: 'teacher123', department: 'Information Technology', phone: '+1 987-654-3211', created_at: new Date().toISOString() },
            { teacher_id: 3, name: 'Prof. John von Neumann', email: 'john@attendance.edu', password: 'teacher123', department: 'Computer Science', phone: '+1 987-654-3212', created_at: new Date().toISOString() }
        ],
        student: [
            { student_id: 1, name: 'Alex Morgan', email: 'alex@attendance.edu', password: 'student123', roll_no: 'CS-2024-001', department: 'Computer Science', semester: 6, phone: '+1 555-0101', status: 'ACTIVE', created_at: new Date().toISOString() },
            { student_id: 2, name: 'Bella Chen', email: 'bella@attendance.edu', password: 'student123', roll_no: 'CS-2024-002', department: 'Computer Science', semester: 6, phone: '+1 555-0102', status: 'ACTIVE', created_at: new Date().toISOString() },
            { student_id: 3, name: 'Carlos Gomez', email: 'carlos@attendance.edu', password: 'student123', roll_no: 'IT-2024-003', department: 'Information Technology', semester: 6, phone: '+1 555-0103', status: 'ACTIVE', created_at: new Date().toISOString() },
            { student_id: 4, name: 'Diana Prince', email: 'diana@attendance.edu', password: 'student123', roll_no: 'CS-2024-004', department: 'Computer Science', semester: 6, phone: '+1 555-0104', status: 'ACTIVE', created_at: new Date().toISOString() },
            { student_id: 5, name: 'Ethan Hunt', email: 'ethan@attendance.edu', password: 'student123', roll_no: 'CS-2024-005', department: 'Computer Science', semester: 6, phone: '+1 555-0105', status: 'ACTIVE', created_at: new Date().toISOString() }
        ],
        subject: [
            { subject_id: 1, subject_name: 'Java Enterprise & Cloud Computing', code: 'CS601', department: 'Computer Science', created_at: new Date().toISOString() },
            { subject_id: 2, subject_name: 'Database Management Systems', code: 'CS602', department: 'Computer Science', created_at: new Date().toISOString() },
            { subject_id: 3, subject_name: 'Web Technologies & Frameworks', code: 'IT603', department: 'Information Technology', created_at: new Date().toISOString() },
            { subject_id: 4, subject_name: 'Computer Networks & Security', code: 'CS604', department: 'Computer Science', created_at: new Date().toISOString() }
        ],
        class: [
            { class_id: 1, class_name: 'Java Advanced Programming', section: 'Sec-A', semester: 6, teacher_id: 1, subject_id: 1, room_no: 'Lab 302', created_at: new Date().toISOString() },
            { class_id: 2, class_name: 'Advanced Database Systems', section: 'Sec-A', semester: 6, teacher_id: 2, subject_id: 2, room_no: 'Hall B', created_at: new Date().toISOString() },
            { class_id: 3, class_name: 'Full Stack Web Development', section: 'Sec-B', semester: 6, teacher_id: 2, subject_id: 3, room_no: 'Lab 201', created_at: new Date().toISOString() },
            { class_id: 4, class_name: 'Network Security & Cryptography', section: 'Sec-A', semester: 6, teacher_id: 3, subject_id: 4, room_no: 'Lab 105', created_at: new Date().toISOString() }
        ],
        qr_code: [],
        attendance: [
            { attendance_id: 1, student_id: 1, qr_id: null, class_id: 1, subject_id: 1, date: getPastDate(2), time_in: '09:05:00', status: 'PRESENT', latitude: 12.9716, longitude: 77.5946, ip_address: '192.168.1.101', device_info: 'Mobile Chrome / Android', created_at: new Date().toISOString() },
            { attendance_id: 2, student_id: 2, qr_id: null, class_id: 1, subject_id: 1, date: getPastDate(2), time_in: '09:07:00', status: 'PRESENT', latitude: 12.9716, longitude: 77.5946, ip_address: '192.168.1.102', device_info: 'Mobile Safari / iOS', created_at: new Date().toISOString() },
            { attendance_id: 3, student_id: 3, qr_id: null, class_id: 1, subject_id: 1, date: getPastDate(2), time_in: '09:18:00', status: 'LATE', latitude: 12.9716, longitude: 77.5946, ip_address: '192.168.1.103', device_info: 'Desktop Chrome / Windows', created_at: new Date().toISOString() },
            { attendance_id: 4, student_id: 4, qr_id: null, class_id: 1, subject_id: 1, date: getPastDate(2), time_in: '09:03:00', status: 'PRESENT', latitude: 12.9716, longitude: 77.5946, ip_address: '192.168.1.104', device_info: 'Mobile Chrome / Android', created_at: new Date().toISOString() },
            { attendance_id: 5, student_id: 1, qr_id: null, class_id: 2, subject_id: 2, date: getPastDate(1), time_in: '11:02:00', status: 'PRESENT', latitude: 12.9716, longitude: 77.5946, ip_address: '192.168.1.101', device_info: 'Mobile Chrome / Android', created_at: new Date().toISOString() },
            { attendance_id: 6, student_id: 2, qr_id: null, class_id: 2, subject_id: 2, date: getPastDate(1), time_in: '11:04:00', status: 'PRESENT', latitude: 12.9716, longitude: 77.5946, ip_address: '192.168.1.102', device_info: 'Mobile Safari / iOS', created_at: new Date().toISOString() },
            { attendance_id: 7, student_id: 4, qr_id: null, class_id: 2, subject_id: 2, date: getPastDate(1), time_in: '11:25:00', status: 'LATE', latitude: 12.9716, longitude: 77.5946, ip_address: '192.168.1.104', device_info: 'Mobile Chrome / Android', created_at: new Date().toISOString() }
        ],
        system_settings: {
            'qr_expiry_seconds': '30',
            'geofence_enabled': 'false',
            'campus_latitude': '12.9716',
            'campus_longitude': '77.5946',
            'geofence_radius_meters': '500',
            'prevent_duplicate_device': 'true'
        },
        system_logs: [
            { log_id: 1, event_type: 'SYSTEM_INIT', user_email: 'admin@attendance.edu', details: 'Vercel Serverless database initialized with seed records', ip_address: '127.0.0.1', timestamp: new Date().toISOString() }
        ]
    };
}

let memoryDb = null;

function loadDb() {
    if (memoryDb) return memoryDb;
    try {
        if (fs.existsSync(TMP_FILE)) {
            const content = fs.readFileSync(TMP_FILE, 'utf8');
            memoryDb = JSON.parse(content);
            return memoryDb;
        }
    } catch (e) {
        console.warn('Could not read from /tmp cache, loading default seed data:', e.message);
    }
    memoryDb = getInitialSeedData();
    saveDb();
    return memoryDb;
}

function saveDb() {
    try {
        if (memoryDb) {
            fs.writeFileSync(TMP_FILE, JSON.stringify(memoryDb, null, 2), 'utf8');
        }
    } catch (e) {
        console.warn('Could not write to /tmp cache:', e.message);
    }
}

function nextId(collection, idField) {
    if (!collection || collection.length === 0) return 1;
    return Math.max(...collection.map(item => item[idField] || 0)) + 1;
}

module.exports = {
    loadDb,
    saveDb,
    nextId
};
