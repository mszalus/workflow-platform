Run all backend JUnit tests across every Gradle module and report results.

```bash
JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.4\jbr' \
  ./gradlew test --no-daemon 2>&1
```

After the run, summarise:
- total tests passed / failed / skipped per module
- any failure messages with the class and method name
- whether the overall build is green or red