// FILE: closureFunctionType.cj
// TARGET_CLASS: User
// SECOND_TARGET_CLASS: Base
// TYPE_CREATION_KIND: CLOSURE_FUNCTION
// EXPECTED_QUALIFIED_TYPE_RENDER: closure (sample.type.creator.User, ...) -> sample.type.creator.Base
// EXPECTED_SHORT_TYPE_RENDER: closure (User, ...) -> Base
package sample.type.creator

interface Base {
}

class User <: Base {
}
