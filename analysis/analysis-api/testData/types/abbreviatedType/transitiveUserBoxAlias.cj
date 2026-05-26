// FILE: transitiveUserBoxAlias.cj
// TARGET_FUNCTION: makeAliasBox
package sample.types.abbreviated

class User {
}

class Box<T> {
}

type UserBox = Box<User>
type RenamedUserBox = UserBox

func makeAliasBox(): RenamedUserBox {
    return Box<User>()
}
