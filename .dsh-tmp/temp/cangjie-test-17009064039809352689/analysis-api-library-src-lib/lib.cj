// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

public type MyByte = UInt8
public type ByteAlias = MyByte
public type Pair<T> = (T, T)
public type ShapeAlias = Shape

public interface Shape {
    func area(): Int64
}

public class Circle <: Shape {
    public func area(): Int64 {
        return 0
    }
}

public struct Point {
    public let x: Int64

    public init(x: Int64) {
        this.x = x
    }
}

public enum Color {
    Red | Green
}

public func shapeOf(): Shape {
    return Circle()
}

public func pointOf(): Point {
    return Point(1)
}

public func colorOf(): Color {
    return Color.Red
}

public func bytesOf(text: String): Array<ByteAlias> {
    return Array<ByteAlias>(text.size, repeat: 0u8)
}

public func pairOf(value: Int64): Pair<Int64> {
    return (value, value)
}

