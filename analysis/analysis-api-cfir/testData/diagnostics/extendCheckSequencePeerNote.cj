package extend_sequence_notes

interface I1 { }
interface I2 <: I1 { }
interface I3 { }
interface I4 <: I3 { }

class Box<T> { }

extend<T> Box<T> <: I1 & I4 { }
extend<T> Box<T> <: I3 & I2 { }
