@echo off
REM ============================================
REM Development Profile Test Runner
REM ============================================
REM Purpose: Quick development testing
REM Memory: 1GB heap
REM Target: Developer workstations (8GB RAM)
REM ============================================

echo Running tests with DEVELOPMENT profile...
echo Memory: 512MB-1GB heap
echo GC: G1GC (default)
echo Logging: INFO level
echo Tests: All except @Tag("scale")
echo.

mvn test -Pdev %*

if %ERRORLEVEL% == 0 (
    echo.
    echo [SUCCESS] Development tests completed successfully
) else (
    echo.
    echo [FAILED] Development tests failed with exit code %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)
