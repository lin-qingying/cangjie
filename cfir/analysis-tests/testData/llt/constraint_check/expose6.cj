


interface I0 {}

interface I1 <: I0 {}

interface I2 <: I0 {}

open class C1 <: I1 & I2 {
    func h(a: I1): Unit {}
}

class C2 <: C1 & I2 {
    func h(a: I2): Unit {}
}

func f<T>(a: T): Unit where T <: C1 & C2 {
    var lam1 = <!AMBIGUOUS_USE!>a.h<!>
    var lam2: (I0) -> Unit = <!NO_MATCH_FUNCTION_DECLARATION_FOR_REF!>a<!>.h
}

main() {}
