// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.core

import std.core

@Frozen
private func arrayInitByCollection<T>(array: std.core.Array<T>, elements: std.core.Collection<T>): std.core.Array<T> { /* compiled code */ }

public interface Collection<T> <: std.core.Iterable<T> {
    public open prop size: Int64

    public open func isEmpty(): Bool

    public open func toArray(): std.core.Array<T> { /* compiled code */ }
}

public abstract class Iterator<T> <: std.core.Object & std.core.Iterable<T> {
    public func next(): std.core.Option<T>

    @Frozen
    init() { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }
}

public interface Iterable<E> {
    public open func iterator(): std.core.Iterator<E>
}

public enum Option<T> {
    | Some(T)

    | None

    @Frozen
    public func getOrThrow(): T { /* compiled code */ }

    @Frozen
    public func getOrThrow(exception: () -> std.core.Exception): T { /* compiled code */ }

    @Frozen
    public func getOrDefault(other: () -> T): T { /* compiled code */ }

    @Frozen
    public func isNone(): Bool { /* compiled code */ }

    @Frozen
    public func isSome(): Bool { /* compiled code */ }

    public func map<R>(transform: (T) -> R): std.core.Option<R> { /* compiled code */ }

    public func filter(predicate: (T) -> Bool): std.core.Option<T> { /* compiled code */ }

    public func flatMap<R>(transform: (T) -> std.core.Option<R>): std.core.Option<R> { /* compiled code */ }
}

@Intrinsic
internal func arraySet<T>(rawArr: std.core.Array<T>, index: Int64, element: T): Unit

@Frozen
private func arrayInitByFunction<T>(array: std.core.Array<T>, initElement: (Int64) -> T): std.core.Array<T> { /* compiled code */ }

@Intrinsic
internal func arraySize<T>(rawArr: std.core.Array<T>): Int64

