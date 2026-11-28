// RUN_PIPELINE_TILL: FRONTEND
open class A {}

class B <: A {}

func convert(a: A): Unit {
    let b = a as B
    let c = a as? B
    let _ = b
    let _ = c
}
