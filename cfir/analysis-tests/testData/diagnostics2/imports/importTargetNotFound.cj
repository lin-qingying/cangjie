// RUN_PIPELINE_TILL: FRONTEND
import <!UNRESOLVED_IMPORT!>ghost<!>.pkg.MissingOne
import <!UNRESOLVED_IMPORT!>ghostv<!>.pkg.deep.MissingTwo
import std.<!UNRESOLVED_IMPORT!>void<!>.NeverThere

func importTargetNotFoundSuite(): Unit {
}
