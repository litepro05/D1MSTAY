@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0dev.ps1" doctor
exit /b %errorlevel%
