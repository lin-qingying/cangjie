// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package std.io


/**
* A BufferedInputStream has a built-in buffer to cache the content of the InputStream.
* BufferedInputStream is meant for cache the InputStream.
*/
@!APILevel[12, atomicservice : true]
public class BufferedInputStream<T> <: InputStream where T <: InputStream {
    /**
    * Constructor
    *
    * The default capaity of the built-in buffer is 4096.
    *
    * @params input - The InputStream
    */
    @!APILevel[12, atomicservice : true]
    public init(input: T)
    
    /**
    * Constructor
    *
    * @params input - The InputStream
    * @params capacity - Capacity of the buit-in buffer
    *
    * @throws IllegalArgumentException - If `capacity` less than 0.
    */
    @!APILevel[12, atomicservice : true]
    public init(input: T, capacity: Int64)
    
    @!APILevel[12, atomicservice : true]
    public init(input: T, buffer: Array<Byte>)
    
    /**
    * Read the current InputSteam into the buffer.
    *
    * @params buffer - Will read from InputStream to the buffer.
    *
    * @return Size read into the buffer.
    * @throws IllegalArgumentException - If the `buffer` is empty.
    */
    @!APILevel[12, atomicservice : true]
    public func read(buffer: Array<Byte>): Int64
    
    @!APILevel[12, atomicservice : true]
    public func readByte(): ?Byte
    
    /**
    * Bind this.inputStream to the new InputStream.
    * Will not change the capacity.
    *
    * @params input - The new InputStream
    */
    @!APILevel[12, atomicservice : true]
    public func reset(input: T): Unit
}

@!APILevel[12, atomicservice : true]
extend<T> BufferedInputStream<T> <: Resource where T <: Resource {
    /**
    * Close the current stream.
    */
    @!APILevel[12, atomicservice : true]
    public func close(): Unit
    
    /**
    * Returns whether the current flow is closed.
    *
    * @return true if the current stream has been closed, otherwise returns false.
    */
    @!APILevel[12, atomicservice : true]
    public func isClosed(): Bool
}

@!APILevel[12, atomicservice : true]
extend<T> BufferedInputStream<T> <: Seekable where T <: Seekable {
    /**
    * Seek to an offset, in bytes, in a stream.
    *
    * @params sp - Start position of the offset and size of the offset.
    *
    * @return the number of bytes in the stream from the beginning of the data to the cursor position.
    */
    @!APILevel[12, atomicservice : true]
    public func seek(sp: SeekPosition): Int64
    
    /**
    * @return the position of the current cursor in the stream.
    */
    @!APILevel[12, atomicservice : true]
    public prop position: Int64
    
    /**
    * @return the number of data bytes from the current cursor position to the end of the file.
    */
    @!APILevel[12, atomicservice : true]
    public prop remainLength: Int64
    
    /**
    * @return the number of bytes from the file header to the file trailer.
    */
    @!APILevel[12, atomicservice : true]
    public prop length: Int64
}

/**
* A BufferedOutputStream has a built-in buffer to cache the content of the OutputStream.
* BufferedOutputStream is meant for cache the OutputStream.
*/
@!APILevel[12, atomicservice : true]
public class BufferedOutputStream<T> <: OutputStream where T <: OutputStream {
    /**
    * Constructor
    *
    * The default capaity of the built-in buffer is 4096.
    *
    * @params output - The OutputStream
    */
    @!APILevel[12, atomicservice : true]
    public init(output: T)
    
    @!APILevel[12, atomicservice : true]
    public init(output: T, buffer: Array<Byte>)
    
    /**
    * Constructor
    *
    * @params output - The OutputStream
    * @params capacity - Capacity of the buit-in buffer
    *
    * @throws IllegalArgumentException - If `capacity` less than 0.
    */
    @!APILevel[12, atomicservice : true]
    public init(output: T, capacity: Int64)
    
    /**
    * Write from buffer to the OutputStream.
    *
    * @params buffer - Will write to the OutputStream from the buffer.
    */
    @!APILevel[12, atomicservice : true]
    public func write(buffer: Array<Byte>): Unit
    
