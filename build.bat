@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo  NEWSROOM Desktop Reader - Phase 2 Build Script
echo ===================================================

if not exist "bin" mkdir bin

echo Compiling Java source files with SQLite JDBC classpath...
set "SOURCES="
for /R "src\main\java" %%F in (*.java) do (
    set "SOURCES=!SOURCES! "%%F""
)

javac -cp "lib/*" -d bin -encoding UTF-8 !SOURCES!

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    exit /b %ERRORLEVEL%
)

if exist "src\main\resources" (
    echo Copying web and application resources...
    xcopy /E /I /Y "src\main\resources" "bin" >nul
)

echo Packaging runnable JAR with manifest...
jar cfm newsroom-2.0.0.jar manifest.txt -C bin .
copy /Y newsroom-2.0.0.jar newsroom-1.0.0.jar >nul

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] JAR packaging failed.
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Build completed successfully!
echo Run the application with: run.bat or java -jar newsroom-2.0.0.jar
