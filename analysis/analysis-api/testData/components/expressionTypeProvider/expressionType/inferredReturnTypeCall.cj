// FILE: inferredReturnTypeCall.cj
// TARGET_CLASS: User
// TARGET_CALL: makeUser()
// EXPECTED_EXPRESSION_TYPE: sample.User

package sample

class User {
}

// 省略返回类型：推导出的 User 必须传播到调用点的表达式类型。
func makeUser() {
    return User()
}

func useUser() {
    return makeUser()
}
