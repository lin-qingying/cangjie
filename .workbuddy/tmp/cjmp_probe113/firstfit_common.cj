package cjmp_firstfit

public common class FirstFitBox<T> {
    public common init()
}

common extend<T> FirstFitBox<T> {
    public common func select(candidate!: T): T { candidate }
}

common extend<T> FirstFitBox<T> {
    public common func select(selected!: T): T { selected }
}
