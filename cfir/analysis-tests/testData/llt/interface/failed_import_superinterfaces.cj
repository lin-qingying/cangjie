import <!UNRESOLVED_IMPORT!>no_such_supertype_package<!>.*

interface Parent<T> {}
interface GoodRoot {}
interface GoodDerived <: GoodRoot {}
interface MissingDirect <: MissingParent {}
interface MissingNested <: Parent<MissingArgument> {}

main() {}
