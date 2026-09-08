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

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>extend<!> Int64 <: ValueType {}

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

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>extend<!> Int64 <: ValueType {}

// FILE: test.cj



package pkg

import <!UNUSED_IMPORT!>pkg.a.ValueType as AValueType<!>
import <!UNUSED_IMPORT!>pkg.b.ValueType as BValueType<!>

main() {}
