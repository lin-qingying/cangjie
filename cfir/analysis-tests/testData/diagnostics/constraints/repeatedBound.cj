interface Named {}
interface Tagged {}

class Holder<T> where T <: Named & Named & Tagged {}

func label<T>() where T <: Named & Named {}
