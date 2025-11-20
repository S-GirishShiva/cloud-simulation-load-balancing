@echo off
REM ============================================
REM Scale Testing Profile Test Runner
REM ============================================
REM Purpose: Large-scale stress testing
REM Memory: 8GB heap
REM Target: Performance machines (32GB RAM)
REM ============================================

REM Check available memory
for /f "tokens=2 delims==" %%a in ('wmic OS Get FreePhysicalMemory /Value') do set /a FreeRAM=%%a/1024

if %FreeRAM% LSS 16384 (
    echo ERROR: Insufficient memory for scale testing
    echo Available: %FreeRAM% MB
    echo Required: At least 16GB free RAM
    echo.
    echo Scale tests require 8GB heap + OS overhead
    exit /b 1
)

echo Running tests with SCALE profile...
echo Memory: 4GB-8GB heap
echo GC: ZGC (low-latency)
echo Logging: OFF (disabled)
echo Tests: @Tag("scale") only
echo Timeout: 10 minutes
echo.
echo WARNING: This will take several minutes to complete
echo.

mvn test -Pscale -Dgroups=scale %*

if %ERRORLEVEL% == 0 (
    echo.
    echo [SUCCESS] Scale tests completed successfully
) else (
    echo.
    echo [FAILED] Scale tests failed with exit code %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)