@Frozen
internal func toArray<T>(raw: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

public struct Array<T> {
    internal let rawptr: std.core.Array<T>

    internal let start: Int64

    internal let len: Int64

    @Frozen
    internal func toRawArray(): std.core.Array<T> { /* compiled code */ }

    @Frozen
    init() { /* compiled code */ }

    @Frozen
    init(size: Int64, repeat!: T) { /* compiled code */ }

    @Frozen
    init(elements: std.core.Collection<T>) { /* compiled code */ }

    @Frozen
    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    @Frozen
    init(data: std.core.Array<T>, start: Int64, len: Int64) { /* compiled code */ }

    @Frozen
    public prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Frozen
    public prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Frozen
    public func slice(start: Int64, len: Int64): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func get(index: Int64): std.core.Option<T> { /* compiled code */ }

    @Frozen
    public operator func [](index: Int64): T { /* compiled code */ }

    @Frozen
    public operator func [](index: Int64, value!: T): Unit { /* compiled code */ }

    @Frozen
    public operator func [](range: std.core.Range<Int64>): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func fill(value: T): Unit { /* compiled code */ }

    @Frozen
    public operator func [](range: std.core.Range<Int64>, value!: std.core.Array<T>): Unit { /* compiled code */ }

    @Frozen
    public func reverse(): Unit { /* compiled code */ }

    @Frozen
    private func rangeSlice(range: std.core.Range<Int64>, _: Int64): std.core.Array<T> { /* compiled code */ }

    @Frozen
    private func isLegal(start_: Int64, end_: Int64): Bool { /* compiled code */ }

    @Frozen
    private func normalize(range: std.core.Range<Int64>): (Int64, Int64) { /* compiled code */ }

    @Frozen
    public func clone(): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func clone(range: std.core.Range<Int64>): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func copyTo(dst: std.core.Array<T>, srcStart: Int64, dstStart: Int64, copyLen: Int64): Unit { /* compiled code */ }

    @Frozen
    public func copyTo(dst: std.core.Array<T>): Unit { /* compiled code */ }

    @Frozen
    public func concat(other: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

    @Frozen
    protected func getUnchecked(index: Int64): T { /* compiled code */ }

    @Frozen
    protected func setUnchecked(index: Int64, element: T): Unit { /* compiled code */ }

    @Frozen
    public func swap(index1: Int64, index2: Int64): Unit { /* compiled code */ }

    @Frozen
    public func splitAt(mid: Int64): (std.core.Array<T>, std.core.Array<T>) { /* compiled code */ }

    @Frozen
    public func repeat(n: Int64): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func map<R>(transform: (T) -> R): std.core.Array<R> { /* compiled code */ }
}

@Frozen
public func acquireArrayRawData<T>(arr: std.core.Array<T>): std.core.CPointerHandle<T> { /* compiled code */ }

public open interface CType {
}

public struct CPointerHandle<T> {
    public let pointer: std.core.CPointer<T>

    public let array: std.core.Array<T>

    @Deprecated[message: "Use global function `public unsafe func acquireArrayRawData<T>(arr: Array<T>): CPointerHandle<T> where T <: CType` instead."]
    init() { /* compiled code */ }

    @Deprecated[message: "Use global function `public unsafe func acquireArrayRawData<T>(arr: Array<T>): CPointerHandle<T> where T <: CType` instead."]
    init(ptr: std.core.CPointer<T>, arr: std.core.Array<T>) { /* compiled code */ }
}

@Intrinsic
internal func acquireRawData<T>(rawArr: std.core.Array<T>): std.core.CPointer<T>

@Frozen
public func releaseArrayRawData<T>(handle: std.core.CPointerHandle<T>): Unit { /* compiled code */ }

@Intrinsic
internal func releaseRawData<T>(rawArr: std.core.Array<T>, ptr: std.core.CPointer<T>): Unit

let ASCII_UPPER_TO_LOWER_DIFF: UInt8 = COMPILED_CODE /* compiled code */

@Frozen
public func max<T>(a: T, b: T, others: std.core.Array<T>): T { /* compiled code */ }

public interface Comparable<T> <: std.core.Equatable<T> & std.core.Less<T> & std.core.Greater<T> & std.core.LessOrEqual<T> & std.core.GreaterOrEqual<T> {
    public open func compare(that: T): std.core.Ordering

    public open operator func ==(rhs: T): Bool { /* compiled code */ }

    public open operator func <(rhs: T): Bool { /* compiled code */ }

    public open operator func >(rhs: T): Bool { /* compiled code */ }

    public open operator func <=(rhs: T): Bool { /* compiled code */ }

    public open operator func >=(rhs: T): Bool { /* compiled code */ }
}

@Frozen
public func min<T>(a: T, b: T, others: std.core.Array<T>): T { /* compiled code */ }

let NS_PER_US: Int64 = COMPILED_CODE /* compiled code */

let NS_PER_MS: Int64 = COMPILED_CODE /* compiled code */

let NS_PER_SEC: Int64 = COMPILED_CODE /* compiled code */

let MS_PER_SEC: Int64 = COMPILED_CODE /* compiled code */

let US_PER_SEC: Int64 = COMPILED_CODE /* compiled code */

let SECS_PER_MINUTE: Int64 = COMPILED_CODE /* compiled code */

let MAX_NANOSECOND: UInt32 = COMPILED_CODE /* compiled code */

let SECS_PER_HOUR: Int64 = COMPILED_CODE /* compiled code */

let SECS_PER_DAY: Int64 = COMPILED_CODE /* compiled code */

let MAX_INT64: Int64 = COMPILED_CODE /* compiled code */

let MIN_INT64: Int64 = COMPILED_CODE /* compiled code */

let FLOAT64_0: Float64 = COMPILED_CODE /* compiled code */

let FLOAT64_1: Float64 = COMPILED_CODE /* compiled code */

let SECS_MAX: UInt64 = COMPILED_CODE /* compiled code */

let DUR_BASE: UInt32 = COMPILED_CODE /* compiled code */

let SECRET0: UInt64 = COMPILED_CODE /* compiled code */

let SECRET1: UInt64 = COMPILED_CODE /* compiled code */

let SECRET2: UInt64 = COMPILED_CODE /* compiled code */

let SECRET3: UInt64 = COMPILED_CODE /* compiled code */

@Frozen
public func print(str: std.core.String, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public struct String <: std.core.Collection<UInt8> & std.core.Comparable<std.core.String> & std.core.Hashable & std.core.ToString {
    internal let myData: std.core.Array<UInt8>

    internal let start: UInt32

    internal let length: UInt32

    public static const empty: std.core.String = COMPILED_CODE /* compiled code */

    @Frozen
    init() { /* compiled code */ }

    @Frozen
    init(value: std.core.Array<Rune>) { /* compiled code */ }

    @Frozen
    init(value: std.core.Collection<Rune>) { /* compiled code */ }

    @Frozen
    init(value: std.core.Array<UInt8>) { /* compiled code */ }

    @Frozen
    init(value: std.core.Array<UInt8>, start: UInt32, len: UInt32) { /* compiled code */ }

    @Frozen
    init(ptr: std.core.CPointer<UInt8>, cSize: Int64) { /* compiled code */ }

    @Frozen
    init(value: std.core.Array<UInt8>) { /* compiled code */ }

    @Frozen
    public func get(index: Int64): std.core.Option<UInt8> { /* compiled code */ }

    @Frozen
    internal func acquireRaw(): std.core.CPointer<UInt8> { /* compiled code */ }

    @Frozen
    internal func releaseRaw(ptr: std.core.CPointer<UInt8>): Unit { /* compiled code */ }

    @Frozen
    public func toRuneArray(): std.core.Array<Rune> { /* compiled code */ }

    @Frozen
    public func toArray(): std.core.Array<UInt8> { /* compiled code */ }

    @Frozen
    public func toString(): std.core.String { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<UInt8> { /* compiled code */ }

    @Frozen
    public func runes(): std.core.Iterator<Rune> { /* compiled code */ }

    @Frozen
    public func lines(): std.core.Iterator<std.core.String> { /* compiled code */ }

    @Frozen
    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func isAscii(): Bool { /* compiled code */ }

    @Frozen
    public func isAsciiBlank(): Bool { /* compiled code */ }

    @Frozen
    public func clone(): std.core.String { /* compiled code */ }

    @Frozen
    public func hashCode(): Int64 { /* compiled code */ }

    @Frozen
    public static func fromUtf8(utf8Data: std.core.Array<UInt8>): std.core.String { /* compiled code */ }

    @Frozen
    public func rawData(): std.core.Array<UInt8> { /* compiled code */ }

    @Frozen
    public static func fromUtf8Unchecked(utf8Data: std.core.Array<UInt8>): std.core.String { /* compiled code */ }

    @Frozen
    public func indexOf(b: UInt8): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func indexOf(b: UInt8, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    private func indexOfImpl(b: UInt8, fromIndex: Int64): Int64 { /* compiled code */ }

    @Frozen
    private func indexOfCommon(b: UInt8, fromIndex: Int64): Int64 { /* compiled code */ }

    @Frozen
    private func indexOfFast(b: UInt8, fromIndex: Int64): Int64 { /* compiled code */ }

    @Frozen
    public func indexOf(str: std.core.String): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func indexOf(str: std.core.String, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func lastIndexOf(b: UInt8): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func lastIndexOf(b: UInt8, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func lastIndexOf(str: std.core.String): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func lastIndexOf(str: std.core.String, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func count(str: std.core.String): Int64 { /* compiled code */ }

    @Frozen
    public func split(str: std.core.String, removeEmpty!: Bool = COMPILED_CODE /* compiled code */): std.core.Array<std.core.String> { /* compiled code */ }

    @Frozen
    public func split(str: std.core.String, maxSplits: Int64, removeEmpty!: Bool = COMPILED_CODE /* compiled code */): std.core.Array<std.core.String> { /* compiled code */ }

    @Frozen
    public func lazySplit(str: std.core.String, removeEmpty!: Bool = COMPILED_CODE /* compiled code */): std.core.Iterator<std.core.String> { /* compiled code */ }

    @Frozen
    public func lazySplit(str: std.core.String, maxSplits: Int64, removeEmpty!: Bool = COMPILED_CODE /* compiled code */): std.core.Iterator<std.core.String> { /* compiled code */ }

    @Frozen
    public func replace(old: std.core.String, new: std.core.String): std.core.String { /* compiled code */ }

    @Frozen
    private func insertString(new: std.core.String): std.core.String { /* compiled code */ }

    @Frozen
    private func replaceString(old: std.core.String, new: std.core.String): std.core.String { /* compiled code */ }

    @Frozen
    public func toAsciiLower(): std.core.String { /* compiled code */ }

    @Frozen
    public func toAsciiUpper(): std.core.String { /* compiled code */ }

    @Frozen
    public func toAsciiTitle(): std.core.String { /* compiled code */ }

    @Frozen
    public func trimAscii(): std.core.String { /* compiled code */ }

    @Frozen
    public func trimAsciiStart(): std.core.String { /* compiled code */ }

    @Frozen
    public func trimAsciiEnd(): std.core.String { /* compiled code */ }

    @Frozen
    public func trimStart(set: std.core.Array<Rune>): std.core.String { /* compiled code */ }

    @Frozen
    public func trimEnd(set: std.core.Array<Rune>): std.core.String { /* compiled code */ }

    @Frozen
    public func trimStart(set: std.core.String): std.core.String { /* compiled code */ }

    @Frozen
    public func trimEnd(set: std.core.String): std.core.String { /* compiled code */ }

    @Frozen
    public func trimStart(predicate: (Rune) -> Bool): std.core.String { /* compiled code */ }

    @Frozen
    public func trimEnd(predicate: (Rune) -> Bool): std.core.String { /* compiled code */ }

    @Frozen
    public func removePrefix(prefix: std.core.String): std.core.String { /* compiled code */ }

    @Frozen
    public func removeSuffix(suffix: std.core.String): std.core.String { /* compiled code */ }

    @Frozen
    public func contains(str: std.core.String): Bool { /* compiled code */ }

    @Frozen
    public func startsWith(prefix: std.core.String): Bool { /* compiled code */ }

    @Frozen
    public func endsWith(suffix: std.core.String): Bool { /* compiled code */ }

    @Frozen
    public func padStart(totalWidth: Int64, padding!: std.core.String = COMPILED_CODE /* compiled code */): std.core.String { /* compiled code */ }

    @Frozen
    public func padEnd(totalWidth: Int64, padding!: std.core.String = COMPILED_CODE /* compiled code */): std.core.String { /* compiled code */ }

    @Frozen
    public func equalsIgnoreAsciiCase(that: std.core.String): Bool { /* compiled code */ }

    @Frozen
    private func byteEqualsIgnoreAsciiCase(b1: UInt8, b2: UInt8): Bool { /* compiled code */ }

    @Frozen
    internal static func compareBytes(ptr1: std.core.Array<UInt8>, start1: Int64, ptr2: std.core.Array<UInt8>, start2: Int64, num: Int64): Int64 { /* compiled code */ }

    @Frozen
    internal static func compareBytesCommon(ptr1: std.core.Array<UInt8>, start1: Int64, ptr2: std.core.Array<UInt8>, start2: Int64, num: Int64): Int64 { /* compiled code */ }

    @Frozen
    internal static func compareBytesFast(ptr1: std.core.Array<UInt8>, start1: Int64, ptr2: std.core.Array<UInt8>, start2: Int64, num: Int64): Int64 { /* compiled code */ }

    @Frozen
    public func compare(str: std.core.String): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func +(right: std.core.String): std.core.String { /* compiled code */ }

    @Frozen
    public operator func *(count: Int64): std.core.String { /* compiled code */ }

    @Frozen
    public operator func <(right: std.core.String): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(right: std.core.String): Bool { /* compiled code */ }

    @Frozen
    public operator func >(right: std.core.String): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(right: std.core.String): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(right: std.core.String): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(right: std.core.String): Bool { /* compiled code */ }

    @Frozen
    public operator func [](index: Int64): UInt8 { /* compiled code */ }

    @Frozen
    private func sliceUnchecked(startIndex: Int64, len: Int64): std.core.String { /* compiled code */ }

    @Frozen
    public operator func [](range: std.core.Range<Int64>): std.core.String { /* compiled code */ }

    @Frozen
    public static func join(strArray: std.core.Array<std.core.String>, delimiter!: std.core.String = COMPILED_CODE /* compiled code */): std.core.String { /* compiled code */ }

    @Frozen
    private func indexOfString(str: std.core.String, startIndex!: Int64 = COMPILED_CODE /* compiled code */, fromLast!: Bool = COMPILED_CODE /* compiled code */): Int64 { /* compiled code */ }

    @Frozen
    private func splitOfString(str: std.core.String, maxSplits: Int64, removeEmpty: Bool): std.core.Array<std.core.String> { /* compiled code */ }

    @Frozen
    private func splitOfStringForOneRune(maxSplits: Int64): std.core.Array<std.core.String> { /* compiled code */ }

    @Frozen
    private func getRealSplitLenWithRemoveEmpty(indexes: std.core.CPointer<Int64>, total: Int64, separatorLen: Int64): Int64 { /* compiled code */ }

    @Frozen
    private func splitAsStringArray(indexes: std.core.CPointer<Int64>, total: Int64, separatorLen: Int64, maxSplits: Int64, removeEmpty: Bool): std.core.Array<std.core.String> { /* compiled code */ }

    @Frozen
    public static func withRawData(rawData: std.core.Array<UInt8>): std.core.String { /* compiled code */ }

    @Frozen
    public static func checkUtf8Encoding(data: std.core.Array<UInt8>): Bool { /* compiled code */ }
}

internal foreign func CJ_CORE_PrintUTF8(c: std.core.CPointer<UInt8>, len: Int64, newLine: Bool, flush: Bool): Int64

public class OutOfMemoryError <: std.core.Error {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

@Frozen
public func println(): Unit { /* compiled code */ }

internal foreign func CJ_CORE_PrintChar(c: UInt32, newLine: Bool, flush: Bool): Unit

@Frozen
public func println(str: std.core.String): Unit { /* compiled code */ }

@Frozen
public func eprint(str: std.core.String, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

internal foreign func CJ_CORE_ErrorPrintUTF8(c: std.core.CPointer<UInt8>, len: Int64, newLine: Bool, flush: Bool): Int64

@Frozen
public func eprintln(str: std.core.String): Unit { /* compiled code */ }

@Frozen
public func eprint<T>(arg: T, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public interface ToString {
    public open func toString(): std.core.String
}

@Frozen
public func eprintln<T>(arg: T): Unit { /* compiled code */ }

@Frozen
public func print<T>(arg: T, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Frozen
public func println<T>(arg: T): Unit { /* compiled code */ }

@Frozen
public func print(b: Bool, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

internal foreign func CJ_CORE_PrintBool(b: Bool, newLine: Bool, flush: Bool): Unit

@Frozen
public func println(b: Bool): Unit { /* compiled code */ }

@Frozen
public func print(c: Rune, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Frozen
public func println(c: Rune): Unit { /* compiled code */ }

@Frozen
public func print(f: Float16, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

internal foreign func CJ_CORE_PrintFloat(f: Float64, newLine: Bool, flush: Bool): Unit

@Frozen
public func println(f: Float16): Unit { /* compiled code */ }

@Frozen
public func print(f: Float32, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Frozen
public func println(f: Float32): Unit { /* compiled code */ }

@Frozen
public func print(f: Float64, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Frozen
public func println(f: Float64): Unit { /* compiled code */ }

@Frozen
public func print(i: Int8, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

internal foreign func CJ_CORE_PrintSigned(i: Int64, newLine: Bool, flush: Bool): Unit

@Frozen
public func print(i: Int16, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Frozen
public func print(i: Int32, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Frozen
public func print(i: Int64, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Frozen
public func print(i: UInt8, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

internal foreign func CJ_CORE_PrintUnsigned(i: UInt64, newLine: Bool, flush: Bool): Unit

@Frozen
public func print(i: UInt16, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Frozen
public func print(i: UInt32, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Frozen
public func print(i: UInt64, flush!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Frozen
public func println(i: Int8): Unit { /* compiled code */ }

@Frozen
public func println(i: Int16): Unit { /* compiled code */ }

@Frozen
public func println(i: Int32): Unit { /* compiled code */ }

@Frozen
public func println(i: Int64): Unit { /* compiled code */ }

@Frozen
public func println(i: UInt8): Unit { /* compiled code */ }

@Frozen
public func println(i: UInt16): Unit { /* compiled code */ }

@Frozen
public func println(i: UInt32): Unit { /* compiled code */ }

@Frozen
public func println(i: UInt64): Unit { /* compiled code */ }

let MAX_TIMEOUT_NS: Int64 = COMPILED_CODE /* compiled code */

let UNICODE_TO_UTF8_MAX_LEN: Int64 = COMPILED_CODE /* compiled code */

let IS_SIMD_SUPPORTED: Bool = COMPILED_CODE /* compiled code */

internal foreign func CJ_CORE_CanUseSIMD(): Bool

let VECTOR_OFFSET_32: Int64 = COMPILED_CODE /* compiled code */

let ASCII_A: UInt8 = COMPILED_CODE /* compiled code */

let ASCII_Z: UInt8 = COMPILED_CODE /* compiled code */

let ASCII_a: UInt8 = COMPILED_CODE /* compiled code */

let ASCII_z: UInt8 = COMPILED_CODE /* compiled code */

let ASCII_UPPER_LOWER_DIFFERENCE: UInt8 = COMPILED_CODE /* compiled code */

let HIGH_1_MASK: UInt32 = COMPILED_CODE /* compiled code */

let HIGH_2_MASK: UInt32 = COMPILED_CODE /* compiled code */

let HIGH_3_MASK: UInt32 = COMPILED_CODE /* compiled code */

let HIGH_4_MASK: UInt32 = COMPILED_CODE /* compiled code */

let HIGH_5_MASK: UInt32 = COMPILED_CODE /* compiled code */

let HIGH_6_MASK: UInt32 = COMPILED_CODE /* compiled code */

let SHIFT_30: UInt32 = COMPILED_CODE /* compiled code */

let SHIFT_24: UInt32 = COMPILED_CODE /* compiled code */

let SHIFT_18: UInt32 = COMPILED_CODE /* compiled code */

let SHIFT_12: UInt32 = COMPILED_CODE /* compiled code */

let SHIFT_6: UInt32 = COMPILED_CODE /* compiled code */

let LOW_6_MASK: UInt32 = COMPILED_CODE /* compiled code */

let LOW_5_MASK: UInt32 = COMPILED_CODE /* compiled code */

let LOW_4_MASK: UInt32 = COMPILED_CODE /* compiled code */

let LOW_3_MASK: UInt32 = COMPILED_CODE /* compiled code */

let LOW_2_MASK: UInt32 = COMPILED_CODE /* compiled code */

let LOW_1_MASK: UInt32 = COMPILED_CODE /* compiled code */

let UTF8_5_MAX: UInt32 = COMPILED_CODE /* compiled code */

let UTF8_4_MAX: UInt32 = COMPILED_CODE /* compiled code */

let UTF8_3_MAX: UInt32 = COMPILED_CODE /* compiled code */

let UTF8_2_MAX: UInt32 = COMPILED_CODE /* compiled code */

let UTF8_1_MAX: UInt32 = COMPILED_CODE /* compiled code */

let HIGH_1_UInt8: UInt8 = COMPILED_CODE /* compiled code */

let HIGH_2_UInt8: UInt8 = COMPILED_CODE /* compiled code */

let HIGH_3_UInt8: UInt8 = COMPILED_CODE /* compiled code */

let HIGH_4_UInt8: UInt8 = COMPILED_CODE /* compiled code */

let HIGH_5_UInt8: UInt8 = COMPILED_CODE /* compiled code */

let HIGH_6_UInt8: UInt8 = COMPILED_CODE /* compiled code */

let EMPTY_UINT8_ARRAY: std.core.Array<UInt8> = COMPILED_CODE /* compiled code */

let EMPTY_UINT8_RAW_ARRAY: std.core.Array<UInt8> = COMPILED_CODE /* compiled code */

let STRING_C_THRESHOLD: Int64 = COMPILED_CODE /* compiled code */

let INVALID_ID: Int64 = COMPILED_CODE /* compiled code */

@Intrinsic
public func zeroValue<T>(): T

@Intrinsic
internal func intrinsicBuiltInCopyTo<T>(src: std.core.Array<T>, dst: std.core.Array<T>, srcStart: Int64, dstStart: Int64, copyLen: Int64): Unit

public class IndexOutOfBoundsException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class StringBuilder <: std.core.Object & std.core.ToString {
    private var data: std.core.Array<UInt8>

    private var endIndex: Int64

    private static const defaultCapacity: Int64 = COMPILED_CODE /* compiled code */

    @Frozen
    init() { /* compiled code */ }

    @Frozen
    init(str: std.core.String) { /* compiled code */ }

    @Frozen
    init(r: Rune, n: Int64) { /* compiled code */ }

    @Frozen
    init(value: std.core.Array<Rune>) { /* compiled code */ }

    @Frozen
    init(capacity: Int64) { /* compiled code */ }

    @Frozen
    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public func toString(): std.core.String { /* compiled code */ }

    @Frozen
    public func reset(capacity!: std.core.Option<Int64> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    @Frozen
    public func append(r: Rune): Unit { /* compiled code */ }

    @Frozen
    public func append(str: std.core.String): Unit { /* compiled code */ }

    @Frozen
    public func append(sb: std.core.StringBuilder): Unit { /* compiled code */ }

    @Frozen
    public func appendFromUtf8(arr: std.core.Array<UInt8>): Unit { /* compiled code */ }

    @Frozen
    public func appendFromUtf8Unchecked(arr: std.core.Array<UInt8>): Unit { /* compiled code */ }

    @Frozen
    public func append(runeArr: std.core.Array<Rune>): Unit { /* compiled code */ }

    @Frozen
    public func append(cstr: CString): Unit { /* compiled code */ }

    @Frozen
    public func append<T>(v: T): Unit { /* compiled code */ }

    @Frozen
    public func append<T>(val: std.core.Array<T>): Unit { /* compiled code */ }

    @Frozen
    public func append(b: Bool): Unit { /* compiled code */ }

    @Frozen
    public func append(n: Int64): Unit { /* compiled code */ }

    @Frozen
    public func append(n: Int32): Unit { /* compiled code */ }

    @Frozen
    public func append(n: Int16): Unit { /* compiled code */ }

    @Frozen
    public func append(n: Int8): Unit { /* compiled code */ }

    @Frozen
    public func append(n: UInt64): Unit { /* compiled code */ }

    @Frozen
    public func append(n: UInt32): Unit { /* compiled code */ }

    @Frozen
    public func append(n: UInt16): Unit { /* compiled code */ }

    @Frozen
    public func append(n: UInt8): Unit { /* compiled code */ }

    @Frozen
    public func append(n: Float64): Unit { /* compiled code */ }

    @Frozen
    public func append(n: Float32): Unit { /* compiled code */ }

    @Frozen
    public func append(n: Float16): Unit { /* compiled code */ }

    @Frozen
    public func reserve(additional: Int64): Unit { /* compiled code */ }

    @Frozen
    private func grow(minCapacity: Int64): Unit { /* compiled code */ }

    @Frozen
    private func append(cp: std.core.CPointer<UInt8>): Unit { /* compiled code */ }
}

@Intrinsic
internal func arrayGetUnchecked<T>(rawArr: std.core.Array<T>, index: Int64): T

@Intrinsic
internal func arraySetUnchecked<T>(rawArr: std.core.Array<T>, index: Int64, element: T): Unit

public struct Range<T> <: std.core.Iterable<T> {
    public let start: T

    public let end: T

    public let step: Int64

    public let hasStart: Bool

    public let hasEnd: Bool

    public let isClosed: Bool

    @Frozen
    init(start: T, end: T, step: Int64, hasStart: Bool, hasEnd: Bool, isClosed: Bool) { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }

    public func isEmpty(): Bool { /* compiled code */ }

    private func last(): T { /* compiled code */ }
}

public open class IllegalArgumentException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected open override func getClassName(): std.core.String { /* compiled code */ }
}

@Intrinsic
internal func arrayGet<T>(rawArr: std.core.Array<T>, index: Int64): T

public class ArrayIterator<T> <: std.core.Iterator<T> {
    private var myData: std.core.Array<T>

    private let end: Int64

    private var myPosition: Int64

    @Frozen
    init(data: std.core.Array<T>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<T> { /* compiled code */ }
}

public interface Equatable<T> <: std.core.Equal<T> & std.core.NotEqual<T> {
    public open operator func !=(rhs: T): Bool { /* compiled code */ }
}

public interface Equal<T> {
    public open operator func ==(rhs: T): Bool
}

@Intrinsic
internal func readPointer<T>(c: std.core.CPointer<T>, idx: Int64): T

@Intrinsic
internal func writePointer<T>(c: std.core.CPointer<T>, idx: Int64, value: T): Unit

public class Box<T> {
    public var value: T

    init(v: T) { /* compiled code */ }
}

public enum Ordering {
    | LT

    | GT

    | EQ
}

public struct Duration <: std.core.ToString & std.core.Hashable & std.core.Comparable<std.core.Duration> {
    protected let sec: Int64

    protected let ns: UInt32

    init(sec: Int64, ns: UInt32) { /* compiled code */ }

    internal static func of(sec: Int64, ns: Int64): std.core.Duration { /* compiled code */ }

    public static const nanosecond: std.core.Duration = COMPILED_CODE /* compiled code */

    public static const microsecond: std.core.Duration = COMPILED_CODE /* compiled code */

    public static const millisecond: std.core.Duration = COMPILED_CODE /* compiled code */

    public static const second: std.core.Duration = COMPILED_CODE /* compiled code */

    public static const minute: std.core.Duration = COMPILED_CODE /* compiled code */

    public static const hour: std.core.Duration = COMPILED_CODE /* compiled code */

    public static const day: std.core.Duration = COMPILED_CODE /* compiled code */

    public static const Zero: std.core.Duration = COMPILED_CODE /* compiled code */

    public static const Max: std.core.Duration = COMPILED_CODE /* compiled code */

    public static const Min: std.core.Duration = COMPILED_CODE /* compiled code */

    public func toNanoseconds(): Int64 { /* compiled code */ }

    public func toMicroseconds(): Int64 { /* compiled code */ }

    public func toMilliseconds(): Int64 { /* compiled code */ }

    public func toSeconds(): Int64 { /* compiled code */ }

    public func toMinutes(): Int64 { /* compiled code */ }

    public func toHours(): Int64 { /* compiled code */ }

    public func toDays(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func abs(): std.core.Duration { /* compiled code */ }

    public operator func +(r: std.core.Duration): std.core.Duration { /* compiled code */ }

    public operator func -(r: std.core.Duration): std.core.Duration { /* compiled code */ }

    public operator func *(r: Int64): std.core.Duration { /* compiled code */ }

    public operator func *(r: Float64): std.core.Duration { /* compiled code */ }

    internal func toFloat64Nanoseconds(): Float64 { /* compiled code */ }

    public operator func /(r: Int64): std.core.Duration { /* compiled code */ }

    public operator func /(r: Float64): std.core.Duration { /* compiled code */ }

    public operator func /(r: std.core.Duration): Float64 { /* compiled code */ }

    public operator func ==(r: std.core.Duration): Bool { /* compiled code */ }

    public operator func !=(r: std.core.Duration): Bool { /* compiled code */ }

    public operator func >=(r: std.core.Duration): Bool { /* compiled code */ }

    public operator func >(r: std.core.Duration): Bool { /* compiled code */ }

    public operator func <=(r: std.core.Duration): Bool { /* compiled code */ }

    public operator func <(r: std.core.Duration): Bool { /* compiled code */ }

    public func compare(rhs: std.core.Duration): std.core.Ordering { /* compiled code */ }

    internal func toUnsignedNum(r: Int64): (UInt64, UInt32, UInt64, Bool) { /* compiled code */ }
}

public class Thread {
    internal static let exceptionHandler: std.core.AtomicBox<std.core.Option<(std.core.Thread, std.core.Exception) -> Unit>>

    private let _name: std.core.AtomicBox<std.core.String>

    private let _hasCancellation: std.core.AtomicBool

    private let _id: std.core.AtomicInt64

    internal var _threadLocalMap: std.core.Option<std.core.ThreadLocalMap>

    private var _rtCJThreadHandle: std.core.CPointer<Unit>

    private let _rtCJThreadHandleRefCount: std.core.AtomicInt64

    init() { /* compiled code */ }

    protected func setRuntimeCJThreadHandle(handle: std.core.CPointer<Unit>): Unit { /* compiled code */ }

    internal func waitForCJThreadHandle(): Unit { /* compiled code */ }

    internal func clearRuntimeCJThreadHandle(): Unit { /* compiled code */ }

    public static prop currentThread: std.core.Thread {
        get() { /* compiled code */ }
    }

    public prop id: Int64 {
        get() { /* compiled code */ }
    }

    public mut prop name: std.core.String {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop hasPendingCancellation: Bool {
        get() { /* compiled code */ }
    }

    internal func setCancellationRequest(): Unit { /* compiled code */ }

    public static func handleUncaughtExceptionBy(exHandler: (std.core.Thread, std.core.Exception) -> Unit): Unit { /* compiled code */ }
}

private open class FutureResult {
    init() { /* compiled code */ }
}

private class FutureResInit <: std.core.FutureResult {
    init() { /* compiled code */ }
}

public open class Object {
    init() { /* compiled code */ }
}

internal class MapIterator<T, R> <: std.core.Iterator<R> {
    private let transform: (T) -> R

    private let it: std.core.Iterator<T>

    @Frozen
    init(it: std.core.Iterator<T>, transform: (T) -> R) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<R> { /* compiled code */ }
}

internal class ZipIterator<T, R> <: std.core.Iterator<(T, R)> {
    private let itt: std.core.Iterator<T>

    private let itr: std.core.Iterator<R>

    init(itt: std.core.Iterator<T>, itr: std.core.Iterator<R>) { /* compiled code */ }

    public func next(): std.core.Option<(T, R)> { /* compiled code */ }
}

internal class EnumerateIterator<T> <: std.core.Iterator<(Int64, T)> {
    private var index: Int64 = COMPILED_CODE /* compiled code */

    private let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>) { /* compiled code */ }

    public func next(): std.core.Option<(Int64, T)> { /* compiled code */ }
}

internal class FilterIterator<T> <: std.core.Iterator<T> {
    private let predicate: (T) -> Bool

    private let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, predicate: (T) -> Bool) { /* compiled code */ }

    public func next(): std.core.Option<T> { /* compiled code */ }
}

internal class FilterMapIterator<T, R> <: std.core.Iterator<R> {
    private let transform: (T) -> std.core.Option<R>

    private let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, transform: (T) -> std.core.Option<R>) { /* compiled code */ }

    public func next(): std.core.Option<R> { /* compiled code */ }
}

internal class StepIterator<T> <: std.core.Iterator<T> {
    private let _count: Int64

    private let it: std.core.Iterator<T>

    private var index: Int64 = COMPILED_CODE /* compiled code */

    init(it: std.core.Iterator<T>, count: Int64) { /* compiled code */ }

    public func next(): std.core.Option<T> { /* compiled code */ }
}

internal class SkipIterator<T> <: std.core.Iterator<T> {
    private var _count: Int64

    private let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, count: Int64) { /* compiled code */ }

    public func next(): std.core.Option<T> { /* compiled code */ }
}

internal class TakeIterator<T> <: std.core.Iterator<T> {
    private var _count: Int64

    private let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, count: Int64) { /* compiled code */ }

    public func next(): std.core.Option<T> { /* compiled code */ }
}

internal class ConcatIterator<T> <: std.core.Iterator<T> {
    private let it: std.core.Iterator<T>

    private let other: std.core.Iterator<T>

    private var itOver: Bool = COMPILED_CODE /* compiled code */

    init(it: std.core.Iterator<T>, other: std.core.Iterator<T>) { /* compiled code */ }

    public func next(): std.core.Option<T> { /* compiled code */ }
}

internal class InspectIterator<T> <: std.core.Iterator<T> {
    private let action: (T) -> Unit

    private let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, action: (T) -> Unit) { /* compiled code */ }

    public func next(): std.core.Option<T> { /* compiled code */ }
}

internal class FlatMapIterator<T, R> <: std.core.Iterator<R> {
    private var subIt: std.core.Option<std.core.Iterator<R>> = COMPILED_CODE /* compiled code */

    private let transform: (T) -> std.core.Iterable<R>

    private let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>, transform: (T) -> std.core.Iterable<R>) { /* compiled code */ }

    public func next(): std.core.Option<R> { /* compiled code */ }
}

internal class IntersperseIterator<T> <: std.core.Iterator<T> {
    private let separator: T

    private let it: std.core.Iterator<T>

    private var item: std.core.Option<T>

    private var isInsert: Bool = COMPILED_CODE /* compiled code */

    init(it: std.core.Iterator<T>, separator: T) { /* compiled code */ }

    public func next(): std.core.Option<T> { /* compiled code */ }
}

public class NoneValueException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public open class Exception <: std.core.Object & std.core.ToString {
    private var detailMessage: std.core.String

    private var pcArray: std.core.Array<UInt64>

    private var traceElementArray: std.core.Array<std.core.StackTraceElement>

    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected open func getClassName(): std.core.String { /* compiled code */ }

    public open prop message: std.core.String {
        get() { /* compiled code */ }
    }

    public open func toString(): std.core.String { /* compiled code */ }

    public func printStackTrace(): Unit { /* compiled code */ }

    public func getStackTrace(): std.core.Array<std.core.StackTraceElement> { /* compiled code */ }
}

public interface Countable<T> {
    public open func next(right: Int64): T

    public open func position(): Int64
}

public class RangeIterator<T> <: std.core.Iterator<T> {
    private let _last: T

    private let _step: Int64

    private var cur: T

    private var hasNext: Bool

    init(first: T, last: T, step: Int64, isEmpty: Bool) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<T> { /* compiled code */ }
}

internal struct RunesToUtf8Bytes {
    public static func getBytes(runes: std.core.Array<Rune>): std.core.Array<UInt8> { /* compiled code */ }

    public static func getBytes(elements: std.core.Collection<Rune>): std.core.Array<UInt8> { /* compiled code */ }

    public static func getSize(runes: std.core.Array<Rune>): Int64 { /* compiled code */ }

    init() { /* compiled code */ }
}

internal foreign func memcpy_s(dest: std.core.CPointer<UInt8>, destMax: UIntNative, src: std.core.CPointer<UInt8>, count: UIntNative): Int32

internal func utf8RuneSize(arr: std.core.Array<UInt8>, start: Int64, length: Int64): Int64 { /* compiled code */ }

internal class StringByteIterator <: std.core.Iterator<UInt8> {
    @Frozen
    public func next(): std.core.Option<UInt8> { /* compiled code */ }

    internal let iteratorData: std.core.StringIteratorData

    @Frozen
    StringByteIterator(iteratorData: std.core.StringIteratorData) { /* compiled code */ }
}

internal class StringIteratorData {
    internal let myData: std.core.Array<UInt8>

    internal var end: Int64

    internal var nextPos: Int64

    init(data: std.core.Array<UInt8>) { /* compiled code */ }

    init(data: std.core.Array<UInt8>) { /* compiled code */ }
}

internal class StringRuneIterator <: std.core.Iterator<Rune> {
    @Frozen
    public func next(): std.core.Option<Rune> { /* compiled code */ }

    internal let iteratorData: std.core.StringIteratorData

    @Frozen
    StringRuneIterator(iteratorData: std.core.StringIteratorData) { /* compiled code */ }
}

internal class StringLineIterator <: std.core.Iterator<std.core.String> {
    @Frozen
    public func next(): std.core.Option<std.core.String> { /* compiled code */ }

    internal let iteratorData: std.core.StringIteratorData

    @Frozen
    StringLineIterator(iteratorData: std.core.StringIteratorData) { /* compiled code */ }
}

internal func wyhash(arr: std.core.Array<UInt8>, start: Int64, size: Int64, see: UInt64): UInt64 { /* compiled code */ }

internal func checkInvalid(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Unit { /* compiled code */ }

@Intrinsic
internal func vectorIndexByte32(arr: std.core.Array<UInt8>, offset: Int64, target: UInt8): Int64

internal foreign func CJ_CORE_CountString(orgStr: std.core.CPointer<UInt8>, subStr: std.core.CPointer<UInt8>, orgSize: Int64, subSize: Int64): Int64

let EMPTY_STRING_ARRAY: std.core.Array<std.core.String>

internal class StringLazySplitIterator <: std.core.Iterator<std.core.String> {
    private let orgData: std.core.Array<UInt8>

    private let separatorLen: Int64

    private var itemArr: std.core.Array<Int64>

    private var maxSplits: Int64

    private var removeEmpty: Bool

    private var resLen: Int64

    private var nextPos: Int64

    private var itemStart: Int64

    private var itemIndex: Int64

    private var itemTotal: Int64

    init(orgStrData: std.core.Array<UInt8>, orgStrSize: Int64, str: std.core.String, maxSplits: Int64, removeEmpty: Bool) { /* compiled code */ }

    public func next(): std.core.Option<std.core.String> { /* compiled code */ }
}

internal foreign func CJ_CORE_CountAndIndexString(orgStr: std.core.CPointer<UInt8>, subStr: std.core.CPointer<UInt8>, orgSize: Int64, subSize: Int64): std.core.CPointer<Int64>

public struct LibC {
    public static func malloc<T>(count!: Int64 = COMPILED_CODE /* compiled code */): std.core.CPointer<T> { /* compiled code */ }

    public static func free<T>(p: std.core.CPointer<T>): Unit { /* compiled code */ }

    public static func mallocCString(str: std.core.String): CString { /* compiled code */ }

    public static func free(cstr: CString): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

internal foreign func CJ_CORE_StringMemcmp(str1: std.core.CPointer<UInt8>, str2: std.core.CPointer<UInt8>, n: UIntNative): Int64

@Intrinsic
internal func vectorCompare32(arr1: std.core.Array<UInt8>, offset1: Int64, arr2: std.core.Array<UInt8>, offset2: Int64): Int64

@Intrinsic
internal func intrinsicRefEqRawArray<T>(a: std.core.Array<T>, b: std.core.Array<T>): Bool

internal foreign func CJ_CORE_LastIndexOfString(orgStr: std.core.CPointer<UInt8>, subStr: std.core.CPointer<UInt8>, orgSize: Int64, subSize: Int64, start: Int64): Int64

internal foreign func CJ_CORE_IndexOfString(orgStr: std.core.CPointer<UInt8>, subStr: std.core.CPointer<UInt8>, orgSize: Int64, subSize: Int64, start: Int64): Int64

internal func checkUtf8Of2BytesWithoutException(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Bool { /* compiled code */ }

internal func checkUtf8Of3BytesWithoutException(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Bool { /* compiled code */ }

internal func checkUtf8Of4BytesWithoutException(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Bool { /* compiled code */ }

internal func calcNumLen(num: UInt64): Int64 { /* compiled code */ }

internal func numberToString(number: UInt64, endIndex: Int64, buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

internal foreign func CJ_CORE_Float64ToCPointer(num: Float64): std.core.CPointer<UInt8>

internal foreign func strlen(str: std.core.CPointer<UInt8>): UIntNative

internal class ThreadLocalKey <: std.core.Object & std.core.Hashable {
    internal static const ALPHA: Int64 = COMPILED_CODE /* compiled code */

    internal static let CURR: std.core.AtomicInt64

    internal static func nextHash(): Int64 { /* compiled code */ }

    internal let code: Int64

    internal func equals(another: std.core.ThreadLocalKey): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    init() { /* compiled code */ }
}

public enum AnnotationKind {
    | Type

    | Parameter

    | Init

    | MemberProperty

    | MemberFunction

    | MemberVariable

    | EnumConstructor

    | GlobalFunction

    | GlobalVariable

    | Extension
}

public open class ArithmeticException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected open override func getClassName(): std.core.String { /* compiled code */ }
}

extend std.core.Array<std.core.Array<T>> {
    @Frozen
    public func flatten(): std.core.Array<T> { /* compiled code */ }
}

extend std.core.Array<T> <: Collection {
    @Frozen
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func toArray(): std.core.Array<T> { /* compiled code */ }
}

extend std.core.Array<T> <: ToString {
    @Frozen
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.core.Array<T> <: Equatable {
    @Frozen
    public operator func ==(that: std.core.Array<T>): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(that: std.core.Array<T>): Bool { /* compiled code */ }

    @Frozen
    public func contains(element: T): Bool { /* compiled code */ }

    @Frozen
    public func indexOf(element: T): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func indexOf(element: T, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func indexOf(elements: std.core.Array<T>): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func indexOf(elements: std.core.Array<T>, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func lastIndexOf(element: T): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func lastIndexOf(element: T, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func lastIndexOf(elements: std.core.Array<T>): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func lastIndexOf(elements: std.core.Array<T>, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    @Frozen
    public func trimStart(set: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func trimEnd(set: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func trimStart(predicate: (T) -> Bool): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func trimEnd(predicate: (T) -> Bool): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func removePrefix(prefix: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func removeSuffix(suffix: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }
}

@Intrinsic
internal func arrayClone<T>(rawArr: std.core.Array<T>): std.core.Array<T>

internal foreign func CJ_CORE_AtExitCallbackListLock(): Unit

internal foreign func CJ_CORE_AtExitCallbackListUnlook(): Unit

internal class AtexitCallbackNode {
    internal var atexitCallback: () -> Unit

    internal var next: std.core.Option<std.core.AtexitCallbackNode>

    init(callback: () -> Unit) { /* compiled code */ }
}

var atexitCallbackHead: std.core.Option<std.core.AtexitCallbackNode>

var callbacksRunningFlag: Bool

protected func CJ_CORE_AddAtexitCallback(callback: () -> Unit): Unit { /* compiled code */ }

protected func CJ_CORE_ExecAtexitCallbacks(): Unit { /* compiled code */ }

internal enum MemoryOrder {
    | SeqCst
}

@Intrinsic
internal func load<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, memoryOrder: std.core.MemoryOrder): TYPE

@Intrinsic
internal func store<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.core.MemoryOrder): Unit

@Intrinsic
internal func fetchAdd<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.core.MemoryOrder): TYPE

@Intrinsic
internal func compareAndSwap<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, oldValue: TYPE, newValue: TYPE, successOrder: std.core.MemoryOrder, failureOrder: std.core.MemoryOrder): Bool

internal class AtomicBool {
    private var value: Int8

    init(val: Bool) { /* compiled code */ }

    internal func load(): Bool { /* compiled code */ }

    internal func store(val: Bool): Unit { /* compiled code */ }
}

internal class AtomicInt64 {
    private var value: Int64

    init(val: Int64) { /* compiled code */ }

    internal func fetchAdd(val: Int64): Int64 { /* compiled code */ }

    internal func load(): Int64 { /* compiled code */ }

    internal func store(val: Int64): Unit { /* compiled code */ }

    internal func compareAndSwap(old: Int64, new: Int64): Bool { /* compiled code */ }
}

internal class AtomicReference<T> {
    private var value: T

    init(val: T) { /* compiled code */ }

    internal func load(): T { /* compiled code */ }

    internal func store(val: T): Unit { /* compiled code */ }
}

internal class AtomicBox<T> {
    private let boxRef: std.core.AtomicReference<std.core.Box<T>>

    init(val: T) { /* compiled code */ }

    internal func load(): T { /* compiled code */ }

    internal func store(val: T): Unit { /* compiled code */ }
}

extend std.core.Box<T> <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }
}

public interface Hashable {
    public open func hashCode(): Int64
}

extend std.core.Box<T> <: Comparable {
    public operator func ==(that: std.core.Box<T>): Bool { /* compiled code */ }

    public operator func !=(that: std.core.Box<T>): Bool { /* compiled code */ }

    public operator func >(that: std.core.Box<T>): Bool { /* compiled code */ }

    public operator func <(that: std.core.Box<T>): Bool { /* compiled code */ }

    public operator func >=(that: std.core.Box<T>): Bool { /* compiled code */ }

    public operator func <=(that: std.core.Box<T>): Bool { /* compiled code */ }

    public func compare(that: std.core.Box<T>): std.core.Ordering { /* compiled code */ }
}

extend std.core.Box<T> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend UInt8 {
    public func isAsciiLetter(): Bool { /* compiled code */ }

    public func isAsciiNumber(): Bool { /* compiled code */ }

    public func isAsciiHex(): Bool { /* compiled code */ }

    public func isAsciiOct(): Bool { /* compiled code */ }

    public func isAsciiPunctuation(): Bool { /* compiled code */ }

    public func isAsciiGraphic(): Bool { /* compiled code */ }

    public func isAsciiControl(): Bool { /* compiled code */ }

    public func isAsciiNumberOrLetter(): Bool { /* compiled code */ }

    public func isAsciiLowerCase(): Bool { /* compiled code */ }

    public func isAsciiUpperCase(): Bool { /* compiled code */ }

    public func isAscii(): Bool { /* compiled code */ }

    public func isAsciiWhiteSpace(): Bool { /* compiled code */ }

    public func toAsciiUpperCase(): UInt8 { /* compiled code */ }

    public func toAsciiLowerCase(): UInt8 { /* compiled code */ }
}

@Intrinsic
internal func getPointerAddress<T>(c: std.core.CPointer<T>): UIntNative

@Intrinsic
internal func addPointer<T>(c: std.core.CPointer<T>, offset: Int64): std.core.CPointer<T>

extend std.core.CPointer<T> {
    public func isNull(): Bool { /* compiled code */ }

    public func isNotNull(): Bool { /* compiled code */ }

    public func toUIntNative(): UIntNative { /* compiled code */ }

    @Frozen
    public func read(): T { /* compiled code */ }

    @Frozen
    public func write(value: T): Unit { /* compiled code */ }

    @Frozen
    public func read(idx: Int64): T { /* compiled code */ }

    @Frozen
    public func write(idx: Int64, value: T): Unit { /* compiled code */ }

    public operator func +(offset: Int64): std.core.CPointer<T> { /* compiled code */ }

    public operator func -(offset: Int64): std.core.CPointer<T> { /* compiled code */ }

    public func asResource(): std.core.CPointerResource<T> { /* compiled code */ }
}

public struct CPointerResource<T> <: std.core.Resource {
    public let value: std.core.CPointer<T>

    private let isFree: std.core.Box<Bool> = COMPILED_CODE /* compiled code */

    init(p: std.core.CPointer<T>) { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }

    public func close(): Unit { /* compiled code */ }
}

public interface Resource {
    public open func isClosed(): Bool

    public open func close(): Unit
}

internal foreign func strcmp(source: std.core.CPointer<UInt8>, target: std.core.CPointer<UInt8>): Int32

internal foreign func strncmp(source: std.core.CPointer<UInt8>, target: std.core.CPointer<UInt8>, cmpSize: UIntNative): Int32

internal foreign func strcasecmp(source: std.core.CPointer<UInt8>, target: std.core.CPointer<UInt8>): Int32

@Intrinsic
internal func convertCStr2Ptr(cs: CString): std.core.CPointer<UInt8>

internal func createStrFromCPointer(cp: std.core.CPointer<UInt8>): std.core.String { /* compiled code */ }

extend CString <: ToString {
    public func getChars(): std.core.CPointer<UInt8> { /* compiled code */ }

    public func isNull(): Bool { /* compiled code */ }

    public func size(): Int64 { /* compiled code */ }

    public func isEmpty(): Bool { /* compiled code */ }

    public func isNotEmpty(): Bool { /* compiled code */ }

    public func startsWith(prefix: CString): Bool { /* compiled code */ }

    public func endsWith(suffix: CString): Bool { /* compiled code */ }

    public func equals(rhs: CString): Bool { /* compiled code */ }

    public func equalsLower(rhs: CString): Bool { /* compiled code */ }

    public func subCString(beginIndex: UIntNative): CString { /* compiled code */ }

    public func subCString(beginIndex: UIntNative, subLen: UIntNative): CString { /* compiled code */ }

    public func compare(str: CString): Int32 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func asResource(): std.core.CStringResource { /* compiled code */ }
}

public struct CStringResource <: std.core.Resource {
    public let value: CString

    private let isFree: std.core.Box<Bool>

    init(p: CString) { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }

    public func close(): Unit { /* compiled code */ }
}

extend Rune {
    public func isAsciiLetter(): Bool { /* compiled code */ }

    public func isAsciiNumber(): Bool { /* compiled code */ }

    public func isAsciiHex(): Bool { /* compiled code */ }

    public func isAsciiOct(): Bool { /* compiled code */ }

    public func isAsciiPunctuation(): Bool { /* compiled code */ }

    public func isAsciiGraphic(): Bool { /* compiled code */ }

    public func isAsciiControl(): Bool { /* compiled code */ }

    public func isAsciiNumberOrLetter(): Bool { /* compiled code */ }

    public func isAsciiLowerCase(): Bool { /* compiled code */ }

    public func isAsciiUpperCase(): Bool { /* compiled code */ }

    public func isAscii(): Bool { /* compiled code */ }

    public func isAsciiWhiteSpace(): Bool { /* compiled code */ }

    public func toAsciiUpperCase(): Rune { /* compiled code */ }

    public func toAsciiLowerCase(): Rune { /* compiled code */ }

    public static func utf8Size(arr: std.core.Array<UInt8>, index: Int64): Int64 { /* compiled code */ }

    internal static func utf8SizeUnchecked(byte: UInt8): Int64 { /* compiled code */ }

    public static func utf8Size(c: Rune): Int64 { /* compiled code */ }

    public static func fromUtf8(arr: std.core.Array<UInt8>, index: Int64): (Rune, Int64) { /* compiled code */ }

    internal static func fromUtf8(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): (Rune, Int64) { /* compiled code */ }

    public static func getPreviousFromUtf8(arr: std.core.Array<UInt8>, index: Int64): (Rune, Int64) { /* compiled code */ }

    public static func intoUtf8Array(c: Rune, arr: std.core.Array<UInt8>, index: Int64): Int64 { /* compiled code */ }
}

extend Rune <: Comparable {
    public func compare(rhs: Rune): std.core.Ordering { /* compiled code */ }
}

@Intrinsic
internal func Int64Less(a: Int64, b: Int64): Bool

@Intrinsic
internal func Int64Greater(a: Int64, b: Int64): Bool

@Intrinsic
internal func Int64LessOrEqual(a: Int64, b: Int64): Bool

@Intrinsic
internal func Int64GreaterOrEqual(a: Int64, b: Int64): Bool

@Intrinsic
internal func Int64Equal(a: Int64, b: Int64): Bool

@Intrinsic
internal func Int64NotEqual(a: Int64, b: Int64): Bool

@Intrinsic
internal func Int32Less(a: Int32, b: Int32): Bool

@Intrinsic
internal func Int32Greater(a: Int32, b: Int32): Bool

@Intrinsic
internal func Int32LessOrEqual(a: Int32, b: Int32): Bool

@Intrinsic
internal func Int32GreaterOrEqual(a: Int32, b: Int32): Bool

@Intrinsic
internal func Int32Equal(a: Int32, b: Int32): Bool

@Intrinsic
internal func Int32NotEqual(a: Int32, b: Int32): Bool

@Intrinsic
internal func Int16Less(a: Int16, b: Int16): Bool

@Intrinsic
internal func Int16Greater(a: Int16, b: Int16): Bool

@Intrinsic
internal func Int16LessOrEqual(a: Int16, b: Int16): Bool

@Intrinsic
internal func Int16GreaterOrEqual(a: Int16, b: Int16): Bool

@Intrinsic
internal func Int16Equal(a: Int16, b: Int16): Bool

@Intrinsic
internal func Int16NotEqual(a: Int16, b: Int16): Bool

@Intrinsic
internal func Int8Less(a: Int8, b: Int8): Bool

@Intrinsic
internal func Int8Greater(a: Int8, b: Int8): Bool

@Intrinsic
internal func Int8LessOrEqual(a: Int8, b: Int8): Bool

@Intrinsic
internal func Int8GreaterOrEqual(a: Int8, b: Int8): Bool

@Intrinsic
internal func Int8Equal(a: Int8, b: Int8): Bool

@Intrinsic
internal func Int8NotEqual(a: Int8, b: Int8): Bool

@Intrinsic
internal func UInt64Less(a: UInt64, b: UInt64): Bool

@Intrinsic
internal func UInt64Greater(a: UInt64, b: UInt64): Bool

@Intrinsic
internal func UInt64LessOrEqual(a: UInt64, b: UInt64): Bool

@Intrinsic
internal func UInt64GreaterOrEqual(a: UInt64, b: UInt64): Bool

@Intrinsic
internal func UInt64Equal(a: UInt64, b: UInt64): Bool

@Intrinsic
internal func UInt64NotEqual(a: UInt64, b: UInt64): Bool

@Intrinsic
internal func UInt32Less(a: UInt32, b: UInt32): Bool

@Intrinsic
internal func UInt32Greater(a: UInt32, b: UInt32): Bool

@Intrinsic
internal func UInt32LessOrEqual(a: UInt32, b: UInt32): Bool

@Intrinsic
internal func UInt32GreaterOrEqual(a: UInt32, b: UInt32): Bool

@Intrinsic
internal func UInt32Equal(a: UInt32, b: UInt32): Bool

@Intrinsic
internal func UInt32NotEqual(a: UInt32, b: UInt32): Bool

@Intrinsic
internal func UInt16Less(a: UInt16, b: UInt16): Bool

@Intrinsic
internal func UInt16Greater(a: UInt16, b: UInt16): Bool

@Intrinsic
internal func UInt16LessOrEqual(a: UInt16, b: UInt16): Bool

@Intrinsic
internal func UInt16GreaterOrEqual(a: UInt16, b: UInt16): Bool

@Intrinsic
internal func UInt16Equal(a: UInt16, b: UInt16): Bool

@Intrinsic
internal func UInt16NotEqual(a: UInt16, b: UInt16): Bool

@Intrinsic
internal func UInt8Less(a: UInt8, b: UInt8): Bool

@Intrinsic
internal func UInt8Greater(a: UInt8, b: UInt8): Bool

@Intrinsic
internal func UInt8LessOrEqual(a: UInt8, b: UInt8): Bool

@Intrinsic
internal func UInt8GreaterOrEqual(a: UInt8, b: UInt8): Bool

@Intrinsic
internal func UInt8Equal(a: UInt8, b: UInt8): Bool

@Intrinsic
internal func UInt8NotEqual(a: UInt8, b: UInt8): Bool

@Intrinsic
internal func Float16Less(a: Float16, b: Float16): Bool

@Intrinsic
internal func Float16Greater(a: Float16, b: Float16): Bool

@Intrinsic
internal func Float16LessOrEqual(a: Float16, b: Float16): Bool

@Intrinsic
internal func Float16GreaterOrEqual(a: Float16, b: Float16): Bool

@Intrinsic
internal func Float16Equal(a: Float16, b: Float16): Bool

@Intrinsic
internal func Float16NotEqual(a: Float16, b: Float16): Bool

@Intrinsic
internal func Float32Less(a: Float32, b: Float32): Bool

@Intrinsic
internal func Float32Greater(a: Float32, b: Float32): Bool

@Intrinsic
internal func Float32LessOrEqual(a: Float32, b: Float32): Bool

@Intrinsic
internal func Float32GreaterOrEqual(a: Float32, b: Float32): Bool

@Intrinsic
internal func Float32Equal(a: Float32, b: Float32): Bool

@Intrinsic
internal func Float32NotEqual(a: Float32, b: Float32): Bool

@Intrinsic
internal func Float64Less(a: Float64, b: Float64): Bool

@Intrinsic
internal func Float64Greater(a: Float64, b: Float64): Bool

@Intrinsic
internal func Float64LessOrEqual(a: Float64, b: Float64): Bool

@Intrinsic
internal func Float64GreaterOrEqual(a: Float64, b: Float64): Bool

@Intrinsic
internal func Float64Equal(a: Float64, b: Float64): Bool

@Intrinsic
internal func Float64NotEqual(a: Float64, b: Float64): Bool

public interface Less<T> {
    public open operator func <(rhs: T): Bool
}

public interface Greater<T> {
    public open operator func >(rhs: T): Bool
}

public interface LessOrEqual<T> {
    public open operator func <=(rhs: T): Bool
}

public interface GreaterOrEqual<T> {
    public open operator func >=(rhs: T): Bool
}

extend std.core.Ordering <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.core.Ordering <: Comparable {
    public func compare(that: std.core.Ordering): std.core.Ordering { /* compiled code */ }
}

extend IntNative <: Comparable {
    public func compare(rhs: IntNative): std.core.Ordering { /* compiled code */ }
}

extend Int64 <: Comparable {
    public func compare(rhs: Int64): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: Int64): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: Int64): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: Int64): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: Int64): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: Int64): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: Int64): Bool { /* compiled code */ }
}

extend Int32 <: Comparable {
    public func compare(rhs: Int32): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: Int32): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: Int32): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: Int32): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: Int32): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: Int32): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: Int32): Bool { /* compiled code */ }
}

extend Int16 <: Comparable {
    public func compare(rhs: Int16): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: Int16): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: Int16): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: Int16): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: Int16): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: Int16): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: Int16): Bool { /* compiled code */ }
}

extend Int8 <: Comparable {
    public func compare(rhs: Int8): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: Int8): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: Int8): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: Int8): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: Int8): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: Int8): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: Int8): Bool { /* compiled code */ }
}

extend UIntNative <: Comparable {
    public func compare(rhs: UIntNative): std.core.Ordering { /* compiled code */ }
}

extend UInt64 <: Comparable {
    public func compare(rhs: UInt64): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: UInt64): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: UInt64): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: UInt64): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: UInt64): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: UInt64): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: UInt64): Bool { /* compiled code */ }
}

