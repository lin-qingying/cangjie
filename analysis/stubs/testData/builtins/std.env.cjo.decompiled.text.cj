// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.env

import std.io.InputStream
import std.sync.*
import std.core.*
import std.io.OutputStream
import std.collection.*
import std.fs.*
import std.io.*

let defaultCapacity: Int64

let STDIN_FD: IntNative

let STDOUT_FD: IntNative

let STDERR_FD: IntNative

let RTN_CODE_MIN_VAL: Int64

let RTN_CODE_MAX_VAL: Int64

let NULL_BYTE: std.core.String

class ConsoleReader <: std.core.Object & std.io.InputStream {
    var buffer: std.core.Array<UInt8>

    var endIndex: Int64

    var startIndex: Int64

    init() { /* compiled code */ }

    func read(): std.core.Option<Rune> { /* compiled code */ }

    func readln(): std.core.Option<std.core.String> { /* compiled code */ }

    func readToEnd(): std.core.Option<std.core.String> { /* compiled code */ }

    func readUntil(ch: Rune): std.core.Option<std.core.String> { /* compiled code */ }

    func readUntil(predicate: (Rune) -> Bool): std.core.Option<std.core.String> { /* compiled code */ }

    func read(arr: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func append(str: std.env.StructureString): Unit { /* compiled code */ }
}

struct StructureString {
    let buffer: CPointer<UInt8>

    let length: UIntNative

    init() { /* compiled code */ }
}

foreign func CJ_CONSOLE_Flush(fd: Int32): Unit

class ConsoleWriter <: std.core.Object & std.io.OutputStream {
    let mutex: std.sync.Mutex

    let bufferSize: Int64

    let buffer: std.core.Array<UInt8>

    let fd: Int32

    init(fd: Int32) { /* compiled code */ }

    func flush(): Unit { /* compiled code */ }

    func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func writeln(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func write(v: std.core.String): Unit { /* compiled code */ }

    func write(v: T): Unit { /* compiled code */ }

    func write(v: Bool): Unit { /* compiled code */ }

    func write(v: Int8): Unit { /* compiled code */ }

    func write(v: Int16): Unit { /* compiled code */ }

    func write(v: Int32): Unit { /* compiled code */ }

    func write(v: Int64): Unit { /* compiled code */ }

    func write(v: UInt8): Unit { /* compiled code */ }

    func write(v: UInt16): Unit { /* compiled code */ }

    func write(v: UInt32): Unit { /* compiled code */ }

    func write(v: UInt64): Unit { /* compiled code */ }

    func write(v: Float16): Unit { /* compiled code */ }

    func write(v: Float32): Unit { /* compiled code */ }

    func write(v: Float64): Unit { /* compiled code */ }

    func write(v: Rune): Unit { /* compiled code */ }

    func writeln(v: std.core.String): Unit { /* compiled code */ }

    func writeln(v: T): Unit { /* compiled code */ }

    func writeln(v: Bool): Unit { /* compiled code */ }

    func writeln(v: Int8): Unit { /* compiled code */ }

    func writeln(v: Int16): Unit { /* compiled code */ }

    func writeln(v: Int32): Unit { /* compiled code */ }

    func writeln(v: Int64): Unit { /* compiled code */ }

    func writeln(v: UInt8): Unit { /* compiled code */ }

    func writeln(v: UInt16): Unit { /* compiled code */ }

    func writeln(v: UInt32): Unit { /* compiled code */ }

    func writeln(v: UInt64): Unit { /* compiled code */ }

    func writeln(v: Float16): Unit { /* compiled code */ }

    func writeln(v: Float32): Unit { /* compiled code */ }

    func writeln(v: Float64): Unit { /* compiled code */ }

    func writeln(v: Rune): Unit { /* compiled code */ }
}

let mtx: std.sync.Mutex

let outConsole: std.env.ConsoleWriter

let errConsole: std.env.ConsoleWriter

let inConsole: std.env.ConsoleReader

func getProcessId(): Int64 { /* compiled code */ }

func getCommand(): std.core.String { /* compiled code */ }

func getCommandLine(): std.core.Array<std.core.String> { /* compiled code */ }

func getWorkingDirectory(): std.fs.Path { /* compiled code */ }

func getHomeDirectory(): std.fs.Path { /* compiled code */ }

func getTempDirectory(): std.fs.Path { /* compiled code */ }

func getVariable(key: std.core.String): std.core.Option<std.core.String> { /* compiled code */ }

func getVariables(): std.core.Array<(std.core.String, std.core.String)> { /* compiled code */ }

func setVariable(key: std.core.String, value: std.core.String): Unit { /* compiled code */ }

func removeVariable(key: std.core.String): Unit { /* compiled code */ }

func getStdErr(): std.env.ConsoleWriter { /* compiled code */ }

func getStdIn(): std.env.ConsoleReader { /* compiled code */ }

func getStdOut(): std.env.ConsoleWriter { /* compiled code */ }

func atExit(callback: () -> Unit): Unit { /* compiled code */ }

func exit(code: Int64): Nothing { /* compiled code */ }

func getCurrentProcessInfo(): (std.core.Option<std.core.String>, std.core.Option<std.fs.Path>, std.core.Option<std.core.Array<std.core.String>>, std.core.Option<std.core.Array<(std.core.String, std.core.String)>>) { /* compiled code */ }

func getProcessInfo(processInfo: std.env.ProcessInfo): (std.core.Option<std.core.String>, std.core.Option<std.fs.Path>, std.core.Option<std.core.Array<std.core.String>>, std.core.Option<std.core.Array<(std.core.String, std.core.String)>>) { /* compiled code */ }

struct ProcessInfo {
    let command: CString

