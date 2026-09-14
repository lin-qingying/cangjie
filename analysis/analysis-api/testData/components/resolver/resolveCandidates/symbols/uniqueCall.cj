// FILE: uniqueCall.cj
// TARGET_NAME: provide
package sample.resolver.candidates

func provide(): Int64 {
    return 1
}

func caller(): Int64 {
    return provide()
}
