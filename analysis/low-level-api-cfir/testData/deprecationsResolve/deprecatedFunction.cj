// FILE: deprecatedFunction.cj
package lowlevel.deprecations

@Deprecated["use newApi instead"]
func legacyApi(): Int64 {
    return 1
}

func useSite(): Int64 {
    return legacyApi()
}
