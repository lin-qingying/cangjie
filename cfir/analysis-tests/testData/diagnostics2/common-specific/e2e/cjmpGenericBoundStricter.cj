// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7c.txt）
// specific 泛型约束严于 common：GENERIC_CONSTRAINT_NOT_LOOSER（锚约束条目——与仓库 override 路径同一官方函数 CheckGenericTypeBoundsMapped 的既有锚点约定；官方起点 specific.cj:3:40 为 where）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public interface I {}
public common func g<T>(x: T): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific func g<T>(x: T): Int64 where <!GENERIC_CONSTRAINT_NOT_LOOSER!>T <: I<!> { 1 }
