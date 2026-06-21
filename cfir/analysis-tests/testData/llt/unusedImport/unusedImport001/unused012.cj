// FILE: unused012pkga.cj


package pkga

public func test(): Unit {}

// FILE: unused012.cj



import <!UNUSED_IMPORT!>pkga.test<!>
import pkga.test as testanew
main(): Int64 {
    testanew()
    return 0
}
