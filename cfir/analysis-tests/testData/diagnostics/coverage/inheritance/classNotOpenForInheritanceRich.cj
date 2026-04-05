// RUN_PIPELINE_TILL: FRONTEND
class FinalBase {}
open class OpenBase {}
abstract class AbstractBase {}

class FinalChild <: <!CLASS_NOT_OPEN_FOR_INHERITANCE!>FinalBase<!> {}

class OpenChild <: OpenBase {}

class AbstractChild <: AbstractBase {}
