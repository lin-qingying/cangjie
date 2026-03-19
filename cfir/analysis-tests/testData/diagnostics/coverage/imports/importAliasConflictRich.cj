// RUN_PIPELINE_TILL: FRONTEND
import <!IMPORT_ALIAS_CONFLICT, IMPORT_CONFLICT, IMPORT_TARGET_NOT_FOUND!>alpha.one.Left as Clash<!>
import <!IMPORT_TARGET_NOT_FOUND!>beta.two.Right as Clash<!>
import <!IMPORT_TARGET_NOT_FOUND!>gamma.three.Third as Another<!>

func importAliasConflictSuite(): Unit {
}

