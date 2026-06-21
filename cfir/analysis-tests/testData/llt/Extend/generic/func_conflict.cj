

class A<T> {}

extend<T> A<T> {
    public static func <!CONFLICTING_OVERLOADS!>f<!>() {}
    public static func <!CONFLICTING_OVERLOADS!>f<!>() {}
}
