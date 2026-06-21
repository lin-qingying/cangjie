

public class C<T> {}

public open class A<T> {
    public static func foo<T>(a: C<T>) {
        return 1
    }
}

public open class B<T> <: A<T> {
    public static redef func foo<T>(a: C<T>) {
        return 1
    }
}

main() {
    return 0
}
