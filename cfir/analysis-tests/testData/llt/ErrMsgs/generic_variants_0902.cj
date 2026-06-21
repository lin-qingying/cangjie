

package pkg

main(): Int64 {
    if (A<String>().b != 0) {
        return 1
    }
    return 0
}

class A<T> {
    public prop a: Int64 {
        get() {
            return 1
        }
    }
    public var b = match (Data.<!NOT_MEMBER_OF!>U<!>) {
        case _: T => 2 // class T{}
        case x: Data => 0
        case _ => 5
    }
}

class T {}

enum Data {
    T | V
}
