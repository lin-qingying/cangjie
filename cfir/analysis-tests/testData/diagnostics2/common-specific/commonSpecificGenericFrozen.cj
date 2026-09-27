// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C10）：泛型 common/specific 声明
// 不能 @Frozen，报 `common/specific declaration g with generics cannot be @Frozen`（锚 @Frozen 注解，官方 ParseCJMPDecl）。
package cjmp_p

<!COMMON_GENERIC_FROZEN_NOT_SUPPORTED!>@Frozen<!>
public common func g<T>(x: T): Int64
