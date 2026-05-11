// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.fs

import std.core.*
import std.collection.*
import std.io.*
import std.time.*
import std.io.IOException

let PATH_SEPARATOR: std.core.String

let PATH_LISTSEPARATOR: std.core.String

func copy(sourcePath: std.fs.Path, to: std.fs.Path, overwrite: Bool): Unit { /* compiled code */ }

struct Path <: std.core.Equatable<std.fs.Path> & std.core.Hashable & std.core.ToString {
    let _rawPath: std.core.String

    let Separator: std.core.String

    let ListSeparator: std.core.String

    init(rawPath: std.core.String) { /* compiled code */ }

    prop parent: std.fs.Path {
        get() { /* compiled code */ }
    }

    prop fileName: std.core.String {
        get() { /* compiled code */ }
    }

    prop extensionName: std.core.String {
        get() { /* compiled code */ }
    }

    prop fileNameWithoutExtension: std.core.String {
        get() { /* compiled code */ }
    }

    func isAbsolute(): Bool { /* compiled code */ }

    func isRelative(): Bool { /* compiled code */ }

    func join(path: std.core.String): std.fs.Path { /* compiled code */ }

    func join(path: std.fs.Path): std.fs.Path { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    func normalize(): std.fs.Path { /* compiled code */ }

    operator func ==(that: std.fs.Path): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    static func canonicalizePath(pathStr: std.core.String): std.core.Array<UInt8> { /* compiled code */ }
}

func copy(sourcePath: std.core.String, to: std.core.String, overwrite: Bool): Unit { /* compiled code */ }

func remove(path: std.fs.Path, recursive: Bool): Unit { /* compiled code */ }

func remove(path: std.core.String, recursive: Bool): Unit { /* compiled code */ }

func exists(path: std.fs.Path): Bool { /* compiled code */ }

func exists(path: std.core.String): Bool { /* compiled code */ }

func rename(sourcePath: std.core.String, to: std.core.String, overwrite: Bool): Unit { /* compiled code */ }

func rename(sourcePath: std.fs.Path, to: std.fs.Path, overwrite: Bool): Unit { /* compiled code */ }

func removeIfExists(path: std.fs.Path, recursive: Bool): Bool { /* compiled code */ }

func removeIfExists(path: std.core.String, recursive: Bool): Bool { /* compiled code */ }

class Directory {
    static func create(path: std.core.String, recursive: Bool): Unit { /* compiled code */ }

    static func create(path: std.fs.Path, recursive: Bool): Unit { /* compiled code */ }

    static func createTemp(directoryPath: std.core.String): std.fs.Path { /* compiled code */ }

    static func createTemp(directoryPath: std.fs.Path): std.fs.Path { /* compiled code */ }

    static func isEmpty(path: std.fs.Path): Bool { /* compiled code */ }

    static func isEmpty(path: std.core.String): Bool { /* compiled code */ }

    static func readFrom(path: std.fs.Path): std.core.Array<std.fs.FileInfo> { /* compiled code */ }

    static func readFrom(path: std.core.String): std.core.Array<std.fs.FileInfo> { /* compiled code */ }

    static func walk(path: std.fs.Path, f: (std.fs.FileInfo) -> Bool): Unit { /* compiled code */ }

    static func walk(path: std.core.String, f: (std.fs.FileInfo) -> Bool): Unit { /* compiled code */ }

    static func copy(sourceDirPath: std.core.String, destinationDirPath: std.core.String, overwrite: Bool): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

struct FileInfo <: std.core.Equatable<std.fs.FileInfo> {
    var _path: std.fs.Path

    init(path: std.fs.Path) { /* compiled code */ }

    init(path: std.core.String) { /* compiled code */ }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop parentDirectory: std.core.Option<std.fs.FileInfo> {
        get() { /* compiled code */ }
    }

    prop path: std.fs.Path {
        get() { /* compiled code */ }
    }

    prop creationTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    prop lastAccessTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    prop lastModificationTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isSymbolicLink(): Bool { /* compiled code */ }

    func isRegular(): Bool { /* compiled code */ }

