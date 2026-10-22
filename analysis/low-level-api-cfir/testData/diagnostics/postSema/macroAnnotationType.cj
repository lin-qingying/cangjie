package test

import std.unittest.*
import std.unittest.testmacro.*

@Test
class MacroAnnotatedTests {
    @TestCase
    func sample(): Unit {
    }
}
