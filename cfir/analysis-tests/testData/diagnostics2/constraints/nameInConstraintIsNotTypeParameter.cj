interface Named {}

class Box<T> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>U<!> <: Named {}

func keep<T>(value: T): T where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>U<!> <: Named {
    return value
}
