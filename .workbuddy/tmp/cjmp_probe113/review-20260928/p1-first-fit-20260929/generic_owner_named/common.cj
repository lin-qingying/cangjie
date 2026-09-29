package firstfit_generic_owner_named

public common class Box<T> {
    public common init() {}
}
common extend<T> Box<T> {
    public common func pick(candidate!: T): T { candidate }
}
common extend<T> Box<T> {
    public common func pick(selected!: T): T { selected }
}

