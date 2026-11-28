package ambiguous_classifier_type_use

// 同名 classifier 重声明后，类型使用位置会产生候选级歧义。
// 该诊断在 analysis API low-level 路径必须保留 PSI 锚点：如果 checker 把锚点降级为
// 仅含 offsets 的源元素，LLCfirDiagnosticReporter 无法把它投影成 PSI 诊断，会抛出
// `error("Unknown diagnostic type CjOffsetsOnlyDiagnosticWithParameters1")` 并中断
// 该 CFIR 元素的整段收集。
open class A<X, Y> {}

open class B<X, Y> {}

open class A<X, Y> {}

open class B<X, Y> {}

// `B<Y, X>` 是歧义的 classifier 类型使用：官方 `sema_ambiguous_use` 由 `DiagAmbiguousUse`
// 以整节点锚定，因此高亮必须覆盖整条类型引用（含类型实参），而不是只标最后一段名称。
// 这里只声明、不调用，以隔离该类型使用歧义（避免牵入调用 no-match 这条独立的锚点路径）。
func foo<X, Y>(a: B<Y, X>): Int64 {
    0
}

main() {}
