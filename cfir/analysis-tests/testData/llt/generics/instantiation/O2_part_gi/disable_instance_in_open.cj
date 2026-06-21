


interface I {
    static func f6<T>(): Int64
    // Every member is accessible in instance functions
    func check_instance() {
        print(f6<Unit>())
    }
}

class A <: I {
    public static func f6<T>() {
        return 6
    }
}

main() {
    let tst = A()
    tst.check_instance()
}
