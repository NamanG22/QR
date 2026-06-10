@echo off
setlocal
cd /d "%~dp0"

echo Building QFRDS standalone Windows app (requires JDK 17+ on Windows 10)...
call mvn clean verify -Pwindows-jpackage -DskipTests
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

echo.
echo Done. Standalone app image:
echo   target\dist\QFRDS\QFRDS.exe
echo.
echo If QFRDS.exe exits immediately, check:
echo   %%LOCALAPPDATA%%\QFRDS\startup.log
echo.
echo Debug launch with console output:
echo   mvn clean verify -Pwindows-jpackage -DskipTests -Djpackage.win.console=true
echo.
echo Hidden operator exit: Ctrl+Shift+Q
endlocal
