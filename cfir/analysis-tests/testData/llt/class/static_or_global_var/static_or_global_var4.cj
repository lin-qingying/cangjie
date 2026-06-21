

import std.collection.HashMap

class A {
    static init() {
        C.fun()
    }
}

class C {
    static let MAP = HashMap<String, String>()
    public static func fun(): Int64 {
        <!USED_BEFORE_INITIALIZATION!>MAP<!>.size
    }
}

main(): Int64 {
    0
}
