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

func Expect(input: std.ast.Tokens): std.ast.Tokens

func Assert(input: std.ast.Tokens): std.ast.Tokens

func Expect(assertion: std.ast.Tokens, body: std.ast.Tokens): std.ast.Tokens

func Assert(assertion: std.ast.Tokens, body: std.ast.Tokens): std.ast.Tokens

func CustomAssertion(body: std.ast.Tokens): std.ast.Tokens

func macroCall_c_Expect_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_c_Assert_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_Expect_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_Assert_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_c_CustomAssertion_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Bench(input: std.ast.Tokens): std.ast.Tokens

func Bench(attr: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_c_Bench_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_Bench_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Configure(args: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_a_Configure_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Fail(message: std.ast.Tokens): std.ast.Tokens

func FailExpect(message: std.ast.Tokens): std.ast.Tokens

func macroCall_c_Fail_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_c_FailExpect_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func BeforeAll(input: std.ast.Tokens): std.ast.Tokens

func AfterAll(input: std.ast.Tokens): std.ast.Tokens

func BeforeEach(input: std.ast.Tokens): std.ast.Tokens

func AfterEach(input: std.ast.Tokens): std.ast.Tokens

func macroCall_c_BeforeAll_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_c_AfterAll_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_c_BeforeEach_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_c_AfterEach_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Measure(attr: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_a_Measure_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Parallel(input: std.ast.Tokens): std.ast.Tokens

func macroCall_c_Parallel_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func PowerAssert(input: std.ast.Tokens): std.ast.Tokens

func macroCall_c_PowerAssert_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Skip(input: std.ast.Tokens): std.ast.Tokens

func Skip(attr: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_c_Skip_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_Skip_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Strategy(input: std.ast.Tokens): std.ast.Tokens

func Strategy(attr: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_c_Strategy_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_Strategy_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Tag(tags: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_a_Tag_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func TestBuilder(input: std.ast.Tokens): std.ast.Tokens

func macroCall_c_TestBuilder_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func TestCase(input: std.ast.Tokens): std.ast.Tokens

func TestCase(args: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_c_TestCase_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_TestCase_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Test(input: std.ast.Tokens): std.ast.Tokens

func Test(dsl: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_c_Test_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_Test_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func TestTemplate(input: std.ast.Tokens): std.ast.Tokens

func TestTemplate(dsl: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_c_TestTemplate_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_TestTemplate_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func AssertThrows(body: std.ast.Tokens): std.ast.Tokens

func AssertThrows(exceptions: std.ast.Tokens, body: std.ast.Tokens): std.ast.Tokens

func ExpectThrows(body: std.ast.Tokens): std.ast.Tokens

func ExpectThrows(exceptions: std.ast.Tokens, body: std.ast.Tokens): std.ast.Tokens

func macroCall_c_AssertThrows_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_AssertThrows_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_c_ExpectThrows_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_ExpectThrows_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Timeout(attrs: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_a_Timeout_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func Types(dsl: std.ast.Tokens, input: std.ast.Tokens): std.ast.Tokens

func macroCall_a_Types_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func UnittestOption(_: std.ast.Tokens): std.ast.Tokens

func UnittestOption(types: std.ast.Tokens, params: std.ast.Tokens): std.ast.Tokens

func macroCall_c_UnittestOption_std_unittest_testmacro(paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>

func macroCall_a_UnittestOption_std_unittest_testmacro(attrPtr: CPointer<UInt8>, attrSize: Int64, paramPtr: CPointer<UInt8>, paramSize: Int64, callMacroCallPtr: CPointer<Unit>): CPointer<UInt8>
