// FILE: functionSame.cj
// LEFT_TYPE_KIND: FUNCTION
// LEFT_TARGET_CLASS: User
// LEFT_SECOND_TARGET_CLASS: Base
// RIGHT_TYPE_KIND: FUNCTION
// RIGHT_TARGET_CLASS: User
// RIGHT_SECOND_TARGET_CLASS: Base
// EXPECTED_IS_SUBTYPE: true
// EXPECTED_SEMANTICALLY_EQUAL: true
package sample.typerelation

interface Base {
}

class User <: Base {
}
