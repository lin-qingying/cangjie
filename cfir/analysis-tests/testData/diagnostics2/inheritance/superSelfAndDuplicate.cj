// RUN_PIPELINE_TILL: FRONTEND
interface IReadable {}
interface IWritable {}

class Loop <: <!SUPER_TYPES_SELF_REFERENCE!>Loop<!> {}

class <!SUPER_TYPES_DUPLICATE!>DupInterfaces<!> <: IReadable & IReadable & IWritable {}