extend UInt32 <: Comparable {
    public func compare(rhs: UInt32): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: UInt32): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: UInt32): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: UInt32): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: UInt32): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: UInt32): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: UInt32): Bool { /* compiled code */ }
}

extend UInt16 <: Comparable {
    public func compare(rhs: UInt16): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: UInt16): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: UInt16): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: UInt16): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: UInt16): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: UInt16): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: UInt16): Bool { /* compiled code */ }
}

extend UInt8 <: Comparable {
    public func compare(rhs: UInt8): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: UInt8): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: UInt8): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: UInt8): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: UInt8): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: UInt8): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: UInt8): Bool { /* compiled code */ }
}

extend Float16 <: Comparable {
    public func compare(rhs: Float16): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: Float16): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: Float16): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: Float16): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: Float16): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: Float16): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: Float16): Bool { /* compiled code */ }
}

extend Float32 <: Comparable {
    public func compare(rhs: Float32): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: Float32): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: Float32): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: Float32): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: Float32): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: Float32): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: Float32): Bool { /* compiled code */ }
}

extend Float64 <: Comparable {
    public func compare(rhs: Float64): std.core.Ordering { /* compiled code */ }

    @Frozen
    public operator func <(rhs: Float64): Bool { /* compiled code */ }

    @Frozen
    public operator func >(rhs: Float64): Bool { /* compiled code */ }

    @Frozen
    public operator func <=(rhs: Float64): Bool { /* compiled code */ }

    @Frozen
    public operator func >=(rhs: Float64): Bool { /* compiled code */ }

    @Frozen
    public operator func ==(rhs: Float64): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(rhs: Float64): Bool { /* compiled code */ }
}

