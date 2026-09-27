// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9_done/T07_DeprecatedOnSpecific
// specific 声明不能写 @Deprecated：`'Deprecated' annotation is not allowed on specific function 'f'`
//（锚注解，官方 PostCheckDeprecatedAnnotation）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common func f(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!SPECIFIC_HAS_DEPRECATED_ANNOTATION!>@Deprecated<!>
public specific func f(): Int64 {
    1
}
