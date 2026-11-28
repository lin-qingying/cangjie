// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.fs

import std.core.*
import std.collection.*
import std.io.*
import std.time.*
import std.io.IOException

let PATH_SEPARATOR: std.core.String = COMPILED_CODE /* compiled code */

let PATH_LISTSEPARATOR: std.core.String = COMPILED_CODE /* compiled code */

public func copy(sourcePath: std.fs.Path, to!: std.fs.Path, overwrite!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public struct Path <: std.core.Equatable<std.fs.Path> & std.core.Hashable & std.core.ToString {
    internal let _rawPath: std.core.String

    public static const Separator: std.core.String = COMPILED_CODE /* compiled code */

    public static const ListSeparator: std.core.String = COMPILED_CODE /* compiled code */

    init(rawPath: std.core.String) { /* compiled code */ }

    public prop parent: std.fs.Path {
        get() { /* compiled code */ }
    }

    public prop fileName: std.core.String {
        get() { /* compiled code */ }
    }

    public prop extensionName: std.core.String {
        get() { /* compiled code */ }
    }

    public prop fileNameWithoutExtension: std.core.String {
        get() { /* compiled code */ }
    }

    public func isAbsolute(): Bool { /* compiled code */ }

    public func isRelative(): Bool { /* compiled code */ }

    public func join(path: std.core.String): std.fs.Path { /* compiled code */ }

    public func join(path: std.fs.Path): std.fs.Path { /* compiled code */ }

    public func isEmpty(): Bool { /* compiled code */ }

    public func normalize(): std.fs.Path { /* compiled code */ }

    public operator func ==(that: std.fs.Path): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    internal static func canonicalizePath(pathStr: std.core.String): std.core.Array<UInt8> { /* compiled code */ }
}

public func copy(sourcePath: std.core.String, to!: std.core.String, overwrite!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func remove(path: std.fs.Path, recursive!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func remove(path: std.core.String, recursive!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func exists(path: std.fs.Path): Bool { /* compiled code */ }

public func exists(path: std.core.String): Bool { /* compiled code */ }

public func rename(sourcePath: std.core.String, to!: std.core.String, overwrite!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func rename(sourcePath: std.fs.Path, to!: std.fs.Path, overwrite!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func removeIfExists(path: std.fs.Path, recursive!: Bool = COMPILED_CODE /* compiled code */): Bool { /* compiled code */ }

public func removeIfExists(path: std.core.String, recursive!: Bool = COMPILED_CODE /* compiled code */): Bool { /* compiled code */ }

public class Directory {
    public static func create(path: std.core.String, recursive!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    public static func create(path: std.fs.Path, recursive!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    public static func createTemp(directoryPath: std.core.String): std.fs.Path { /* compiled code */ }

    public static func createTemp(directoryPath: std.fs.Path): std.fs.Path { /* compiled code */ }

    public static func isEmpty(path: std.fs.Path): Bool { /* compiled code */ }

    public static func isEmpty(path: std.core.String): Bool { /* compiled code */ }

    public static func readFrom(path: std.fs.Path): std.core.Array<std.fs.FileInfo> { /* compiled code */ }

    public static func readFrom(path: std.core.String): std.core.Array<std.fs.FileInfo> { /* compiled code */ }

    public static func walk(path: std.fs.Path, f: (std.fs.FileInfo) -> Bool): Unit { /* compiled code */ }

    public static func walk(path: std.core.String, f: (std.fs.FileInfo) -> Bool): Unit { /* compiled code */ }

    internal static func copy(sourceDirPath: std.core.String, destinationDirPath: std.core.String, overwrite: Bool): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

public struct FileInfo <: std.core.Equatable<std.fs.FileInfo> {
    internal var _path: std.fs.Path

    init(path: std.fs.Path) { /* compiled code */ }

    init(path: std.core.String) { /* compiled code */ }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop parentDirectory: std.core.Option<std.fs.FileInfo> {
        get() { /* compiled code */ }
    }

    public prop path: std.fs.Path {
        get() { /* compiled code */ }
    }

    public prop creationTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    public prop lastAccessTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    public prop lastModificationTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public func isSymbolicLink(): Bool { /* compiled code */ }

    public func isRegular(): Bool { /* compiled code */ }

    public func isDirectory(): Bool { /* compiled code */ }

    public func isReadOnly(): Bool { /* compiled code */ }

    public func isHidden(): Bool { /* compiled code */ }

    public func canExecute(): Bool { /* compiled code */ }

    public func canRead(): Bool { /* compiled code */ }

    public func canWrite(): Bool { /* compiled code */ }

    public func setExecutable(executable: Bool): Bool { /* compiled code */ }

    public func setReadable(readable: Bool): Bool { /* compiled code */ }

    public func setWritable(writable: Bool): Bool { /* compiled code */ }

    public operator func ==(that: std.fs.FileInfo): Bool { /* compiled code */ }
}

public enum OpenMode <: std.core.ToString & std.core.Equatable<std.fs.OpenMode> {
    | Read

    | Write

    | Append

    | ReadWrite

    public operator func ==(that: std.fs.OpenMode): Bool { /* compiled code */ }

    public operator func !=(that: std.fs.OpenMode): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public struct FileDescriptor {
    internal var _fileHandle: IntNative

    public prop fileHandle: IntNative {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public class File <: std.core.Object & std.core.Resource & std.io.IOStream & std.io.Seekable {
    private var _fileInfo: std.fs.FileInfo

    private var _openMode: std.fs.OpenMode

    internal var _fileDescriptor: std.fs.FileDescriptor

    private var _canRead: Bool

    private var _canWrite: Bool

    private var _canSeek: Bool

    ~init() { /* compiled code */ }

    init(path: std.core.String, mode: std.fs.OpenMode) { /* compiled code */ }

    init(path: std.fs.Path, mode: std.fs.OpenMode) { /* compiled code */ }

    public prop info: std.fs.FileInfo {
        get() { /* compiled code */ }
    }

    public prop fileDescriptor: std.fs.FileDescriptor {
        get() { /* compiled code */ }
    }

    public prop length: Int64 {
        get() { /* compiled code */ }
    }

    public func setLength(length: Int64): Unit { /* compiled code */ }

    public func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func flush(): Unit { /* compiled code */ }

    public func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }

    public func canRead(): Bool { /* compiled code */ }

    public func canWrite(): Bool { /* compiled code */ }

    public func close(): Unit { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }

    public static func create(path: std.fs.Path): std.fs.File { /* compiled code */ }

    public static func create(path: std.core.String): std.fs.File { /* compiled code */ }

    public static func createTemp(directoryPath: std.core.String): std.fs.File { /* compiled code */ }

    public static func createTemp(directoryPath: std.fs.Path): std.fs.File { /* compiled code */ }

    internal static func copy(sourcePath: std.core.String, destinationPath: std.core.String, overwrite: Bool): Unit { /* compiled code */ }

    public static func readFrom(path: std.fs.Path): std.core.Array<UInt8> { /* compiled code */ }

    public static func readFrom(path: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

    public static func writeTo(path: std.fs.Path, buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public static func writeTo(path: std.core.String, buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public static func appendTo(path: std.fs.Path, buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public static func appendTo(path: std.core.String, buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }
}

public class FSException <: std.io.IOException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class HardLink {
    public static func create(link: std.fs.Path, to!: std.fs.Path): Unit { /* compiled code */ }

    public static func create(link: std.core.String, to!: std.core.String): Unit { /* compiled code */ }
}

internal struct FsError {
    internal let rtnCode: Int64

    internal let msg: CString

    init() { /* compiled code */ }
}

internal struct FsInfo {
    internal let fd: IntNative

    internal let msg: CString

    init() { /* compiled code */ }
}

public func canonicalize(path: std.core.String): std.fs.Path { /* compiled code */ }

public func canonicalize(path: std.fs.Path): std.fs.Path { /* compiled code */ }

public class SymbolicLink {
    public static func create(link: std.fs.Path, to!: std.fs.Path): Unit { /* compiled code */ }

    public static func create(link: std.core.String, to!: std.core.String): Unit { /* compiled code */ }

    public static func readFrom(path: std.core.String, recursive!: Bool = COMPILED_CODE /* compiled code */): std.fs.Path { /* compiled code */ }

    public static func readFrom(path: std.fs.Path, recursive!: Bool = COMPILED_CODE /* compiled code */): std.fs.Path { /* compiled code */ }
}