    /**
    * Write a Byte to the OutputStream.
    *
    * @params v - The byte write to the OutputStream.
    */
    @!APILevel[12, atomicservice : true]
    public func writeByte(v: Byte): Unit
    
    /**
    * Flush the OutputStream.
    */
    @!APILevel[12, atomicservice : true]
    public func flush(): Unit
    
    /**
    * Flush and bind this.outputSteam to the new OutputStream.
    * Will not change the capacity.
    *
    * @params output - The OutputStream
    */
    @!APILevel[12, atomicservice : true]
    public func reset(output: T): Unit
}

@!APILevel[12, atomicservice : true]
extend<T> BufferedOutputStream<T> <: Resource where T <: Resource {
    /**
    * Close the current stream.
    */
    @!APILevel[12, atomicservice : true]
    public func close(): Unit
    
    /**
    * Returns whether the current flow is closed.
    *
    * @return true if the current stream has been closed, otherwise returns false.
    */
    @!APILevel[12, atomicservice : true]
    public func isClosed(): Bool
}

@!APILevel[12, atomicservice : true]
extend<T> BufferedOutputStream<T> <: Seekable where T <: Seekable {
    /**
    * Seek to an offset, in bytes, in a stream.
    *
    * @params sp - Start position of the offset and size of the offset.
    *
    * @return the number of bytes in the stream from the beginning of the data to the cursor position.
    */
    @!APILevel[12, atomicservice : true]
    public func seek(sp: SeekPosition): Int64
    
    /**
    * @return the position of the current cursor in the stream.
    */
    @!APILevel[12, atomicservice : true]
    public prop position: Int64
    
    /**
    * @return the number of data bytes from the current cursor position to the end of the file.
    */
    @!APILevel[12, atomicservice : true]
    public prop remainLength: Int64
    
    /**
    * @return the number of bytes from the file header to the file trailer.
    */
    @!APILevel[12, atomicservice : true]
    public prop length: Int64
}

/**
* A ByteBuffer obtains a Byte Array to operate the Byte as Stream.
* ByteBuffer is meant for writing and reading byte streams.
*/
@!APILevel[12, atomicservice : true]
public class ByteBuffer <: IOStream & Seekable {
    /**
    * Creates a byte stream of the default capacity.
    */
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Constructors
    *
    * @params capacity - Capacity of the ByteBuffer.
    *
    * @throws IllegalArgumentException - If `capcacity` less than 0.
    */
    @!APILevel[12, atomicservice : true]
    public init(capacity: Int64)
    
    @!APILevel[12, atomicservice : true]
    public init(source: Array<Byte>)
    
    /**
    * Returns the capacity of the ByteBuffer.
    */
    @!APILevel[12, atomicservice : true]
    public prop capacity: Int64
    
    /**
    * Returns a copy of the ByteBuffer.
    */
    @!APILevel[12, atomicservice : true]
    public func clone(): ByteBuffer
    
    /**
    * Clears data from the ByteBuffer.
    */
    @!APILevel[12, atomicservice : true]
    public func clear(): Unit
    
    /**
    * Returns a reference to the original Array, any modification changes the original Array.
    */
    @!APILevel[12, atomicservice : true]
    public func bytes(): Array<Byte>
    
    /**
    * Read the data to the buffer.
    *
    * @params buffer - Will read the data to the buffer.
    *
    * @returns Length of read data.
    * @throws IllegalArgumentException - If the buffer is empty.
    */
    @!APILevel[12, atomicservice : true]
    public func read(buffer: Array<Byte>): Int64
    
    /**
    * Read a byte from the buffer.
    *
    *
    * @returns read byte.
    * @throws IllegalArgumentException - If the buffer is empty.
    */
    @!APILevel[12, atomicservice : true]
    public func readByte(): ?Byte
    
