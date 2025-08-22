@echo off
REM Proxy gradle wrapper to the android_frontend container's wrapper to satisfy CI environments
SETLOCAL ENABLEDELAYEDEXPANSION

SET SCRIPT_DIR=%~dp0
SET FRONTEND_DIR=%SCRIPT_DIR%android_frontend

IF EXIST "%FRONTEND_DIR%\gradlew.bat" (
  CALL "%FRONTEND_DIR%\gradlew.bat" %*
) ELSE (
  ECHO Error: gradle wrapper not found at %FRONTEND_DIR%\gradlew.bat 1>&2
  EXIT /B 127
)
ENDLOCAL
