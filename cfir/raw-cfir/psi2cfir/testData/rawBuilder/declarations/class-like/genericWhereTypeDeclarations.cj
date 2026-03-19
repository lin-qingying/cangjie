// RUN_PIPELINE_TILL: FRONTEND
interface Constraint {}

class GenericClass<T> where T <: Constraint {
    var value: T = seed
}

interface GenericInterface<T> where T <: Constraint {
    var value: T
}

struct GenericStruct<T> where T <: Constraint {
    var value: T = seed
}

enum GenericEnum<T> where T <: Constraint {
    | Value(T)
}
