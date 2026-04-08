// RUN_PIPELINE_TILL: FRONTEND
class Box<T> {
    static func create(): Int64 {
        return 0
    }
}

func takeBox(v: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Box<!>): Unit {}

func useTypePosition(): Unit {
    let x: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Box<!>
}

func useQualifier(): Int64 {
    return <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Box<!>.create()
}
