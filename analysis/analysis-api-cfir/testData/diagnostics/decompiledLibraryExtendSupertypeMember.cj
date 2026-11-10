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

// MODULE: main(lib)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.main

import sample.lib.*

// 库签名返回 Box；`size` 只能经 `extend Box <: Sized` 得到。
// 若 STUBS 丢失 extend 提供的超类型/成员，这里会报 NOT_MEMBER_OF。
func takeSize(): Int64 {
    let box = boxOf()
    return box.size
}
