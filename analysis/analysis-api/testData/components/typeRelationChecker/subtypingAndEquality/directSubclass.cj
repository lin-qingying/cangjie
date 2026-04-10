// FILE: directSubclass.cj
// LEFT_TYPE_KIND: CLASS
// LEFT_TARGET_CLASS: User
// RIGHT_TYPE_KIND: CLASS
// RIGHT_TARGET_CLASS: Base
// EXPECTED_IS_SUBTYPE: true
// EXPECTED_SEMANTICALLY_EQUAL: false
package sample.typerelation

open class Base {
}

class User <: Base {
}
