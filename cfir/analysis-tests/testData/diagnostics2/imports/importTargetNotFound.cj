// RUN_PIPELINE_TILL: FRONTEND
import <!UNRESOLVED_IMPORT!>ghost.pkg<!>.MissingOne
import <!UNRESOLVED_IMPORT!>ghostv.pkg.deep<!>.MissingTwo
import <!UNRESOLVED_IMPORT!>std.void<!>.NeverThere

func importTargetNotFoundSuite(): Unit {
}
