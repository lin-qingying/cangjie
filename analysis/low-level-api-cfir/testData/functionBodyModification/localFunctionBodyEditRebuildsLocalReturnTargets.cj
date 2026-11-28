package lowlevel.functionbodymodification

func compute(): Int64 {
    func local(): Int64 {
        return 7
    }
    return local()
}

func following(): Int64 {
    91
}
