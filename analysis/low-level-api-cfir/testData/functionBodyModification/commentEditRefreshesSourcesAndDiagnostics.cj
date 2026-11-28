package lowlevel.functionbodymodification

func compute(): Int64 { 1 }

func use(): Int64 { compute() }

func following(): Unit { missing }
