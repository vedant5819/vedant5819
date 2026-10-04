@echo off
title QR Code Attendance System - Java Mini Project
color 0b

echo ===============================================================
echo        QR CODE ATTENDANCE SYSTEM - COMPILING & LAUNCHING
echo ===============================================================

where javac >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java Compiler (javac) not found in system PATH!
    echo Please install JDK 11+ or ensure javac is in your PATH.
    pause
    exit /b 1
)

if not exist bin mkdir bin

echo [1/2] Compiling Java backend files...
javac -encoding UTF-8 -cp "sqlite-jdbc.jar;slf4j-api.jar;slf4j-simple.jar" -d bin src/com/attendance/*.java src/com/attendance/model/*.java src/com/attendance/dao/*.java src/com/attendance/server/*.java

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed! Check error messages above.
    pause
    exit /b 1
)

echo [2/2] Compilation successful! Starting Java HTTP server...
echo.
java -cp "sqlite-jdbc.jar;slf4j-api.jar;slf4j-simple.jar;bin" com.attendance.Main

pause
