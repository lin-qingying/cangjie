// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.process

import std.collection.*
import std.fs.*
import std.io.*
import std.sync.*
import std.core.*
import std.time.*

class CurrentProcess <: std.process.Process {
    let _stdIn: std.io.InputStream

    let _stdOut: std.io.OutputStream

    let _stdErr: std.io.OutputStream

    ~init() { /* compiled code */ }

    prop stdErr: std.io.OutputStream {
        get() { /* compiled code */ }
    }

    prop stdOut: std.io.OutputStream {
        get() { /* compiled code */ }
    }

    prop stdIn: std.io.InputStream {
        get() { /* compiled code */ }
    }

    prop arguments: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    prop homeDirectory: std.fs.Path {
        get() { /* compiled code */ }
    }

    prop tempDirectory: std.fs.Path {
        get() { /* compiled code */ }
    }

    func getEnv(key: std.core.String): std.core.Option<std.core.String> { /* compiled code */ }

    func setEnv(k: std.core.String, v: std.core.String): Unit { /* compiled code */ }

    func removeEnv(k: std.core.String): Unit { /* compiled code */ }

    func atExit(callback: () -> Unit): Unit { /* compiled code */ }

    func exit(code: Int64): Nothing { /* compiled code */ }

    func terminateAliveProcess(pid: Int32, force: Bool): Unit { /* compiled code */ }
}

class Process {
    var _pid: Int64

    var _handle: IntNative

    var _name: std.core.Option<std.core.String>

    var _command: std.core.Option<std.core.String>

    var _arguments: std.core.Option<std.core.Array<std.core.String>>

    var _commandLine: std.core.Option<std.core.Array<std.core.String>>

    var _workingDirectory: std.core.Option<std.fs.Path>

    var _environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>

    var _startTimeSinceUnixEpoch: Int64

    var _startTime: Int64

    static prop current: std.process.CurrentProcess {
        get() { /* compiled code */ }
    }

    prop pid: Int64 {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop command: std.core.String {
        get() { /* compiled code */ }
    }

    prop startTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    prop systemTime: std.core.Duration {
        get() { /* compiled code */ }
    }

    prop userTime: std.core.Duration {
        get() { /* compiled code */ }
    }

    prop arguments: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    prop commandLine: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    prop workingDirectory: std.fs.Path {
        get() { /* compiled code */ }
    }

    prop environment: std.collection.Map<std.core.String, std.core.String> {
        get() { /* compiled code */ }
    }

    static func of(pid: Int64): std.process.Process { /* compiled code */ }

    static func start(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect): std.process.SubProcess { /* compiled code */ }

    static func run(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect, timeout: std.core.Option<std.core.Duration>): Int64 { /* compiled code */ }

    static func runOutput(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect): (Int64, std.core.Array<UInt8>, std.core.Array<UInt8>) { /* compiled code */ }

    static func buildCurrentProcess(): std.process.CurrentProcess { /* compiled code */ }

    static func buildProcess(pid: Int32): std.process.Process { /* compiled code */ }

    func isAlive(): Bool { /* compiled code */ }

    func terminate(force: Bool): Unit { /* compiled code */ }

    func terminateAliveProcess(pid: Int32, force: Bool): Unit { /* compiled code */ }

    static func hasDiscardMode(std_redirects: std.core.Array<std.process.ProcessRedirect>): Bool { /* compiled code */ }
}

struct ProcessInfo {
    let command: CString

    let commandLine: CPointer<CString>

    let arguments: CPointer<CString>

    let workingDirectory: CString

    let environment: CPointer<CString>

    init() { /* compiled code */ }
}

struct ProcessStartInfo {
    let _programName: CString

    let _commandLine: CString

    let _workingDirectory: CString

    let _environment: CString

    let _stdIn: IntNative

    let _stdOut: IntNative

    let _stdErr: IntNative
}

struct ProcessRtnData {
    let pid: Int32

    let `handle`: IntNative

    let stdInHandle: IntNative

    let stdOutHandle: IntNative

    let stdErrHandle: IntNative

    let errCode: Int32

    let errMessage: CString

    init() { /* compiled code */ }
}

struct ExitInfo {
    let exitCode: Int64

    let error: Bool

    init() { /* compiled code */ }
}

enum ProcessRedirect {
    | Inherit

    | Pipe

    | FromFile(std.fs.File)

    | Discard
}

class SubProcess <: std.process.Process {
    var _stdIn: std.io.OutputStream

    var _stdOut: std.io.InputStream

    var _stdErr: std.io.InputStream

    ~init() { /* compiled code */ }

    prop stdInPipe: std.io.OutputStream {
        get() { /* compiled code */ }
    }

    prop stdOutPipe: std.io.InputStream {
        get() { /* compiled code */ }
    }

    prop stdErrPipe: std.io.InputStream {
        get() { /* compiled code */ }
    }

    prop stdErr: std.io.InputStream {
        get() { /* compiled code */ }
    }

    prop stdOut: std.io.InputStream {
        get() { /* compiled code */ }
    }

    prop stdIn: std.io.OutputStream {
        get() { /* compiled code */ }
    }

    func wait(timeout: std.core.Option<std.core.Duration>): Int64 { /* compiled code */ }

    func waitOutput(): (Int64, std.core.Array<UInt8>, std.core.Array<UInt8>) { /* compiled code */ }
}

func findProcess(pid: Int64): std.process.Process { /* compiled code */ }

func launch(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect): std.process.SubProcess { /* compiled code */ }

func execute(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect, timeout: std.core.Option<std.core.Duration>): Int64 { /* compiled code */ }

func executeWithOutput(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect): (Int64, std.core.Array<UInt8>, std.core.Array<UInt8>) { /* compiled code */ }

class ProcessException <: std.io.IOException {
    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}
