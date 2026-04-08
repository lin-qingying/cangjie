// RUN_PIPELINE_TILL: FRONTEND
class BoundA {}
class BoundB {}

class Host<T> {}

interface DefaultOps<T> {
    func stableId(): Int64 {
        return 1
    }
}

extend<T> Host<T> <: <!EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT, EXTEND_SPECIALIZATION_CONFLICT!>DefaultOps<T><!> where T <: BoundA {}
extend<T> Host<T> <: <!EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT, EXTEND_SPECIALIZATION_CONFLICT!>DefaultOps<T><!> where T <: BoundB {}
