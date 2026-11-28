//! LSPServer shim: forwards stdio to a Java-based language server.
//!
//! The vscode_cangjie extension spawns `<sdk>/tools/bin/LSPServer.exe` with
//! stdio pipes. This binary acts as that executable and starts the real Java
//! language server, passing stdin/stdout through untouched so LSP frames flow
//! directly between the extension and the JVM.
//!
//! Termination:
//! - Windows: the JVM is placed in a job object with `KILL_ON_JOB_CLOSE`, so
//!   killing this shim — even via `TerminateProcess` — kills the JVM too.
//!   No orphaned `java.exe` is left behind.
//! - Unix: SIGHUP/SIGINT/SIGTERM received by the shim are forwarded to the
//!   JVM (SIGKILL); the shim then exits with the conventional 128+signal
//!   code. A JVM killed by any other signal also exits the shim with
//!   128+signal, never a generic 1. Additionally, the JVM is started with
//!   `PR_SET_PDEATHSIG`, so even a `SIGKILL`ed shim (where no handler can
//!   run) takes the JVM down with it.
//!
//! Configuration (environment variables):
//! - `LSPSERVER_SHIM_JAVA`: java executable (default: `java` resolved from PATH)
//! - `LSPSERVER_SHIM_JAR`:  path to the language server jar
//!   (default: `<shim dir>/lspserver.jar`, resolved through symlinks)
//! - `LSPSERVER_SHIM_MAIN`: main class (default: `org.cangnova.cangjie.lsp.Main`)
//! - `LSPSERVER_SHIM_JVM_ARGS`: extra JVM options, space separated, double
//!   quotes honored for values containing spaces
//!   (e.g. `-Xmx2g -Dcangjie.home="C:\Program Files\cangjie"`); placed before
//!   the classpath
//! - `LSPSERVER_SHIM_CP_EXTRA`: additional classpath entries, joined with
//!   the platform path separator (`;` on Windows, `:` elsewhere)
//!
//! All command-line arguments given to the shim are forwarded to the JVM
//! (after `-cp <jar> <mainclass>`), so SDK-specific args can be inspected or
//! ignored by the Java server.

use std::env;
use std::io::Write as _;
use std::path::{Path, PathBuf};
use std::process::{Child, Command, ExitStatus, Stdio};
use std::time::Duration;

#[cfg(windows)]
use std::os::windows::io::AsRawHandle;
#[cfg(windows)]
use std::os::windows::process::CommandExt;

const DEFAULT_MAIN_CLASS: &str = "org.cangnova.cangjie.lsp.Main";
const JAR_FILE_NAME: &str = "lspserver.jar";

/// Poll interval while waiting for the JVM to exit. The signal handler kills
/// the child immediately; this loop only observes the exit and covers the
/// race where a signal arrives before the child pid is registered.
const WAIT_POLL_INTERVAL: Duration = Duration::from_millis(50);

fn main() {
    let exit_code = run();
    std::process::exit(exit_code);
}

