interface Named {}

class Box<T> where <!UNDECLARED_TYPE_NAME!>U<!> <: Named {}

func keep<T>(value: T): T where <!UNDECLARED_TYPE_NAME!>U<!> <: Named {
    return value
}
