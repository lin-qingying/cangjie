// FILE: nestedGenericReturnType.cj
// TARGET_FUNCTION: wrapTwice
// TARGET_CLASS: User
package sample.types.substitution

class User {
}

class Box<T> {
}

class Envelope<T> {
}

func wrapTwice<T>(value: T): Envelope<Box<T>> {
    value
    return Envelope<Box<T>>()
}
