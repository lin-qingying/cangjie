@echo off
REM ======================================================================
REM  gradlew-queue.bat —— 全局串行化 gradlew 调用的包装入口（Windows）
REM
REM  退出码：与真实 gradlew.bat 100% 对齐，可直接用于 CI。
REM
REM  关键点：
REM   1) java -jar <queue-cli.jar> --project-dir <ROOT> <USER_ARGS>
REM   2) 若 jar 不存在：自动先构建 :gradle-queue-cli:shadowJar，由全局 bootstrap.lock
REM      保证同一时刻只有一个终端做首次构建，其余终端自动等待 jar 出现。
REM   3) 为了避免 cmd.exe 在解析 %* 时把用户参数里形如 %FOO% 的字符串当作环境变量替换，
REM      我们采用"先把原始命令行写入临时 UTF-16 文件 → Java 端用 sun.misc.Unsafe/JNA？不，没有
REM      那么复杂：改为使用 PowerShell 转发原始 CLI 到 Java 进程"方案来彻底规避 cmd 的变量展开。
REM      PowerShell 解析命令行时不做 %VAR% 替换，从而保证用户参数原样传到 JVM 的 main(args)。
REM ======================================================================
setlocal EnableExtensions DisableDelayedExpansion

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

set "QUEUE_DIR=%PROJECT_ROOT%\.gradle\queue"
if not exist "%QUEUE_DIR%" mkdir "%QUEUE_DIR%" 2>nul

set "JAR=%PROJECT_ROOT%\gradle-queue-cli\build\libs\gradle-queue-cli.jar"
set "BOOTSTRAP_LOCK=%QUEUE_DIR%\bootstrap.lock"

REM ======= 1) Jar 已存在 → 走 PS 转发原始命令行 =======
if exist "%JAR%" goto :runJar

REM ======= 2) 首次构建：PowerShell 拿 bootstrap.lock 再调 gradlew 构建 jar =======
echo [GradleQueue] Jar missing. First-run: building gradle-queue-cli under bootstrap lock...

REM 生成一次性 PS 脚本：持 File.Open FileShare=None 独占锁 → 构建 jar
set "PSBLD=%TEMP%\gq-bootstrap-%RANDOM%-%RANDOM%.ps1"
>"%PSBLD%" (
echo $ErrorActionPreference = 'Continue'
echo $lockFile='%BOOTSTRAP_LOCK:'=''%'
echo $jar='%JAR:'=''%'
echo $gradlew='%PROJECT_ROOT:'=''%\gradlew.bat'
echo $timeoutMs=[int](15 * 60 * 1000)
echo $pollMs=200
echo $start=[DateTimeOffset]::Now.ToUnixTimeMilliseconds()
echo $fs = $null
echo function Release-Lock {
echo   param([System.IO.FileStream]$s)
echo   if($s){ try { $s.Close() } catch {} }
echo   Remove-Item -Path $lockFile -Force -ErrorAction SilentlyContinue
echo }
echo while($true) {
echo   try {
echo     $fs = [System.IO.File]::Open($lockFile, [System.IO.FileMode]::OpenOrCreate, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
echo     break
echo   } catch [System.IO.IOException] {
echo     $elapsed=[DateTimeOffset]::Now.ToUnixTimeMilliseconds()-$start
echo     if($elapsed -gt $timeoutMs){ Write-Host '[GradleQueue] bootstrap lock timeout after 15min' -ForegroundColor Red; exit 4 }
echo     Start-Sleep -Milliseconds $pollMs
echo   } catch {
echo     Write-Host ('[GradleQueue] bootstrap lock error: '+$_.Exception.Message) -ForegroundColor Red
echo     exit 5
echo   }
echo }
echo Write-Host '[GradleQueue] bootstrap lock acquired'
echo try {
echo   if(-not (Test-Path $jar)) {
echo     Write-Host '[GradleQueue] running: gradlew.bat :gradle-queue-cli:shadowJar --no-daemon'
echo     $psi=New-Object System.Diagnostics.ProcessStartInfo
echo     $psi.FileName=$gradlew
echo     $psi.Arguments=':gradle-queue-cli:shadowJar --no-daemon'
echo     $psi.WorkingDirectory='%PROJECT_ROOT:'=''%'
echo     $psi.UseShellExecute=$false
echo     $p=[System.Diagnostics.Process]::Start($psi)
echo     $p.WaitForExit()
echo     $rc=$p.ExitCode
echo     if($rc -ne 0){
echo       Write-Host "[GradleQueue] shadowJar failed exitCode=$rc" -ForegroundColor Red
echo       Release-Lock $fs
echo       exit $rc
echo     }
echo     if(-not (Test-Path $jar)){
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

REM ======= 3) 调用 Java：用 PowerShell 转发，避免 cmd %VAR% 替换 =======
:runJar
REM 构造用户原始参数：%* 里可能含 ^ 百分号 毒字符，但在 DisableDelayedExpansion 下
REM 用 %* 传参给 PS 时，PS 不做 %% 展开，因此可以把参数安全送达 JVM。
powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command ^
  "$jar='%JAR%';" ^
  "$prj='%PROJECT_ROOT%';" ^
  "$userArgs=@($args);" ^
  "$cmdArgs=@('-jar',$jar,'--project-dir',$prj) + $userArgs;" ^
  "& java @cmdArgs; exit $LASTEXITCODE" -- %*

endlocal & exit /b %ERRORLEVEL%
