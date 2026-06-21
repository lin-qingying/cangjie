


class RPtr {
    public var a = CPointer<Int64>()
    public var b: Array<Int64>

    public init(ptr: CPointer<Int64>) {
        a = CPointer<Int64>(ptr)

        b = Array<Int64>(10, {i: Int64 => 0})
        for (i in 0..10) {
            b[i] = unsafe { a.read(i) }
        }
    }

    public func update() {
        for (i in 0..10) {
            b[i] = unsafe { a.read(i) }
        }
    }
}

foreign func ArrayGen(): CPointer<Int64>

foreign func ArrayAdd(a: CPointer<Int64>): CPointer<Int64>

foreign func free(ptr: CPointer<Int64>): Unit

main(): Int64 {
    unsafe {
        var t = RPtr(ArrayGen())
        println(t.b)

        t.a = ArrayAdd(t.a)
        t.update()
        println(t.b)
        free(t.a)
    }
    return 0
}