    func isDirectory(): Bool { /* compiled code */ }

    func isReadOnly(): Bool { /* compiled code */ }

    func isHidden(): Bool { /* compiled code */ }

    func canExecute(): Bool { /* compiled code */ }

    func canRead(): Bool { /* compiled code */ }

    func canWrite(): Bool { /* compiled code */ }

    func setExecutable(executable: Bool): Bool { /* compiled code */ }

    func setReadable(readable: Bool): Bool { /* compiled code */ }

    func setWritable(writable: Bool): Bool { /* compiled code */ }

    operator func ==(that: std.fs.FileInfo): Bool { /* compiled code */ }
}

enum OpenMode <: std.core.ToString & std.core.Equatable<std.fs.OpenMode> {
    | Read

    | Write

    | Append

    | ReadWrite

    operator func ==(that: std.fs.OpenMode): Bool { /* compiled code */ }

    operator func !=(that: std.fs.OpenMode): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

struct FileDescriptor {
    var _fileHandle: IntNative

    prop fileHandle: IntNative {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

class File <: std.core.Object & std.core.Resource & std.io.IOStream & std.io.Seekable {
    var _fileInfo: std.fs.FileInfo

    var _openMode: std.fs.OpenMode

    var _fileDescriptor: std.fs.FileDescriptor

    var _canRead: Bool

    var _canWrite: Bool

    var _canSeek: Bool

    ~init() { /* compiled code */ }

    init(path: std.core.String, mode: std.fs.OpenMode) { /* compiled code */ }

    init(path: std.fs.Path, mode: std.fs.OpenMode) { /* compiled code */ }

    prop info: std.fs.FileInfo {
        get() { /* compiled code */ }
    }

    prop fileDescriptor: std.fs.FileDescriptor {
        get() { /* compiled code */ }
    }

    prop length: Int64 {
        get() { /* compiled code */ }
    }

    func setLength(length: Int64): Unit { /* compiled code */ }

    func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func flush(): Unit { /* compiled code */ }

    func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }

    func canRead(): Bool { /* compiled code */ }

    func canWrite(): Bool { /* compiled code */ }

    func close(): Unit { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    static func create(path: std.fs.Path): std.fs.File { /* compiled code */ }

    static func create(path: std.core.String): std.fs.File { /* compiled code */ }

    static func createTemp(directoryPath: std.core.String): std.fs.File { /* compiled code */ }

    static func createTemp(directoryPath: std.fs.Path): std.fs.File { /* compiled code */ }

    static func copy(sourcePath: std.core.String, destinationPath: std.core.String, overwrite: Bool): Unit { /* compiled code */ }

    static func readFrom(path: std.fs.Path): std.core.Array<UInt8> { /* compiled code */ }

    static func readFrom(path: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

    static func writeTo(path: std.fs.Path, buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    static func writeTo(path: std.core.String, buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    static func appendTo(path: std.fs.Path, buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    static func appendTo(path: std.core.String, buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }
}

class FSException <: std.io.IOException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class HardLink {
    static func create(link: std.fs.Path, to: std.fs.Path): Unit { /* compiled code */ }

    static func create(link: std.core.String, to: std.core.String): Unit { /* compiled code */ }
}

struct FsError {
    let rtnCode: Int64

    let msg: CString

    init() { /* compiled code */ }
}

struct FsInfo {
    let fd: IntNative

    let msg: CString

    init() { /* compiled code */ }
}

func canonicalize(path: std.core.String): std.fs.Path { /* compiled code */ }

func canonicalize(path: std.fs.Path): std.fs.Path { /* compiled code */ }

class SymbolicLink {
    static func create(link: std.fs.Path, to: std.fs.Path): Unit { /* compiled code */ }

    static func create(link: std.core.String, to: std.core.String): Unit { /* compiled code */ }

    static func readFrom(path: std.core.String, recursive: Bool): std.fs.Path { /* compiled code */ }

    static func readFrom(path: std.fs.Path, recursive: Bool): std.fs.Path { /* compiled code */ }
}
