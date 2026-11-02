// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unittest.testmacro

import std.ast.*
import std.collection.*
import std.core.*
import std.ast.BinaryExpr
import std.ast.Tokens
import std.ast.Token
import std.ast.Expr
import std.ast.TokenKind
import std.ast.ASTException
import std.collection.ArrayList
import std.collection.concurrent.ConcurrentHashMap
import std.sync.AtomicUInt64
import std.unittest.common.shieldEscapeSeq
import std.unicode.*
import std.collection.map

public func Expect(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func Assert(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func Expect(assertion: std.ast.Tokens, body: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func Assert(assertion: std.ast.Tokens, body: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func CustomAssertion(body: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_Expect_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_c_Assert_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_Expect_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_Assert_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_c_CustomAssertion_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Bench(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func Bench(attr: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_Bench_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_Bench_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Configure(args: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_a_Configure_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Fail(message: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func FailExpect(message: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_Fail_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_c_FailExpect_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func BeforeAll(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func AfterAll(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func BeforeEach(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func AfterEach(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_BeforeAll_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_c_AfterAll_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_c_BeforeEach_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_c_AfterEach_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Measure(attr: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_a_Measure_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Parallel(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_Parallel_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func PowerAssert(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_PowerAssert_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Skip(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func Skip(attr: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_Skip_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_Skip_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Strategy(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func Strategy(attr: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_Strategy_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_Strategy_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Tag(tags: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_a_Tag_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func TestBuilder(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_TestBuilder_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func TestCase(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func TestCase(args: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_TestCase_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_TestCase_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Test(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func Test(dsl: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_Test_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_Test_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func TestTemplate(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func TestTemplate(dsl: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_TestTemplate_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_TestTemplate_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func AssertThrows(body: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func AssertThrows(exceptions: std.ast.Tokens, body: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func ExpectThrows(body: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func ExpectThrows(exceptions: std.ast.Tokens, body: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_AssertThrows_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_AssertThrows_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_c_ExpectThrows_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_ExpectThrows_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Timeout(attrs: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_a_Timeout_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func Types(dsl: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_a_Types_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func UnittestOption(_: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func UnittestOption(types: std.ast.Tokens, params: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public func macroCall_c_UnittestOption_std_unittest_testmacro(paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }

public func macroCall_a_UnittestOption_std_unittest_testmacro(attrPtr: std.core.CPointer<UInt8>, attrSize: Int64, paramPtr: std.core.CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: std.core.CPointer<Unit>): std.core.CPointer<UInt8> { /* compiled code */ }
