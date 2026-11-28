// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// SDK 1.1.3：20261113-cjmp-body-matrix-test-writer/cjc-results.json。
// 空 property body 无 getter；空 getter body 仍是已有 getter，实例属性只报显式 abstract 带体。
// STATIC 不因 getter 存在而豁免缺体，因此静态 getter 版本同时有两条不同诊断。
// FILE: property-empty-body.cj
package body_matrix
public common abstract class EmptyOwner {
    public common init()
    public static abstract <!MISSING_FUNC_BODY!>prop value: Int64<!> {}
}

// FILE: getter-empty-body.cj
package body_matrix
public common abstract class GetterOwner {
    public common init()
    <!EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY!>public static abstract <!MISSING_FUNC_BODY!>prop value: Unit<!> {
        get() {}
    }<!>
}

// FILE: getter-instance-empty-body.cj
package body_matrix
public common abstract class InstanceGetterOwner {
    public common init()
    <!EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY!>public abstract prop value: Unit {
        get() {}
    }<!>
}
