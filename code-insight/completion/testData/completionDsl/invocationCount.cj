// PREFIX: loc
// INVOCATION_COUNT: 3
// EXIST: localValue
package completion.dsl

func invocationCount(): Int64 {
    let localValue = 1
    return completionTarget
}
