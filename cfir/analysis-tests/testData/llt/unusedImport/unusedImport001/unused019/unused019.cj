// FILE: a.cj


package org1::a

public class A {}

// FILE: unused019.cj



package d

<!UNUSED_IMPORT!>import org1::a.A<!>

main() {}
