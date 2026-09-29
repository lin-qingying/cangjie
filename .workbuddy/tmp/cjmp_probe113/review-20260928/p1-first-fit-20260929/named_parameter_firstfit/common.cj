package firstfit_named_parameters

common extend Int64 {
    public common func pick(candidate!: Int64): Int64 { candidate }
}
common extend Int64 {
    public common func pick(selected!: Int64): Int64 { selected }
}

