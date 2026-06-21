// FILE: pkga.cj


package pkg.a

public interface ValueType {
    prop code: String {
        get() {
            "in pkg.a"
        }
    }

    func fCode(): String {
        return "in pkg.a fCode"
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend Int64 <: ValueType {}<!>

// FILE: pkgb.cj


package pkg.b

public interface ValueType {
    prop code: String {
        get() {
            "in pkg.b"
        }
    }

    func fCode(): String {
        return "in pkg.b fCode"
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend Int64 <: ValueType {}<!>

// FILE: test.cj



package pkg

import pkg.a.<!UNUSED_IMPORT!>ValueType as AValueType<!>
import pkg.b.<!UNUSED_IMPORT!>ValueType as BValueType<!>

main() {}
