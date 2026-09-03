@echo off
REM ======================================================================
REM  gradlew-queue.bat -- global serialized wrapper for gradlew calls (Windows)
REM
REM  Exit code: 100% aligned with the real gradlew.bat, safe to use in CI.
REM
REM  Key points:
REM   1) java -jar <queue-cli.jar> --project-dir <ROOT> <USER_ARGS>
REM   2) If the jar is missing: first build :gradle-queue-cli:shadowJar automatically; the global
REM      bootstrap.lock guarantees only one terminal performs the first build at a time, and the
REM      other terminals wait for the jar to appear.
REM   3) To prevent cmd.exe from expanding strings like %FOO% inside user args while parsing %*,
REM      we forward the raw CLI to the Java process via PowerShell, which does not perform
REM      %VAR% substitution, so user args reach JVM main(args) unchanged.
REM ======================================================================
setlocal EnableExtensions DisableDelayedExpansion

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

set "QUEUE_DIR=%PROJECT_ROOT%\.gradle\queue"
if not exist "%QUEUE_DIR%" mkdir "%QUEUE_DIR%" 2>nul

set "JAR=%PROJECT_ROOT%\gradle-queue-cli\build\libs\gradle-queue-cli.jar"
set "BOOTSTRAP_LOCK=%QUEUE_DIR%\bootstrap.lock"

REM ======= 1) Jar exists -> forward raw CLI via PowerShell =======
if exist "%JAR%" goto :runJar

REM ======= 2) First build: acquire bootstrap.lock in PowerShell, then build jar via gradlew =======
echo [GradleQueue] Jar missing. First-run: building gradle-queue-cli under bootstrap lock...

REM Generate a one-shot PS script: hold File.Open FileShare=None exclusive lock -> build jar
set "PSBLD=%TEMP%\gq-bootstrap-%RANDOM%-%RANDOM%.ps1"
>"%PSBLD%" (
echo $ErrorActionPreference = 'Continue'
echo $lockFile='%BOOTSTRAP_LOCK:'=''%'
echo $jar='%JAR:'=''%'
echo $gradlew='%PROJECT_ROOT:'=''%\gradlew.bat'
echo $timeoutMs=[int]^(15 * 60 * 1000^)
echo $pollMs=200
echo $start=[DateTimeOffset]::Now.ToUnixTimeMilliseconds()
echo $fs = $null
echo function Release-Lock {
echo   param^([System.IO.FileStream]^)$s
echo   if^($s^){ try { $s.Close^() } catch {} }
echo   Remove-Item -Path $lockFile -Force -ErrorAction SilentlyContinue
echo }
echo while^($true^) {
echo   try {
echo     $fs = [System.IO.File]::Open^($lockFile, [System.IO.FileMode]::OpenOrCreate, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None^)
echo     break
echo   } catch [System.IO.IOException] {
echo     $elapsed=[DateTimeOffset]::Now.ToUnixTimeMilliseconds^()-$start
echo     if^($elapsed -gt $timeoutMs^){ Write-Host '[GradleQueue] bootstrap lock timeout after 15min' -ForegroundColor Red; exit 4 }
echo     Start-Sleep -Milliseconds $pollMs
echo   } catch {
echo     Write-Host ^('[GradleQueue] bootstrap lock error: '+$_.Exception.Message^) -ForegroundColor Red
echo     exit 5
echo   }
echo }
echo Write-Host '[GradleQueue] bootstrap lock acquired'
echo try {
echo   if^(-not ^(Test-Path $jar^)^) {
echo     Write-Host '[GradleQueue] running: gradlew.bat :gradle-queue-cli:shadowJar --no-daemon'
echo     $psi=New-Object System.Diagnostics.ProcessStartInfo
echo     $psi.FileName=$gradlew
echo     $psi.Arguments=':gradle-queue-cli:shadowJar --no-daemon'
echo     $psi.WorkingDirectory='%PROJECT_ROOT:'=''%'
echo     $psi.UseShellExecute=$false
echo     $p=[System.Diagnostics.Process]::Start^($psi^)
echo     $p.WaitForExit^()
echo     $rc=$p.ExitCode
echo     if^($rc -ne 0^){
echo       Write-Host "[GradleQueue] shadowJar failed exitCode=$rc" -ForegroundColor Red
echo       Release-Lock $fs
echo       exit $rc
echo     }
echo     if^(-not ^(Test-Path $jar^)^){
echo       Write-Host "[GradleQueue] shadowJar succeeded but jar missing: $jar" -ForegroundColor Red
echo       Release-Lock $fs
echo       exit 6
echo     }
echo   } else {
echo     Write-Host '[GradleQueue] jar already built while waiting lock; skipping build'
echo   }
echo } finally {
echo   Release-Lock $fs
echo }
echo Write-Host '[GradleQueue] bootstrap lock released'
echo exit 0
)

powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%PSBLD%"
set "BUILD_RC=%ERRORLEVEL%"
del "%PSBLD%" 2>nul

if not "%BUILD_RC%"=="0" (
    echo [GradleQueue] ERROR: bootstrap jar build failed, exitCode=%BUILD_RC% 1>&2
    exit /b %BUILD_RC%
)
if not exist "%JAR%" (
    echo [GradleQueue] ERROR: jar missing after bootstrap build: %JAR% 1>&2
    exit /b 7
)

REM ======= 3) Invoke Java: forward via PowerShell to avoid cmd %VAR% expansion =======
:runJar
REM Build raw user args: %* may contain poison characters like ^ or %, but with
REM DisableDelayedExpansion and passing %* to a -File script, PS stores them in
REM $args without further %-expansion, so the args reach the JVM safely.
set "PSRUN=%TEMP%\gq-run-%RANDOM%-%RANDOM%.ps1"
>"%PSRUN%" (
echo $ErrorActionPreference = 'Continue'
echo $jar='%JAR:'=''%'
echo $prj='%PROJECT_ROOT:'=''%'
echo $cmdArgs=@^('-jar', $jar, '--project-dir', $prj^) + $args
echo ^& java @cmdArgs
echo exit $LASTEXITCODE
)
powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%PSRUN%" %*
set "RUN_RC=%ERRORLEVEL%"
del "%PSRUN%" 2>nul
endlocal & exit /b %RUN_RC%