extend IntNative <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): IntNative { /* compiled code */ }
}

extend Int64 <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): Int64 { /* compiled code */ }
}

extend Int32 <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): Int32 { /* compiled code */ }
}

extend Int16 <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): Int16 { /* compiled code */ }
}

extend Int8 <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): Int8 { /* compiled code */ }
}

extend UIntNative <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): UIntNative { /* compiled code */ }
}

extend UInt64 <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): UInt64 { /* compiled code */ }
}

extend UInt32 <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): UInt32 { /* compiled code */ }
}

extend UInt16 <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): UInt16 { /* compiled code */ }
}

extend UInt8 <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): UInt8 { /* compiled code */ }
}

extend Rune <: Countable {
    public func position(): Int64 { /* compiled code */ }

    public func next(right: Int64): Rune { /* compiled code */ }
}

let MAX_NS_FLOAT64: Float64

let MIN_NS_FLOAT64: Float64

internal func durMul(secs: UInt64, nanos: UInt32, multiplier: UInt64, sign: Bool): std.core.Option<std.core.Duration> { /* compiled code */ }

internal func u32MulU64(thisInt: UInt32, thatInt: UInt64): (UInt64, UInt32) { /* compiled code */ }

internal func tryU64Mul(left: UInt64, right: UInt64): std.core.Option<UInt64> { /* compiled code */ }

