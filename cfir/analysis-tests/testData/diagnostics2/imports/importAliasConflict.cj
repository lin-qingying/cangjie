// RUN_PIPELINE_TILL: FRONTEND
import <!IMPORT_ALIAS_CONFLICT, IMPORT_CONFLICT!><!UNRESOLVED_IMPORT!>alpha<!>.one.Left as Clash<!>
import <!UNRESOLVED_IMPORT!>beta<!>.two.Right as Clash
import <!UNRESOLVED_IMPORT!>gamma<!>.three.Third as Another

func importAliasConflictSuite(): Unit {
}
