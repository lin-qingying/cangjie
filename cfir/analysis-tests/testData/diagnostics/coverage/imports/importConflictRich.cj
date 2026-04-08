// RUN_PIPELINE_TILL: FRONTEND
import <!IMPORT_CONFLICT!><!UNRESOLVED_IMPORT!>alpha<!>.one.Duplicate<!>
import <!UNRESOLVED_IMPORT!>beta<!>.two.Duplicate
import <!UNRESOLVED_IMPORT!>gamma<!>.three.Other

func importConflictSuite(): Unit {
}

