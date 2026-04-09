// FILE: genericClass.cj
// TARGET_CLASS: User
// CONTAINER_CLASS: Box
// TYPE_CREATION_KIND: GENERIC_CLASS
// EXPECTED_QUALIFIED_TYPE_RENDER: sample.type.creator.Box<sample.type.creator.User>
// EXPECTED_SHORT_TYPE_RENDER: Box<User>
package sample.type.creator

class User {
}

class Box<T> {
}
