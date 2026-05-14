// RUN_PIPELINE_TILL: FRONTEND

// FILE: main.cj
import <!UNRESOLVED_IMPORT!>macros<!>.missing.MakeInt64

func useMissingMacroArtifact(): Int64 {
    let value: Int64 = <!MACRO_DEPENDENCY_COMPILE_FAILED!>@MakeInt64()<!>
    return value
}
