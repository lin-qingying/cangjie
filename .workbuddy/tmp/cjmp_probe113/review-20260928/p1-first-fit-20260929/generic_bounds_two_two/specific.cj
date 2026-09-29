package firstfit_generic_bounds

specific extend Int64 {
    public specific func pick<T>(v: T): Unit where T <: A {}
    public specific func pick<T>(v: T): Unit where T <: B {}
}

