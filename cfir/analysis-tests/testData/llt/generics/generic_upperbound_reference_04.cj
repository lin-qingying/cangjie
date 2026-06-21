

// DIAGNOSTICS: -UNUSED_VARIABLE

interface P<T> {
    static prop p: <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>
}

class A <: P<A> {
    public static prop p: A {
        get() {
            return A()
        }
    }
}

open class B<T1> where T1 <: P<T1> {
    prop test: T1 {
        get() {
            return T1.p
        }
    }
}

main(): Int64 {
    let x = B<A>().test
    if (x is A) {
        return 0
    }
    return 1
}
