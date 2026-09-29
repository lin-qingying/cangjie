package firstfit_generic_owner_one

public specific class Box<T> {}
specific extend<T> Box<T> {
    public specific func pick(v: T): T { v }
}

