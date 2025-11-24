#!/bin/bash
# Story 8.5: Demo Workflow Script
# Runs simulation with export enabled and starts dashboard

set -e

echo "========================================="
echo "  Cloud Simulation Demo Workflow"
echo "========================================="
echo ""

echo "[1/3] Running simulation with export enabled..."
mvn exec:java -Pdemo

echo ""
echo "[2/3] Verifying JSON files generated..."
if [ ! -f "src/main/resources/dashboard/server/data/events.json" ]; then
    echo "ERROR: JSON files not found. Check simulation output."
    exit 1
fi
echo "✓ All JSON files generated successfully"

echo ""
echo "[3/3] Starting dashboard..."
cd src/main/resources/dashboard

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing dashboard dependencies..."
    pnpm install
fi

echo ""
echo "========================================="
echo "  Dashboard starting..."
echo "  Open: http://localhost:5173"
echo "========================================="
pnpm dev
