// IMPORT_PATH: ../../libraries/extend_conflicts/nominal
import base.Foo
import <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED, UNUSED_IMPORT!>a.IA<!>
import <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED, UNUSED_IMPORT!>b.IB<!>

main() {
    Foo()
    return 0
}
