# Maven Wrapper Guide

## What is Maven Wrapper?

Maven Wrapper is a script that **downloads and uses the correct Maven version automatically**. Users don't need to install Maven manually!

## Files Added

```
cloud-simulation-load-balancing/
├── mvnw           # Linux/Mac wrapper script
├── mvnw.cmd       # Windows wrapper script
└── .mvn/
    └── wrapper/
        ├── maven-wrapper.jar          # Wrapper executable
        └── maven-wrapper.properties   # Maven version config
```

## How It Works

When users run `./mvnw` (or `mvnw.cmd` on Windows):
1. ✅ Wrapper checks if Maven is installed locally in `.mvn/`
2. ✅ If not, it downloads Maven 3.8.6 automatically
3. ✅ Runs the Maven command with the correct version
4. ✅ Future runs use the cached Maven (no re-download)

**Result**: Users only need Java 17, not Maven!

---

## Usage

### Instead of `mvn`, use `./mvnw` (or `mvnw` on Windows)

**Linux/Mac:**
```bash
# Build project
./mvnw clean compile

# Run tests
./mvnw test

# Create executable JAR
./mvnw package

# Run simulation
./mvnw exec:java -Dexec.mainClass="com.cloudsimulation.Main"
```

**Windows:**
```batch
# Build project
mvnw.cmd clean compile

# Run tests
mvnw.cmd test

# Create executable JAR
mvnw.cmd package

# Run simulation
mvnw.cmd exec:java -Dexec.mainClass="com.cloudsimulation.Main"
```

---

## Why Maven Wrapper?

### ❌ Without Maven Wrapper
```
User: "I want to run your simulation"
You: "Install Java 17, then Maven 3.8.6+, add to PATH..."
User: "Which version? How do I add to PATH?"
You: "It depends on your OS..."
[30 minutes of troubleshooting]
```

### ✅ With Maven Wrapper
```
User: "I want to run your simulation"
You: "Have Java 17? Run: ./mvnw package"
User: "Done! JAR created."
[2 minutes total]
```

---

## Benefits

1. **No Maven Installation Required**
   - Users only need Java 17
   - Wrapper downloads Maven automatically
   - Correct version guaranteed

2. **Consistent Builds**
   - Everyone uses the same Maven version (3.8.6)
   - Eliminates "works on my machine" issues
   - Reproducible builds across teams

3. **CI/CD Ready**
   - GitHub Actions, Jenkins, etc. don't need Maven pre-installed
   - Faster setup in build pipelines
   - Portable across environments

4. **Version Lock**
   - Project specifies exact Maven version
   - No surprises from Maven updates
   - Stable, predictable builds

---

## First-Time Setup (Automatic)

**First run downloads Maven (one-time only):**

```bash
$ ./mvnw clean compile
Downloading Maven 3.8.6 to /home/user/.m2/wrapper/...
[============================] 100%
Maven downloaded successfully!
[INFO] Scanning for projects...
[INFO] BUILD SUCCESS
```

**Subsequent runs use cached Maven:**

```bash
$ ./mvnw package
[INFO] Scanning for projects...
[INFO] Building Federated Cloud Simulation Platform 1.0-SNAPSHOT
[INFO] BUILD SUCCESS
```

---

## For Users Without Maven

Your updated installation instructions should now say:

### Quick Start (No Maven Needed!)

```bash
# 1. Install Java 17 only
# Download from: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html

# 2. Clone repository
git clone <repo-url>
cd cloud-simulation-load-balancing

# 3. Build with Maven Wrapper (downloads Maven automatically)
./mvnw clean package

# 4. Run
java -jar target/*-executable.jar
```

---

## Updated README Section

Here's what to add to your README:

### Prerequisites

**Required:**
- Java 17 LTS (JDK or JRE)

**Not Required:**
- ~~Maven~~ (Maven Wrapper included - auto-downloads Maven 3.8.6)

### Installation

```bash
# 1. Clone repository
git clone <repo-url>
cd cloud-simulation-load-balancing

# 2. Build (Maven downloads automatically)
./mvnw clean compile          # Linux/Mac
mvnw.cmd clean compile         # Windows

# 3. Run
./mvnw exec:java -Dexec.mainClass="com.cloudsimulation.Main"
```

---

## Technical Details

### Maven Version

The wrapper uses **Maven 3.8.6** (specified in `.mvn/wrapper/maven-wrapper.properties`)

### Download Location

Maven is downloaded to:
- **Linux/Mac**: `~/.m2/wrapper/dists/apache-maven-3.8.6/`
- **Windows**: `%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.8.6\`

### File Permissions

**Linux/Mac**: `mvnw` must be executable
```bash
chmod +x mvnw
git add mvnw
git commit -m "Make mvnw executable"
```

### .gitignore

Maven Wrapper files **should be committed** to Git:
```gitignore
# Commit these
mvnw
mvnw.cmd
.mvn/

# Don't commit Maven local repository
.m2/
```

---

## Troubleshooting

### "Permission denied: ./mvnw"

**Linux/Mac**: Make script executable
```bash
chmod +x mvnw
./mvnw clean compile
```

### "mvnw: command not found"

**Windows**: Use `mvnw.cmd` instead
```batch
mvnw.cmd clean compile
```

### "Could not download Maven"

**Solution**: Check internet connection, or download Maven manually
```bash
# Manual download
curl -O https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.6/apache-maven-3.8.6-bin.tar.gz
# Extract to ~/.m2/wrapper/dists/
```

### "Java version mismatch"

**Solution**: Maven Wrapper uses system Java. Verify Java 17:
```bash
java -version    # Should show 17.x.x
```

---

## Migration from Regular Maven

If users already have Maven installed, both work identically:

```bash
# These are equivalent
mvn clean compile
./mvnw clean compile

# These are equivalent
mvn package
./mvnw package
```

**Recommendation**: Document both, but prefer `./mvnw` in examples for portability.

---

## Summary

✅ **What Users Need**: Java 17
❌ **What Users DON'T Need**: Maven

✅ **First Command**: `./mvnw clean compile` (downloads Maven automatically)
✅ **All Future Commands**: Use `./mvnw` instead of `mvn`

✅ **Result**: Easier onboarding, fewer dependencies, consistent builds!

---

**Generated with Maven 3.8.6**
**Compatible with Java 17+**
