// RUN_PIPELINE_TILL: FRONTEND
class FinalBase {}

class FinalChild <: <!NON_INHERITABLE_SUPER_CLASS!>FinalBase<!> {}
