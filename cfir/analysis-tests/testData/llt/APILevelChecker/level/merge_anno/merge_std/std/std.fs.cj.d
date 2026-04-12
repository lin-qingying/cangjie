// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package std.fs

import std.collection.*
import std.io.*
import std.time.*
import std.io.IOException

@!APILevel[since: "12", atomicservice : true]
public func copy(sourcePath: Path, to!: Path, overwrite!: Bool = false): Unit

@!APILevel[since: "12", atomicservice : true]
public func copy(sourcePath: String, to!: String, overwrite!: Bool = false): Unit

@!APILevel[since: "12", atomicservice : true]
public func remove(path: Path, recursive!: Bool = false): Unit

@!APILevel[since: "12", atomicservice : true]
public func remove(path: String, recursive!: Bool = false): Unit

@!APILevel[since: "12", atomicservice : true]
public func exists(path: Path): Bool

@!APILevel[since: "12", atomicservice : true]
public func exists(path: String): Bool

@!APILevel[since: "12", atomicservice : true]
public func rename(sourcePath: String, to!: String, overwrite!: Bool = false): Unit

@!APILevel[since: "12", atomicservice : true]
public func rename(sourcePath: Path, to!: Path, overwrite!: Bool = false): Unit

@!APILevel[since: "12", atomicservice : true]
public func removeIfExists(path: Path, recursive!: Bool = false): Bool

@!APILevel[since: "12", atomicservice : true]
public func removeIfExists(path: String, recursive!: Bool = false): Bool

@!APILevel[since: "12", atomicservice : true]
public class Directory {
    /*
    * @throws IllegalArgumentException while path is empty, or path is current directory,
    * or path is root directory, or path contains null character.
    * @throws FSException while path is exist, or while recursive is false
    * and the middle path is not exist.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func create(path: String, recursive!: Bool = false): Unit
    
    /*
    * @throws IllegalArgumentException while path is empty, or path is current directory,
    * or path is root directory, or path contains null character.
    * @throws FSException while path is exist, or while recursive is false
    * and the middle path is not exist.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func create(path: Path, recursive!: Bool = false): Unit
    
    /*
    * @throws FSException while path is not exist or failed to create temporary directory
    * @throws IllegalArgumentException while path is empty or path contains null character
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func createTemp(directoryPath: String): Path
    
    /*
    * @throws FSException while path is not exist or failed to create temporary directory.
    * @throws IllegalArgumentException while path is empty or path contains null character
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func createTemp(directoryPath: Path): Path
    
    @!APILevel[since: "12", atomicservice : true]
    public static func isEmpty(path: Path): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public static func isEmpty(path: String): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readFrom(path: Path): Array<FileInfo>
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readFrom(path: String): Array<FileInfo>
    
    @!APILevel[since: "12", atomicservice : true]
    public static func walk(path: Path, f: (FileInfo)->Bool): Unit
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func walk(path: String, f: (FileInfo)->Bool): Unit
}

@!APILevel[since: "12", atomicservice : true]
public enum OpenMode <: ToString & Equatable<OpenMode> {
    @!APILevel[since: "12", atomicservice : true]
    Read |
    @!APILevel[since: "12", atomicservice : true]
    Write |
    @!APILevel[since: "12", atomicservice : true]
    Append |
    @!APILevel[since: "12", atomicservice : true]
    ReadWrite
    @!APILevel[since: "12", atomicservice : true]
    public operator func ==(that: OpenMode): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public operator func !=(that: OpenMode): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func toString(): String
}

// FD numbers are indexes into the FD table.// Unix/Linux use the Int type for handle, while Windows use HANDLE type.// HANDLE is typedef'd void *, which is really just a 32-bit index, only for more opaque.
@!APILevel[since: "12", atomicservice : true]
public struct FileDescriptor {
    @!APILevel[since: "12", atomicservice : true]
    public prop fileHandle: IntNative
}

@!APILevel[since: "12", atomicservice : true]
public class File <: Resource & IOStream & Seekable {
    /**
    * Constructors
    *
    * @param path - The file path.
    * @param mode - The open mode.
    *
    * @throws IllegalArgumentException - If path is empty or path contains null character.
    * @throws FSException - If the file does not exist while operation is open,
    * the file already exists while operation is create,
    * the parent directory of the file does not exist,
    * or other reasons caused fail to open file.
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(path: String, mode: OpenMode)
    
    /**
    * Constructors
    *
    * @param path - The file path.
    * @param mode - The open mode.
    *
    * @throws IllegalArgumentException - If path is empty or contains null character.
    * @throws FSException - If the file does not exist while operation is open,
    * the file already exists while operation is create,
    * the parent directory of the file does not exist,
    * or other reasons caused fail to open file.
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(path: Path, mode: OpenMode)
    
    @!APILevel[since: "12", atomicservice : true]
    public prop info: FileInfo
    
    @!APILevel[since: "12", atomicservice : true]
    public prop fileDescriptor: FileDescriptor
    
    @!APILevel[since: "12", atomicservice : true]
    public prop length: Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public func setLength(length: Int64): Unit
    
    /**
    * Read data from file.
    *
    * @param buffer - Read data from file to the byte array.
    *
    * @return Int64 - Size of the data be read.
    *
    * @throws IllegalArgumentException - If buffer is empty.
    * @throws FSException - If the file to read is not opened,
    *      or the file does not have the read permission
    *      or failed to read file
    */
    @!APILevel[since: "12", atomicservice : true]
    public func read(buffer: Array<Byte>): Int64
    