fn run() -> i32 {
    let java = env_var("LSPSERVER_SHIM_JAVA").unwrap_or_else(|| "java".to_string());
    let main_class =
        env_var("LSPSERVER_SHIM_MAIN").unwrap_or_else(|| DEFAULT_MAIN_CLASS.to_string());
    let jar = resolve_jar();
    if !jar.is_file() {
        fail(&format!(
            "language server jar not found at {:?}; set LSPSERVER_SHIM_JAR to override",
            jar
        ));
    }
    let classpath = build_classpath(&jar);
    let jvm_args = env_var("LSPSERVER_SHIM_JVM_ARGS")
        .map(|v| split_jvm_args(&v))
        .unwrap_or_default();

    let mut cmd = Command::new(&java);
    cmd.args(&jvm_args)
        .arg("-cp")
        .arg(&classpath)
        .arg(&main_class)
        .args(env::args().skip(1))
        .stdin(Stdio::inherit())
        .stdout(Stdio::inherit())
        .stderr(Stdio::inherit());

    // Keep a console window from popping up when the parent has none.
    // This is the base of the creation-flag set: any future flag must be OR'd
    // in, never replace it.
    #[cfg(windows)]
    cmd.creation_flags(job::CREATE_NO_WINDOW);

    // Install signal handling before the child exists so a signal arriving in
    // between is not lost.
    #[cfg(unix)]
    signal_guard::install();

    // Kernel-side orphan protection (Linux only: prctl is not available on
    // macOS/BSD): if this shim is SIGKILLed (no signal handler can run), the
    // kernel still delivers SIGKILL to the JVM. The getppid check closes the
    // race where the parent dies between fork and prctl. Complements (not
    // replaces) the signal forwarding above.
    #[cfg(target_os = "linux")]
    {
        use std::os::unix::process::CommandExt;
        const PR_SET_PDEATHSIG: i32 = 1;
        let ppid = std::process::id() as i32;
        unsafe {
            cmd.pre_exec(move || {
                if signal_guard::prctl(PR_SET_PDEATHSIG, signal_guard::SIGKILL) != 0 {
                    return Err(std::io::Error::last_os_error());
                }
                if signal_guard::getppid() != ppid {
                    // Parent already died before prctl took effect.
                    std::process::abort();
                }
                Ok(())
            });
        }
    }

    let mut child = match cmd.spawn() {
        Ok(child) => child,
        Err(e) => fail(&format!(
            "failed to launch java at {:?}: {} (jar={:?} main={})",
            java, e, jar, main_class
        )),
    };

    // Tie the JVM's lifetime to this shim's.
    #[cfg(windows)]
    job::attach(child.as_raw_handle());
    #[cfg(unix)]
    signal_guard::set_child_pid(child.id());

    let status = match wait_for_exit(&mut child) {
        Ok(status) => status,
        Err(e) => {
            let _ = writeln!(std::io::stderr(), "lspserver: {e}");
            return 1;
        }
    };
    exit_code_for(&status)
}

/// Explicit `LSPSERVER_SHIM_JAR` wins; otherwise the jar next to this
/// executable, resolved through symlinks/hardlinks so the lookup still works
/// when the shim is launched through a link.
fn resolve_jar() -> PathBuf {
    if let Some(j) = env_var("LSPSERVER_SHIM_JAR") {
        // A user-provided path can be in extended-length form too, which the
        // JVM's URLClassLoader cannot reliably consume; normalize it the same
        // way as the canonicalized default location.
        return strip_windows_extended_prefix(PathBuf::from(j));
    }
    let exe_dir = env::current_exe()
        .ok()
        .and_then(|p| p.parent().map(|d| d.to_path_buf()))
        .map(|d| std::fs::canonicalize(&d).unwrap_or(d))
        .map(strip_windows_extended_prefix);
    match exe_dir {
        Some(dir) => dir.join(JAR_FILE_NAME),
        // Last resort: CWD-relative (the extension may launch from anywhere).
        None => PathBuf::from(JAR_FILE_NAME),
    }
}

/// `std::fs::canonicalize` returns extended-length paths on Windows
/// (`\\?\C:\...`), which the JVM's URLClassLoader cannot reliably consume as
/// classpath entries; convert them back to ordinary paths.
#[cfg(windows)]
fn strip_windows_extended_prefix(path: PathBuf) -> PathBuf {
    let s = path.to_string_lossy();
    if let Some(rest) = s.strip_prefix(r"\\?\UNC\") {
        return PathBuf::from(format!(r"\\{rest}"));
    }
    if let Some(rest) = s.strip_prefix(r"\\?\") {
        return PathBuf::from(rest);
    }
    path
}

#[cfg(not(windows))]
fn strip_windows_extended_prefix(path: PathBuf) -> PathBuf {
    path
}

