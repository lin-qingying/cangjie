// RUN_PIPELINE_TILL: FRONTEND
// FILE: alpha.cj
package alpha.one

public class Left {}

// FILE: beta.cj
package beta.two

public class Right {}

// FILE: gamma.cj
package gamma.three

public class Third {}

// FILE: main.cj
import alpha.one.Left as Clash
import beta.two.Right as <!IMPORT_ALIAS_CONFLICT!>Clash<!>
import gamma.three.Third as Another

func importAliasConflictSuite(): Unit {
    let _ = Another()
}
