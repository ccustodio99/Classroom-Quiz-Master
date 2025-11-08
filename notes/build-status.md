## Build Status Notes

### Reproduction
- Command: `./gradlew test --stacktrace` (set `JAVA_HOME` via `E:/SynologyDrive/MITStudies/Projects/Classroom-Quiz-Master/jdk17/jdk-17.0.11+9`).
- Environment: Windows 10, JDK 17.0.11, Gradle 8.7.3, Kotlin 1.9.24, AGP 8.7.3.

### Failure
- `app:kaptGenerateStubsDebugKotlin` fails repeatedly with `e: Could not load module <Error module>` coming from the Kotlin compiler worker.
- `gradle-kapt.log` captured at the root shows the task crashing inside `GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction`.
- No additional Kotlin diagnostics appear in the captured log, so the precise source file is still under investigation.

### Next Steps
1. Run `./gradlew :app:kaptGenerateStubsDebugKotlin --stacktrace --debug` and inspect `gradle-kapt.log` for any `CompilerMessageSeverity` lines; they may be hidden farther in the file.
2. Try a clean build with `--rerun-tasks` to ensure no stale kapt artifacts remain.
3. If the issue persists, isolate the Kotlin source that causes the compiler to emit `<Error module>` by sequentially disabling modules or Hilt components.
