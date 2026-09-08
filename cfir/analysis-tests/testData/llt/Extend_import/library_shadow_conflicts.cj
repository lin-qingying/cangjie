// IMPORT_PATH: ../../libraries/extend_conflicts/shadow
import A.Foo
import <!EXTEND_MEMBER_CANNOT_SHADOW!>B.I1<!>
import <!EXTEND_MEMBER_CANNOT_SHADOW!>C.I2<!>

main() {
    let a = Foo()
    a.<!AMBIGUOUS_FUNCTION_CALL!>f<!>()
    return 0
}
