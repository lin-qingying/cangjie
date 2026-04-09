// FILE: cFunctionType.cj
// TARGET_CLASS: User
// SECOND_TARGET_CLASS: Base
// TYPE_CREATION_KIND: C_FUNCTION
// EXPECTED_QUALIFIED_TYPE_RENDER: cfunc (sample.type.creator.User) -> sample.type.creator.Base
// EXPECTED_SHORT_TYPE_RENDER: cfunc (User) -> Base
package sample.type.creator

interface Base {
}

class User <: Base {
}
