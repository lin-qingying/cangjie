package firstfit_generic_owner_two

public specific class Box<T> {}
specific extend<T> Box<T> {
    public specific func pick(v: T): T { v }
    public specific func pick(w: T): T { w }
}