    let commandLine: CPointer<CString>

    let arguments: CPointer<CString>

    let workingDirectory: CString

    let environment: CPointer<CString>

    init() { /* compiled code */ }
}

func cStringToOption(cstring: CString): std.core.Option<std.core.String> { /* compiled code */ }

func cpointerToArray(cpointer: CPointer<CString>): std.core.Option<std.core.Array<std.core.String>> { /* compiled code */ }

func getEnvirments(env: CPointer<CString>): std.core.Option<std.core.Array<(std.core.String, std.core.String)>> { /* compiled code */ }

func homeDirectory(): std.fs.Path { /* compiled code */ }

func tempDirectory(): std.fs.Path { /* compiled code */ }

func nativeGetEnv(name: CString): std.core.Option<std.core.String> { /* compiled code */ }

func nativeSetEnv(name: CString, value: CString, _: Int32): Int32 { /* compiled code */ }

func getStdErrHandle(): IntNative { /* compiled code */ }

func nativeRemoveEnv(name: CString): Int32 { /* compiled code */ }

func getStdOutHandle(): IntNative { /* compiled code */ }

func getStdInHandle(): IntNative { /* compiled code */ }

func getStdHandle(fd: IntNative): IntNative { /* compiled code */ }

func processExit(exitCode: Int32): Nothing { /* compiled code */ }

class EnvException <: std.core.Exception {
    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

foreign func CJ_OS_GetCurrentPid(): Int32

foreign func CJ_OS_GetProcessInfoByPid(pid: Int32): CPointer<std.env.ProcessInfo>

foreign func CJ_OS_FreeProcessInfo(processInfo_cp: CPointer<std.env.ProcessInfo>): Unit

foreign func exit(status: Int32): Unit

foreign func CJ_BUFFER_Int64ToCPointer(num: Int64, buf: CPointer<UInt8>, destMax: Int64): Int64

foreign func CJ_BUFFER_UInt64ToCPointer(num: UInt64, buf: CPointer<UInt8>, destMax: Int64): Int64

foreign func CJ_BUFFER_Float64ToCPointer(num: Float64, buf: CPointer<UInt8>, bufferSize: Int64): Int64

foreign func CJ_CONSOLE_Readline(): CPointer<std.env.StructureString>

foreign func CJ_CONSOLE_ClearstdInerr(): Unit

foreign func memcpy_s(dest: CPointer<UInt8>, destMax: UIntNative, src: CPointer<UInt8>, count: UIntNative): Int32

foreign func memset_s(dest: CPointer<UInt8>, destMax: UIntNative, c: Int32, count: UIntNative): Int32

foreign func CJ_CONSOLE_Write(fd: Int32, c: CPointer<UInt8>, len: Int64, newLine: Bool): Unit

foreign func CJ_OS_GetEnvVal(envName: CString): CString

foreign func CJ_OS_SetEnvEntry(envEntry: CString): Int32

foreign func CJ_OS_GetStdHandle(fd: IntNative): IntNative

foreign func CJ_OS_CloseFile(fd: IntNative): Int64

foreign func CJ_OS_FileRead(fd: IntNative, buffer: CPointer<UInt8>, maxLen: UIntNative): Int64

foreign func CJ_OS_FileWrite(fd: IntNative, buffer: CPointer<UInt8>, maxLen: UIntNative): Bool

foreign func CJ_OS_HomeDir(): CString

foreign func FiniCJRuntime(): Int64