    /**
    * Write data from the `buffer` to the ByteBuffer.
    *
    * @params buffer - The data buffer.
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func write(buffer: Array<Byte>): Unit
    
    /**
    * Write a byte to the ByteBuffer.
    *
    * @params v - The byte to write.
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func writeByte(v: Byte): Unit
    
    @!APILevel[12, atomicservice : true]
    public func setLength(length: Int64): Unit
    
    /**
    * Increases the capacity to ensure that it can hold at least the number of elements specified by the
    * `additional` argument.
    *
    * @params additional - The desired additional capacity.
    *
    * @throws IllegalArgumentException - If `additional` is less than 0.
    */
    @!APILevel[12, atomicservice : true]
    public func reserve(additional: Int64): Unit
    
    /**
    * Seek to an offset, in bytes, in a stream.
    * The specified location cannot be before the data header in the stream.
    * The specified location can be beyond the end of the file.
    *
    * @params sp - Start position of the offset and size of the offset.
    *
    * @return the number of bytes in the stream from the beginning of the data to the cursor position.
    *
    * @throws IOException - The specified position is before the data header in the stream.
    */
    @!APILevel[12, atomicservice : true]
    public func seek(sp: SeekPosition): Int64
}

/**
* A ChainedInputStream obtains a list of InputSteam.
* ChainedInputStream is meant for reading InputSteams one by one.
*/
@!APILevel[12, atomicservice : true]
public class ChainedInputStream<T> <: InputStream where T <: InputStream {
    /**
    * Constructor
    *
    * @throws IllegalArgumentException -If 'input' is empty.
    */
    @!APILevel[12, atomicservice : true]
    public init(input: Array<T>)
    
    /**
    * Read the current InputSteam into the buffer.
    *
    * @return Size read into the buffer.
    * @throws IllegalArgumentException -If the buffer is empty.
    * @throws IOException -If failed to read InputSteam.
    */
    @!APILevel[12, atomicservice : true]
    public func read(buffer: Array<Byte>): Int64
}

/**
* The general class of exceptions produced by format conversion errors.
*/
@!APILevel[12, atomicservice : true]
public class ContentFormatException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

/**
* The general class of exceptions produced by failed or interrupted I/O operations.
*/
@!APILevel[12, atomicservice : true]
public open class IOException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

/**
* SeekPosition  is the position of the cursor in the file.
*/
@!APILevel[12, atomicservice : true]
public enum SeekPosition  {
    @!APILevel[12, atomicservice : true]
    Current(Int64) |
    @!APILevel[12, atomicservice : true]
    Begin(Int64) |
    @!APILevel[12, atomicservice : true]
    End(Int64)
}

@!APILevel[12, atomicservice : true]
public func readString<T>(from: T): String where T <: InputStream & Seekable

@!APILevel[12, atomicservice : true]
public unsafe func readStringUnchecked<T>(from: T): String where T <: InputStream & Seekable

@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func readToEnd<T>(from: T): Array<Byte> where T <: InputStream & Seekable

@!APILevel[12, atomicservice : true]
public func copy(from: InputStream, to!: OutputStream): Int64

@!APILevel[12, atomicservice : true]
public class MultiOutputStream<T> <: OutputStream where T <: OutputStream {
    /**
    * @throws IllegalArgumentException if output is empty
    */
    @!APILevel[12, atomicservice : true]
    public init(output: Array<T>)
    
    @!APILevel[12, atomicservice : true]
    public func write(buffer: Array<Byte>): Unit
    
    @!APILevel[12, atomicservice : true]
    public func flush(): Unit
}

/**
* Represents an input stream of bytes from some source and provides a way to read and consume them.
*
* A stream could be created for a file, a network socket or a bytes blob in memory.
* It could also generate bytes on demand, for example a random bytes stream.
*
* Some streams read couldn't be undone: once you read bytes, you can get them back.
*/
@!APILevel[12, atomicservice : true]
public interface InputStream {
}

/**
* This interface is used to construct an output stream.
*/
@!APILevel[12, atomicservice : true]
public interface OutputStream {
}

/**
* Represents a duplex stream that is both InputStream and OutputStream.
*/
@!APILevel[12, atomicservice : true]
public interface IOStream <: InputStream & OutputStream {
}

/**
* This interface provides a cursor that can be moved through the stream.
*/
@!APILevel[12, atomicservice : true]
public interface Seekable {
}

