

open class A<T> {}

extend<T> A<T> {
    public prop f: String {
        get() {
            return "C"
        }
    }
}

class B<T> <: A<T> {
    public func g() {
        f
    }
}

main() {
    let x = B<String>()
    if (x.f == "C") {
        print("${x.f}")
        return 0
    }
    return 1
}
