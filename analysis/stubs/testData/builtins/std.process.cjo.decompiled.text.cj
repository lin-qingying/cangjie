// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.process

import std.collection.*
import std.fs.*
import std.io.*
import std.sync.*
import std.core.*
import std.time.*

@Deprecated[message: "Use related global functions in the std.env instead."]
public class CurrentProcess <: std.process.Process {
    internal let _stdIn: std.io.InputStream

    internal let _stdOut: std.io.OutputStream

    internal let _stdErr: std.io.OutputStream

    ~init() { /* compiled code */ }

    public prop stdErr: std.io.OutputStream {
        get() { /* compiled code */ }
    }

    public prop stdOut: std.io.OutputStream {
        get() { /* compiled code */ }
    }

    public prop stdIn: std.io.InputStream {
        get() { /* compiled code */ }
    }

    public prop arguments: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    public prop homeDirectory: std.fs.Path {
        get() { /* compiled code */ }
    }

    public prop tempDirectory: std.fs.Path {
        get() { /* compiled code */ }
    }

    public func getEnv(key: std.core.String): std.core.Option<std.core.String> { /* compiled code */ }

    public func setEnv(k: std.core.String, v: std.core.String): Unit { /* compiled code */ }

    public func removeEnv(k: std.core.String): Unit { /* compiled code */ }

    public func atExit(callback: () -> Unit): Unit { /* compiled code */ }

    public func exit(code: Int64): Nothing { /* compiled code */ }

    protected func terminateAliveProcess(pid: Int32, force: Bool): Unit { /* compiled code */ }
}

public open class Process {
    internal var _pid: Int64

    internal var _handle: IntNative

    internal var _name: std.core.Option<std.core.String>

    internal var _command: std.core.Option<std.core.String>

    internal var _arguments: std.core.Option<std.core.Array<std.core.String>>

    internal var _commandLine: std.core.Option<std.core.Array<std.core.String>>

    internal var _workingDirectory: std.core.Option<std.fs.Path>

    internal var _environment: std.core.Option<std.collection.Map<std.core.String, std.core.String>>

    internal var _startTimeSinceUnixEpoch: Int64

    internal var _startTime: Int64

    @Deprecated[message: "CurrentProcess has been marked as deprecated, use related global functions in the std.env instead."]
    public static prop current: std.process.CurrentProcess {
        get() { /* compiled code */ }
    }

