// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.posix

import std.core.*

@Deprecated
let O_RDONLY: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let O_RDWR: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let O_WRONLY: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let O_APPEND: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let O_CREAT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let O_EXCL: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let O_TRUNC: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IRUSR: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IWUSR: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IXUSR: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IRWXU: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IRGRP: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IWGRP: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IXGRP: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IRWXG: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IROTH: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IWOTH: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IXOTH: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IRWXO: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let SEEK_SET: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SEEK_CUR: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SEEK_END: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IFREG: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IFDIR: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IFCHR: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IFBLK: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IFIFO: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IFLNK: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let S_IFSOCK: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
let AT_EMPTY_PATH: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let AT_SYMLINK_FOLLOW: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let AT_FDCWD: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let AT_REMOVEDIR: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGHUP: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGINT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGQUIT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGILL: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGTRAP: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGABRT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGIOT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGBUS: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGFPE: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGKILL: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGUSR1: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGSEGV: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGUSR2: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGPIPE: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGALRM: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGTERM: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGSTKFLT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGCHLD: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGCONT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGSTOP: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGTSTP: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGTTIN: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGTTOU: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGURG: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGXCPU: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGXFSZ: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGVTALRM: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGPROF: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGWINCH: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGIO: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGPWR: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let SIGSYS: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let R_OK: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let W_OK: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let X_OK: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
let F_OK: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
public func open(path: std.core.String, oflag: Int32, flag: UInt32): Int32 { /* compiled code */ }

@Deprecated
public func open(path: std.core.String, oflag: Int32): Int32 { /* compiled code */ }

@Deprecated
public func creat(path: std.core.String, flag: UInt32): Int32 { /* compiled code */ }

@Deprecated
public func close(fd: Int32): Int32 { /* compiled code */ }

@Deprecated
public func lseek(fd: Int32, offset: Int64, whence: Int32): Int64 { /* compiled code */ }

@Deprecated
public func dup(fd: Int32): Int32 { /* compiled code */ }

@Deprecated
public func dup2(fd: Int32, fd2: Int32): Int32 { /* compiled code */ }

@Deprecated
public func read(fd: Int32, buffer: std.core.CPointer<UInt8>, nbyte: UIntNative): IntNative { /* compiled code */ }

@Deprecated
public func write(fd: Int32, buffer: std.core.CPointer<UInt8>, nbyte: UIntNative): IntNative { /* compiled code */ }

@Deprecated
public func isReg(path: std.core.String): Bool { /* compiled code */ }

@Deprecated
public func isDir(path: std.core.String): Bool { /* compiled code */ }

@Deprecated
public func isChr(path: std.core.String): Bool { /* compiled code */ }

@Deprecated
public func isBlk(path: std.core.String): Bool { /* compiled code */ }

@Deprecated
public func isFIFO(path: std.core.String): Bool { /* compiled code */ }

@Deprecated
public func chdir(path: std.core.String): Int32 { /* compiled code */ }

@Deprecated
public func getcwd(): std.core.String { /* compiled code */ }

@Deprecated
public func remove(path: std.core.String): Int32 { /* compiled code */ }

@Deprecated
public func rename(oldName: std.core.String, newName: std.core.String): Int32 { /* compiled code */ }

@Deprecated
public func getpid(): Int32 { /* compiled code */ }

@Deprecated
public func access(path: std.core.String, mode: Int32): Int32 { /* compiled code */ }

@Deprecated
public func umask(cmask: UInt32): UInt32 { /* compiled code */ }

@Deprecated
public func chmod(path: std.core.String, mode: UInt32): Int32 { /* compiled code */ }

@Deprecated
public func isatty(fd: Int32): Bool { /* compiled code */ }
