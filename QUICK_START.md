# Quick Start Guide

Choose your preferred deployment method:

## 🐳 Option 1: Docker (Easiest)

**Requirements**: Docker installed

```bash
# Clone repository
git clone <repo-url>
cd cloud-simulation-load-balancing

# Run simulation
docker-compose up simulation

# View results in results/ directory
```

**Done!** No other dependencies needed.

---

## ☕ Option 2: Executable JAR (Portable)

**Requirements**: Java 17

```bash
# 1. Download executable JAR
# Download cloud-simulation-load-balancing-1.0-SNAPSHOT-executable.jar

# 2. Download configs folder

# 3. Run
java -jar cloud-simulation-load-balancing-1.0-SNAPSHOT-executable.jar \
  configs/benchmarks/traffic_spike.yaml
```

**Done!** Single file, works anywhere with Java 17.

---

## 🔧 Option 3: Automated Install

**Requirements**: None (script installs everything)

### Linux/Mac
```bash
git clone <repo-url>
cd cloud-simulation-load-balancing
chmod +x install.sh
./install.sh
```

### Windows
```batch
git clone <repo-url>
cd cloud-simulation-load-balancing
install.bat
```

**Done!** Script installs Java, Maven, builds project, creates run scripts.

---

## 📋 Option 4: Manual Install

**Requirements**: Java 17 + Maven 3.8.6+

```bash
# 1. Clone
git clone <repo-url>
cd cloud-simulation-load-balancing

# 2. Build
mvn clean compile

# 3. Run
mvn exec:java -Dexec.mainClass="com.cloudsimulation.Main"
```

---

## 🎯 What to Run

### Pre-configured Scenarios

```bash
# Steady load (baseline)
<run-command> configs/benchmarks/steady_load.yaml

# Traffic spike (stress test)
<run-command> configs/benchmarks/traffic_spike.yaml

# Daily pattern (realistic)
<run-command> configs/benchmarks/diurnal_pattern.yaml

# Chaos test (worst case)
<run-command> configs/benchmarks/chaos_oscillation.yaml
```

Replace `<run-command>` with:
- Docker: `docker-compose run simulation`
- JAR: `java -jar target/*-executable.jar`
- Installed: `./run-simulation.sh` (Linux/Mac) or `run-simulation.bat` (Windows)
- Maven: `mvn exec:java -Dexec.mainClass="com.cloudsimulation.Main" -Dexec.args="`

### View Results

Results are saved to: `results/<scenario>/<timestamp>/`

Files:
- `metrics.csv` - Time-series performance metrics
- `decisions.csv` - Load balancing decisions
- `metadata.json` - Run configuration

---

## 📊 Dashboard (Optional)

```bash
# 1. Install pnpm
npm install -g pnpm

# 2. Start dashboard
cd src/main/resources/dashboard
pnpm install
pnpm dev

# 3. Run demo simulation
mvn exec:java -Pdemo

# 4. Open http://localhost:5173
```

---

## 🆘 Common Issues

### "java: command not found"
Install Java 17: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html

### "mvn: command not found"
Install Maven: https://maven.apache.org/download.cgi

### "Docker not found"
Install Docker: https://docs.docker.com/get-docker/

### OutOfMemoryError
Increase heap: `java -Xms2g -Xmx4g -jar app.jar`

---

## 📚 Full Documentation

- **README.md** - Complete project documentation
- **DEPLOYMENT.md** - Detailed deployment guide
- **docs/** - Architecture and design docs

---

## ⏱️ Expected Times

| Action | Time |
|--------|------|
| Docker first build | ~10 min |
| Maven first build | ~5 min |
| Running simulation (100s) | ~30 sec real time |
| Dashboard setup | ~5 min |

---

## 🚀 Next Steps

1. ✅ Run a benchmark scenario
2. ✅ Check results in `results/` directory
3. ✅ Try the dashboard (optional)
4. ✅ Create custom configuration (see README.md)
5. ✅ Run your own experiments

---

**Need Help?** Open an issue on GitHub or check DEPLOYMENT.md for troubleshooting.
