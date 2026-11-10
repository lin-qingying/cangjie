// PREFIX: loc
// EXIST: localValue
package completion.dsl

func repeatedInvocationWidensCandidates(): Int64 {
    let localValue = 1
    return completionTarget
}
