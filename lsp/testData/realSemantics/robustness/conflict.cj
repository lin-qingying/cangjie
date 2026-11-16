package sample.conflict

func first(): Int64 {
    return 1
}

func second(): Int64 {
    return 2
}

func caller(): Int64 {
    return first()
}
