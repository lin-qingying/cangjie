// RUN_PIPELINE_TILL: FRONTEND
struct S {
    func f(): Unit {
        <!STRUCT_SUPER_NOT_ALLOWED!>super<!>
    }
}

enum E {
    | A

    func f(): Unit {
        <!ENUM_SUPER_NOT_ALLOWED!>super<!>
    }
}
