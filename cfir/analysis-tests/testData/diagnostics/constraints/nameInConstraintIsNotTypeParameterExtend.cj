interface Named {}
class Wrapper<T> {}

extend<T> Wrapper<T> <: Named where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>U<!> <: Named {}