/// `env::var` fails on non-UTF-8 values and silently discards them; read via
/// `var_os` and accept lossy conversion instead.
fn env_var(name: &str) -> Option<String> {
    env::var_os(name).map(|v| v.to_string_lossy().into_owned())
}

/// The jar plus any `LSPSERVER_SHIM_CP_EXTRA` entries, joined with the
/// platform path separator (`;` on Windows, `:` elsewhere).
fn build_classpath(jar: &Path) -> String {
    let sep = if cfg!(windows) { ";" } else { ":" };
    let extra = match env_var("LSPSERVER_SHIM_CP_EXTRA") {
        Some(v) if !v.is_empty() => v,
        _ => return jar.to_string_lossy().into_owned(),
    };
    let mut cp = jar.to_string_lossy().into_owned();
    for entry in extra.split(sep) {
        if !entry.is_empty() {
            cp.push_str(sep);
            cp.push_str(entry);
        }
    }
    cp
}

/// Split `LSPSERVER_SHIM_JVM_ARGS` on whitespace, honoring double quotes so
/// values containing spaces work (e.g. `-Dcangjie.home="C:\Program Files\cj"`).
/// Quotes themselves are stripped. A backslash escapes the next character
/// (typically `\"` for a literal quote or `\ ` for a literal space); a
/// trailing lone backslash is kept as-is.
fn split_jvm_args(value: &str) -> Vec<String> {
    let mut args = Vec::new();
    let mut current = String::new();
    let mut in_quotes = false;
    let mut escaped = false;
    let mut has_token = false;
    for c in value.chars() {
        if escaped {
            current.push(c);
            has_token = true;
            escaped = false;
            continue;
        }
        match c {
            '\\' => escaped = true,
            '"' => {
                in_quotes = !in_quotes;
                has_token = true;
            }
            c if c.is_whitespace() && !in_quotes => {
                if has_token {
                    args.push(std::mem::take(&mut current));
                    has_token = false;
                }
            }
            c => {
                current.push(c);
                has_token = true;
            }
        }
    }
    if escaped {
        // Trailing lone backslash: keep it.
        current.push('\\');
        has_token = true;
    }
    if has_token {
        args.push(current);
    }
    args
}

fn wait_for_exit(child: &mut Child) -> Result<ExitStatus, String> {
    // The signal handler already forwarded the original signal to the JVM;
    // this escalation only fires if the JVM ignored it (or the signal arrived
    // before the child pid was registered and nothing was forwarded).
    #[cfg(unix)]
    let mut received_at: Option<std::time::Instant> = None;
    #[cfg(unix)]
    let mut escalated = false;
    loop {
        match child.try_wait() {
            Ok(Some(status)) => {
                // Stop treating the reaped pid as ours: a late signal must
                // never kill an unrelated process that reused the pid.
                #[cfg(unix)]
                signal_guard::clear_child_pid();
                return Ok(status);
            }
            Ok(None) => {
                #[cfg(unix)]
                {
                    if signal_guard::received() > 0 {
                        if received_at.is_none() {
                            received_at = Some(std::time::Instant::now());
                        } else if !escalated
                            && received_at
                                .is_some_and(|t| t.elapsed() >= signal_guard::GRACE_PERIOD)
                        {
                            escalated = true;
                            let _ = child.kill();
                        }
                    }
                }
                std::thread::sleep(WAIT_POLL_INTERVAL);
            }
            Err(e) => return Err(format!("error waiting for java: {e}")),
        }
    }
}

fn exit_code_for(status: &ExitStatus) -> i32 {
    if let Some(code) = status.code() {
        return code;
    }
    // The JVM was killed by a signal (no exit code).
    #[cfg(unix)]
    {
        use std::os::unix::process::ExitStatusExt;
        // If we forwarded a signal to the child, report the one *we* received
        // (128+sig is the conventional mapping) — that is what the parent
        // sent us.
        let received = signal_guard::received();
        if received > 0 {
            return 128 + received;
        }
        return 128 + status.signal().unwrap_or(1);
    }
    #[cfg(windows)]
    {
        // Windows always reports an exit code (TerminateProcess → 1), so this
        // branch is effectively unreachable.
        1
    }
}

