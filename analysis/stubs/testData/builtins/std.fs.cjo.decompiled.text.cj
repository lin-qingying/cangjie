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

func copy(sourcePath: std.fs.Path, to: std.fs.Path, overwrite: Bool): Unit

struct Path <: std.core.Equatable<std.fs.Path> & std.core.Hashable & std.core.ToString {
    let _rawPath: std.core.String

    let Separator: std.core.String

    let ListSeparator: std.core.String

    init(rawPath: std.core.String) { /* compiled code */ }

    prop parent: std.fs.Path

    prop fileName: std.core.String

    prop extensionName: std.core.String

    prop fileNameWithoutExtension: std.core.String

    func isAbsolute: Bool

    func isRelative: Bool

    func join(path: std.core.String): std.fs.Path

    func join(path: std.fs.Path): std.fs.Path

    func isEmpty: Bool

    func normalize: std.fs.Path

    operator func ==(that: std.fs.Path): Bool

    func hashCode: Int64

    func toString: std.core.String

    func canonicalizePath(pathStr: std.core.String): std.core.Array<UInt8>
}

func copy(sourcePath: std.core.String, to: std.core.String, overwrite: Bool): Unit

func remove(path: std.fs.Path, recursive: Bool): Unit

func remove(path: std.core.String, recursive: Bool): Unit

func exists(path: std.fs.Path): Bool

func exists(path: std.core.String): Bool

func rename(sourcePath: std.core.String, to: std.core.String, overwrite: Bool): Unit

func rename(sourcePath: std.fs.Path, to: std.fs.Path, overwrite: Bool): Unit

func removeIfExists(path: std.fs.Path, recursive: Bool): Bool

func removeIfExists(path: std.core.String, recursive: Bool): Bool

class Directory {
    func create(path: std.core.String, recursive: Bool): Unit

    func create(path: std.fs.Path, recursive: Bool): Unit

    func createTemp(directoryPath: std.core.String): std.fs.Path

    func createTemp(directoryPath: std.fs.Path): std.fs.Path

    func isEmpty(path: std.fs.Path): Bool

    func isEmpty(path: std.core.String): Bool

    func readFrom(path: std.fs.Path): std.core.Array<std.fs.FileInfo>

    func readFrom(path: std.core.String): std.core.Array<std.fs.FileInfo>

    func walk(path: std.fs.Path, f: (std.fs.FileInfo) -> Bool): Unit

    func walk(path: std.core.String, f: (std.fs.FileInfo) -> Bool): Unit

    func copy(sourceDirPath: std.core.String, destinationDirPath: std.core.String, overwrite: Bool): Unit

    init() { /* compiled code */ }
}

struct FileInfo <: std.core.Equatable<std.fs.FileInfo> {
    var _path: std.fs.Path

    init(path: std.fs.Path) { /* compiled code */ }

    init(path: std.core.String) { /* compiled code */ }

    prop name: std.core.String

    prop parentDirectory: std.core.Option<std.fs.FileInfo>

    prop path: std.fs.Path

    prop creationTime: std.time.DateTime

    prop lastAccessTime: std.time.DateTime

    prop lastModificationTime: std.time.DateTime

    prop size: Int64

    func isSymbolicLink: Bool

    func isRegular: Bool

    func isDirectory: Bool

    func isReadOnly: Bool

    func isHidden: Bool

    func canExecute: Bool

    func canRead: Bool

    func canWrite: Bool

    func setExecutable(executable: Bool): Bool

    func setReadable(readable: Bool): Bool

    func setWritable(writable: Bool): Bool

    operator func ==(that: std.fs.FileInfo): Bool
}

enum OpenMode <: std.core.ToString & std.core.Equatable<std.fs.OpenMode> {
    | Read

    | Write

    | Append

    | ReadWrite

    operator func ==(that: std.fs.OpenMode): Bool

    operator func !=(that: std.fs.OpenMode): Bool

    func toString: std.core.String
}

struct FileDescriptor {
    var _fileHandle: IntNative

    prop fileHandle: IntNative

    init() { /* compiled code */ }
}

class File <: std.core.Object & std.core.Resource & std.io.IOStream & std.io.Seekable {
    var _fileInfo: std.fs.FileInfo

    var _openMode: std.fs.OpenMode

    var _fileDescriptor: std.fs.FileDescriptor

    var _canRead: Bool

    var _canWrite: Bool

    var _canSeek: Bool

    ~init()

    init(path: std.core.String, mode: std.fs.OpenMode) { /* compiled code */ }

    init(path: std.fs.Path, mode: std.fs.OpenMode) { /* compiled code */ }

    prop info: std.fs.FileInfo

    prop fileDescriptor: std.fs.FileDescriptor

    prop length: Int64

    func setLength(length: Int64): Unit

    func read(buffer: std.core.Array<UInt8>): Int64

    func write(buffer: std.core.Array<UInt8>): Unit

    func flush: Unit

    func seek(sp: std.io.SeekPosition): Int64

    func canRead: Bool

    func canWrite: Bool

    func close: Unit

    func isClosed: Bool

    func create(path: std.fs.Path): std.fs.File

    func create(path: std.core.String): std.fs.File

    func createTemp(directoryPath: std.core.String): std.fs.File

    func createTemp(directoryPath: std.fs.Path): std.fs.File

    func copy(sourcePath: std.core.String, destinationPath: std.core.String, overwrite: Bool): Unit

    func readFrom(path: std.fs.Path): std.core.Array<UInt8>

    func readFrom(path: std.core.String): std.core.Array<UInt8>

    func writeTo(path: std.fs.Path, buffer: std.core.Array<UInt8>): Unit

    func writeTo(path: std.core.String, buffer: std.core.Array<UInt8>): Unit

    func appendTo(path: std.fs.Path, buffer: std.core.Array<UInt8>): Unit

    func appendTo(path: std.core.String, buffer: std.core.Array<UInt8>): Unit
}

class FSException <: std.io.IOException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class HardLink {
    func create(link: std.fs.Path, to: std.fs.Path): Unit

    func create(link: std.core.String, to: std.core.String): Unit
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

func canonicalize(path: std.core.String): std.fs.Path

func canonicalize(path: std.fs.Path): std.fs.Path

class SymbolicLink {
    func create(link: std.fs.Path, to: std.fs.Path): Unit

    func create(link: std.core.String, to: std.core.String): Unit

    func readFrom(path: std.core.String, recursive: Bool): std.fs.Path

    func readFrom(path: std.fs.Path, recursive: Bool): std.fs.Path
}