    /**
    * @throws FSException if system failed to write the file
    * @throws FSException if file is not opened
    * @throws FSException if the file is not allowed to write
    */
    @!APILevel[since: "12", atomicservice : true]
    public func write(buffer: Array<Byte>): Unit
    
    @!APILevel[since: "12", atomicservice : true]
    public func flush(): Unit
    
    /**
    * @throws FSException if file can not seek
    * @throws FSException if there is error in parameter
    * @throws FSException if unknown errors occurred
    */
    @!APILevel[since: "12", atomicservice : true]
    public func seek(sp: SeekPosition): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public func canRead(): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func canWrite(): Bool
    
    /**
    * @throws FSException if system failed to close file
    */
    @!APILevel[since: "12", atomicservice : true]
    public func close(): Unit
    
    @!APILevel[since: "12", atomicservice : true]
    public func isClosed(): Bool
    
    /**
    * @throws FSException while path is empty.
    * @throws IllegalArgumentException while path contains null character.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func create(path: Path): File
    
    /**
    * @throws FSException while path is empty.
    * @throws IllegalArgumentException while path contains null character.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func create(path: String): File
    
    /**
    * @throws FSException if failed to create the temporary file or directoryPath is invalid.
    * @throws IllegalArgumentException while path contains null character or path is empty.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func createTemp(directoryPath: String): File
    
    /**
    * @throws FSException if failed to create the temporary file or directoryPath is invalid.
    * @throws IllegalArgumentException while path contains null character or path is empty.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func createTemp(directoryPath: Path): File
    
    /**
    * @throws FSException while path is empty.
    * @throws IllegalArgumentException while path contains null character.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func readFrom(path: Path): Array<Byte>
    
    /**
    * @throws FSException while path is empty.
    * @throws IllegalArgumentException while path contains null character.
    * @throws FSException if file read failed
    * @throws FSException if system failed to close file
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func readFrom(path: String): Array<Byte>
    
    /**
    * @throws FSException while path is empty.
    * @throws IllegalArgumentException while path contains null character.
    * @throws FSException if system failed to close file.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func writeTo(path: Path, buffer: Array<Byte>): Unit
    
    /**
    * @throws FSException while path is empty.
    * @throws IllegalArgumentException while path contains null character.
    * @throws FSException if system failed to close file.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func writeTo(path: String, buffer: Array<Byte>): Unit
    
    /**
    * @throws FSException while path is empty.
    * @throws IllegalArgumentException while path contains null character.
    * @throws FSException if system failed to close file
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func appendTo(path: Path, buffer: Array<Byte>): Unit
    
    /**
    * @throws FSException while path is empty.
    * @throws IllegalArgumentException while path contains null character.
    * @throws FSException if system failed to close file
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func appendTo(path: String, buffer: Array<Byte>): Unit
}

@!APILevel[since: "12", atomicservice : true]
public struct FileInfo <: Equatable<FileInfo> {
    @!APILevel[since: "12", atomicservice : true]
    public init(path: Path)
    
    @!APILevel[since: "12", atomicservice : true]
    public init(path: String)
    
    @!APILevel[since: "12", atomicservice : true]
    public prop name: String
    
    @!APILevel[since: "12", atomicservice : true]
    public prop parentDirectory: Option<FileInfo>
    
    @!APILevel[since: "12", atomicservice : true]
    public prop path: Path
    
    /**
    * @throws FSException if system failed to get creation time
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop creationTime: DateTime
    
    /**
    * @throws FSException if system failed to get last access time
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop lastAccessTime: DateTime
    
    /**
    * @throws FSException if system failed to get last modification time
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop lastModificationTime: DateTime
    
    /**
    * @throws FSException if memory copy failed or get directory/file size failed
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop size: Int64
    
    /**
    * @throws FSException if cPath is invalid or symbol not linked
    */
    @!APILevel[since: "12", atomicservice : true]
    public func isSymbolicLink(): Bool
    
