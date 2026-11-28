// FILE: assignmentRhs.cj
package sample.expectedtype

class User {
}

func store(): User {
    var slot: User = User()
    slot = <expr>User()</expr>
    return slot
}
