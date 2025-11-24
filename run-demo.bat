@echo off
REM Story 8.5: Demo Workflow Script (Windows)
REM Runs simulation with export enabled and starts dashboard

echo =========================================
echo   Cloud Simulation Demo Workflow
echo =========================================
echo.

echo [1/3] Running simulation with export enabled...
call mvn exec:java -Pdemo

if errorlevel 1 (
    echo ERROR: Simulation failed
    exit /b 1
)

echo.
echo [2/3] Verifying JSON files generated...
if not exist "src\main\resources\dashboard\server\data\events.json" (
    echo ERROR: JSON files not found. Check simulation output.
    exit /b 1
)
echo + All JSON files generated successfully

echo.
echo [3/3] Starting dashboard...
cd src\main\resources\dashboard

REM Install dependencies if needed
if not exist "node_modules" (
    echo Installing dashboard dependencies...
    call pnpm install
)

echo.
echo =========================================
echo   Dashboard starting...
echo   Open: http://localhost:5173
echo =========================================
call pnpm dev
