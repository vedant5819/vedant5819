@echo off
title QR Code Attendance System - Server Runner
color 0a

echo ===============================================================
echo        QR CODE ATTENDANCE SYSTEM - LAUNCHING SERVER
echo ===============================================================

java -cp "sqlite-jdbc.jar;slf4j-api.jar;slf4j-simple.jar;bin" com.attendance.Main
pause
