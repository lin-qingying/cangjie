package firstfit_static_instance

specific extend Int64 {
    public specific static func pick(v: Int64): Int64 { v + 10 }
    public specific func pick(w: Int64): Int64 { w + 20 }
}

