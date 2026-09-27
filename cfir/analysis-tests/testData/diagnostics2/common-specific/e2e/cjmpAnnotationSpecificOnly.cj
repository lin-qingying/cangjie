// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9_done/T06_DifferentAnnotationSpecificOnly
// 仅 specific 侧带 @OverflowWrapping：`'specific' function annotation is not match 'common' annotation`
//（锚 specific 注解，官方 MatchAnnotationsFromSource / ReportAnnotationMismatch）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common func f(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!SPECIFIC_HAS_DIFFERENT_ANNOTATION!>@OverflowWrapping<!>
public specific func f(): Int64 {
    1
}
