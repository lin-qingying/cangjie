// RUN_PIPELINE_TILL: FRONTEND
func destructure(t: (Int64, String)): Int64 {
    let (a, b) = t
    let _ = b
    return a
}

func destructureOption(o: ?(Int64, String)): Int64 {
    if (let Some((a, b)) <- o) {
        let _ = b
        return a
    }
    return 0
}
