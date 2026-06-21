// FILE: pkg1/a.cj


package pkg1

public interface fatherInterface<T> {
    static func foo1(): UInt32
    static func foo2<K>(a: K): UInt32 {
        foo1()
    }
}

public interface MyInterface<T> <: fatherInterface<T> {
    static func foo3(): UInt32 {
        foo1() + foo1()
    }
}

public enum enum1 <: MyInterface<UInt32> {
    A(UInt32)

    public static func foo1(): UInt32 {
        1
    }
}

public enum enum2 <: MyInterface<UInt32> {
    A(UInt32)

    public static func foo1(): UInt32 {
        2
    }
}

public enum enum3 <: MyInterface<UInt32> {
    A(UInt32)

    public static func foo1(): UInt32 {
        3
    }
}

// FILE: use_interface_def_imple_008.cj



internal import pkg1.*

main() {
    var en1: enum1 = enum1.A(enum1.foo2<UInt32>(enum1.foo1() + enum1.foo1()))
    var en2: enum1 = enum1.A(enum1.foo3())
    var en3: enum2 = enum2.A(enum2.foo2<UInt32>(enum2.foo1()))
    var en4: enum2 = enum2.A(enum2.foo3())
    var en5: enum3 = enum3.A(enum3.foo2<UInt32>(enum3.foo1()))
    var en6: enum3 = enum3.A(enum3.foo3())

    var e1 = match (en1) {
        case A(x) => x
    }
    var e2 = match (en2) {
        case A(x) => x
    }
    var e3 = match (en3) {
        case A(x) => x
    }
    var e4 = match (en4) {
        case A(x) => x
    }
    var e5 = match (en5) {
        case A(x) => x
    }
    var e6 = match (en6) {
        case A(x) => x
    }

    if (e1 == 1 && e2 == 2 && e3 == 2 && e4 == 4 && e5 == 3 && e6 == 6) {
        return 0
    } else {
        return 1
    }
}
