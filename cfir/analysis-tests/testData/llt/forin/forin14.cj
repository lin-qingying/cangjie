


class NativePointerResource<T> <: Resource {
    public var v: Array<T>

    prop value: T {
        get() {
            v[0]
        }
    }

    init(x: T) {
        v = [x]
    }

    public override func isClosed() {
        true
    }
    public override func close() {}
}

main() {
    try (v = NativePointerResource<UIntNative>(0)) {
        if (v.value > 0) {}
    }

    let v = NativePointerResource<Int>(0)

    for (i in 1..10) {
        if (v.value > 0) {}
    }

    println("hello ${if (v.value > 0) {}} world!")
}
