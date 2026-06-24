@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo Preparing build directory...
if not exist bin mkdir bin
del /q bin\* 2>nul
for /d %%D in (bin\*) do rd /s /q "%%D" 2>nul

echo Gathering Java sources...
dir /s /B *.java > sources.txt
(for /f "usebackq delims=" %%F in ("sources.txt") do (
    set "p=%%F"
    set "p=!p:\=/!"
    echo "!p!"
)) > sources.tmp
move /y sources.tmp sources.txt >nul

echo Compiling Java sources...
javac -d bin @sources.txt
if errorlevel 1 (
    echo.
    echo Compilation failed. See errors above.
    del sources.txt 2>nul
    pause
    exit /b 1
)

echo Cleaning up temporary files...
del sources.txt

echo.
echo Starting the HTTP Server...
echo Open http://localhost:8080/app/index.html in your browser.
echo Press Ctrl+C to stop the server.
echo.
java -cp bin Main

pause
