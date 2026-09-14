// FILE: unresolvedCall.cj
// TARGET_NAME: missingFunction
package sample.resolver.candidates

func caller(): Int64 {
    return missingFunction()
}
