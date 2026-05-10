// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unittest.diff

import std.unittest.common.PrettyPrinter
import std.core.*
import std.collection.*
import std.unittest.common.*
import std.collection.ArrayList
import std.convert.Formattable
import std.math.ceil

interface AssertPrintable {
    prop hasNestedDiff: Bool

    func pprintForAssertion(pp: std.unittest.common.PrettyPrinter, that: T, thisPrefix: std.core.String, thatPrefix: std.core.String, level: Int64): std.unittest.common.PrettyPrinter
}

extend Extend <: AssertPrintable {
    prop hasNestedDiff: Bool

    func pprintForAssertion(pp: std.unittest.common.PrettyPrinter, right: Float16, leftPrefix: std.core.String, rightPrefix: std.core.String, level: Int64): std.unittest.common.PrettyPrinter
}

extend Extend <: AssertPrintable {
    prop hasNestedDiff: Bool

    func pprintForAssertion(pp: std.unittest.common.PrettyPrinter, right: Float32, leftPrefix: std.core.String, rightPrefix: std.core.String, level: Int64): std.unittest.common.PrettyPrinter
}

extend Extend <: AssertPrintable {
    prop hasNestedDiff: Bool

    func pprintForAssertion(pp: std.unittest.common.PrettyPrinter, right: Float64, leftPrefix: std.core.String, rightPrefix: std.core.String, level: Int64): std.unittest.common.PrettyPrinter
}

extend Option <: AssertPrintable {
    prop hasNestedDiff: Bool

    func pprintForAssertion(pp: std.unittest.common.PrettyPrinter, right: std.core.Option<T>, leftPrefix: std.core.String, rightPrefix: std.core.String, level: Int64): std.unittest.common.PrettyPrinter
}

extend String <: AssertPrintable {
    prop hasNestedDiff: Bool

    func pprintForAssertion(pp: std.unittest.common.PrettyPrinter, right: std.core.String, leftPrefix: std.core.String, rightPrefix: std.core.String, level: Int64): std.unittest.common.PrettyPrinter
}
