@echo off
where gradle >NUL 2>&1
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /B %ERRORLEVEL%
)

echo Gradle is not installed on this system.
echo Please install Gradle 8.5 or newer and re-run this command.
exit /B 1
