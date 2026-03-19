// RUN_PIPELINE_TILL: FRONTEND
interface C {}

func add<T>(a: T, b: T): T where T <: C {
    return a
}
