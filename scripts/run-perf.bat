@echo off
REM ============================================
REM Performance Profile Test Runner
REM ============================================
REM Purpose: Performance validation
REM Memory: 4GB heap
REM Target: Development servers (16GB RAM)
REM ============================================

REM Check available memory
for /f "tokens=2 delims==" %%a in ('wmic OS Get FreePhysicalMemory /Value') do set /a FreeRAM=%%a/1024

if %FreeRAM% LSS 8192 (
    echo WARNING: Low available memory detected (%FreeRAM% MB free)
    echo Recommended: At least 8GB free RAM for performance testing
    echo.
    set /p Continue="Continue anyway? (y/n): "
    if /i not "%Continue%"=="y" exit /b 1
)

echo Running tests with PERFORMANCE profile...
echo Memory: 2GB-4GB heap
echo GC: G1GC (tuned)
echo Logging: WARN level
echo Tests: Medium scale included
echo.

mvn test -Pperf %*

if %ERRORLEVEL% == 0 (
    echo.
    echo [SUCCESS] Performance tests completed successfully
) else (
    echo.
    echo [FAILED] Performance tests failed with exit code %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)
