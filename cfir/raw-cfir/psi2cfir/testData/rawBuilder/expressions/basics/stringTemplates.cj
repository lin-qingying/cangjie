// RUN_PIPELINE_TILL: FRONTEND
func greet(name: String, x: Int64): String {
    let a = "hi ${name} ${x}"
    let b = """
        multi ${name}
    """
    return a + b
}
