


public interface faterInterface<T> {
    static func foo1(): T
    static func foo2<K>(a: K): K {
        foo1();
        a
    }
}

interface MyInterface <: faterInterface<Float16> {
    static func foo3(): Float16 {
        foo2<Float16>(5.0)
    }
}

extend UIntNative <: MyInterface {
    public static func foo1(): Float16 {
        0.0
    }
}

main() {
    UIntNative.foo3()
    UIntNative.foo2<Float16>(1.0)
    0
}
