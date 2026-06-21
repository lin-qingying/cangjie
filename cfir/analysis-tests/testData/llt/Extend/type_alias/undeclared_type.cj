


type A<T, k> = structA<T, k>

type TI<T, K> = <!NOT_A_TYPE!>I<!><T, <!UNDECLARED_TYPE_NAME!>O<!>>

var d = r'#'

extend<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>K<!>> structA<T, K> <: <!EXTEND_NOT_INTERFACE!>TI<T, K><!> where T <: Rune, K <: String {
    mut static prop i: Rune {
        get() {
            d
        }
        set(v) {
            d = v
        }
    }
}

interface I<T, K> {
    mut static prop i: Rune {
        get() {
            d
        }
        set(v) {
            d = v
        }
    }
}

struct structA<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>K<!>> where T <: Rune, K <: String {
    var a: T
    var b: K
    structA(x: T, y: K) {
        this.a = x
        this.b = y
    }
}

main(): Int64 {
    func a(a: A<Rune, String>) {
        return a.a.toString() + a.b
    }

    A<Rune, String>.i = r'@'

    if (a(A<Rune, String>(A<Rune, String>.i, "345")) == "@345") {
        return 0
    } else {
        return 1
    }
}
