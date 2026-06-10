@echo off
setlocal
cd /d "%~dp0"

echo Building QFRDS standalone Windows installer (requires JDK 17+ on Windows 10)...
call mvn clean verify -Pwindows-jpackage -DskipTests
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

echo.
echo Done. Standalone app image:
echo   target\dist\QFRDS\QFRDS.exe
echo.
echo Run QFRDS.exe directly. Hidden operator exit: Ctrl+Shift+Q
endlocal
