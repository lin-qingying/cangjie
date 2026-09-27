package cjmp_firstfit

public specific class FirstFitBox<T> {
    public specific init() {}
}

specific extend<T> FirstFitBox<T> {
    public specific func select(selected!: T): T { selected }
}
