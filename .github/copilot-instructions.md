# Ballerina Language Server - Copilot Instructions

## Project Overview

This repository implements a Language Server Protocol (LSP) server for the Ballerina programming language. It provides IDE features like auto-completion, hover, diagnostics, code actions, and refactoring support for Ballerina code across various IDEs.

**Key Facts:**
- **Language:** Java (JDK 21 required for compilation, sourceCompatibility set to Java 21)
- **Build System:** Gradle 8.11.1 (uses Gradle wrapper)
- **Runtime Dependency:** Ballerina 2201.12.3 (Swan Lake) must be installed and in PATH for building
- **Ballerina Lang Dependency:** Project depends on ballerinaLangVersion=2201.13.0-m3 (defined in gradle.properties)
- **Project Type:** Multi-module Gradle project with ~20 submodules
- **Main JAR Entry Point:** `org.ballerinalang.langserver.launchers.stdio.Main`
- **Target Platforms:** Windows and Ubuntu (CI runs on both)

## Critical Build Requirements

### Prerequisites (ALWAYS verify these are set up)

1. **JDK 21.0.3** (Temurin distribution) - Required for building
2. **Ballerina 2201.12.3** - Must be installed and `bal` command available in PATH (used for CI builds)
   - Note: The project dependencies use ballerinaLangVersion=2201.13.0-m3 (from gradle.properties), but Ballerina 2201.12.3 is sufficient for running the build
3. **Environment Variables** (CRITICAL - build will fail without these):
   - `packageUser` - GitHub username or `${{ github.actor }}`
   - `packagePAT` - GitHub token with package read permissions or `${{ secrets.GITHUB_TOKEN }}`
   
   These are required to access Ballerina platform dependencies from GitHub Packages.

### Build Commands (Use these exact sequences)

**Clean build:**
```bash
export packageUser=<github-username>
export packagePAT=<github-token>
./gradlew clean build
```

**On Windows:**
```cmd
set packageUser=<github-username>
set packagePAT=<github-token>
gradlew.bat clean build
```

**Build timeout:** Allow 10+ minutes for a full build with tests. The build task has a 60-minute timeout in CI.

**Common build issues:**
- **"Could not resolve org.ballerinalang:*" errors**: 
  - First, verify `packageUser` and `packagePAT` environment variables are set correctly
  - Ensure you have network access to maven.wso2.org and maven.pkg.github.com
  - The build requires access to: WSO2 Maven repositories (maven.wso2.org/nexus), GitHub Packages, and Maven Central
  - If packagePAT authentication fails, you need a valid GitHub token with `read:packages` permission
- **Network connectivity**: Build requires internet access to download dependencies from multiple Maven repositories
- **On Windows**: Enable long paths before checkout: `git config --system core.longpaths true`
- **Gradle daemon issues**: If build behaves unexpectedly, try `./gradlew --stop` then rebuild

## Project Structure

### Root Directory Files
```
.
├── build.gradle              # Root build configuration, defines 'pack' task
├── settings.gradle           # Multi-module project structure
├── gradle.properties         # Version definitions for all dependencies
├── gradlew / gradlew.bat    # Gradle wrapper scripts
├── spotbugs-exclude.xml     # SpotBugs exclusions (root level)
└── .gitignore               # Ignores build/, .gradle/, .idea/, .vscode/
```

### Core Modules (Most Important)

1. **langserver-core** - Main language server implementation
   - Location: `/langserver-core/src/main/java/`
   - Contains LSP feature implementations (completion, hover, diagnostics, etc.)
   
2. **langserver-commons** - Shared utilities and interfaces
   - Location: `/langserver-commons/src/main/java/`
   - Common data structures and helper classes

3. **langserver-stdlib** - Ballerina standard library mock for testing
   - Location: `/langserver-stdlib/src/main/ballerina/`
   - Contains `.bal` files used in tests

4. **launcher** - Entry point and launcher logic
   - Location: `/launcher/src/main/java/`

### Model Generators (Language Server Extensions)
- **architecture-model-generator** - Generates architecture diagrams
- **sequence-model-generator** - Generates sequence diagrams
- **flow-model-generator** - Generates flow diagrams
- **service-model-generator** - Service model generation
- **graphql-model-generator** - GraphQL schema handling
- **test-manager-service** - Test management features
- **openapi-service** - OpenAPI specification support

