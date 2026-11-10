// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

// 库声明的 extend：`size` 只声明在接口上，由 extend 给 struct 加超类型并提供实现。
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

func takeSize(): Int64 {
    let box = boxOf()
    return box.size
}
