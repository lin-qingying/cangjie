// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 e8-outer-constraints：外层 T -> U 与方法 V -> W 必须共同参与约束替换。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package e8_outer_constraints

public interface Rel<T> {}
public interface Extra<T> {}

public common class Box<T> {
    public common init() {}
    public common func apply<V>(value: V): Unit where V <: Rel<T>
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package e8_outer_constraints

public specific class Box<U> {
    public specific func apply<W>(value: W): Unit where W <: Rel<U> {}
}
