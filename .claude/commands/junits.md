Run all backend JUnit tests across every Gradle module and report results.

```bash
./gradlew test --no-daemon 2>&1
```

After the run, summarise:
- total tests passed / failed / skipped per module
- any failure messages with the class and method name
- whether the overall build is green or red