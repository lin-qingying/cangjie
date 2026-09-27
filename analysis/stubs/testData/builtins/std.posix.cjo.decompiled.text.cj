// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.posix

import std.core.*

@Deprecated
@Annotation
let O_RDONLY: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let O_RDWR: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let O_WRONLY: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let O_APPEND: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let O_CREAT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let O_EXCL: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let O_TRUNC: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IRUSR: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IWUSR: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IXUSR: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IRWXU: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IRGRP: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IWGRP: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IXGRP: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IRWXG: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IROTH: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IWOTH: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IXOTH: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IRWXO: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SEEK_SET: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SEEK_CUR: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SEEK_END: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IFREG: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IFDIR: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IFCHR: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IFBLK: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IFIFO: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IFLNK: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let S_IFSOCK: UInt32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let AT_EMPTY_PATH: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let AT_SYMLINK_FOLLOW: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let AT_FDCWD: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let AT_REMOVEDIR: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGHUP: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGINT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGQUIT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGILL: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGTRAP: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGABRT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGIOT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGBUS: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGFPE: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGKILL: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGUSR1: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGSEGV: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGUSR2: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGPIPE: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGALRM: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGTERM: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGSTKFLT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGCHLD: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGCONT: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGSTOP: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGTSTP: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGTTIN: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGTTOU: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGURG: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGXCPU: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGXFSZ: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGVTALRM: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGPROF: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGWINCH: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGIO: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGPWR: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let SIGSYS: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let R_OK: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let W_OK: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let X_OK: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
@Annotation
let F_OK: Int32 = COMPILED_CODE /* compiled code */

@Deprecated
func open(path: std.core.String, oflag: Int32, flag: UInt32): Int32 { /* compiled code */ }

@Deprecated
func open(path: std.core.String, oflag: Int32): Int32 { /* compiled code */ }

@Deprecated
func creat(path: std.core.String, flag: UInt32): Int32 { /* compiled code */ }

@Deprecated
func close(fd: Int32): Int32 { /* compiled code */ }

@Deprecated
func lseek(fd: Int32, offset: Int64, whence: Int32): Int64 { /* compiled code */ }

@Deprecated
func dup(fd: Int32): Int32 { /* compiled code */ }

@Deprecated
func dup2(fd: Int32, fd2: Int32): Int32 { /* compiled code */ }

@Deprecated
func read(fd: Int32, buffer: CPointer<UInt8>, nbyte: UIntNative): IntNative { /* compiled code */ }

@Deprecated
func write(fd: Int32, buffer: CPointer<UInt8>, nbyte: UIntNative): IntNative { /* compiled code */ }

@Deprecated
func isReg(path: std.core.String): Bool { /* compiled code */ }

@Deprecated
func isDir(path: std.core.String): Bool { /* compiled code */ }

@Deprecated
func isChr(path: std.core.String): Bool { /* compiled code */ }

@Deprecated
func isBlk(path: std.core.String): Bool { /* compiled code */ }

@Deprecated
func isFIFO(path: std.core.String): Bool { /* compiled code */ }

@Deprecated
func chdir(path: std.core.String): Int32 { /* compiled code */ }

@Deprecated
func getcwd(): std.core.String { /* compiled code */ }

@Deprecated
func remove(path: std.core.String): Int32 { /* compiled code */ }

@Deprecated
func rename(oldName: std.core.String, newName: std.core.String): Int32 { /* compiled code */ }

@Deprecated
func getpid(): Int32 { /* compiled code */ }

@Deprecated
func access(path: std.core.String, mode: Int32): Int32 { /* compiled code */ }

@Deprecated
func umask(cmask: UInt32): UInt32 { /* compiled code */ }

@Deprecated
func chmod(path: std.core.String, mode: UInt32): Int32 { /* compiled code */ }

@Deprecated
func isatty(fd: Int32): Bool { /* compiled code */ }
