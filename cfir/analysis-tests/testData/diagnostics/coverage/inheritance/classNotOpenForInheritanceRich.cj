// RUN_PIPELINE_TILL: FRONTEND
class FinalBase {}
open class OpenBase {}
abstract class AbstractBase {}

class FinalChild <: <!NON_INHERITABLE_SUPER_CLASS!>FinalBase<!> {}

class OpenChild <: OpenBase {}

class AbstractChild <: AbstractBase {}
