// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: function_with_common
// CJMP_MODE: COMMON
// FILE: function-with-common.cj
package body_matrix_default
public common abstract class DefaultOwner {
    public common init()
    <!EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY!>public common static abstract func fn(): Unit {}<!>
}
// MODULE: function_without_common
// CJMP_MODE: COMMON
// FILE: function-without-common.cj
package body_matrix_default
public common abstract class DefaultOwner {
    public common init()
    <!EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY!>public static abstract <!MISSING_FUNC_BODY!>func fn(): Unit<!> {}<!>
}
// MODULE: property_with_common
// CJMP_MODE: COMMON
// FILE: property-with-common.cj
package body_matrix
public common abstract class GetterOwner {
    public common init()
    <!EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY!>public common static abstract prop value: Unit {
        get() {}
    }<!>
}
// MODULE: property_without_common
// CJMP_MODE: COMMON
// FILE: property-without-common.cj
package body_matrix
public common abstract class GetterOwner {
    public common init()
    <!EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY!>public static abstract <!MISSING_FUNC_BODY!>prop value: Unit<!> {
        get() {}
    }<!>
}
