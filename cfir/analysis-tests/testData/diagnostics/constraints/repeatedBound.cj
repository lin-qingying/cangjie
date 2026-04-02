interface Named {}
interface Tagged {}

class Holder<T> where T <: Named, T <: <!REPEATED_BOUND!>Named<!>, T <: Tagged {}

func label<T>() where T <: Named, T <: <!REPEATED_BOUND!>Named<!> {}
