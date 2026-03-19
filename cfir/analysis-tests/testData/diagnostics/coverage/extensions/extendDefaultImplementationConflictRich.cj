// RUN_PIPELINE_TILL: FRONTEND
interface DefaultOps<T> {
    func stableId(): Int64 {
        return 1
    }

    func echo(v: T): T
}

class Host<T> {}

extend Host<Int64> <: <!EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT, EXTEND_SPECIALIZATION_CONFLICT!>DefaultOps<Int64><!> {}
extend<U> Host<U> <: <!EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT, EXTEND_SPECIALIZATION_CONFLICT!>DefaultOps<U><!> {}

