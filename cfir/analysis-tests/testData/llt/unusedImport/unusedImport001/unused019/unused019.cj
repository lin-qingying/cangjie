// FILE: a.cj


package org1::a

public class A {}

// FILE: unused019.cj



package d

import <!UNUSED_IMPORT!>org1::a.A<!>

main() {}