internal func tryU64Add(left: UInt64, right: UInt64): std.core.Option<UInt64> { /* compiled code */ }

internal func checkSub(left: Int64, right: Int64): Bool { /* compiled code */ }

internal func toSecNsBase(num: UInt64): (UInt32, UInt32, UInt32) { /* compiled code */ }

internal func durDiv(secs: UInt64, nanos: UInt32, divisor: UInt64): (Int64, Int64) { /* compiled code */ }

internal func getDifference(num3: Int64, num2: Int64, num1: Int64, num0: Int64): (Int64, Int64, Int64, Int64) { /* compiled code */ }

internal func getDifference(num2: Int64, num1: Int64, num0: Int64): (UInt32, UInt32, UInt32) { /* compiled code */ }

internal func u32TwoMul(num1: UInt32, num0: UInt32, multiplier: UInt32): (UInt32, UInt32, UInt32) { /* compiled code */ }

internal func u32ThreeMul(num2: UInt32, num1: UInt32, num0: UInt32, multiplier: UInt32): (UInt32, UInt32, UInt32, UInt32) { /* compiled code */ }

internal func u32DividendMul(num3: UInt32, num2: UInt32, num1: UInt32, num0: UInt32, times: UInt32): (UInt32, UInt32, UInt32, UInt32, UInt32) { /* compiled code */ }