fn fail(msg: &str) -> ! {
    let _ = writeln!(std::io::stderr(), "lspserver: {msg}");
    std::process::exit(1);
}

/// Unix signal handling: SIGHUP/SIGINT/SIGTERM received by the shim are
/// forwarded to the JVM — SIGTERM/SIGINT as themselves so the JVM can run its
/// shutdown hooks and flush state; SIGHUP escalates straight to SIGKILL (a
/// hangup means the controlling terminal is gone, graceful shutdown is
/// pointless). If the JVM is still alive after `GRACE_PERIOD`, `wait_for_exit`
/// escalates to SIGKILL. The shim then exits with the conventional
/// 128+signal code.
#[cfg(unix)]
mod signal_guard {
    use std::io::Write as _;
    use std::sync::atomic::{AtomicI32, Ordering};
    use std::time::Duration;

    static CHILD_PID: AtomicI32 = AtomicI32::new(-1);
    static RECEIVED_SIGNAL: AtomicI32 = AtomicI32::new(0);

    /// How long to let the JVM honor a forwarded SIGTERM/SIGINT before
    /// escalating to SIGKILL.
    pub const GRACE_PERIOD: Duration = Duration::from_secs(2);

    // Signal numbers, identical on Linux, macOS and the BSDs.
    const SIGHUP: i32 = 1;
    const SIGINT: i32 = 2;
    const SIGTERM: i32 = 15;
    pub const SIGKILL: i32 = 9;

    unsafe extern "C" {
        fn kill(pid: i32, sig: i32) -> i32;
        fn signal(sig: i32, handler: usize) -> usize;
        #[cfg(target_os = "linux")]
        pub fn prctl(option: i32, arg: i32) -> i32;
        pub fn getppid() -> i32;
    }

    // Async-signal-safe body: only atomic stores and kill(2).
    extern "C" fn on_signal(sig: i32) {
        RECEIVED_SIGNAL.store(sig, Ordering::SeqCst);
        let pid = CHILD_PID.load(Ordering::SeqCst);
        if pid > 0 {
            unsafe {
                let forward = if sig == SIGHUP { SIGKILL } else { sig };
                kill(pid, forward);
            }
        }
    }

    pub fn install() {
        let mut failed = Vec::new();
        unsafe {
            for sig in [SIGHUP, SIGINT, SIGTERM] {
                if signal(sig, on_signal as usize) == usize::MAX {
                    failed.push(sig);
                }
            }
        }
        if !failed.is_empty() {
            let _ = writeln!(
                std::io::stderr(),
                "lspserver: warning: could not install signal handler(s) for {failed:?}"
            );
        }
    }

    pub fn set_child_pid(pid: u32) {
        CHILD_PID.store(pid as i32, Ordering::SeqCst);
    }

    pub fn clear_child_pid() {
        CHILD_PID.store(-1, Ordering::SeqCst);
    }

    pub fn received() -> i32 {
        RECEIVED_SIGNAL.load(Ordering::SeqCst)
    }
}

/// Windows child lifetime: the JVM is placed in a job object with
/// `KILL_ON_JOB_CLOSE` so that killing this shim — even via
/// `TerminateProcess` — kills the JVM as well.
///
/// The job handle is intentionally never closed; the OS closes it at process
/// exit, which is what triggers KILL_ON_JOB_CLOSE when the shim dies.
#[cfg(windows)]
mod job {
    use std::ffi::c_void;
    use std::io::Write as _;

    type Handle = *mut c_void;
    type DWord = u32;

    pub const CREATE_NO_WINDOW: DWord = 0x0800_0000;

