// RUN_PIPELINE_TILL: FRONTEND
class FinalBase {}

class FinalChild <: <!CLASS_NOT_OPEN_FOR_INHERITANCE!>FinalBase<!> {}
