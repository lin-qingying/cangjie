package firstfit_generic_bounds

public interface A {}
public interface B {}
common extend Int64 {
    public common func pick<T>(v: T): Unit where T <: A {}
}
common extend Int64 {
    public common func pick<T>(v: T): Unit where T <: B {}
}