internal func divAndMod(dividend: UInt64, dividend2: UInt32, divisor2: UInt32, divisor1: UInt32): UInt32 { /* compiled code */ }

extend Int64 {
    public operator func *(r: std.core.Duration): std.core.Duration { /* compiled code */ }
}

extend Int64 {
    internal operator func -(r: UInt32): Int64 { /* compiled code */ }
}

extend UInt64 {
    internal operator func *(r: UInt32): UInt64 { /* compiled code */ }

    internal operator func /(r: UInt32): UInt64 { /* compiled code */ }

    internal operator func %(r: UInt32): UInt32 { /* compiled code */ }
}

extend UInt32 {
    internal operator func *(r: UInt64): UInt64 { /* compiled code */ }
}

extend Float64 {
    public operator func *(r: std.core.Duration): std.core.Duration { /* compiled code */ }
}

extend std.core.StringBuilder {
    internal func append(num: Int64, unit: std.core.String): std.core.StringBuilder { /* compiled code */ }
}

internal func isSumOverflow(left: Int64, right: Int64): Bool { /* compiled code */ }

public enum Endian {
    | Big

    | Little

    public static prop Platform: std.core.Endian {
        get() { /* compiled code */ }
    }
}

internal foreign func CJ_PlatformEndian(): UInt8

