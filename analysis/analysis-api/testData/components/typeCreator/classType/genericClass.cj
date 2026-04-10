// FILE: genericClass.cj
// TARGET_CLASS: User
// CONTAINER_CLASS: Box
// TYPE_CREATION_KIND: GENERIC_CLASS
// EXPECTED_QUALIFIED_TYPE_RENDER: sample.typecreator.Box<sample.typecreator.User>
// EXPECTED_SHORT_TYPE_RENDER: Box<User>
package sample.typecreator

class User {
}

class Box<T> {
}