    const JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE: DWord = 0x0000_2000;
    // Allow processes inside the job (e.g. a nested shim) to break away to
    // their own job.
    const JOB_OBJECT_LIMIT_SILENT_BREAKAWAY_OK: DWord = 0x0000_1000;
    // JOBOBJECTINFOCLASS::JobObjectExtendedLimitInformation (not 9 — that is
    // JobObjectCreationInformation, which rejects this struct).
    const JOB_OBJECT_EXTENDED_LIMIT_INFORMATION: DWord = 13;

    #[repr(C)]
    struct IoCounters {
        read_operations: u64,
        write_operations: u64,
        other_operations: u64,
        read_transfer: u64,
        write_transfer: u64,
        other_transfer: u64,
    }

    #[repr(C)]
    struct BasicLimitInformation {
        per_process_user_time_limit: i64,
        per_job_user_time_limit: i64,
        limit_flags: DWord,
        minimum_working_set_size: usize,
        maximum_working_set_size: usize,
        active_process_limit: DWord,
        affinity: DWord,
        priority_class: DWord,
        scheduling_class: DWord,
    }

    #[repr(C)]
    struct ExtendedLimitInformation {
        basic: BasicLimitInformation,
        io: IoCounters,
        process_memory_limit: usize,
        job_memory_limit: usize,
        peak_process_memory_used: usize,
        peak_job_memory_used: usize,
    }

    unsafe extern "system" {
        fn CreateJobObjectW(
            lp_job_object_attributes: *const c_void,
            lp_name: *const u16,
        ) -> Handle;
        fn SetInformationJobObject(
            h_job: Handle,
            job_object_information_class: DWord,
            lp_job_object_information: *const u8,
            job_object_information_length: DWord,
        ) -> i32;
        fn AssignProcessToJobObject(h_job: Handle, h_process: Handle) -> i32;
        fn GetLastError() -> DWord;
    }

    /// Attach the child JVM to a kill-on-close job object. Failure here never
    /// aborts the launch — the shim just continues without orphan protection
    /// and logs a warning.
    pub fn attach(child_handle: Handle) {
        let job = match ensure_job() {
            Ok(job) => job,
            Err(msg) => {
                warn(&format!("orphan protection unavailable: {msg}"));
                return;
            }
        };
        let ok = unsafe { AssignProcessToJobObject(job, child_handle) };
        if ok == 0 {
            warn("failed to assign java process to job object");
        }
    }

    // Windows 8+ supports nested jobs, so we never need to break away from a
    // parent job (the IDE may already have this process in one): just create
    // our own kill-on-close job and assign the JVM to it.
    fn ensure_job() -> Result<Handle, String> {
        let job = unsafe { CreateJobObjectW(std::ptr::null(), std::ptr::null()) };
        if job.is_null() {
            let err = unsafe { GetLastError() };
            return Err(format!("CreateJobObjectW failed (Win32 error {err})"));
        }

        let mut info: ExtendedLimitInformation = unsafe { std::mem::zeroed() };
        info.basic.limit_flags =
            JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE | JOB_OBJECT_LIMIT_SILENT_BREAKAWAY_OK;
        let ok = unsafe {
            SetInformationJobObject(
                job,
                JOB_OBJECT_EXTENDED_LIMIT_INFORMATION,
                &info as *const ExtendedLimitInformation as *const u8,
                std::mem::size_of::<ExtendedLimitInformation>() as DWord,
            )
        };
        if ok == 0 {
            // Failure here means the limit flags (KILL_ON_JOB_CLOSE) were not
            // applied; reporting it beats silently losing orphan protection.
            let err = unsafe { GetLastError() };
            return Err(format!("SetInformationJobObject failed (Win32 error {err})"));
        }

        // The handle is intentionally never closed: the OS closes it at
        // process exit, which is what triggers KILL_ON_JOB_CLOSE when the
        // shim dies.
        Ok(job)
    }

    fn warn(msg: &str) {
        let _ = writeln!(std::io::stderr(), "lspserver: warning: {msg}");
    }
}
