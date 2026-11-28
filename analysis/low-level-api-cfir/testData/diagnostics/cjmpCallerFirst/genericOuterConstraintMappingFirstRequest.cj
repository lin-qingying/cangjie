// LANGUAGE_VERSION: 1.1.3
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package e8_outer_constraints

public interface Rel<T> {}
public common class Box<T> {
    public common init() {}
    public common func apply<V>(value: V): Unit where V <: Rel<T>
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package e8_outer_constraints

public specific class Box<U> {
    public specific func apply<W>(value: W): Unit where W <: Rel<U> {}
}
