// RUN_PIPELINE_TILL: FRONTEND
class BoundA {}
class BoundB {}

class Host<T> {}

interface DefaultOps<T> {
}

extend<T> Host<T> <: DefaultOps<T> where T <: BoundA {}
extend<T> Host<T> <: <!EXTEND_DUPLICATE_INTERFACE!>DefaultOps<T><!> where T <: BoundB {}
