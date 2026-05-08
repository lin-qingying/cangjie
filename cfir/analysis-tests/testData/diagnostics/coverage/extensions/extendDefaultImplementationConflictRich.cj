// RUN_PIPELINE_TILL: FRONTEND
interface DefaultOps<T> {
}

class Host<T> {}

extend Host<Int64> <: <!EXTEND_DUPLICATE_INTERFACE!>DefaultOps<Int64><!> {}
extend<U> Host<U> <: DefaultOps<U> {}
