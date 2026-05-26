// MODULE: main
// MAIN_MODULE
// FILE: transitiveTypeAliasExpandedType.cj
package sample.standalone.behavior

class Target {
}

type MidAlias = Target

type FinalAlias = MidAlias