public interface NotEqual<T> {
    public open operator func !=(rhs: T): Bool
}

extend Bool <: Equatable {
    public operator func ==(right: Bool): Bool { /* compiled code */ }
}

extend Unit <: Equatable {
    public operator func ==(right: Unit): Bool { /* compiled code */ }
}

extend std.core.Range<T> <: Equatable {
    public operator func ==(that: std.core.Range<T>): Bool { /* compiled code */ }
}

public open class Error <: std.core.Object & std.core.ToString {
    private var detailMessage: std.core.String

    internal var pcArray: std.core.Array<UInt64>

    internal var traceElementArray: std.core.Array<std.core.StackTraceElement>

    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected open func getClassName(): std.core.String { /* compiled code */ }

    public open prop message: std.core.String {
        get() { /* compiled code */ }
    }

    public open func toString(): std.core.String { /* compiled code */ }

    public open func printStackTrace(): Unit { /* compiled code */ }

    public open func getStackTraceMessage(): std.core.String { /* compiled code */ }

    public func getStackTrace(): std.core.Array<std.core.StackTraceElement> { /* compiled code */ }
}

public open class StackTraceElement {
    public let declaringClass: std.core.String

    public let methodName: std.core.String

    public let fileName: std.core.String

    public let lineNumber: Int64

    init(declaringClass: std.core.String, methodName: std.core.String, fileName: std.core.String, lineNumber: Int64) { /* compiled code */ }
}

@Intrinsic
internal func fillInStackTrace(pc: std.core.Array<UInt64>, message: std.core.Array<UInt8>): std.core.Array<UInt64>

@Intrinsic
internal func decodeStackTrace(pc: UInt64, funcStart: UInt64, funcDesc: UInt64): std.core.StackTraceData

internal struct StackTraceData {
    internal let className: std.core.Array<UInt8>

    internal let methodName: std.core.Array<UInt8>

    internal let fileName: std.core.Array<UInt8>

    internal let lineNumber: Int64

    init() { /* compiled code */ }
}

internal func getStackTraceImpl(pcArray: std.core.Array<UInt64>): std.core.Array<std.core.StackTraceElement> { /* compiled code */ }

@Intrinsic
internal func bitCast<From, To>(val: From): To

internal interface BitCastRange {
}

extend Float64 {
    public static func fromBits(bits: UInt64): Float64 { /* compiled code */ }

    public func toBits(): UInt64 { /* compiled code */ }
}

extend Float32 {
    public static func fromBits(bits: UInt32): Float32 { /* compiled code */ }

    public func toBits(): UInt32 { /* compiled code */ }
}

extend Float16 {
    public static func fromBits(bits: UInt16): Float16 { /* compiled code */ }

    public func toBits(): UInt16 { /* compiled code */ }
}

extend UInt64 <: BitCastRange {
}

extend Float64 <: BitCastRange {
}

extend UInt32 <: BitCastRange {
}

extend Float32 <: BitCastRange {
}

extend UInt16 <: BitCastRange {
}

extend Float16 <: BitCastRange {
}

internal func composition<T1, T2, T3>(f: (T1) -> T2, g: (T2) -> T3): T3 { /* compiled code */ }

@Intrinsic
internal func futureInit<T>(future: std.core.Future<T>): Unit

public class Future<T> {
    private let _thread: std.core.Thread = COMPILED_CODE /* compiled code */

    private var result: std.core.FutureResult = COMPILED_CODE /* compiled code */

    private var executeFn: () -> T

    init(fn: () -> T) { /* compiled code */ }

    public prop thread: std.core.Thread {
        get() { /* compiled code */ }
    }

    public func get(): T { /* compiled code */ }

    public func tryGet(): std.core.Option<T> { /* compiled code */ }

    public func get(timeout: std.core.Duration): T { /* compiled code */ }

    public func cancel(): Unit { /* compiled code */ }

    internal func execute(): Unit { /* compiled code */ }

    internal static func executeClosure(fn: () -> T): Unit { /* compiled code */ }
}

@Intrinsic
internal func futureIsComplete<T>(future: std.core.Future<T>): Bool

@Intrinsic
internal func futureWait<T>(future: std.core.Future<T>, timeout: Int64): Unit

@Intrinsic
internal func futureNotifyAll<T>(future: std.core.Future<T>): Unit

internal foreign func CJ_CORE_PrintOomHint(): Unit

let INT64_MAX: Int64

private class FutureOk<T> <: std.core.FutureResult {
    internal var value: T

    init(v: T) { /* compiled code */ }
}

private class FutureErr <: std.core.FutureResult {
    internal static let cachedResult: std.core.FutureErr

    internal var err: std.core.Error

    init(e: std.core.Error) { /* compiled code */ }
}

private class FutureExcept <: std.core.FutureResult {
    internal var except: std.core.Exception

    init(e: std.core.Exception) { /* compiled code */ }
}

public interface ThreadContext {
    public open func end(): Unit

    public open func hasEnded(): Bool
}

extend std.core.Ordering <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }
}

extend Unit <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }
}

extend Bool <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }
}

extend Rune <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }
}

internal func i32tof32(num: Int32): Float32 { /* compiled code */ }

internal func f32toi32(num: Float32): Int32 { /* compiled code */ }

internal func i64tof64(num: Int64): Float64 { /* compiled code */ }

internal func f642i64(num: Float64): Int64 { /* compiled code */ }

extend IntNative <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop Max: IntNative {
        get() { /* compiled code */ }
    }

    public static prop Min: IntNative {
        get() { /* compiled code */ }
    }
}

extend Int64 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop Max: Int64 {
        get() { /* compiled code */ }
    }

    public static prop Min: Int64 {
        get() { /* compiled code */ }
    }
}

extend Int32 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop Max: Int32 {
        get() { /* compiled code */ }
    }

    public static prop Min: Int32 {
        get() { /* compiled code */ }
    }
}

extend Int16 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop Max: Int16 {
        get() { /* compiled code */ }
    }

    public static prop Min: Int16 {
        get() { /* compiled code */ }
    }
}

extend Int8 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop Max: Int8 {
        get() { /* compiled code */ }
    }

    public static prop Min: Int8 {
        get() { /* compiled code */ }
    }
}

extend UIntNative <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop Max: UIntNative {
        get() { /* compiled code */ }
    }

    public static prop Min: UIntNative {
        get() { /* compiled code */ }
    }
}

extend UInt64 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop Max: UInt64 {
        get() { /* compiled code */ }
    }

    public static prop Min: UInt64 {
        get() { /* compiled code */ }
    }
}

extend UInt32 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop Max: UInt32 {
        get() { /* compiled code */ }
    }

    public static prop Min: UInt32 {
        get() { /* compiled code */ }
    }
}

extend UInt16 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop Max: UInt16 {
        get() { /* compiled code */ }
    }

    public static prop Min: UInt16 {
        get() { /* compiled code */ }
    }
}

extend UInt8 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop Max: UInt8 {
        get() { /* compiled code */ }
    }

    public static prop Min: UInt8 {
        get() { /* compiled code */ }
    }
}

extend Float64 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop NaN: Float64 {
        get() { /* compiled code */ }
    }

    public static prop Inf: Float64 {
        get() { /* compiled code */ }
    }

    public static prop Max: Float64 {
        get() { /* compiled code */ }
    }

    public static prop Min: Float64 {
        get() { /* compiled code */ }
    }

    public static prop MinDenormal: Float64 {
        get() { /* compiled code */ }
    }

    public static prop MinNormal: Float64 {
        get() { /* compiled code */ }
    }

    public func isInf(): Bool { /* compiled code */ }

    public func isNaN(): Bool { /* compiled code */ }

    public func isNormal(): Bool { /* compiled code */ }

    public static func max(a: Float64, b: Float64, others: std.core.Array<Float64>): Float64 { /* compiled code */ }

    public static func min(a: Float64, b: Float64, others: std.core.Array<Float64>): Float64 { /* compiled code */ }
}

extend Float32 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop NaN: Float32 {
        get() { /* compiled code */ }
    }

    public static prop Inf: Float32 {
        get() { /* compiled code */ }
    }

    public static prop Max: Float32 {
        get() { /* compiled code */ }
    }

    public static prop Min: Float32 {
        get() { /* compiled code */ }
    }

    public static prop MinDenormal: Float32 {
        get() { /* compiled code */ }
    }

    public static prop MinNormal: Float32 {
        get() { /* compiled code */ }
    }

    public func isInf(): Bool { /* compiled code */ }

    public func isNaN(): Bool { /* compiled code */ }

    public func isNormal(): Bool { /* compiled code */ }

    public static func max(a: Float32, b: Float32, others: std.core.Array<Float32>): Float32 { /* compiled code */ }

    public static func min(a: Float32, b: Float32, others: std.core.Array<Float32>): Float32 { /* compiled code */ }
}

extend Float16 <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    public static prop NaN: Float16 {
        get() { /* compiled code */ }
    }

    public static prop Inf: Float16 {
        get() { /* compiled code */ }
    }

    public static prop Max: Float16 {
        get() { /* compiled code */ }
    }

    public static prop Min: Float16 {
        get() { /* compiled code */ }
    }

    public static prop MinDenormal: Float16 {
        get() { /* compiled code */ }
    }

    public static prop MinNormal: Float16 {
        get() { /* compiled code */ }
    }

    public func isInf(): Bool { /* compiled code */ }

    public func isNaN(): Bool { /* compiled code */ }

    public func isNormal(): Bool { /* compiled code */ }

    public static func max(a: Float16, b: Float16, others: std.core.Array<Float16>): Float16 { /* compiled code */ }

    public static func min(a: Float16, b: Float16, others: std.core.Array<Float16>): Float16 { /* compiled code */ }
}

extend std.core.Range<T> <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }
}

public interface Hasher {
    public open func finish(): Int64

    public open mut func reset(): Unit

    public open mut func write(value: Bool): Unit

    public open mut func write(value: Rune): Unit

    public open mut func write(value: Int8): Unit

    public open mut func write(value: Int16): Unit

    public open mut func write(value: Int32): Unit

    public open mut func write(value: Int64): Unit

    public open mut func write(value: UInt8): Unit

    public open mut func write(value: UInt16): Unit

    public open mut func write(value: UInt32): Unit

    public open mut func write(value: UInt64): Unit

    public open mut func write(value: Float16): Unit

    public open mut func write(value: Float32): Unit

    public open mut func write(value: Float64): Unit

    public open mut func write(value: std.core.String): Unit
}

public struct DefaultHasher <: std.core.Hasher {
    private var res: Int64

    public func finish(): Int64 { /* compiled code */ }

    public mut func reset(): Unit { /* compiled code */ }

    public mut func write(value: Bool): Unit { /* compiled code */ }

