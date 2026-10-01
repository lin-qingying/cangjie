//! LSPServer.exe shim: forwards stdio to a Java-based language server.
//!
//! The vscode_cangjie extension spawns `<sdk>/tools/bin/LSPServer.exe` with
//! stdio pipes. This binary acts as that executable and starts the real Java
//! language server, passing stdin/stdout through untouched so LSP frames flow
//! directly between the extension and the JVM.
//!
//! Configuration (environment variables):
//! - `LSPSERVER_SHIM_JAVA`: java executable (default: `java` resolved from PATH)
//! - `LSPSERVER_SHIM_JAR`:  path to the language server jar
//!   (default: `<shim dir>/lspserver.jar`)
//! - `LSPSERVER_SHIM_MAIN`: main class (default: `org.cangnova.cangjie.lsp.Main`)
//!
//! All command-line arguments given to the shim are forwarded to the JVM
//! (after `-cp <jar> <mainclass>`), so SDK-specific args can be inspected or
//! ignored by the Java server.

use std::env;
use std::path::PathBuf;
use std::process::Command;

const DEFAULT_MAIN_CLASS: &str = "org.cangnova.cangjie.lsp.Main";

fn main() {
    let exit_code = run();
    std::process::exit(exit_code);
}

fn run() -> i32 {
    let java = env::var("LSPSERVER_SHIM_JAVA").unwrap_or_else(|_| "java".to_string());
    let main_class = env::var("LSPSERVER_SHIM_MAIN").unwrap_or_else(|_| DEFAULT_MAIN_CLASS.to_string());
    let jar = match env::var("LSPSERVER_SHIM_JAR") {
        Ok(j) => PathBuf::from(j),
        Err(_) => env::current_exe()
            .ok()
            .and_then(|p| p.parent().map(|d| d.join("lspserver.jar")))
            .unwrap_or_else(|| PathBuf::from("lspserver.jar")),
    };

    let mut cmd = Command::new(&java);
    cmd.arg("-cp")
        .arg(&jar)
        .arg(&main_class)
        .args(env::args().skip(1))
        .stdin(std::process::Stdio::inherit())
        .stdout(std::process::Stdio::inherit())
        .stderr(std::process::Stdio::inherit());

    // Do not show a console window if the parent ever spawns us without one.
    #[cfg(windows)]
    {
        use std::os::windows::process::CommandExt;
        cmd.creation_flags(0x0800_0000); // CREATE_NO_WINDOW
    }

    match cmd.status() {
        Ok(status) => status.code().unwrap_or(1),
        Err(e) => {
            eprintln!("lspserver-shim: failed to launch java at {:?}: {}", java, e);
            eprintln!("lspserver-shim: jar={:?} main={}", jar, main_class);
            1
        }
    }
}
