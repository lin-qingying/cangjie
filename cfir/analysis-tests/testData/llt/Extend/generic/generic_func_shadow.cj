

class C {
    func f<T>(a: T) {}
}

extend C {
    func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!><V>(a: V) {}
}
