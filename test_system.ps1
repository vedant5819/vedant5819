# 1. Login Student
$studentLogin = @{ email = "alex@attendance.edu"; password = "student123"; role = "STUDENT" } | ConvertTo-Json
$res1 = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $studentLogin -ContentType "application/json"
Write-Host "1. Student Login:" $res1.success "- Name:" $res1.user.name

# 2. Login Teacher
$teacherLogin = @{ email = "turing@attendance.edu"; password = "teacher123"; role = "TEACHER" } | ConvertTo-Json
$res2 = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $teacherLogin -ContentType "application/json"
Write-Host "2. Teacher Login:" $res2.success "- Name:" $res2.user.name

# 3. Teacher Starts Session & Generates Dynamic QR Code
$sessionBody = @{ classId = 1; subjectId = 1; teacherId = 1 } | ConvertTo-Json
$res3 = Invoke-RestMethod -Uri "http://localhost:8080/api/teacher/start-session" -Method Post -Body $sessionBody -ContentType "application/json"
$qrToken = $res3.qr.qrData
Write-Host "3. Generated Dynamic QR Token:" $qrToken

# 4. Student Scans Dynamic QR Code
$scanBody = @{
    studentId = 1;
    qrPayload = $qrToken;
    latitude = 12.9716;
    longitude = 77.5946;
    deviceInfo = "Test Mobile Browser"
} | ConvertTo-Json
$res4 = Invoke-RestMethod -Uri "http://localhost:8080/api/student/scan-qr" -Method Post -Body $scanBody -ContentType "application/json"
Write-Host "4. Student Scan Result:" $res4.success "- Msg:" $res4.message

# 5. Duplicate Scan Prevention Check
$res5 = Invoke-RestMethod -Uri "http://localhost:8080/api/student/scan-qr" -Method Post -Body $scanBody -ContentType "application/json"
Write-Host "5. Anti-Proxy Duplicate Scan Check:" (-not $res5.success) "- Expected Failure Msg:" $res5.message

# 6. Student Attendance Percentage & Stats Check
$res6 = Invoke-RestMethod -Uri "http://localhost:8080/api/student/stats?studentId=1" -Method Get
Write-Host "6. Student Attendance Percentage:" $res6.stats.overallPercentage "% - Exam Eligible:" $res6.stats.isEligible

# 7. Teacher Live Attendance Stream Check
$res7 = Invoke-RestMethod -Uri "http://localhost:8080/api/teacher/live-attendance?classId=1" -Method Get
Write-Host "7. Teacher Live Attendance Count:" $res7.records.Count
