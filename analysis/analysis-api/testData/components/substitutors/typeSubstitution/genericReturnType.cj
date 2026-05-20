// FILE: genericReturnType.cj
// TARGET_FUNCTION: wrap
// TARGET_CLASS: User
package sample.types.substitution

class User {
}

class Box<T> {
}

func wrap<T>(value: T): Box<T> {
    value
    return Box<T>()
}