Each generator has submodules in `modules/` directory with typical structure:
- `*-core` - Core logic
- `*-ls-extension` - Language server extension integration
- `*-plugin` - Compiler plugin (if applicable)

### Additional Components
- **misc/** - Miscellaneous utilities:
  - `ls-extensions/modules/` - Additional LS extensions (bal-shell-service, json-to-record-converter, etc.)
  - `debug-adapter/` - Debug adapter protocol implementation
  - `diagram-util/` - Diagram generation utilities
  - `ballerinalang-data-mapper/` - Data mapping functionality

## Build Configuration

### Quality Checks (Always run before committing)

**Checkstyle:**
- Configuration: `build-config/checkstyle/build/checkstyle.xml`
- Runs automatically during `build`
- Exclude pattern: `**/module-info.java`
- Run standalone: `./gradlew checkstyleMain checkstyleTest`

**SpotBugs:**
- Configuration: Module-specific `spotbugs-exclude.xml` files exist in many modules
- Root exclusions: `/spotbugs-exclude.xml`
- Runs with effort=MAX, reportLevel=LOW
- Run standalone: `./gradlew spotbugsMain`
- Reports: `build/reports/spotbugs/`

**Tests:**
- Run: `./gradlew test`
- System properties set by build:
  - `ballerina.home` - Points to extracted jBallerina tools
  - `responseTimeThreshold` - 2000ms
- Test reports: `build/reports/tests/`
- Code coverage: `build/coverage-reports/jacoco.exec`

### Special Build Tasks

**pack** - Creates a single uber JAR with all submodules:
```bash
./gradlew pack
```
- Output: `build/ballerina-language-server-<version>.jar`
- Includes all dependencies and submodule classes
- Merges service files, excludes signature files
- Includes generated POM at `META-INF/maven/io.ballerina/ballerina-language-server/pom.xml`

**Module-specific builds:**
```bash
./gradlew buildFlow        # Flow model generator
./gradlew buildService     # Service model generator  
./gradlew buildDesign      # Architecture model generator
./gradlew buildSequence    # Sequence model generator
./gradlew buildTestService # Test manager service
./gradlew buildCommons     # Model generator commons
```

**Security and SBOM:**
```bash
./gradlew cyclonedxBom     # Generate Software Bill of Materials
# Output: build/sbom.json (CycloneDX 1.4 JSON format)
```

**Publishing (requires appropriate credentials):**
```bash
./gradlew publishToMavenLocal  # Publish to local Maven repository
./gradlew publish              # Publish to GitHub Packages (needs packageUser/packagePAT)
```

## Testing

### Test Execution Requirements

**CRITICAL:** Tests depend on unpacking Ballerina tools. The test task:
1. Depends on `unpackJballerinaTools` task
2. Depends on `buildAndCopyLangserverStdlib` task  
3. Depends on `pullBallerinaModules` task (pulls external Ballerina packages from Ballerina Central)

**External modules pulled for tests:**
- `ballerinax/ai` (version from gradle.properties)
- `ballerinax/kafka`
- `ballerinax/rabbitmq`
- `ballerinax/trigger.github`
- `ballerinax/np`

These are downloaded on first test run and cached in `~/.ballerina/repositories/`

**To run tests:**
```bash
./gradlew test  # Runs all tests across all modules
./gradlew :langserver-core:test  # Single module tests
```

**Test configuration details:**
- Tests use TestNG framework (version defined in gradle.properties)
- Logging: Shows stack traces, standard streams, and failures
- Coverage: JaCoCo enabled, reports at `build/coverage-reports/jacoco.exec`

## GitHub Workflows (CI Pipeline)

**All PRs and master builds run:**

1. **pull-request.yml** - Runs on all PRs
   - Ubuntu and Windows builds in parallel
   - Timeout: 60 minutes per job
   - Caches Ballerina dependencies at `~/.ballerina/repositories/` (Linux) or `C:\Users\runneradmin\.ballerina\repositories\` (Windows)
   - Command: `./gradlew build` (or `gradlew.bat build` on Windows)

2. **build-master.yml** - Runs on pushes to main branch
   - Same configuration as PR build

3. **daily-build.yml** - Runs every 12 hours
   - Same build steps as PR build

4. **publish-release.yml** - Manual release workflow (workflow_dispatch)
   - Generates SBOM: `./gradlew cyclonedxBom`
   - Runs Trivy vulnerability scanning on SBOM
   - Executes release: `./gradlew release -Prelease.useAutomaticVersion=true -x test`
   - Publishes artifacts: `./gradlew publish -x check -x test`
   - Creates GitHub release with built JAR
   - Output JAR: `build/ballerina-language-server-<version>.jar`

5. **trivy.yml** - Security scanning workflow
   - Generates SBOM: `./gradlew cyclonedxBom` 
   - Output: `build/sbom.json` (CycloneDX format)
   - Runs on schedule and pull requests

**Windows-specific:** Long paths must be enabled: `git config --system core.longpaths true` (done in CI, may be needed locally)

## Code Style Guidelines

**Follow existing patterns:**
- Java code follows Google Java Style (enforced by Checkstyle)
- Use existing spotbugs-exclude.xml patterns when adding new code
- All Java files include Apache 2.0 license header
- Java 21 features are allowed (project uses Java 21)

**Common conventions:**
- Package structure follows module organization
- Tests are in `src/test/java/` mirroring `src/main/java/`
- Resources in `src/test/resources/` and `src/main/resources/`

## Dependency Management

**All versions defined in:** `gradle.properties`

**Key dependency groups:**
- **Ballerina Platform:** `ballerinaLangVersion` (currently 2201.13.0-m3)
- **Standard Library:** Various `stdlib*Version` properties (constraint, io, http, graphql, sql, grpc, etc.)
- **External Libraries:** 
  - Eclipse LSP4J (eclipseLsp4jVersion)
  - Jackson (jacksonDatabindVersion, jacksonDataformatYamlVersion)
  - Guava, Commons libraries, Netty, etc.

**To update a dependency:**
1. Modify version in `gradle.properties`
2. Rebuild: `./gradlew clean build`
3. Check for compatibility issues in tests

## Common Development Tasks

**Making code changes:**
1. Identify the correct module (see project structure above)
2. Make changes in `src/main/java/` or `src/main/ballerina/`
3. Run checkstyle: `./gradlew checkstyleMain`
4. Run spotbugs: `./gradlew spotbugsMain`
5. Run tests: `./gradlew test`
6. Build: `./gradlew build`

**Adding new features:**
- Core LSP features → `langserver-core`
- Shared utilities → `langserver-commons`
- New diagram types → Create/modify model generator module
- New LS extensions → `misc/ls-extensions/modules/`

**Debugging tips:**
- Build with stacktrace: `./gradlew build --stacktrace`
- Build with debug info: `./gradlew build --debug`
- Skip tests for faster iteration: `./gradlew build -x test` (not recommended for final verification)

## Important Notes

**WORKAROUNDS/TODO items in code:**
- `build.gradle` line 107: "TODO: Remove this once the workspace manager is refactored to import modules where necessary" - The `pullBallerinaModule` function is a temporary workaround

**Repository exclusions (.gitignore):**
- Build artifacts: `build/`, `.gradle/`, `**/bin/`, `*.jar`, `*.war`, etc.
- IDE files: `.idea/`, `.vscode/`
- macOS: `.DS_Store`

**Publishing:**
- Artifacts published to GitHub Packages (requires packageUser/packagePAT)
- Maven publication configured in root `build.gradle`
- Release process uses `net.researchgate.release` plugin

## Quick Reference

**Fast feedback loop:**
```bash
# 1. Make changes
# 2. Quick compile check (no tests)
./gradlew compileJava -x test

# 3. Run checkstyle
./gradlew checkstyleMain

# 4. Run specific module tests
./gradlew :langserver-core:test

# 5. Full build
./gradlew build
```

**Trust these instructions.** Only search for additional information if something here is incomplete, incorrect, or a command fails unexpectedly. The build process is complex but well-defined - follow the sequences documented above to avoid common pitfalls.
