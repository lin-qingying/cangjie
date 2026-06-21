

extend Int32 {
    func b() {}
}

extend Int8 {
    func b() {}
}

var a = 1.<!AMBIGUOUS_MATCH_PRIMITIVE_EXTEND!>b<!>()
