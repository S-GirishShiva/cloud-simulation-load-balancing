#!/bin/bash
# Installation script for Linux/Mac
# Federated Cloud Simulation Platform

set -e

echo "=========================================="
echo "Cloud Simulation Platform - Installation"
echo "=========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo -e "${YELLOW}Note: Not running as root. Some installations may require sudo.${NC}"
fi

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check Java version
check_java() {
    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            echo -e "${GREEN}✓ Java 17+ found: $(java -version 2>&1 | head -n 1)${NC}"
            return 0
        else
            echo -e "${RED}✗ Java version is less than 17${NC}"
            return 1
        fi
    else
        echo -e "${RED}✗ Java not found${NC}"
        return 1
    fi
}

# Function to check Maven
check_maven() {
    if command_exists mvn; then
        echo -e "${GREEN}✓ Maven found: $(mvn --version | head -n 1)${NC}"
        return 0
    else
        echo -e "${RED}✗ Maven not found${NC}"
        return 1
    fi
}

# Function to install Java on Linux
install_java_linux() {
    echo "Installing Java 17..."
    if command_exists apt-get; then
        # Debian/Ubuntu
        sudo apt-get update
        sudo apt-get install -y openjdk-17-jdk
    elif command_exists yum; then
        # RHEL/CentOS
        sudo yum install -y java-17-openjdk-devel
    elif command_exists dnf; then
        # Fedora
        sudo dnf install -y java-17-openjdk-devel
    else
        echo -e "${RED}Cannot automatically install Java. Please install manually.${NC}"
        exit 1
    fi
}

# Function to install Java on Mac
install_java_mac() {
    echo "Installing Java 17..."
    if command_exists brew; then
        brew install openjdk@17
        echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
        echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.bash_profile
    else
        echo -e "${RED}Homebrew not found. Please install from https://brew.sh${NC}"
        exit 1
    fi
}

# Function to install Maven
install_maven() {
    echo "Installing Maven..."
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if command_exists apt-get; then
            sudo apt-get install -y maven
        elif command_exists yum; then
            sudo yum install -y maven
        elif command_exists dnf; then
            sudo dnf install -y maven
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        if command_exists brew; then
            brew install maven
        fi
    fi
}

# Function to install Node.js and pnpm (optional)
install_nodejs() {
    echo ""
    read -p "Install Node.js and pnpm for dashboard? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if [[ "$OSTYPE" == "linux-gnu"* ]]; then
            # Install Node.js 18 via NodeSource
            curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
            sudo apt-get install -y nodejs
        elif [[ "$OSTYPE" == "darwin"* ]]; then
            if command_exists brew; then
                brew install node@18
            fi
        fi

        # Install pnpm
        if command_exists npm; then
            sudo npm install -g pnpm
            echo -e "${GREEN}✓ Node.js and pnpm installed${NC}"
        fi
    fi
}

# Main installation flow
echo "Step 1: Checking prerequisites..."
echo ""

NEED_JAVA=false
NEED_MAVEN=false

if ! check_java; then
    NEED_JAVA=true
fi

if ! check_maven; then
    NEED_MAVEN=true
fi

# Install missing prerequisites
if [ "$NEED_JAVA" = true ] || [ "$NEED_MAVEN" = true ]; then
    echo ""
    echo "Missing prerequisites detected."
    read -p "Install missing prerequisites? (y/n): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if [ "$NEED_JAVA" = true ]; then
            if [[ "$OSTYPE" == "linux-gnu"* ]]; then
                install_java_linux
            elif [[ "$OSTYPE" == "darwin"* ]]; then
                install_java_mac
            fi
        fi

        if [ "$NEED_MAVEN" = true ]; then
            install_maven
        fi
    else
        echo -e "${RED}Cannot proceed without prerequisites.${NC}"
        exit 1
    fi
fi

# Optional: Install Node.js/pnpm
install_nodejs

# Build project
echo ""
echo "Step 2: Building project..."
echo ""

mvn clean compile

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful!${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

# Run tests
echo ""
read -p "Run tests? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    mvn test
fi

# Create executable JAR
echo ""
echo "Step 3: Creating executable JAR..."
mvn package -DskipTests

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Executable JAR created: target/cloud-simulation-load-balancing-1.0-SNAPSHOT.jar${NC}"
fi

# Create run script
echo ""
echo "Step 4: Creating run script..."

cat > run-simulation.sh << 'EOF'
#!/bin/bash
# Quick run script for cloud simulation

JAR_FILE="target/cloud-simulation-load-balancing-1.0-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found. Building..."
    mvn package -DskipTests
fi

if [ -z "$1" ]; then
    # No arguments, run with default
    java -Xms512m -Xmx2g -XX:+UseG1GC -jar "$JAR_FILE"
else
    # Run with provided config
    java -Xms512m -Xmx2g -XX:+UseG1GC -jar "$JAR_FILE" "$@"
fi
EOF

chmod +x run-simulation.sh

echo -e "${GREEN}✓ Created run-simulation.sh${NC}"

# Summary
echo ""
echo "=========================================="
echo "Installation Complete!"
echo "=========================================="
echo ""
echo "Quick Start:"
echo "  1. Run basic simulation:    ./run-simulation.sh"
echo "  2. Run with config:         ./run-simulation.sh configs/benchmarks/traffic_spike.yaml"
echo "  3. Run with Maven:          mvn exec:java -Dexec.mainClass=\"com.cloudsimulation.Main\""
echo ""
echo "Documentation: See README.md for detailed usage"
echo ""
