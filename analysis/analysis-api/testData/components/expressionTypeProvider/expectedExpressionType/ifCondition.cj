// FILE: ifCondition.cj
package sample.expectedtype

class User {
}

func check(): Int64 {
    if (<expr>1 < 2</expr>) {
        return 1
    }
    return 0
}
