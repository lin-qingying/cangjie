// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 e8-outer-constraints/stricter.cj：映射后的 Rel<U> 等价，额外 Extra<U> 使约束更严格。
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
    public specific func apply<W>(value: W): Unit where <!GENERIC_CONSTRAINT_NOT_LOOSER!>W <: Rel<U> & Extra<U><!> {}
}
