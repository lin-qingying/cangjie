

public class C<T> {}

public open class A {
    public static func foo<T>(a: C<T>) {
        return 1
    }
}

public open class B <: A {
    public static redef func foo<T>(a: C<T>) {
        return 1
    }
}

main() {
    return 0
}
