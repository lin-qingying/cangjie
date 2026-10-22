// MODULE: main
// MAIN_FILE_NAME: useCrossFileConstant.cj
// FILE: constants.cj
package test

const SHARED_CONST: Int64 = 60

// FILE: useCrossFileConstant.cj
package test

func useSharedConst(): Int64 {
    return SHARED_CONST
}
