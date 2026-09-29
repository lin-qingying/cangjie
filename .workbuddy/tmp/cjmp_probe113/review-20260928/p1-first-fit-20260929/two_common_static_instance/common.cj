package firstfit_static_instance

common extend Int64 {
    public common static func pick(v: Int64): Int64 { v }
}
common extend Int64 {
    public common func pick(v: Int64): Int64 { v + 1 }
}