    public mut func write(value: Rune): Unit { /* compiled code */ }

    public mut func write(value: Int8): Unit { /* compiled code */ }

    public mut func write(value: Int16): Unit { /* compiled code */ }

    public mut func write(value: Int32): Unit { /* compiled code */ }

    public mut func write(value: Int64): Unit { /* compiled code */ }

    public mut func write(value: UInt8): Unit { /* compiled code */ }

    public mut func write(value: UInt16): Unit { /* compiled code */ }

    public mut func write(value: UInt32): Unit { /* compiled code */ }

    public mut func write(value: UInt64): Unit { /* compiled code */ }

    public mut func write(value: Float16): Unit { /* compiled code */ }

    public mut func write(value: Float32): Unit { /* compiled code */ }

    public mut func write(value: Float64): Unit { /* compiled code */ }

    public mut func write(value: std.core.String): Unit { /* compiled code */ }

    init(res!: Int64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal func wyrand(seed: Int64): Int64 { /* compiled code */ }

internal func mix(A: UInt64, B: UInt64): UInt64 { /* compiled code */ }

internal func r4(arr: std.core.Array<UInt8>, pos: Int64): UInt64 { /* compiled code */ }

internal func r8(arr: std.core.Array<UInt8>, pos: Int64): UInt64 { /* compiled code */ }

public open class IllegalFormatException <: std.core.IllegalArgumentException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class IllegalMemoryException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class IllegalStateException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class IncompatiblePackageException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

extend std.core.Iterator<T> {
    @Frozen
    public func contains(element: T): Bool { /* compiled code */ }
}

extend std.core.Iterator<T> {
    public func max(): std.core.Option<T> { /* compiled code */ }

    public func min(): std.core.Option<T> { /* compiled code */ }
}

extend std.core.Iterator<T> {
    @Frozen
    public func zip<R>(it: std.core.Iterator<R>): std.core.Iterator<(T, R)> { /* compiled code */ }

    @Frozen
    public func enumerate(): std.core.Iterator<(Int64, T)> { /* compiled code */ }

    @Frozen
    public func filter(predicate: (T) -> Bool): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    public func filterMap<R>(transform: (T) -> std.core.Option<R>): std.core.Iterator<R> { /* compiled code */ }

    @Frozen
    public func map<R>(transform: (T) -> R): std.core.Iterator<R> { /* compiled code */ }

    @Frozen
    public func step(count: Int64): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    public func skip(count: Int64): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    public func take(count: Int64): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    public func concat(other: std.core.Iterator<T>): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    public func inspect(action: (T) -> Unit): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    public func flatMap<R>(transform: (T) -> std.core.Iterator<R>): std.core.Iterator<R> { /* compiled code */ }

    @Frozen
    public func intersperse(separator: T): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    public func all(predicate: (T) -> Bool): Bool { /* compiled code */ }

    @Frozen
    public func any(predicate: (T) -> Bool): Bool { /* compiled code */ }

    @Frozen
    public func none(predicate: (T) -> Bool): Bool { /* compiled code */ }

    @Frozen
    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func count(): Int64 { /* compiled code */ }

    @Frozen
    public func forEach(action: (T) -> Unit): Unit { /* compiled code */ }

    @Frozen
    public func first(): std.core.Option<T> { /* compiled code */ }

    @Frozen
    public func last(): std.core.Option<T> { /* compiled code */ }

    @Frozen
    public func at(n: Int64): std.core.Option<T> { /* compiled code */ }

    @Frozen
    public func reduce(operation: (T, T) -> T): std.core.Option<T> { /* compiled code */ }

    @Frozen
    public func fold<R>(initial: R, operation: (R, T) -> R): R { /* compiled code */ }
}

@Intrinsic
internal func sizeOfType<T>(): UIntNative

@Intrinsic
internal func alignOfType<T>(): UIntNative

internal foreign func malloc(size: UIntNative): std.core.CPointer<Unit>

internal foreign func free(p: std.core.CPointer<Unit>): Unit

let SECUREC_MEM_MAX_LEN: Int64

public func sizeOf<T>(): UIntNative { /* compiled code */ }

public func alignOf<T>(): UIntNative { /* compiled code */ }

internal func getCommandLineArgsFromRuntime(): std.core.CPointer<std.core.CPointer<UInt8>> { /* compiled code */ }

internal func getCommandLineArgs(): std.core.Array<std.core.String> { /* compiled code */ }

internal foreign func CJ_CORE_Float64ToHash(x: Float64): Int64

internal foreign func CJ_CORE_Float32ToHash(x: Float32): Int64

internal foreign func CJ_CONSOLE_Readline(): std.core.CPointer<std.core.StructureString>

internal struct StructureString {
    internal let buffer: std.core.CPointer<UInt8>

    internal let length: UIntNative

    init() { /* compiled code */ }
}

internal foreign func CJ_CONSOLE_ClearstdInerr(): Unit

internal foreign func CJ_CORE_StringSize(str: std.core.CPointer<UInt8>, len: Int64): Int64

internal foreign func CJ_CORE_IndexOfChar(orgStr: std.core.CPointer<UInt8>, patC: UInt32, orgSize: Int64, start: Int64): Int64

internal foreign func CJ_CORE_LastIndexOfChar(orgStr: std.core.CPointer<UInt8>, patC: UInt32, orgSize: Int64, start: Int64): Int64

internal foreign func CJ_CORE_CountChar(orgStr: std.core.CPointer<UInt8>, patC: UInt32, orgSize: Int64): Int64

internal foreign func CJ_MRT_GetCommandLineArgs(): std.core.CPointer<std.core.CPointer<UInt8>>

public class NegativeArraySizeException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public interface Any {
}

public func refEq(a: std.core.Object, b: std.core.Object): Bool { /* compiled code */ }

@Intrinsic
internal func intrinsicRefEq(a: std.core.Object, b: std.core.Object): Bool

public func ifSome<T>(o: std.core.Option<T>, action: (T) -> Unit): Unit { /* compiled code */ }

public func ifNone<T>(o: std.core.Option<T>, action: () -> Unit): Unit { /* compiled code */ }

extend std.core.Option<T> <: Equatable {
    @Frozen
    public operator func ==(that: std.core.Option<T>): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(that: std.core.Option<T>): Bool { /* compiled code */ }
}

extend std.core.Option<T> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.core.Option<T> <: Hashable {
    public func hashCode(): Int64 { /* compiled code */ }
}

extend std.core.Option<std.core.Option<T>> {
    public func flatten(): std.core.Option<T> { /* compiled code */ }
}

let MAX_INT_NATIVE: IntNative

let MIN_INT_NATIVE: IntNative

let MAX_UINT_NATIVE: UIntNative

public class OverflowException <: std.core.ArithmeticException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public func readln(): std.core.String { /* compiled code */ }

var globalOutOfMemoryError: std.core.OutOfMemoryError

internal func throwImplicitException(id: Int64, args: std.core.CPointer<Unit>): Unit { /* compiled code */ }

internal func throwArithmeticException(): Unit { /* compiled code */ }

internal func createArithmeticExceptionMsg(msg: std.core.String): std.core.ArithmeticException { /* compiled code */ }

internal func throwArithmeticExceptionMsg(msg: std.core.String): Nothing { /* compiled code */ }

internal func createOverflowExceptionMsg(msg: std.core.String): std.core.OverflowException { /* compiled code */ }

internal func throwOverflowException(): Unit { /* compiled code */ }

internal func throwIndexOutOfBoundsException(): Unit { /* compiled code */ }

internal func throwNegativeArraySizeException(): Unit { /* compiled code */ }

internal func callToString(e: std.core.ToString): std.core.String { /* compiled code */ }

internal func checkIsError(e: std.core.Object): Bool { /* compiled code */ }

internal func throwError(msg: std.core.String): Unit { /* compiled code */ }

internal func callPrintStackTrace(e: std.core.Exception): Unit { /* compiled code */ }

internal func callPrintStackTraceError(e: std.core.Error): Unit { /* compiled code */ }

let MAX_TIMEOUT_DURATION: std.core.Duration

@Intrinsic
internal func intrinsicSleep(ns: Int64): Unit

public func sleep(dur: std.core.Duration): Unit { /* compiled code */ }

public class SpawnException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class StackOverflowError <: std.core.Error {
    private let NotFolded: UInt64

    private let TopFolded: UInt64

    private let BottomFolded: UInt64

    private var FoldedFlag: UInt64

    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }

    public override func printStackTrace(): Unit { /* compiled code */ }
}

let digits100: std.core.Array<UInt8>

let EMPTY_INT64_ARRAY: std.core.Array<Int64>

internal func checkUtf8Of2Bytes(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Rune { /* compiled code */ }

internal func checkUtf8Of3Bytes(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Rune { /* compiled code */ }

internal func checkUtf8Of4Bytes(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Rune { /* compiled code */ }

@Intrinsic
internal func isThreadObjectInited(): Bool

@Intrinsic
internal func getThreadObject(): std.core.Thread

internal foreign func CJ_MRT_SetCJThreadName(handle: std.core.CPointer<Unit>, name: std.core.CPointer<UInt8>, len: Int64): Unit

internal foreign func CJ_MRT_GetCJThreadId(handle: std.core.CPointer<Unit>): Int64

internal foreign func CJ_MRT_GetCurrentCJThread(): std.core.CPointer<Unit>

internal class ThreadLocalMap {
    internal var entries: std.core.Array<std.core.Entry>

    internal var size: Int64

    init() { /* compiled code */ }

    internal func get<T>(tlKey!: std.core.ThreadLocalKey): std.core.Option<T> { /* compiled code */ }

    internal func set<T>(tlKey!: std.core.ThreadLocalKey, value!: T): Unit { /* compiled code */ }

    internal func remove(tlKey!: std.core.ThreadLocalKey): Unit { /* compiled code */ }
}

@Intrinsic
internal func setThreadObject(thread: std.core.Thread): Unit

internal func handleException(ex: std.core.Exception): Unit { /* compiled code */ }

internal func handleError(er: std.core.Error): Unit { /* compiled code */ }

let EMPTY_KEY: std.core.ThreadLocalKey

let DELETED_KEY: std.core.ThreadLocalKey

public class ThreadLocal<T> {
    internal let key: std.core.ThreadLocalKey = COMPILED_CODE /* compiled code */

    public func get(): std.core.Option<T> { /* compiled code */ }

    public func set(value: std.core.Option<T>): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

internal func getThreadLocalMap(): std.core.ThreadLocalMap { /* compiled code */ }

internal struct Entry {
    internal let tlKey: std.core.ThreadLocalKey

    internal let boxedValue: std.core.Object

    init(k: std.core.ThreadLocalKey, v: std.core.Object) { /* compiled code */ }

    internal func isValid(): Bool { /* compiled code */ }
}

let EMPTY_ENTRY: std.core.Entry

let DELETED_ENTRY: std.core.Entry

public class TimeoutException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

extend Int64 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend IntNative <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend Int32 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend Int16 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend Int8 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend UInt64 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend UIntNative <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend UInt32 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend UInt16 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend UInt8 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend Float64 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend Float32 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend Float16 <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend Bool <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend Rune <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend Unit <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

public type Byte = UInt8

public type Int = Int64

public type UInt = UInt64

public class UnsupportedException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}
