// FILE: fileScopeQueries.cj
// FILE_SCOPE_AVAILABLE_NAME: User
// FILE_SCOPE_AVAILABLE_NAME: makeUser
// FILE_SCOPE_CLASSIFIER: User
// FILE_SCOPE_CALLABLE: makeUser
// PACKAGE_SCOPE_CLASSIFIER: User
// PACKAGE_SCOPE_CALLABLE: makeUser

package analysis.scopes

class User {}

func makeUser(): User {
    return User()
}

func consume(): User {
    return makeUser()
}
