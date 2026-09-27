package cjmp_pointer_extend

public common interface Contains<T> {
    public common func contains(value: T): Bool { false }
}

common extend<T> CPointer<T> <: Contains<T> {}
