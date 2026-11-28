// RUN_PIPELINE_TILL: FRONTEND
open class A {}

func useBinding(a: A): Int64 {
    return match (a) {
        case x => 1
    }
}
