// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.posix

import std.core.*

let O_RDONLY: Int32

let O_RDWR: Int32

let O_WRONLY: Int32

let O_APPEND: Int32

let O_CREAT: Int32

let O_EXCL: Int32

let O_TRUNC: Int32

let S_IRUSR: UInt32

let S_IWUSR: UInt32

let S_IXUSR: UInt32

let S_IRWXU: UInt32

let S_IRGRP: UInt32

let S_IWGRP: UInt32

let S_IXGRP: UInt32

let S_IRWXG: UInt32

let S_IROTH: UInt32

let S_IWOTH: UInt32

let S_IXOTH: UInt32

let S_IRWXO: UInt32

let SEEK_SET: Int32

let SEEK_CUR: Int32

let SEEK_END: Int32

let S_IFREG: UInt32

let S_IFDIR: UInt32

let S_IFCHR: UInt32

let S_IFBLK: UInt32

let S_IFIFO: UInt32

let S_IFLNK: UInt32

let S_IFSOCK: UInt32

let AT_EMPTY_PATH: Int32

let AT_SYMLINK_FOLLOW: Int32

let AT_FDCWD: Int32

let AT_REMOVEDIR: Int32

let SIGHUP: Int32

let SIGINT: Int32

let SIGQUIT: Int32

let SIGILL: Int32

let SIGTRAP: Int32

let SIGABRT: Int32

let SIGIOT: Int32

let SIGBUS: Int32

let SIGFPE: Int32

let SIGKILL: Int32

let SIGUSR1: Int32

let SIGSEGV: Int32

let SIGUSR2: Int32

let SIGPIPE: Int32

let SIGALRM: Int32

let SIGTERM: Int32

let SIGSTKFLT: Int32

let SIGCHLD: Int32

let SIGCONT: Int32

let SIGSTOP: Int32

let SIGTSTP: Int32

let SIGTTIN: Int32

let SIGTTOU: Int32

let SIGURG: Int32

let SIGXCPU: Int32

let SIGXFSZ: Int32

let SIGVTALRM: Int32

let SIGPROF: Int32

let SIGWINCH: Int32

let SIGIO: Int32

let SIGPWR: Int32

let SIGSYS: Int32

let R_OK: Int32

let W_OK: Int32

let X_OK: Int32

let F_OK: Int32

func `open`(path: std.core.String, oflag: Int32, flag: UInt32): Int32 { /* compiled code */ }

func `open`(path: std.core.String, oflag: Int32): Int32 { /* compiled code */ }

func creat(path: std.core.String, flag: UInt32): Int32 { /* compiled code */ }

func close(fd: Int32): Int32 { /* compiled code */ }

func lseek(fd: Int32, offset: Int64, whence: Int32): Int64 { /* compiled code */ }

func dup(fd: Int32): Int32 { /* compiled code */ }

func dup2(fd: Int32, fd2: Int32): Int32 { /* compiled code */ }

func read(fd: Int32, buffer: CPointer<UInt8>, nbyte: UIntNative): IntNative { /* compiled code */ }

func write(fd: Int32, buffer: CPointer<UInt8>, nbyte: UIntNative): IntNative { /* compiled code */ }

func isReg(path: std.core.String): Bool { /* compiled code */ }

func isDir(path: std.core.String): Bool { /* compiled code */ }

func isChr(path: std.core.String): Bool { /* compiled code */ }

func isBlk(path: std.core.String): Bool { /* compiled code */ }

func isFIFO(path: std.core.String): Bool { /* compiled code */ }

func chdir(path: std.core.String): Int32 { /* compiled code */ }

func getcwd(): std.core.String { /* compiled code */ }

func remove(path: std.core.String): Int32 { /* compiled code */ }

func rename(oldName: std.core.String, newName: std.core.String): Int32 { /* compiled code */ }

func getpid(): Int32 { /* compiled code */ }

func access(path: std.core.String, mode: Int32): Int32 { /* compiled code */ }

func umask(cmask: UInt32): UInt32 { /* compiled code */ }

func chmod(path: std.core.String, mode: UInt32): Int32 { /* compiled code */ }

func isatty(fd: Int32): Bool { /* compiled code */ }
