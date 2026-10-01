// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：class 成员上写 `abstract prop`（抽象属性只能写在 interface；CJMP 的 common/specific 或 Native FFI 镜像类除外）。
// 官方 cjc 1.0.5 / 1.1.3：parse_illegal_modifier_in_scope "unexpected modifier 'abstract' on property declaration in class body"
//   （1.1.3 另给 parse_explicitly_abstract_only_for_cjmp_abstract_class）。
abstract class AbstractProps {
    public init() {
    }

    public abstract prop abstractProp: Int64
}
