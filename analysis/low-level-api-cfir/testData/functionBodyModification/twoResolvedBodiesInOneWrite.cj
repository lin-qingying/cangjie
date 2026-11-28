package lowlevel.functionbodymodification

func first(): Int64 { 1 }

func second(): Int64 { 2 }

func use(): Int64 { first() + second() }
