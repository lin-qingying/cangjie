public interface fatherInterface<T> where T <: UInt32 {
    static func foo1(): UInt32
    static func foo2<K>(a: K): UInt32 {
        foo1()
    }
}

public interface MyInterface<T> <: fatherInterface<T> where T <: UInt32 {
    static func foo3(): UInt32 {
        foo1() + foo1()
    }
}

public enum enum1<T> <: MyInterface<UInt32> {
    A(UInt32)

    public static func foo1(): UInt32 {
        1
    }
}

enum enum2<T> <: MyInterface<UInt32> {
    A(UInt32)

    public static func foo1(): UInt32 {
        2
    }
}

main() {
    var en1: enum1<MyInterface<UInt32>> = enum1<MyInterface<UInt32>>.A(
        enum1<MyInterface<UInt32>>.foo2<UInt32>(enum1<MyInterface<UInt32>>.foo1() + enum1<MyInterface<UInt16>>.foo1()))
}
