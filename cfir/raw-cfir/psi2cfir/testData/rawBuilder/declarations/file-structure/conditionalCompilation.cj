// RUN_PIPELINE_TILL: FRONTEND
@When[os == "Windows"]
func windowsOnly(): Int64 {
    return 1
}

func withParams(@When[os == "Windows"] a: Int64, @When[os == "Windows"] b: Int64): Int64 {
    @When[os == "Windows"]
    let x = 1
    return a + b + x
}

class C {
    @When[os == "Windows"]
    func member(): Int64 {
        return 1
    }
}
