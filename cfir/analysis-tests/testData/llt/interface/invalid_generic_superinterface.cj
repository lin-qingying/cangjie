interface Marker {}
class Good <: Marker {}
class Bad {}
class Box<T> where T <: Marker {}
interface Parent<T> where T <: Marker {}
interface Unconstrained<T> {}
type P<T> = Parent<T>

interface GoodDirect <: Parent<Good> {}
interface GoodAlias <: P<Good> {}
interface GoodNested <: Unconstrained<Box<Good>> {}
interface <!INTERFACE_CANNOT_INHERIT_CLASS!>BadDirect<!> <: Parent<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Bad<!>> {}
interface <!INTERFACE_CANNOT_INHERIT_CLASS!>BadAlias<!> <: <!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>P<!><Bad> {}
interface <!INTERFACE_CANNOT_INHERIT_CLASS!>BadNested<!> <: Unconstrained<Box<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Bad<!>>> {}
interface BadTuple <: Unconstrained<(Box<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Bad<!>>, Int64)> {}
interface BadFunction <: Unconstrained<(Box<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Bad<!>>) -> Unit> {}
interface <!INTERFACE_CANNOT_INHERIT_CLASS!>WrongArity<!> <: <!GENERIC_ARGUMENT_NO_MATCH!>Parent<Good, Bad><!> {}
interface <!INTERFACE_CANNOT_INHERIT_CLASS!>UnknownParent<!> <: <!UNDECLARED_TYPE_NAME!>Missing<!> {}
class ClassBad <: Parent<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Bad<!>> {}

interface <!INTERFACE_CANNOT_INHERIT_CLASS!>MultipleInvalid<!> <: Parent<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Bad<!>> & Unconstrained<Box<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Bad<!>>> & Bad {}
interface <!INTERFACE_CANNOT_INHERIT_CLASS!>MultipleMissing<!> <: <!UNDECLARED_TYPE_NAME!>MissingA<!> & <!UNDECLARED_TYPE_NAME!>MissingB<!> {}

main() {}
