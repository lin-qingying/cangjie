// FILE: cFunctionType.cj
// TARGET_CLASS: User
// SECOND_TARGET_CLASS: Base
// TYPE_CREATION_KIND: C_FUNCTION
// EXPECTED_QUALIFIED_TYPE_RENDER: cfunc (sample.typecreator.User) -> sample.typecreator.Base
// EXPECTED_SHORT_TYPE_RENDER: cfunc (User) -> Base
package sample.typecreator

interface Base {
}

class User <: Base {
}
