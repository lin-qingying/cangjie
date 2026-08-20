// RUN_PIPELINE_TILL: FRONTEND
// FILE: hidden.cj
package visibilitytype

open class HiddenBase {}

// FILE: use.cj
package outsider
import <!UNUSED_IMPORT!>visibilitytype.*<!>

func consume(value: <!UNDECLARED_TYPE_NAME!>HiddenBase<!>) {}
