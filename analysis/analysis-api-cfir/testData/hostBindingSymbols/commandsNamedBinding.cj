// LANGUAGE_VERSION: 1.1.3
// LANGUAGE: +EffectHandlers
// WITH_STDLIB
package test.hosts
import stdx.effect.Command

func commandHost(): Int64 {
    return try {
        1
    } handle (oper<caret_commandDeclaration>ation: Command<Int64>) {
        let alias = oper<caret_commandUse>ation
        2
    }
}
