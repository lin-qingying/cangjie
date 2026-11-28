// PREFIX: value
// WITH_ORDER: valueA, valueB
// NOTHING_ELSE
package completion.dsl

func withOrder(): Int64 {
    let valueA = 1
    let valueB = 2
    return completionTarget
}
