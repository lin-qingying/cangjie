// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package std.env
import std.io.InputStream
import std.sync.*

import std.io.OutputStream
import std.collection.*
import std.fs.*
import std.io.*
import std.io.IOException

/**
* This class Provides the ability to read data from console
*/
@!APILevel[12, atomicservice : true]
public class ConsoleReader <: InputStream {
    /**
    * Read one char
    * @throws IllegalMemoryException if there are some system errors.
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func read(): ?Rune
    
    /** Read one line */
    @!APILevel[12, atomicservice : true]
    public func readln(): ?String
    
    /** Read all line */
    @!APILevel[12, atomicservice : true]
    public func readToEnd(): ?String
    
    /**
    * Reads until some contents is encountered, or the end of the stream is reached.
    * ch is included in the result
    */
    @!APILevel[12, atomicservice : true]
    public func readUntil(ch: Rune): ?String
    
    /**
    * Reads until predicate is encountered, or the end of the stream is reached.
    * ch is included in the result
    */
    @!APILevel[12, atomicservice : true]
    public func readUntil(predicate: (Rune) -> Bool): ?String
    
    /*
    * write Array<Byte> from stdIn to arr and return the length
    */
    @!APILevel[12, atomicservice : true]
    public func read(arr: Array<Byte>): Int64
}

@!APILevel[12, atomicservice : true]
public class ConsoleWriter <: OutputStream {
    @!APILevel[12, atomicservice : true]
    public func flush(): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(buffer: Array<Byte>): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(buffer: Array<Byte>): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: String): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write<T>(v: T): Unit where T <: ToString
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Bool): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Int8): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Int16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Int32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Int64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: UInt8): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: UInt16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: UInt32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: UInt64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Float16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Float32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Float64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Rune): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: String): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln<T>(v: T): Unit where T <: ToString
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Bool): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Int8): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Int16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Int32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Int64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: UInt8): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: UInt16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: UInt32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: UInt64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Float16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Float32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Float64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Rune): Unit
}

@!APILevel[12, atomicservice : true]
public class ConsoleWriter <: OutputStream {
    /**
    * func write() will write direct to the console
    * func flush() is no need in this class
    */
    @!APILevel[12, atomicservice : true]
    public func flush(): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(buffer: Array<Byte>): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(buffer: Array<Byte>): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: String): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write<T>(v: T): Unit where T <: ToString
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Bool): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Int8): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Int16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Int32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Int64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: UInt8): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: UInt16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: UInt32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: UInt64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Float16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Float32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Float64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func write(v: Rune): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: String): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln<T>(v: T): Unit where T <: ToString
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Bool): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Int8): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Int16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Int32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Int64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: UInt8): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: UInt16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: UInt32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: UInt64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Float16): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Float32): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Float64): Unit
    
    @!APILevel[12, atomicservice : true]
    public func writeln(v: Rune): Unit
}

@!APILevel[12, atomicservice : true]
public func getProcessId(): Int

@!APILevel[12, atomicservice : true]
public func getCommand(): String

@!APILevel[12, atomicservice : true]
public func getCommandLine(): Array<String>

@!APILevel[12, atomicservice : true]
public func getWorkingDirectory(): Path

@!APILevel[12, atomicservice : true]
public func getHomeDirectory(): Path

@!APILevel[12, atomicservice : true]
public func getTempDirectory(): Path

@!APILevel[12, atomicservice : true]
public func getVariable(key: String): ?String

@!APILevel[12, atomicservice : true]
public func getVariables(): Array<(String, String)>

@!APILevel[12, atomicservice : true]
public func setVariable(key: String, value: String): Unit

@!APILevel[12, atomicservice : true]
public func removeVariable(key: String): Unit

@!APILevel[12, atomicservice : true]
public func getStdErr(): ConsoleWriter

@!APILevel[12, atomicservice : true]
public func getStdIn(): ConsoleReader

@!APILevel[12, atomicservice : true]
public func getStdOut(): ConsoleWriter

@!APILevel[12, atomicservice : true]
public func atExit(callback: () -> Unit): Unit

@!APILevel[12, atomicservice : true]
public func exit(code: Int64): Nothing

/**
* ProcessException
*
*/
@!APILevel[12, atomicservice : true]
public class EnvException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