/**
* This class Provides the ability to read data from an input stream and convert it to characters or strings.
*/
@!APILevel[12, atomicservice : true]
public class StringReader<T> where T <: InputStream {
    @!APILevel[12, atomicservice : true]
    public init(input: T)
    
    /**
    * @throws ContentFormatException if the format of the read data is incorrect.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func read(): ?Rune
    
    @!APILevel[12, atomicservice : true]
    public func runes(): Iterator<Rune>
    
    /**
    * @throws ContentFormatException if the format of the read data is incorrect.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func readln(): Option<String>
    
    @!APILevel[12, atomicservice : true]
    public func lines(): Iterator<String>
    
    /**
    * @throws ContentFormatException if the format of the read data is incorrect.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func readToEnd(): String
    
    /**
    * @throws ContentFormatException if the format of the read data is incorrect.
    */
    @!APILevel[12, atomicservice : true]
    public func readUntil(v: Rune): Option<String>
    
    /**
    * @throws ContentFormatException if the format of the read data is incorrect.
    */
    @!APILevel[12, atomicservice : true]
    public func readUntil(predicate: (Rune) -> Bool): Option<String>
}

@!APILevel[12, atomicservice : true]
extend<T> StringReader<T> <: Resource where T <: Resource {
    /**
    * Close the current stream.
    */
    @!APILevel[12, atomicservice : true]
    public func close(): Unit
    
    /**
    * Returns whether the current flow is closed.
    *
    * @return true if the current stream has been closed, otherwise returns false.
    */
    @!APILevel[12, atomicservice : true]
    public func isClosed(): Bool
}

@!APILevel[12, atomicservice : true]
extend<T> StringReader<T> <: Seekable where T <: Seekable {
    /**
    * Seek to an offset, in bytes, in a stream.
    *
    * @params sp - Start position of the offset and size of the offset.
    *
    * @return the number of bytes in the stream from the beginning of the data to the cursor position.
    */
    @!APILevel[12, atomicservice : true]
    public func seek(sp: SeekPosition): Int64
    
    /**
    * Gets the current read position of StringReader .
    * 
    * This property calls the `seek()` function each time it is accessed, moving the cursor to a specific location.
    * The position is adjusted by setting the offset to the negative value of the current available bytes (`inputBIS.availLen` which not be read yet),
    * ensuring that read operations continue from the appropriate position. 
    * 
    * Note: This property only performs operations when accessed, adjusting the cursor and returning the current position.
    * 
    * @return The current read position of the input stream (of type `Int64`), representing the offset of the cursor
    *         from the beginning of the stream.
    */
    @!APILevel[12, atomicservice : true]
    public prop position: Int64
}

/**
* This class provides the ability to convert some types to strings with specified string encoding format
*  and endian configuration and write them to the output stream.
*/
@!APILevel[12, atomicservice : true]
public class StringWriter<T> where T <: OutputStream {
    /**
    * @throws IllegalArgumentException if encoding is UTF16 or UTF32
    */
    @!APILevel[12, atomicservice : true]
    public init(output: T)
    
    @!APILevel[12, atomicservice : true]
    public func flush(): Unit
    
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
    public func writeln(): Unit
    
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
extend<T> StringWriter<T> <: Resource where T <: Resource {
    /**
    * Close the current stream.
    */
    @!APILevel[12, atomicservice : true]
    public func close(): Unit
    
    /**
    * Returns whether the current flow is closed.
    *
    * @return true if the current stream has been closed, otherwise returns false.
    */
    @!APILevel[12, atomicservice : true]
    public func isClosed(): Bool
}

@!APILevel[12, atomicservice : true]
extend<T> StringWriter<T> <: Seekable where T <: Seekable {
    /**
    * Seek to an offset, in bytes, in a stream.
    *
    * @params sp - Start position of the offset and size of the offset.
    *
    * @return the number of bytes in the stream from the beginning of the data to the cursor position.
    */
    @!APILevel[12, atomicservice : true]
    public func seek(sp: SeekPosition): Int64
}

