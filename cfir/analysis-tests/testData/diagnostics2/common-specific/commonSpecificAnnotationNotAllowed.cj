// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C11）：官方不支持集合内的注解
// 不能用于 common/specific 声明，报 `annotation EnsurePreparedToMock is not allowed on a common/specific declaration`
//（锚注解，官方 CheckNotAllowedAnnotations）。
package cjmp_p

<!COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED!>@EnsurePreparedToMock<!>
public common func h(): Int64