    /**
    * @throws FSException if cPath is invalid
    */
    @!APILevel[since: "12", atomicservice : true]
    public func isRegular(): Bool
    
    /**
    * @throws FSException if cPath is invalid
    */
    @!APILevel[since: "12", atomicservice : true]
    public func isDirectory(): Bool
    
    /**
    * @throws FSException if cPath is invalid
    */
    @!APILevel[since: "12", atomicservice : true]
    public func isReadOnly(): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func isHidden(): Bool
    
    /**
    * @throws FSException if cPath is invalid
    */
    @!APILevel[since: "12", atomicservice : true]
    public func canExecute(): Bool
    
    /**
    * @throws FSException if cPath is invalid
    */
    @!APILevel[since: "12", atomicservice : true]
    public func canRead(): Bool
    
    /**
    * @throws FSException if cPath is invalid
    */
    @!APILevel[since: "12", atomicservice : true]
    public func canWrite(): Bool
    
    /**
    * @throws FSException if cPath is invalid
    * @throws FSException if operation not permitted on Windows
    */
    @!APILevel[since: "12", atomicservice : true]
    public func setExecutable(executable: Bool): Bool
    
    /**
    * @throws FSException if cPath is invalid
    * @throws FSException if operation not permitted on Windows
    */
    @!APILevel[since: "12", atomicservice : true]
    public func setReadable(readable: Bool): Bool
    
    /**
    * @throws FSException if cPath is invalid
    * @throws FSException if operation not permitted on Windows
    */
    @!APILevel[since: "12", atomicservice : true]
    public func setWritable(writable: Bool): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public operator func ==(that: FileInfo): Bool
}

@!APILevel[since: "12", atomicservice : true]
public class FSException <: IOException {
    @!APILevel[since: "12", atomicservice : true]
    public init()
    
    @!APILevel[since: "12", atomicservice : true]
    public init(message: String)
}

@!APILevel[since: "12", atomicservice : true]
public class HardLink {
    @!APILevel[since: "12", atomicservice : true]
    public static func create(link: Path, to!: Path): Unit
    
    @!APILevel[since: "12", atomicservice : true]
    public static func create(link: String, to!: String): Unit
}

@!APILevel[since: "12", atomicservice : true]
public func canonicalize(path: String): Path

@!APILevel[since: "12", atomicservice : true]
public func canonicalize(path: Path): Path

@!APILevel[since: "12", atomicservice : true]
public struct Path <: Equatable<Path> & Hashable & ToString {
    @!APILevel[since: "12", atomicservice : true]
    public static const Separator: String = PATH_SEPARATOR
    
    @!APILevel[since: "12", atomicservice : true]
    public static const ListSeparator: String = PATH_LISTSEPARATOR
    
    @!APILevel[since: "12", atomicservice : true]
    public init(rawPath: String)
    
    /**
    * @throws IllegalArgumentException while path contains null character,
    * or path is empty.
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop parent: Path
    
    /**
    * @throws IllegalArgumentException while path contains null character,
    * or path is empty.
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop fileName: String
    
    /**
    * @throws IllegalArgumentException while path contains null character,
    * or path is empty.
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop extensionName: String
    
    /**
    * @throws IllegalArgumentException while path contains null character,
    * or path is empty.
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop fileNameWithoutExtension: String
    
    /**
    * @throws IllegalArgumentException while path contains null character,
    * or path is empty.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func isAbsolute(): Bool
    
    /**
    * @throws IllegalArgumentException while path contains null character,
    * or path is empty.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func isRelative(): Bool
    
    /**
    * @throws IllegalArgumentException while rawpath or path contains null character, or rawpath is empty.
    * @throws FSException if path is an absolute path.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func join(path: String): Path
    
    /**
    * @throws IllegalArgumentException while path contains null character,
    * or path is empty.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func join(path: Path): Path
    
    @!APILevel[since: "12", atomicservice : true]
    public func isEmpty(): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func normalize(): Path
    
    @!APILevel[since: "12", atomicservice : true]
    public operator func ==(that: Path): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public func toString(): String
}

@!APILevel[since: "12", atomicservice : true]
public class SymbolicLink {
    @!APILevel[since: "12", atomicservice : true]
    public static func create(link: Path, to!: Path): Unit
    
    @!APILevel[since: "12", atomicservice : true]
    public static func create(link: String, to!: String): Unit
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readFrom(path: String, recursive!: Bool = false): Path
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readFrom(path: Path, recursive!: Bool = false): Path
}

