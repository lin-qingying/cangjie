

public interface faterInterface<T> {
    static func foo1(): T
    static func foo2(a: Int16): Int16 {
        a
    }
}

struct enum1<T> <: MyInterface {
    public static func A(a: Int16): enum1<T> {
        enum1<T>()
    }
    public static func foo1(): Int16 {
        1
    }
}

interface MyInterface <: faterInterface<Int16> {}

main() {
    var en1: enum1<faterInterface<Int16>> = enum1<faterInterface<Int16>>.A(
        enum1<faterInterface<Int16>>.foo2(enum1<faterInterface<Int16>>.foo1()))
    0
}
