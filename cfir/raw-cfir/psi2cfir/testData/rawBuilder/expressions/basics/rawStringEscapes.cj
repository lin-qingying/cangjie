// RUN_PIPELINE_TILL: FRONTEND
func f(name: String): String {
    let a = "tab\tnew\nquote\"done"
    let b = """
        raw \t not escaped
    """
    return a + b + name
}
