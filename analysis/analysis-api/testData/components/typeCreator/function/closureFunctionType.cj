// FILE: closureFunctionType.cj
// TARGET_CLASS: User
// SECOND_TARGET_CLASS: Base
// TYPE_CREATION_KIND: CLOSURE_FUNCTION
// EXPECTED_QUALIFIED_TYPE_RENDER: closure (sample.typecreator.User, ...) -> sample.typecreator.Base
// EXPECTED_SHORT_TYPE_RENDER: closure (User, ...) -> Base
package sample.typecreator

interface Base {
}

class User <: Base {
}
