package extend_sequence_notes

interface I1 { }
interface I2 <: I1 { }
interface I3 { }
interface I4 <: I3 { }

class Box { }

extend Box <: I1 & I4 { }
extend Box <: I3 & I2 { }
