// FILE: userBoxAlias.cj
// TARGET_FUNCTION: makeBox
package sample.types.abbreviated

class User {
}

class Box<T> {
}

type UserBox = Box<User>

func makeBox(): UserBox {
    return Box<User>()
}
