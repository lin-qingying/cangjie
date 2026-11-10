// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

// 对齐 std.core 的 `Array<T> <: Collection<T>` 机制：
// `size` 声明在接口上，由 `extend` 给 struct 加上超类型并实现该成员。
public interface Sized {
    prop size: Int64
}

public struct Box {
    public var value: Int64 = 0
}

extend Box <: Sized {
    public prop size: Int64 {
        get() {
            return value
        }
    }
}

public func boxOf(): Box {
    return Box()
}

