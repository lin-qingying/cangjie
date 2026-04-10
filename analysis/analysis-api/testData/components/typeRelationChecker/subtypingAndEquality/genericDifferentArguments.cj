// FILE: genericDifferentArguments.cj
// LEFT_TYPE_KIND: GENERIC_CLASS
// LEFT_TARGET_CLASS: User
// LEFT_CONTAINER_CLASS: Box
// RIGHT_TYPE_KIND: GENERIC_CLASS
// RIGHT_TARGET_CLASS: Base
// RIGHT_CONTAINER_CLASS: Box
// EXPECTED_IS_SUBTYPE: false
// EXPECTED_SEMANTICALLY_EQUAL: false
package sample.typerelation

interface Base {
}

class User <: Base {
}

class Box<T> {
}
