// MODULE: helper
// FILE: helper.cj
package sample.fragment

class Helper {
    public func compute(value: Int64): Int64 {
        return value
    }
}

// MODULE: scratch(helper)
// MODULE_KIND: CodeFragment
// MAIN_MODULE
// MAIN_FILE_NAME: scratch.fragment.cj
// EXPECTED_PRIMARY_MODULE_SHAPE: DanglingFileModule
// EXPECTED_DIRECT_REGULAR_DEPENDENCY: helper
// EXPECTED_CONTEXT_MODULE: helper
// EXPECTED_IS_RESOLVABLE: true
// FILE: scratch.fragment.cj
package sample.fragment

func runFragment(): Int64 {
    let helper = Helper()
    return helper.compute(1)
}
