// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C02、T02、T03）：主构造器不能带
// common/specific（`unexpected modifier 'common' on primary constructor`，锚修饰符）。因此
// `sema_specific_init_common_primary_constructor` / `sema_specific_primary_unmatched_var_decl` 在 1.1.3 无可达触发点。
package cjmp_p

public common class P {
    public <!WRONG_MODIFIER_TARGET!>common<!> P(x: Int64) {}
}