    public prop pid: Int64 {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop command: std.core.String {
        get() { /* compiled code */ }
    }

    public prop startTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    public prop systemTime: std.core.Duration {
        get() { /* compiled code */ }
    }

    public prop userTime: std.core.Duration {
        get() { /* compiled code */ }
    }

    @Deprecated
    public open prop arguments: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use global function `public func getCommandLine(): Array<String>` in std.env instead."]
    public prop commandLine: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use global function `public func getHomeDirectory(): Path` in std.env instead."]
    public prop workingDirectory: std.fs.Path {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use global function `public func getVariables(): Array<(String, String)>` in std.env instead."]
    public prop environment: std.collection.Map<std.core.String, std.core.String> {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use global function `public func findProcess(pid: Int64): Process` instead."]
    public static func of(pid: Int64): std.process.Process { /* compiled code */ }

    @Deprecated[message: "Use global function `public func launch(command: String, arguments: Array<String>, workingDirectory!: ?Path = None, environment!: ?Map<String, String> = None, stdIn!: ProcessRedirect = Inherit, stdOut!: ProcessRedirect = Inherit, stdErr!: ProcessRedirect = Inherit): SubProcess` instead."]
    public static func start(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory!: std.core.Option<std.fs.Path> = COMPILED_CODE /* compiled code */, environment!: std.core.Option<std.collection.Map<std.core.String, std.core.String>> = COMPILED_CODE /* compiled code */, stdIn!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdOut!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdErr!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */): std.process.SubProcess { /* compiled code */ }

    @Deprecated[message: "Use global function `public func execute(command: String, arguments: Array<String>, workingDirectory!: ?Path = None, environment!: ?Map<String, String> = None, stdIn!: ProcessRedirect = Inherit, stdOut!: ProcessRedirect = Inherit, stdErr!: ProcessRedirect = Inherit, timeout!: ?Duration = None): Int64` instead."]
    public static func run(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory!: std.core.Option<std.fs.Path> = COMPILED_CODE /* compiled code */, environment!: std.core.Option<std.collection.Map<std.core.String, std.core.String>> = COMPILED_CODE /* compiled code */, stdIn!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdOut!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdErr!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, timeout!: std.core.Option<std.core.Duration> = COMPILED_CODE /* compiled code */): Int64 { /* compiled code */ }

    @Deprecated[message: "Use global function `public func executeWithOutput(command: String, arguments: Array<String>, workingDirectory!: ?Path = None, environment!: ?Map<String, String> = None, stdIn!: ProcessRedirect = Inherit, stdOut!: ProcessRedirect = Pipe, stdErr!: ProcessRedirect = Pipe): (Int64, Array<Byte>, Array<Byte>)` instead."]
    public static func runOutput(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory!: std.core.Option<std.fs.Path> = COMPILED_CODE /* compiled code */, environment!: std.core.Option<std.collection.Map<std.core.String, std.core.String>> = COMPILED_CODE /* compiled code */, stdIn!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdOut!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdErr!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */): (Int64, std.core.Array<UInt8>, std.core.Array<UInt8>) { /* compiled code */ }

    internal static func buildCurrentProcess(): std.process.CurrentProcess { /* compiled code */ }

    internal static func buildProcess(pid: Int32): std.process.Process { /* compiled code */ }

    public func isAlive(): Bool { /* compiled code */ }

    public func terminate(force!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    protected open func terminateAliveProcess(pid: Int32, force: Bool): Unit { /* compiled code */ }

    internal static func hasDiscardMode(std_redirects: std.core.Array<std.process.ProcessRedirect>): Bool { /* compiled code */ }
}

internal struct ProcessInfo {
    internal let command: CString

    internal let commandLine: std.core.CPointer<CString>

    internal let arguments: std.core.CPointer<CString>

    internal let workingDirectory: CString

    internal let environment: std.core.CPointer<CString>

    init() { /* compiled code */ }
}

internal struct ProcessStartInfo {
    internal let _programName: CString

    internal let _commandLine: CString

    internal let _workingDirectory: CString

    internal let _environment: CString

    internal let _stdIn: IntNative

    internal let _stdOut: IntNative

    internal let _stdErr: IntNative
}

internal struct ProcessRtnData {
    internal let pid: Int32

    internal let handle: IntNative

    internal let stdInHandle: IntNative

    internal let stdOutHandle: IntNative

    internal let stdErrHandle: IntNative

    internal let errCode: Int32

    internal let errMessage: CString

    init() { /* compiled code */ }
}

internal struct ExitInfo {
    internal let exitCode: Int64

    internal let error: Bool

    init() { /* compiled code */ }
}

public enum ProcessRedirect {
    | Inherit

    | Pipe

    | FromFile(std.fs.File)

    | Discard
}

public class SubProcess <: std.process.Process {
    internal var _stdIn: std.io.OutputStream

    internal var _stdOut: std.io.InputStream

    internal var _stdErr: std.io.InputStream

    ~init() { /* compiled code */ }

    public prop stdInPipe: std.io.OutputStream {
        get() { /* compiled code */ }
    }

    public prop stdOutPipe: std.io.InputStream {
        get() { /* compiled code */ }
    }

    public prop stdErrPipe: std.io.InputStream {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use `public prop stdErrPipe: InputStream` instead."]
    public prop stdErr: std.io.InputStream {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use `public prop stdOutPipe: InputStream` instead."]
    public prop stdOut: std.io.InputStream {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use `public prop stdInPipe: OutputStream` instead."]
    public prop stdIn: std.io.OutputStream {
        get() { /* compiled code */ }
    }

    public func wait(timeout!: std.core.Option<std.core.Duration> = COMPILED_CODE /* compiled code */): Int64 { /* compiled code */ }

    public func waitOutput(): (Int64, std.core.Array<UInt8>, std.core.Array<UInt8>) { /* compiled code */ }
}

public func findProcess(pid: Int64): std.process.Process { /* compiled code */ }

public func launch(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory!: std.core.Option<std.fs.Path> = COMPILED_CODE /* compiled code */, environment!: std.core.Option<std.collection.Map<std.core.String, std.core.String>> = COMPILED_CODE /* compiled code */, stdIn!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdOut!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdErr!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */): std.process.SubProcess { /* compiled code */ }

public func execute(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory!: std.core.Option<std.fs.Path> = COMPILED_CODE /* compiled code */, environment!: std.core.Option<std.collection.Map<std.core.String, std.core.String>> = COMPILED_CODE /* compiled code */, stdIn!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdOut!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdErr!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, timeout!: std.core.Option<std.core.Duration> = COMPILED_CODE /* compiled code */): Int64 { /* compiled code */ }

public func executeWithOutput(command: std.core.String, arguments: std.core.Array<std.core.String>, workingDirectory!: std.core.Option<std.fs.Path> = COMPILED_CODE /* compiled code */, environment!: std.core.Option<std.collection.Map<std.core.String, std.core.String>> = COMPILED_CODE /* compiled code */, stdIn!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdOut!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */, stdErr!: std.process.ProcessRedirect = COMPILED_CODE /* compiled code */): (Int64, std.core.Array<UInt8>, std.core.Array<UInt8>) { /* compiled code */ }

public class ProcessException <: std.io.IOException {
    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}
