# Testing

Use JDK 25 from the repository root. Runtime directories and reports are written beneath ignored build or run directories.

## Data generation

Run data generation twice. A clean checkout should remain unchanged after each pass.

```powershell
.\gradlew.bat runDatagen --rerun-tasks --no-daemon
git status --short -- src/main/generated
.\gradlew.bat runDatagen --rerun-tasks --no-daemon
git status --short -- src/main/generated
```

## GameTests and build

```powershell
.\gradlew.bat runGameTest --no-daemon
.\gradlew.bat clean build --no-daemon
```

## Static and package checks

With Python 3 available:

```powershell
python -m unittest discover -s tools/tests -p "test_*.py" -v
python tools/tests/audit_production_jar.py build/libs/before-the-blight-0.1.0-alpha.1.jar
```

The exact approved CurseForge artifact hash is recorded in [RELEASE_MANIFEST.md](RELEASE_MANIFEST.md).
