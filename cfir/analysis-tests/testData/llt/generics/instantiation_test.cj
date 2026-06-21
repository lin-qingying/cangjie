

interface a<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>b<!>, c> where b <: Int8, c <: d<UInt8, Range<UInt16>> {
    static func e<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>b<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>c<!>>(f!: Float64) where b <: UInt64, c <: Int64 {
        {=>} is d<UInt8, Range<UInt16>>
    }
}

interface d<b, g> {}

interface h<b, c> <: a<Int8, d<UInt8, Range<UInt16>>> {}

class i <: h<UInt16, UInt16> {
    static var m = i()
    var j: Int32 = 4
    static func k<b>(): Int32 {
        if (e(f: 52.0)) {
            1
        } else {
            m.j
        }
    }
}

func l() {
    i.k<Range<Int64>>
}

main() {
    l()
    0
}
