package firstfit_generic_owner_named

public specific class Box<T> {}
specific extend<T> Box<T> {
    public specific func pick(selected!: T): T { selected }
}

