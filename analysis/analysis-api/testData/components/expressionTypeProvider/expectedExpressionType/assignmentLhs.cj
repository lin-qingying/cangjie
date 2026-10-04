// FILE: assignmentLhs.cj
package sample.expectedtype

class User {
}

func store(): User {
    var slot: User = User()
    <expr>slot</expr> = User()
    return slot
}
