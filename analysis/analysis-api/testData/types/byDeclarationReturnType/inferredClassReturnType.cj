// FILE: inferredClassReturnType.cj
package sample.types.returntype

class User {
}

// 省略返回类型：由 return 表达式的类型推导为 sample.types.returntype.User。
func infer<caret>redUser() {
    return User()
}
