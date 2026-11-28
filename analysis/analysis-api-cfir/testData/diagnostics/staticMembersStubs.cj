// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

public enum Color {
    | Red
    | Green
}

public class Counter {
    public static var total: Int64 = 0

    public static prop tag: Int64 {
        get() {
            return 7
        }
    }

    public static func bump(): Int64 {
        return 1
    }
}

// MODULE: main(lib)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.main

import sample.lib.*

func isRed(c: Color): Bool {
    match (c) {
        case Red => true
        case Green => false
    }
}

func useCounter(): Int64 {
    let c = Color.Red
    let n = Counter.bump()
    Counter.total = Counter.total + n
    if (isRed(c)) {
        return Counter.total + Counter.tag
    }
    return Counter.tag
}
