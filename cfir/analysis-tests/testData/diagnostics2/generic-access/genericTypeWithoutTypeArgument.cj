// RUN_PIPELINE_TILL: FRONTEND
class Box<T> {
    static func create(): Int64 {
        return 0
    }
}

func takeBox(v: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Box<!>): Unit {
    let _ = v
}

func useTypePosition(): Unit {
    let x: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Box<!>
    let _ = x
}

func useQualifier(): Int64 {
    return <!UNABLE_TO_INFER_GENERIC_FUNC!>Box.create()<!>
}
