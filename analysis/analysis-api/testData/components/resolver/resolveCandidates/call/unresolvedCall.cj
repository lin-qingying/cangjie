// FILE: unresolvedCall.cj
// TARGET_CALL: missingFunction()
package sample.resolver.candidates

func caller(): Int64 {
    return missingFunction()
}
