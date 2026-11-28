// LANGUAGE_VERSION: 1.1.3
// LANGUAGE: +EffectHandlers
// WITH_STDLIB
package test.hosts
import stdx.effect.Command

func commandRedefinition(): Int64 {
    return try { 1 } handle (oper<caret_commandDeclaration>ation: Command<Int64>) {
        let oper<caret_duplicateDeclaration>ation: Int64 = 2
        3
    }
}
