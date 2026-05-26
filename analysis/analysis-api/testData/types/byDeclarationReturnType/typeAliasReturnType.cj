// FILE: typeAliasReturnType.cj
package sample.types.returntype

type UserName = String

func userNa<caret>me(): UserName {
    return ""
}
