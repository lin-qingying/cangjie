// FILE: ifBranchWithoutElse.cj
package sample.expectedtype

class User {
}

func discard(flag: Bool) {
    if (flag) {
        <expr>User()</expr>
    }
}
