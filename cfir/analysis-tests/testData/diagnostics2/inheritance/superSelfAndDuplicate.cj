// RUN_PIPELINE_TILL: FRONTEND
interface IReadable {}
interface IWritable {}

class Loop <: <!SUPER_TYPES_SELF_REFERENCE!>Loop<!> {}

<!SUPER_TYPES_DUPLICATE!>class DupInterfaces<!> <: IReadable & IReadable & IWritable {}
