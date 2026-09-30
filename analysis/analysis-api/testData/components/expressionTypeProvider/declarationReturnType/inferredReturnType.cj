// FILE: inferredReturnType.cj
// TARGET_CLASS: User
// TARGET_FUNCTION: makeUser
// EXPECTED_DECLARATION_RETURN_TYPE: sample.User

package sample

class User {
}

// 省略返回类型：由函数体推导，不写显式返回类型。
func makeUser() {
    return User()
}
