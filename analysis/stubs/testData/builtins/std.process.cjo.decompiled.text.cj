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

    ~init()

    prop stdErr: std.io.OutputStream

    prop stdOut: std.io.OutputStream

    prop stdIn: std.io.InputStream

    prop arguments: std.core.Array<std.core.String>

    prop homeDirectory: std.fs.Path

    prop tempDirectory: std.fs.Path

    func getEnv(key: std.core.String): std.core.Option<std.core.String>

    func setEnv(k: std.core.String, v: std.core.String): Unit

    func removeEnv(k: std.core.String): Unit

    func atExit(callback: () -> Unit): Unit

    func exit(code: Int64): Nothing

    func terminateAliveProcess(pid: Int32, force: Bool): Unit
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

    prop current: std.process.CurrentProcess

    prop pid: Int64

    prop name: std.core.String

    prop command: std.core.String

    prop startTime: std.time.DateTime

    prop systemTime: std.core.Duration

    prop userTime: std.core.Duration

    prop arguments: std.core.Array<std.core.String>

    prop commandLine: std.core.Array<std.core.String>

    prop workingDirectory: std.fs.Path

    prop environment: std.collection.Map<std.core.String, std.core.String>

    func of(pid: Int64): std.process.Process

    func start(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect): std.process.SubProcess

    func run(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect, timeout: std.core.Option<std.core.Duration>): Int64

    func runOutput(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect): (Int64, std.core.Array<UInt8>, std.core.Array<UInt8>)

    func buildCurrentProcess: std.process.CurrentProcess

    func buildProcess(pid: Int32): std.process.Process

    func isAlive: Bool

    func terminate(force: Bool): Unit

    func terminateAliveProcess(pid: Int32, force: Bool): Unit

    func hasDiscardMode(std_redirects: std.core.Array<std.process.ProcessRedirect>): Bool
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

    ~init()

    prop stdInPipe: std.io.OutputStream

    prop stdOutPipe: std.io.InputStream

    prop stdErrPipe: std.io.InputStream

    prop stdErr: std.io.InputStream

    prop stdOut: std.io.InputStream

    prop stdIn: std.io.OutputStream

    func wait(timeout: std.core.Option<std.core.Duration>): Int64

    func waitOutput: (Int64, std.core.Array<UInt8>, std.core.Array<UInt8>)
}

func findProcess(pid: Int64): std.process.Process

func launch(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect): std.process.SubProcess

func execute(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect, timeout: std.core.Option<std.core.Duration>): Int64

func executeWithOutput(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory: std.core.Option<std.fs.Path>, environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>, stdIn: std.process.ProcessRedirect, stdOut: std.process.ProcessRedirect, stdErr: std.process.ProcessRedirect): (Int64, std.core.Array<UInt8>, std.core.Array<UInt8>)

class ProcessException <: std.io.IOException {
    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}
