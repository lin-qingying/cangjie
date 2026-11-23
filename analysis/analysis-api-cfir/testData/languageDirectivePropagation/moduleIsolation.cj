// LANGUAGE_VERSION: 1.1.3
// LANGUAGE: +EffectHandlers
// MODULE: base
// FILE: base.cj
package test.language.base
func baseEntry(): Unit {}

// MODULE: main(base)
// LANGUAGE: -EffectHandlers
// FILE: main.cj
package test.language.main
func entry(): Unit {}
