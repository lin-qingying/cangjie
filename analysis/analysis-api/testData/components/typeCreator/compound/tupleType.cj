// FILE: tupleType.cj
// TARGET_CLASS: User
// SECOND_TARGET_CLASS: Base
// TYPE_CREATION_KIND: TUPLE
// EXPECTED_QUALIFIED_TYPE_RENDER: (sample.typecreator.User, sample.typecreator.Base)
// EXPECTED_SHORT_TYPE_RENDER: (User, Base)
package sample.typecreator

interface Base {
}

class User <: Base {
}
