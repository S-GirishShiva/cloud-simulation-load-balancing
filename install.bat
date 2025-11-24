@echo off
REM Installation script for Windows
REM Federated Cloud Simulation Platform

echo ==========================================
echo Cloud Simulation Platform - Installation
echo ==========================================
echo.

REM Check for Java
echo Step 1: Checking prerequisites...
echo.

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java not found
    echo Please install Java 17 from: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
    echo or OpenJDK from: https://jdk.java.net/17/
    pause
    exit /b 1
) else (
    echo [OK] Java found
    java -version
)

echo.

REM Check for Maven
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found
    echo Please install Maven from: https://maven.apache.org/download.cgi
    echo Add Maven bin directory to PATH environment variable
    pause
    exit /b 1
) else (
    echo [OK] Maven found
    mvn --version
)

echo.

REM Optional: Check for Node.js
set /p INSTALL_DASHBOARD="Install dashboard dependencies? (y/n): "
if /i "%INSTALL_DASHBOARD%"=="y" (
    where node >nul 2>nul
    if %errorlevel% neq 0 (
        echo [WARN] Node.js not found
        echo Install from: https://nodejs.org/ ^(version 18+^)
    ) else (
        echo [OK] Node.js found

        REM Install pnpm
        where pnpm >nul 2>nul
        if %errorlevel% neq 0 (
            echo Installing pnpm...
            npm install -g pnpm
        ) else (
            echo [OK] pnpm found
        )

        REM Install dashboard dependencies
        echo Installing dashboard dependencies...
        cd src\main\resources\dashboard
        pnpm install
        cd ..\..\..\..
    )
)

echo.
echo Step 2: Building project...
echo.

call mvn clean compile

if %errorlevel% neq 0 (
    echo [ERROR] Build failed
    pause
    exit /b 1
) else (
    echo [OK] Build successful
)

echo.
set /p RUN_TESTS="Run tests? (y/n): "
if /i "%RUN_TESTS%"=="y" (
    call mvn test
)

echo.
echo Step 3: Creating executable JAR...
echo.

call mvn package -DskipTests

if %errorlevel% neq 0 (
    echo [ERROR] Package creation failed
    pause
    exit /b 1
) else (
    echo [OK] Executable JAR created: target\cloud-simulation-load-balancing-1.0-SNAPSHOT.jar
)

echo.
echo Step 4: Creating run script...
echo.

REM Create Windows run script
(
echo @echo off
echo REM Quick run script for cloud simulation
echo.
echo set JAR_FILE=target\cloud-simulation-load-balancing-1.0-SNAPSHOT.jar
echo.
echo if not exist "%%JAR_FILE%%" ^(
echo     echo JAR file not found. Building...
echo     call mvn package -DskipTests
echo ^)
echo.
echo if "%%~1"=="" ^(
echo     REM No arguments, run with default
echo     java -Xms512m -Xmx2g -XX:+UseG1GC -jar "%%JAR_FILE%%"
echo ^) else ^(
echo     REM Run with provided config
echo     java -Xms512m -Xmx2g -XX:+UseG1GC -jar "%%JAR_FILE%%" %%*
echo ^)
) > run-simulation.bat

echo [OK] Created run-simulation.bat

echo.
echo ==========================================
echo Installation Complete!
echo ==========================================
echo.
echo Quick Start:
echo   1. Run basic simulation:    run-simulation.bat
echo   2. Run with config:         run-simulation.bat configs\benchmarks\traffic_spike.yaml
echo   3. Run with Maven:          mvn exec:java -Dexec.mainClass="com.cloudsimulation.Main"
echo.
echo Documentation: See README.md for detailed usage
echo.
pause
