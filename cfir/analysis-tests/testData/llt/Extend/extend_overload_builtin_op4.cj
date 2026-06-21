

/*
The compiler will add a built-in operator function to each extend decl below, where their signatures are the same.
When creating Box<Int64>, those two built-in operator functions will be added to the class body of Box<Int64>, resulting
in having two functions with the same signatures in the same scope.
 */

interface Eq<T> {
    operator func ==(right: T): Bool
}

interface Equal<T> {
    operator func ==(right: T): Bool
}

extend Int64 <: Eq<Int64> {}

extend Int64 <: Equal<Int64> {}

main(): Int64 {
    let a: Eq<Int64> = 1 // auto boxing
    if (a == 1) {
        return 0
    }
    return 1
}
