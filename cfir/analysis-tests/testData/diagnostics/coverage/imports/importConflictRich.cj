// RUN_PIPELINE_TILL: FRONTEND
// FILE: alpha.cj
package alpha.one

public class Duplicate {}

// FILE: beta.cj
package beta.two

public class Duplicate {}

// FILE: gamma.cj
package gamma.three

public class Other {}

// FILE: main.cj
import alpha.one.Duplicate
import beta.two.<!IMPORT_CONFLICT!>Duplicate<!>
import gamma.three.Other

func importConflictSuite(): Unit {
    let _ = Other()
}
