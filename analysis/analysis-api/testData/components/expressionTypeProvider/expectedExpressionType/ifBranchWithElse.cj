// FILE: ifBranchWithElse.cj
package sample.expectedtype

class User {
}

func pick(flag: Bool): User {
    let chosen: User = if (flag) {
        <expr>User()</expr>
    } else {
        User()
    }
    return chosen
}
