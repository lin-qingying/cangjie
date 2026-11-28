// LANGUAGE_VERSION: 1.1.3
// LANGUAGE: +EffectHandlers
// WITH_STDLIB
package test.hosts
import stdx.effect.Command

func commandScope(): Int64 {
    let oper<caret_outerDeclaration>ation: Int64 = 10
    let result = try {
        oper<caret_tryOuter>ation
    } handle (oper<caret_firstDeclaration>ation: Command<Int64>) {
        let firstAlias = oper<caret_firstUse>ation
        2
    } handle (oper<caret_secondDeclaration>ation: Command<Bool>) {
        let secondAlias = oper<caret_secondUse>ation
        3
    } finally {
        let finalAlias = oper<caret_finallyOuter>ation
    }
    return result + oper<caret_afterOuter>ation
}
