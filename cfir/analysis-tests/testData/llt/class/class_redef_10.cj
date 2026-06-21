

interface I1 {
    static func foo(): Int64
    func f1(): Int64
}

open class C1 <: I1 {
    public static redef func foo(): Int64 {
        return 3
    }
    public override func f1(): Int64 {
        return 4
    }
}

main(): Int64 {
    return C1.foo() + C1().f1() - 7
}
