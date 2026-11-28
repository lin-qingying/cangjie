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

// MODULE: main(lib)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.main

import sample.lib.*

// decompiled 库签名里的声明种类矩阵（interface/class/struct/enum/typealias，含嵌套与泛型别名）
// 以及函数类型、Option、tuple、VArray 等类型形态都必须被 stub 链路正确重建。
func useAll(color: Color): Int64 {
    let shape: Shape = shapeOf()
    let shapeViaAlias: ShapeAlias = shapeOf()
    let point: Point = pointOf()
    let bytes = bytesOf("hi")
    let sized = bytes.size
    let pair: (Int64, Int64) = pairOf(1)
    let callback: (Int64) -> Int64 = { value => value }
    let maybe: Option<Int64> = Some(1)
    let fixed: VArray<Int64, $3> = [1, 2, 3]
    if (let Some(value) <- maybe) {
        return shape.area() + shapeViaAlias.area() + point.x + sized + pair[0] + fixed[0] + callback(value)
    }
    return 0
}

func colorName(color: Color): String {
    match (color) {
        case Color.Red => return "red"
        case Color.Green => return "green"
    }
}
