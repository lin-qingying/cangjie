class PMemberHolder {
    var v: Int64 = 1
}

func pMemberBody(): Unit {
    let g = {x => x.v}
    let _ = g
    return
}