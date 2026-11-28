package lowlevel.functionbodymodification

func compute(): Int64 { 1 }

class Repeated {}
class Repeated {}

func ambiguous(value: Repeated): Unit {}
