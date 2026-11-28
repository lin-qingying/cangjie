// LANGUAGE_VERSION: 1.1.3
// LANGUAGE: +EffectHandlers
// WITH_STDLIB
package test.hosts
import stdx.effect.Command

func commandWildcard(): Int64 {
    let result = try {
        let invalidTry = oper<caret_tryUnavailable>ation
        1
    } handle (oper<caret_commandDeclaration>ation: Command<Int64>) {
        let alias = operation
        2
    } handle (<caret_wildcardDeclaration>_: Command<Bool>) {
        let invalidSibling = oper<caret_siblingUnavailable>ation
        3
    } finally {
        let invalidFinally = oper<caret_finallyUnavailable>ation
    }
    let invalidAfter = oper<caret_afterUnavailable>ation
    return result
}
