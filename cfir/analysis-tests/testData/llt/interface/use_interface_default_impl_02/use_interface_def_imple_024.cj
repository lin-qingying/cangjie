// FILE: pkg1/a.cj


package pkg1

public func foo() {
    1
}

public interface fatherInterface<T> {
    static func foo1(): T
    static func foo2<T>(a!: T): T {
        a
    }
}

public interface MyInterface<T> <: fatherInterface<T> {
    static func foo3(): T {
        foo1()
    }
}

// FILE: pkg2/b.cj


package pkg2

internal import pkg1.*

public class myClass<K> {
    public static var x: Int64 = 10
    static func f() {
        x = 20
        return foo
    }
}

extend<K> myClass<K> <: MyInterface<() -> Int64> {
    public static func foo1(): () -> Int64 {
        f()
    }
}

// FILE: pkg3/c.cj


package pkg3

internal import pkg1.*
internal import pkg2.*

public func call<T>(): Int64 {
    if (myClass<Int32>.foo2<() -> Int64>(a: myClass<Int32>.foo1())() == 1 && myClass<Int32>.foo3()() == 1 &&
        myClass<Int32>.x == 20) {
        return 0
    } else {
        return 1
    }
}

// FILE: use_interface_def_imple_024.cj



internal import pkg1.*
internal import pkg2.*
internal import pkg3.*

main() {
    call<MyInterface<() -> Int64>>()
}
