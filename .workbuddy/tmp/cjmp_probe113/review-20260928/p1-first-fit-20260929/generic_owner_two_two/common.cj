package firstfit_generic_owner_two

public common class Box<T> {
    public common init() {}
}
common extend<T> Box<T> {
    public common func pick(v: T): T { v }
}
common extend<T> Box<T> {
    public common func pick(v: T): T { v }
}

