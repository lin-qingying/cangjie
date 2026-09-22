// 官方多包探针（codex-probes/gf_unused）证实：package a 存在且 import a.* 未使用时，
// cjc 1.0.5 报 `unused import 'a.*'`（warning），而非 UNRESOLVED_IMPORT。
// 本文件按 `<name>_dep.cj` 依赖文件约定为 globalfunc.cj 提供 package a。
package a

@Annotation
public class Key {
    public const init() {}
}
