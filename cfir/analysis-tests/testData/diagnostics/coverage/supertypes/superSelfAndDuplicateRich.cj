// RUN_PIPELINE_TILL: FRONTEND
interface IReadable {}
interface IWritable {}

class Loop <: <!SUPER_TYPES_SELF_REFERENCE!>Loop<!> {}

class DupInterfaces <: <!SUPER_TYPES_DUPLICATE!>IReadable<!> & IReadable & IWritable {}

