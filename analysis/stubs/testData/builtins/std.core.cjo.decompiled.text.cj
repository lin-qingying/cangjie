// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.core

import std.core

func arrayInitByCollection(array: std.core.Array<T>, elements: std.core.Collection<T>): std.core.Array<T>

interface Collection <: std.core.Iterable<T> {
    prop size: Int64

    func isEmpty: Bool

    func toArray: std.core.Array<T>
}

class Iterator <: std.core.Object & std.core.Iterable<T> {
    func next: std.core.Option<T>

    init() { /* compiled code */ }

    func iterator: std.core.Iterator<T>
}

interface Iterable {
    func iterator: std.core.Iterator<E>
}

enum Option {
    | Some(T)

    | None

    func getOrThrow: T

    func getOrThrow(exception: () -> std.core.Exception): T

    func getOrDefault(other: () -> T): T

    func isNone: Bool

    func isSome: Bool

    func map(transform: (T) -> R): std.core.Option<R>

    func filter(predicate: (T) -> Bool): std.core.Option<T>

    func flatMap(transform: (T) -> std.core.Option<R>): std.core.Option<R>
}

func arraySet(rawArr: std.core.Array<T>, index: Int64, element: T): Unit

func arrayInitByFunction(array: std.core.Array<T>, initElement: (Int64) -> T): std.core.Array<T>

func arraySize(rawArr: std.core.Array<T>): Int64

func toArray(raw: std.core.Array<T>): std.core.Array<T>

struct Array {
    let rawptr: std.core.Array<T>

    let start: Int64

    let len: Int64

    func toRawArray: std.core.Array<T>

    init() { /* compiled code */ }

    init(size: Int64, repeat: T) { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    init(data: std.core.Array<T>, start: Int64, len: Int64) { /* compiled code */ }

    prop first: std.core.Option<T>

    prop last: std.core.Option<T>

    func slice(start: Int64, len: Int64): std.core.Array<T>

    func `get`(index: Int64): std.core.Option<T>

    operator func [](index: Int64): T

    operator func [](index: Int64, value: T): Unit

    operator func [](range: std.core.Range<Int64>): std.core.Array<T>

    func fill(value: T): Unit

    operator func [](range: std.core.Range<Int64>, value: std.core.Array<T>): Unit

    func reverse: Unit

    func rangeSlice(range: std.core.Range<Int64>, _: Int64): std.core.Array<T>

    func isLegal(start_: Int64, end_: Int64): Bool

    func normalize(range: std.core.Range<Int64>): (Int64, Int64)

    func clone: std.core.Array<T>

    func clone(range: std.core.Range<Int64>): std.core.Array<T>

    func copyTo(dst: std.core.Array<T>, srcStart: Int64, dstStart: Int64, copyLen: Int64): Unit

    func copyTo(dst: std.core.Array<T>): Unit

    func concat(other: std.core.Array<T>): std.core.Array<T>

    func getUnchecked(index: Int64): T

    func setUnchecked(index: Int64, element: T): Unit

    func swap(index1: Int64, index2: Int64): Unit

    func splitAt(mid: Int64): (std.core.Array<T>, std.core.Array<T>)

    func repeat(n: Int64): std.core.Array<T>

    func map(transform: (T) -> R): std.core.Array<R>
}

func acquireArrayRawData(arr: std.core.Array<T>): std.core.CPointerHandle<T>

interface CType {
}

struct CPointerHandle {
    let pointer: CPointer<T>

    let array: std.core.Array<T>

    init() { /* compiled code */ }

    init(ptr: CPointer<T>, arr: std.core.Array<T>) { /* compiled code */ }
}

func acquireRawData(rawArr: std.core.Array<T>): CPointer<T>

func releaseArrayRawData(`handle`: std.core.CPointerHandle<T>): Unit

func releaseRawData(rawArr: std.core.Array<T>, ptr: CPointer<T>): Unit

let ASCII_UPPER_TO_LOWER_DIFF: UInt8

func max(a: T, b: T, others: std.core.Array<T>): T

interface Comparable <: std.core.Equatable<T> & std.core.Less<T> & std.core.Greater<T> & std.core.LessOrEqual<T> & std.core.GreaterOrEqual<T> {
    func compare(that: T): std.core.Ordering

    operator func ==(rhs: T): Bool

    operator func <(rhs: T): Bool

    operator func >(rhs: T): Bool

    operator func <=(rhs: T): Bool

    operator func >=(rhs: T): Bool
}

func min(a: T, b: T, others: std.core.Array<T>): T

let NS_PER_US: Int64

let NS_PER_MS: Int64

let NS_PER_SEC: Int64

let MS_PER_SEC: Int64

let US_PER_SEC: Int64

let SECS_PER_MINUTE: Int64

let MAX_NANOSECOND: UInt32

let SECS_PER_HOUR: Int64

let SECS_PER_DAY: Int64

let MAX_INT64: Int64

let MIN_INT64: Int64

let FLOAT64_0: Float64

let FLOAT64_1: Float64

let SECS_MAX: UInt64

let DUR_BASE: UInt32

let SECRET0: UInt64

let SECRET1: UInt64

let SECRET2: UInt64

let SECRET3: UInt64

func print(str: std.core.String, flush: Bool): Unit

struct String <: std.core.Collection<UInt8> & std.core.Comparable<std.core.String> & std.core.Hashable & std.core.ToString {
    let myData: std.core.Array<UInt8>

    let start: UInt32

    let length: UInt32

    let empty: std.core.String

    init() { /* compiled code */ }

    init(value: std.core.Array<Rune>) { /* compiled code */ }

    init(value: std.core.Collection<Rune>) { /* compiled code */ }

    init(value: std.core.Array<UInt8>) { /* compiled code */ }

    init(value: std.core.Array<UInt8>, start: UInt32, len: UInt32) { /* compiled code */ }

    init(ptr: CPointer<UInt8>, cSize: Int64) { /* compiled code */ }

    init(value: std.core.Array<UInt8>) { /* compiled code */ }

    func `get`(index: Int64): std.core.Option<UInt8>

    func acquireRaw: CPointer<UInt8>

    func releaseRaw(ptr: CPointer<UInt8>): Unit

    func toRuneArray: std.core.Array<Rune>

    func toArray: std.core.Array<UInt8>

    func toString: std.core.String

    func iterator: std.core.Iterator<UInt8>

    func runes: std.core.Iterator<Rune>

    func lines: std.core.Iterator<std.core.String>

    prop size: Int64

    func isEmpty: Bool

    func isAscii: Bool

    func isAsciiBlank: Bool

    func clone: std.core.String

    func hashCode: Int64

    func fromUtf8(utf8Data: std.core.Array<UInt8>): std.core.String

    func rawData: std.core.Array<UInt8>

    func fromUtf8Unchecked(utf8Data: std.core.Array<UInt8>): std.core.String

    func indexOf(b: UInt8): std.core.Option<Int64>

    func indexOf(b: UInt8, fromIndex: Int64): std.core.Option<Int64>

    func indexOfImpl(b: UInt8, fromIndex: Int64): Int64

    func indexOfCommon(b: UInt8, fromIndex: Int64): Int64

    func indexOfFast(b: UInt8, fromIndex: Int64): Int64

    func indexOf(str: std.core.String): std.core.Option<Int64>

    func indexOf(str: std.core.String, fromIndex: Int64): std.core.Option<Int64>

    func lastIndexOf(b: UInt8): std.core.Option<Int64>

    func lastIndexOf(b: UInt8, fromIndex: Int64): std.core.Option<Int64>

    func lastIndexOf(str: std.core.String): std.core.Option<Int64>

    func lastIndexOf(str: std.core.String, fromIndex: Int64): std.core.Option<Int64>

    func count(str: std.core.String): Int64

    func split(str: std.core.String, removeEmpty: Bool): std.core.Array<std.core.String>

    func split(str: std.core.String, maxSplits: Int64, removeEmpty: Bool): std.core.Array<std.core.String>

    func lazySplit(str: std.core.String, removeEmpty: Bool): std.core.Iterator<std.core.String>

    func lazySplit(str: std.core.String, maxSplits: Int64, removeEmpty: Bool): std.core.Iterator<std.core.String>

    func replace(old: std.core.String, new: std.core.String): std.core.String

    func insertString(new: std.core.String): std.core.String

    func replaceString(old: std.core.String, new: std.core.String): std.core.String

    func toAsciiLower: std.core.String

    func toAsciiUpper: std.core.String

    func toAsciiTitle: std.core.String

    func trimAscii: std.core.String

    func trimAsciiStart: std.core.String

    func trimAsciiEnd: std.core.String

    func trimStart(`set`: std.core.Array<Rune>): std.core.String

    func trimEnd(`set`: std.core.Array<Rune>): std.core.String

    func trimStart(`set`: std.core.String): std.core.String

    func trimEnd(`set`: std.core.String): std.core.String

    func trimStart(predicate: (Rune) -> Bool): std.core.String

    func trimEnd(predicate: (Rune) -> Bool): std.core.String

    func removePrefix(prefix: std.core.String): std.core.String

    func removeSuffix(suffix: std.core.String): std.core.String

    func contains(str: std.core.String): Bool

    func startsWith(prefix: std.core.String): Bool

    func endsWith(suffix: std.core.String): Bool

    func padStart(totalWidth: Int64, padding: std.core.String): std.core.String

    func padEnd(totalWidth: Int64, padding: std.core.String): std.core.String

    func equalsIgnoreAsciiCase(that: std.core.String): Bool

    func byteEqualsIgnoreAsciiCase(b1: UInt8, b2: UInt8): Bool

    func compareBytes(ptr1: std.core.Array<UInt8>, start1: Int64, ptr2: std.core.Array<UInt8>, start2: Int64, num: Int64): Int64

    func compareBytesCommon(ptr1: std.core.Array<UInt8>, start1: Int64, ptr2: std.core.Array<UInt8>, start2: Int64, num: Int64): Int64

    func compareBytesFast(ptr1: std.core.Array<UInt8>, start1: Int64, ptr2: std.core.Array<UInt8>, start2: Int64, num: Int64): Int64

    func compare(str: std.core.String): std.core.Ordering

    operator func +(right: std.core.String): std.core.String

    operator func *(count: Int64): std.core.String

    operator func <(right: std.core.String): Bool

    operator func <=(right: std.core.String): Bool

    operator func >(right: std.core.String): Bool

    operator func >=(right: std.core.String): Bool

    operator func ==(right: std.core.String): Bool

    operator func !=(right: std.core.String): Bool

    operator func [](index: Int64): UInt8

    func sliceUnchecked(startIndex: Int64, len: Int64): std.core.String

    operator func [](range: std.core.Range<Int64>): std.core.String

    func join(strArray: std.core.Array<std.core.String>, delimiter: std.core.String): std.core.String

    func indexOfString(str: std.core.String, startIndex: Int64, fromLast: Bool): Int64

    func splitOfString(str: std.core.String, maxSplits: Int64, removeEmpty: Bool): std.core.Array<std.core.String>

    func splitOfStringForOneRune(maxSplits: Int64): std.core.Array<std.core.String>

    func getRealSplitLenWithRemoveEmpty(indexes: CPointer<Int64>, total: Int64, separatorLen: Int64): Int64

    func splitAsStringArray(indexes: CPointer<Int64>, total: Int64, separatorLen: Int64, maxSplits: Int64, removeEmpty: Bool): std.core.Array<std.core.String>

    func withRawData(rawData: std.core.Array<UInt8>): std.core.String

    func checkUtf8Encoding(data: std.core.Array<UInt8>): Bool
}

func CJ_CORE_PrintUTF8(c: CPointer<UInt8>, len: Int64, newLine: Bool, flush: Bool): Int64

class OutOfMemoryError <: std.core.Error {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

func println: Unit

func CJ_CORE_PrintChar(c: UInt32, newLine: Bool, flush: Bool): Unit

func println(str: std.core.String): Unit

func eprint(str: std.core.String, flush: Bool): Unit

func CJ_CORE_ErrorPrintUTF8(c: CPointer<UInt8>, len: Int64, newLine: Bool, flush: Bool): Int64

func eprintln(str: std.core.String): Unit

func eprint(arg: T, flush: Bool): Unit

interface ToString {
    func toString: std.core.String
}

func eprintln(arg: T): Unit

func print(arg: T, flush: Bool): Unit

func println(arg: T): Unit

func print(b: Bool, flush: Bool): Unit

func CJ_CORE_PrintBool(b: Bool, newLine: Bool, flush: Bool): Unit

func println(b: Bool): Unit

func print(c: Rune, flush: Bool): Unit

func println(c: Rune): Unit

func print(f: Float16, flush: Bool): Unit

func CJ_CORE_PrintFloat(f: Float64, newLine: Bool, flush: Bool): Unit

func println(f: Float16): Unit

func print(f: Float32, flush: Bool): Unit

func println(f: Float32): Unit

func print(f: Float64, flush: Bool): Unit

func println(f: Float64): Unit

func print(i: Int8, flush: Bool): Unit

func CJ_CORE_PrintSigned(i: Int64, newLine: Bool, flush: Bool): Unit

func print(i: Int16, flush: Bool): Unit

func print(i: Int32, flush: Bool): Unit

func print(i: Int64, flush: Bool): Unit

func print(i: UInt8, flush: Bool): Unit

func CJ_CORE_PrintUnsigned(i: UInt64, newLine: Bool, flush: Bool): Unit

func print(i: UInt16, flush: Bool): Unit

func print(i: UInt32, flush: Bool): Unit

func print(i: UInt64, flush: Bool): Unit

func println(i: Int8): Unit

func println(i: Int16): Unit

func println(i: Int32): Unit

func println(i: Int64): Unit

func println(i: UInt8): Unit

func println(i: UInt16): Unit

func println(i: UInt32): Unit

func println(i: UInt64): Unit

let MAX_TIMEOUT_NS: Int64

let UNICODE_TO_UTF8_MAX_LEN: Int64

let IS_SIMD_SUPPORTED: Bool

func CJ_CORE_CanUseSIMD: Bool

let VECTOR_OFFSET_32: Int64

let ASCII_A: UInt8

let ASCII_Z: UInt8

let ASCII_a: UInt8

let ASCII_z: UInt8

let ASCII_UPPER_LOWER_DIFFERENCE: UInt8

let HIGH_1_MASK: UInt32

let HIGH_2_MASK: UInt32

let HIGH_3_MASK: UInt32

let HIGH_4_MASK: UInt32

let HIGH_5_MASK: UInt32

let HIGH_6_MASK: UInt32

let SHIFT_30: UInt32

let SHIFT_24: UInt32

let SHIFT_18: UInt32

let SHIFT_12: UInt32

let SHIFT_6: UInt32

let LOW_6_MASK: UInt32

let LOW_5_MASK: UInt32

let LOW_4_MASK: UInt32

let LOW_3_MASK: UInt32

let LOW_2_MASK: UInt32

let LOW_1_MASK: UInt32

let UTF8_5_MAX: UInt32

let UTF8_4_MAX: UInt32

let UTF8_3_MAX: UInt32

let UTF8_2_MAX: UInt32

let UTF8_1_MAX: UInt32

let HIGH_1_UInt8: UInt8

let HIGH_2_UInt8: UInt8

let HIGH_3_UInt8: UInt8

let HIGH_4_UInt8: UInt8

let HIGH_5_UInt8: UInt8

let HIGH_6_UInt8: UInt8

let EMPTY_UINT8_ARRAY: std.core.Array<UInt8>

let EMPTY_UINT8_RAW_ARRAY: std.core.Array<UInt8>

let STRING_C_THRESHOLD: Int64

let INVALID_ID: Int64

func zeroValue: T

func intrinsicBuiltInCopyTo(src: std.core.Array<T>, dst: std.core.Array<T>, srcStart: Int64, dstStart: Int64, copyLen: Int64): Unit

class IndexOutOfBoundsException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class StringBuilder <: std.core.Object & std.core.ToString {
    var data: std.core.Array<UInt8>

    var endIndex: Int64

    let defaultCapacity: Int64

    init() { /* compiled code */ }

    init(str: std.core.String) { /* compiled code */ }

    init(r: Rune, n: Int64) { /* compiled code */ }

    init(value: std.core.Array<Rune>) { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    prop size: Int64

    prop capacity: Int64

    func toString: std.core.String

    func reset(capacity: std.core.Option<Int64>): Unit

    func append(r: Rune): Unit

    func append(str: std.core.String): Unit

    func append(sb: std.core.StringBuilder): Unit

    func appendFromUtf8(arr: std.core.Array<UInt8>): Unit

    func appendFromUtf8Unchecked(arr: std.core.Array<UInt8>): Unit

    func append(runeArr: std.core.Array<Rune>): Unit

    func append(cstr: CString): Unit

    func append(v: T): Unit

    func append(val: std.core.Array<T>): Unit

    func append(b: Bool): Unit

    func append(n: Int64): Unit

    func append(n: Int32): Unit

    func append(n: Int16): Unit

    func append(n: Int8): Unit

    func append(n: UInt64): Unit

    func append(n: UInt32): Unit

    func append(n: UInt16): Unit

    func append(n: UInt8): Unit

    func append(n: Float64): Unit

    func append(n: Float32): Unit

    func append(n: Float16): Unit

    func reserve(additional: Int64): Unit

    func grow(minCapacity: Int64): Unit

    func append(cp: CPointer<UInt8>): Unit
}

func arrayGetUnchecked(rawArr: std.core.Array<T>, index: Int64): T

func arraySetUnchecked(rawArr: std.core.Array<T>, index: Int64, element: T): Unit

struct Range <: std.core.Iterable<T> {
    let start: T

    let end: T

    let step: Int64

    let hasStart: Bool

    let hasEnd: Bool

    let isClosed: Bool

    init(start: T, end: T, step: Int64, hasStart: Bool, hasEnd: Bool, isClosed: Bool) { /* compiled code */ }

    func iterator: std.core.Iterator<T>

    func isEmpty: Bool

    func last: T
}

class IllegalArgumentException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

func arrayGet(rawArr: std.core.Array<T>, index: Int64): T

class ArrayIterator <: std.core.Iterator<T> {
    var myData: std.core.Array<T>

    let end: Int64

    var myPosition: Int64

    init(data: std.core.Array<T>) { /* compiled code */ }

    func next: std.core.Option<T>
}

interface Equatable <: std.core.Equal<T> & std.core.NotEqual<T> {
    operator func !=(rhs: T): Bool
}

interface Equal {
    operator func ==(rhs: T): Bool
}

func readPointer(c: CPointer<T>, idx: Int64): T

func writePointer(c: CPointer<T>, idx: Int64, value: T): Unit

class Box {
    var value: T

    init(v: T) { /* compiled code */ }
}

enum Ordering {
    | LT

    | GT

    | EQ
}

struct Duration <: std.core.ToString & std.core.Hashable & std.core.Comparable<std.core.Duration> {
    let sec: Int64

    let ns: UInt32

    init(sec: Int64, ns: UInt32) { /* compiled code */ }

    func of(sec: Int64, ns: Int64): std.core.Duration

    let nanosecond: std.core.Duration

    let microsecond: std.core.Duration

    let millisecond: std.core.Duration

    let second: std.core.Duration

    let minute: std.core.Duration

    let hour: std.core.Duration

    let day: std.core.Duration

    let Zero: std.core.Duration

    let Max: std.core.Duration

    let Min: std.core.Duration

    func toNanoseconds: Int64

    func toMicroseconds: Int64

    func toMilliseconds: Int64

    func toSeconds: Int64

    func toMinutes: Int64

    func toHours: Int64

    func toDays: Int64

    func toString: std.core.String

    func hashCode: Int64

    func abs: std.core.Duration

    operator func +(r: std.core.Duration): std.core.Duration

    operator func -(r: std.core.Duration): std.core.Duration

    operator func *(r: Int64): std.core.Duration

    operator func *(r: Float64): std.core.Duration

    func toFloat64Nanoseconds: Float64

    operator func /(r: Int64): std.core.Duration

    operator func /(r: Float64): std.core.Duration

    operator func /(r: std.core.Duration): Float64

    operator func ==(r: std.core.Duration): Bool

    operator func !=(r: std.core.Duration): Bool

    operator func >=(r: std.core.Duration): Bool

    operator func >(r: std.core.Duration): Bool

    operator func <=(r: std.core.Duration): Bool

    operator func <(r: std.core.Duration): Bool

    func compare(rhs: std.core.Duration): std.core.Ordering

    func toUnsignedNum(r: Int64): (UInt64, UInt32, UInt64, Bool)
}

class Thread {
    let exceptionHandler: std.core.AtomicBox<std.core.Option<(std.core.Thread, std.core.Exception) -> Unit>>

    let _name: std.core.AtomicBox<std.core.String>

    let _hasCancellation: std.core.AtomicBool

    let _id: std.core.AtomicInt64

    var _threadLocalMap: std.core.Option<std.core.ThreadLocalMap>

    var _rtCJThreadHandle: CPointer<Unit>

    let _rtCJThreadHandleRefCount: std.core.AtomicInt64

    init() { /* compiled code */ }

    func setRuntimeCJThreadHandle(`handle`: CPointer<Unit>): Unit

    func waitForCJThreadHandle: Unit

    func clearRuntimeCJThreadHandle: Unit

    prop currentThread: std.core.Thread

    prop id: Int64

    prop name: std.core.String

    prop hasPendingCancellation: Bool

    func setCancellationRequest: Unit

    func handleUncaughtExceptionBy(exHandler: (std.core.Thread, std.core.Exception) -> Unit): Unit
}

class FutureResult {
    init() { /* compiled code */ }
}

class FutureResInit <: std.core.FutureResult {
    init() { /* compiled code */ }
}

class Object {
    init() { /* compiled code */ }
}

class MapIterator <: std.core.Iterator<R> {
    let transform: (T) -> R

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, transform: (T) -> R) { /* compiled code */ }

    func next: std.core.Option<R>
}

class ZipIterator <: std.core.Iterator<(T, R)> {
    let itt: std.core.Iterator<T>

    let itr: std.core.Iterator<R>

    init(itt: std.core.Iterator<T>, itr: std.core.Iterator<R>) { /* compiled code */ }

    func next: std.core.Option<(T, R)>
}

class EnumerateIterator <: std.core.Iterator<(Int64, T)> {
    var index: Int64

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>) { /* compiled code */ }

    func next: std.core.Option<(Int64, T)>
}

class FilterIterator <: std.core.Iterator<T> {
    let predicate: (T) -> Bool

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, predicate: (T) -> Bool) { /* compiled code */ }

    func next: std.core.Option<T>
}

class FilterMapIterator <: std.core.Iterator<R> {
    let transform: (T) -> std.core.Option<R>

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, transform: (T) -> std.core.Option<R>) { /* compiled code */ }

    func next: std.core.Option<R>
}

class StepIterator <: std.core.Iterator<T> {
    let _count: Int64

    let it: std.core.Iterator<T>

    var index: Int64

    init(it: std.core.Iterator<T>, count: Int64) { /* compiled code */ }

    func next: std.core.Option<T>
}

class SkipIterator <: std.core.Iterator<T> {
    var _count: Int64

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, count: Int64) { /* compiled code */ }

    func next: std.core.Option<T>
}

class TakeIterator <: std.core.Iterator<T> {
    var _count: Int64

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, count: Int64) { /* compiled code */ }

    func next: std.core.Option<T>
}

class ConcatIterator <: std.core.Iterator<T> {
    let it: std.core.Iterator<T>

    let other: std.core.Iterator<T>

    var itOver: Bool

    init(it: std.core.Iterator<T>, other: std.core.Iterator<T>) { /* compiled code */ }

    func next: std.core.Option<T>
}

class InspectIterator <: std.core.Iterator<T> {
    let action: (T) -> Unit

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, action: (T) -> Unit) { /* compiled code */ }

    func next: std.core.Option<T>
}

class FlatMapIterator <: std.core.Iterator<R> {
    var subIt: std.core.Option<std.core.Iterator<R>>

    let transform: (T) -> std.core.Iterable<R>

    let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>, transform: (T) -> std.core.Iterable<R>) { /* compiled code */ }

    func next: std.core.Option<R>
}

class IntersperseIterator <: std.core.Iterator<T> {
    let separator: T

    let it: std.core.Iterator<T>

    var item: std.core.Option<T>

    var isInsert: Bool

    init(it: std.core.Iterator<T>, separator: T) { /* compiled code */ }

    func next: std.core.Option<T>
}

class NoneValueException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class Exception <: std.core.Object & std.core.ToString {
    var detailMessage: std.core.String

    var pcArray: std.core.Array<UInt64>

    var traceElementArray: std.core.Array<std.core.StackTraceElement>

    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String

    prop message: std.core.String

    func toString: std.core.String

    func printStackTrace: Unit

    func getStackTrace: std.core.Array<std.core.StackTraceElement>
}

interface Countable {
    func next(right: Int64): T

    func position: Int64
}

class RangeIterator <: std.core.Iterator<T> {
    let _last: T

    let _step: Int64

    var cur: T

    var hasNext: Bool

    init(first: T, last: T, step: Int64, isEmpty: Bool) { /* compiled code */ }

    func next: std.core.Option<T>
}

struct RunesToUtf8Bytes {
    func getBytes(runes: std.core.Array<Rune>): std.core.Array<UInt8>

    func getBytes(elements: std.core.Collection<Rune>): std.core.Array<UInt8>

    func getSize(runes: std.core.Array<Rune>): Int64

    init() { /* compiled code */ }
}

func memcpy_s(dest: CPointer<UInt8>, destMax: UIntNative, src: CPointer<UInt8>, count: UIntNative): Int32

func utf8RuneSize(arr: std.core.Array<UInt8>, start: Int64, length: Int64): Int64

class StringByteIterator <: std.core.Iterator<UInt8> {
    func next: std.core.Option<UInt8>

    let iteratorData: std.core.StringIteratorData

    init(iteratorData: std.core.StringIteratorData) { /* compiled code */ }
}

class StringIteratorData {
    let myData: std.core.Array<UInt8>

    var end: Int64

    var nextPos: Int64

    init(data: std.core.Array<UInt8>) { /* compiled code */ }

    init(data: std.core.Array<UInt8>) { /* compiled code */ }
}

class StringRuneIterator <: std.core.Iterator<Rune> {
    func next: std.core.Option<Rune>

    let iteratorData: std.core.StringIteratorData

    init(iteratorData: std.core.StringIteratorData) { /* compiled code */ }
}

class StringLineIterator <: std.core.Iterator<std.core.String> {
    func next: std.core.Option<std.core.String>

    let iteratorData: std.core.StringIteratorData

    init(iteratorData: std.core.StringIteratorData) { /* compiled code */ }
}

func wyhash(arr: std.core.Array<UInt8>, start: Int64, size: Int64, see: UInt64): UInt64

func checkInvalid(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Unit

func vectorIndexByte32(arr: std.core.Array<UInt8>, offset: Int64, target: UInt8): Int64

func CJ_CORE_CountString(orgStr: CPointer<UInt8>, subStr: CPointer<UInt8>, orgSize: Int64, subSize: Int64): Int64

let EMPTY_STRING_ARRAY: std.core.Array<std.core.String>

class StringLazySplitIterator <: std.core.Iterator<std.core.String> {
    let orgData: std.core.Array<UInt8>

    let separatorLen: Int64

    var itemArr: std.core.Array<Int64>

    var maxSplits: Int64

    var removeEmpty: Bool

    var resLen: Int64

    var nextPos: Int64

    var itemStart: Int64

    var itemIndex: Int64

    var itemTotal: Int64

    init(orgStrData: std.core.Array<UInt8>, orgStrSize: Int64, str: std.core.String, maxSplits: Int64, removeEmpty: Bool) { /* compiled code */ }

    func next: std.core.Option<std.core.String>
}

func CJ_CORE_CountAndIndexString(orgStr: CPointer<UInt8>, subStr: CPointer<UInt8>, orgSize: Int64, subSize: Int64): CPointer<Int64>

struct LibC {
    func malloc(count: Int64): CPointer<T>

    func free(p: CPointer<T>): Unit

    func mallocCString(str: std.core.String): CString

    func free(cstr: CString): Unit

    init() { /* compiled code */ }
}

func CJ_CORE_StringMemcmp(str1: CPointer<UInt8>, str2: CPointer<UInt8>, n: UIntNative): Int64

func vectorCompare32(arr1: std.core.Array<UInt8>, offset1: Int64, arr2: std.core.Array<UInt8>, offset2: Int64): Int64

func intrinsicRefEqRawArray(a: std.core.Array<T>, b: std.core.Array<T>): Bool

func CJ_CORE_LastIndexOfString(orgStr: CPointer<UInt8>, subStr: CPointer<UInt8>, orgSize: Int64, subSize: Int64, start: Int64): Int64

func CJ_CORE_IndexOfString(orgStr: CPointer<UInt8>, subStr: CPointer<UInt8>, orgSize: Int64, subSize: Int64, start: Int64): Int64

func checkUtf8Of2BytesWithoutException(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Bool

func checkUtf8Of3BytesWithoutException(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Bool

func checkUtf8Of4BytesWithoutException(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Bool

func calcNumLen(num: UInt64): Int64

func numberToString(number: UInt64, endIndex: Int64, buffer: std.core.Array<UInt8>): Unit

func CJ_CORE_Float64ToCPointer(num: Float64): CPointer<UInt8>

func strlen(str: CPointer<UInt8>): UIntNative

class ThreadLocalKey <: std.core.Object & std.core.Hashable {
    let ALPHA: Int64

    let CURR: std.core.AtomicInt64

    func nextHash: Int64

    let code: Int64

    func equals(another: std.core.ThreadLocalKey): Bool

    func hashCode: Int64

    init() { /* compiled code */ }
}

enum AnnotationKind {
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

class ArithmeticException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

extend Array {
    func flatten: std.core.Array<T>
}

extend Array <: Collection {
    func iterator: std.core.Iterator<T>

    prop size: Int64

    func isEmpty: Bool

    func toArray: std.core.Array<T>
}

extend Array <: ToString {
    func toString: std.core.String
}

extend Array <: Equatable {
    operator func ==(that: std.core.Array<T>): Bool

    operator func !=(that: std.core.Array<T>): Bool

    func contains(element: T): Bool

    func indexOf(element: T): std.core.Option<Int64>

    func indexOf(element: T, fromIndex: Int64): std.core.Option<Int64>

    func indexOf(elements: std.core.Array<T>): std.core.Option<Int64>

    func indexOf(elements: std.core.Array<T>, fromIndex: Int64): std.core.Option<Int64>

    func lastIndexOf(element: T): std.core.Option<Int64>

    func lastIndexOf(element: T, fromIndex: Int64): std.core.Option<Int64>

    func lastIndexOf(elements: std.core.Array<T>): std.core.Option<Int64>

    func lastIndexOf(elements: std.core.Array<T>, fromIndex: Int64): std.core.Option<Int64>

    func trimStart(`set`: std.core.Array<T>): std.core.Array<T>

    func trimEnd(`set`: std.core.Array<T>): std.core.Array<T>

    func trimStart(predicate: (T) -> Bool): std.core.Array<T>

    func trimEnd(predicate: (T) -> Bool): std.core.Array<T>

    func removePrefix(prefix: std.core.Array<T>): std.core.Array<T>

    func removeSuffix(suffix: std.core.Array<T>): std.core.Array<T>
}

func arrayClone(rawArr: std.core.Array<T>): std.core.Array<T>

func CJ_CORE_AtExitCallbackListLock: Unit

func CJ_CORE_AtExitCallbackListUnlook: Unit

class AtexitCallbackNode {
    var atexitCallback: () -> Unit

    var next: std.core.Option<std.core.AtexitCallbackNode>

    init(callback: () -> Unit) { /* compiled code */ }
}

var atexitCallbackHead: std.core.Option<std.core.AtexitCallbackNode>

var callbacksRunningFlag: Bool

func CJ_CORE_AddAtexitCallback(callback: () -> Unit): Unit

func CJ_CORE_ExecAtexitCallbacks: Unit

enum MemoryOrder {
    | SeqCst
}

func load(atomicType: ATOMIC_TYPE, memoryOrder: std.core.MemoryOrder): TYPE

func store(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.core.MemoryOrder): Unit

func fetchAdd(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.core.MemoryOrder): TYPE

func compareAndSwap(atomicType: ATOMIC_TYPE, oldValue: TYPE, newValue: TYPE, successOrder: std.core.MemoryOrder, failureOrder: std.core.MemoryOrder): Bool

class AtomicBool {
    var value: Int8

    init(val: Bool) { /* compiled code */ }

    func load: Bool

    func store(val: Bool): Unit
}

class AtomicInt64 {
    var value: Int64

    init(val: Int64) { /* compiled code */ }

    func fetchAdd(val: Int64): Int64

    func load: Int64

    func store(val: Int64): Unit

    func compareAndSwap(old: Int64, new: Int64): Bool
}

class AtomicReference {
    var value: T

    init(val: T) { /* compiled code */ }

    func load: T

    func store(val: T): Unit
}

class AtomicBox {
    let boxRef: std.core.AtomicReference<std.core.Box<T>>

    init(val: T) { /* compiled code */ }

    func load: T

    func store(val: T): Unit
}

extend Box <: Hashable {
    func hashCode: Int64
}

interface Hashable {
    func hashCode: Int64
}

extend Box <: Comparable {
    operator func ==(that: std.core.Box<T>): Bool

    operator func !=(that: std.core.Box<T>): Bool

    operator func >(that: std.core.Box<T>): Bool

    operator func <(that: std.core.Box<T>): Bool

    operator func >=(that: std.core.Box<T>): Bool

    operator func <=(that: std.core.Box<T>): Bool

    func compare(that: std.core.Box<T>): std.core.Ordering
}

extend Box <: ToString {
    func toString: std.core.String
}

extend Extend {
    func isAsciiLetter: Bool

    func isAsciiNumber: Bool

    func isAsciiHex: Bool

    func isAsciiOct: Bool

    func isAsciiPunctuation: Bool

    func isAsciiGraphic: Bool

    func isAsciiControl: Bool

    func isAsciiNumberOrLetter: Bool

    func isAsciiLowerCase: Bool

    func isAsciiUpperCase: Bool

    func isAscii: Bool

    func isAsciiWhiteSpace: Bool

    func toAsciiUpperCase: UInt8

    func toAsciiLowerCase: UInt8
}

func getPointerAddress(c: CPointer<T>): UIntNative

func addPointer(c: CPointer<T>, offset: Int64): CPointer<T>

extend Extend {
    func isNull: Bool

    func isNotNull: Bool

    func toUIntNative: UIntNative

    func read: T

    func write(value: T): Unit

    func read(idx: Int64): T

    func write(idx: Int64, value: T): Unit

    operator func +(offset: Int64): CPointer<T>

    operator func -(offset: Int64): CPointer<T>

    func asResource: std.core.CPointerResource<T>
}

struct CPointerResource <: std.core.Resource {
    let value: CPointer<T>

    let isFree: std.core.Box<Bool>

    init(p: CPointer<T>) { /* compiled code */ }

    func isClosed: Bool

    func close: Unit
}

interface Resource {
    func isClosed: Bool

    func close: Unit
}

func strcmp(source: CPointer<UInt8>, target: CPointer<UInt8>): Int32

func strncmp(source: CPointer<UInt8>, target: CPointer<UInt8>, cmpSize: UIntNative): Int32

func strcasecmp(source: CPointer<UInt8>, target: CPointer<UInt8>): Int32

func convertCStr2Ptr(cs: CString): CPointer<UInt8>

func createStrFromCPointer(cp: CPointer<UInt8>): std.core.String

extend Extend <: ToString {
    func getChars: CPointer<UInt8>

    func isNull: Bool

    func size: Int64

    func isEmpty: Bool

    func isNotEmpty: Bool

    func startsWith(prefix: CString): Bool

    func endsWith(suffix: CString): Bool

    func equals(rhs: CString): Bool

    func equalsLower(rhs: CString): Bool

    func subCString(beginIndex: UIntNative): CString

    func subCString(beginIndex: UIntNative, subLen: UIntNative): CString

    func compare(str: CString): Int32

    func toString: std.core.String

    func asResource: std.core.CStringResource
}

struct CStringResource <: std.core.Resource {
    let value: CString

    let isFree: std.core.Box<Bool>

    init(p: CString) { /* compiled code */ }

    func isClosed: Bool

    func close: Unit
}

extend Extend {
    func isAsciiLetter: Bool

    func isAsciiNumber: Bool

    func isAsciiHex: Bool

    func isAsciiOct: Bool

    func isAsciiPunctuation: Bool

    func isAsciiGraphic: Bool

    func isAsciiControl: Bool

    func isAsciiNumberOrLetter: Bool

    func isAsciiLowerCase: Bool

    func isAsciiUpperCase: Bool

    func isAscii: Bool

    func isAsciiWhiteSpace: Bool

    func toAsciiUpperCase: Rune

    func toAsciiLowerCase: Rune

    func utf8Size(arr: std.core.Array<UInt8>, index: Int64): Int64

    func utf8SizeUnchecked(byte: UInt8): Int64

    func utf8Size(c: Rune): Int64

    func fromUtf8(arr: std.core.Array<UInt8>, index: Int64): (Rune, Int64)

    func fromUtf8(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): (Rune, Int64)

    func getPreviousFromUtf8(arr: std.core.Array<UInt8>, index: Int64): (Rune, Int64)

    func intoUtf8Array(c: Rune, arr: std.core.Array<UInt8>, index: Int64): Int64
}

extend Extend <: Comparable {
    func compare(rhs: Rune): std.core.Ordering
}

func Int64Less(a: Int64, b: Int64): Bool

func Int64Greater(a: Int64, b: Int64): Bool

func Int64LessOrEqual(a: Int64, b: Int64): Bool

func Int64GreaterOrEqual(a: Int64, b: Int64): Bool

func Int64Equal(a: Int64, b: Int64): Bool

func Int64NotEqual(a: Int64, b: Int64): Bool

func Int32Less(a: Int32, b: Int32): Bool

func Int32Greater(a: Int32, b: Int32): Bool

func Int32LessOrEqual(a: Int32, b: Int32): Bool

func Int32GreaterOrEqual(a: Int32, b: Int32): Bool

func Int32Equal(a: Int32, b: Int32): Bool

func Int32NotEqual(a: Int32, b: Int32): Bool

func Int16Less(a: Int16, b: Int16): Bool

func Int16Greater(a: Int16, b: Int16): Bool

func Int16LessOrEqual(a: Int16, b: Int16): Bool

func Int16GreaterOrEqual(a: Int16, b: Int16): Bool

func Int16Equal(a: Int16, b: Int16): Bool

func Int16NotEqual(a: Int16, b: Int16): Bool

func Int8Less(a: Int8, b: Int8): Bool

func Int8Greater(a: Int8, b: Int8): Bool

func Int8LessOrEqual(a: Int8, b: Int8): Bool

func Int8GreaterOrEqual(a: Int8, b: Int8): Bool

func Int8Equal(a: Int8, b: Int8): Bool

func Int8NotEqual(a: Int8, b: Int8): Bool

func UInt64Less(a: UInt64, b: UInt64): Bool

func UInt64Greater(a: UInt64, b: UInt64): Bool

func UInt64LessOrEqual(a: UInt64, b: UInt64): Bool

func UInt64GreaterOrEqual(a: UInt64, b: UInt64): Bool

func UInt64Equal(a: UInt64, b: UInt64): Bool

func UInt64NotEqual(a: UInt64, b: UInt64): Bool

func UInt32Less(a: UInt32, b: UInt32): Bool

func UInt32Greater(a: UInt32, b: UInt32): Bool

func UInt32LessOrEqual(a: UInt32, b: UInt32): Bool

func UInt32GreaterOrEqual(a: UInt32, b: UInt32): Bool

func UInt32Equal(a: UInt32, b: UInt32): Bool

func UInt32NotEqual(a: UInt32, b: UInt32): Bool

func UInt16Less(a: UInt16, b: UInt16): Bool

func UInt16Greater(a: UInt16, b: UInt16): Bool

func UInt16LessOrEqual(a: UInt16, b: UInt16): Bool

func UInt16GreaterOrEqual(a: UInt16, b: UInt16): Bool

func UInt16Equal(a: UInt16, b: UInt16): Bool

func UInt16NotEqual(a: UInt16, b: UInt16): Bool

func UInt8Less(a: UInt8, b: UInt8): Bool

func UInt8Greater(a: UInt8, b: UInt8): Bool

func UInt8LessOrEqual(a: UInt8, b: UInt8): Bool

func UInt8GreaterOrEqual(a: UInt8, b: UInt8): Bool

func UInt8Equal(a: UInt8, b: UInt8): Bool

func UInt8NotEqual(a: UInt8, b: UInt8): Bool

func Float16Less(a: Float16, b: Float16): Bool

func Float16Greater(a: Float16, b: Float16): Bool

func Float16LessOrEqual(a: Float16, b: Float16): Bool

func Float16GreaterOrEqual(a: Float16, b: Float16): Bool

func Float16Equal(a: Float16, b: Float16): Bool

func Float16NotEqual(a: Float16, b: Float16): Bool

func Float32Less(a: Float32, b: Float32): Bool

func Float32Greater(a: Float32, b: Float32): Bool

func Float32LessOrEqual(a: Float32, b: Float32): Bool

func Float32GreaterOrEqual(a: Float32, b: Float32): Bool

func Float32Equal(a: Float32, b: Float32): Bool

func Float32NotEqual(a: Float32, b: Float32): Bool

func Float64Less(a: Float64, b: Float64): Bool

func Float64Greater(a: Float64, b: Float64): Bool

func Float64LessOrEqual(a: Float64, b: Float64): Bool

func Float64GreaterOrEqual(a: Float64, b: Float64): Bool

func Float64Equal(a: Float64, b: Float64): Bool

func Float64NotEqual(a: Float64, b: Float64): Bool

interface Less {
    operator func <(rhs: T): Bool
}

interface Greater {
    operator func >(rhs: T): Bool
}

interface LessOrEqual {
    operator func <=(rhs: T): Bool
}

interface GreaterOrEqual {
    operator func >=(rhs: T): Bool
}

extend Ordering <: ToString {
    func toString: std.core.String
}

extend Ordering <: Comparable {
    func compare(that: std.core.Ordering): std.core.Ordering
}

extend Extend <: Comparable {
    func compare(rhs: IntNative): std.core.Ordering
}

extend Extend <: Comparable {
    func compare(rhs: Int64): std.core.Ordering

    operator func <(rhs: Int64): Bool

    operator func >(rhs: Int64): Bool

    operator func <=(rhs: Int64): Bool

    operator func >=(rhs: Int64): Bool

    operator func ==(rhs: Int64): Bool

    operator func !=(rhs: Int64): Bool
}

extend Extend <: Comparable {
    func compare(rhs: Int32): std.core.Ordering

    operator func <(rhs: Int32): Bool

    operator func >(rhs: Int32): Bool

    operator func <=(rhs: Int32): Bool

    operator func >=(rhs: Int32): Bool

    operator func ==(rhs: Int32): Bool

    operator func !=(rhs: Int32): Bool
}

extend Extend <: Comparable {
    func compare(rhs: Int16): std.core.Ordering

    operator func <(rhs: Int16): Bool

    operator func >(rhs: Int16): Bool

    operator func <=(rhs: Int16): Bool

    operator func >=(rhs: Int16): Bool

    operator func ==(rhs: Int16): Bool

    operator func !=(rhs: Int16): Bool
}

extend Extend <: Comparable {
    func compare(rhs: Int8): std.core.Ordering

    operator func <(rhs: Int8): Bool

    operator func >(rhs: Int8): Bool

    operator func <=(rhs: Int8): Bool

    operator func >=(rhs: Int8): Bool

    operator func ==(rhs: Int8): Bool

    operator func !=(rhs: Int8): Bool
}

extend Extend <: Comparable {
    func compare(rhs: UIntNative): std.core.Ordering
}

extend Extend <: Comparable {
    func compare(rhs: UInt64): std.core.Ordering

    operator func <(rhs: UInt64): Bool

    operator func >(rhs: UInt64): Bool

    operator func <=(rhs: UInt64): Bool

    operator func >=(rhs: UInt64): Bool

    operator func ==(rhs: UInt64): Bool

    operator func !=(rhs: UInt64): Bool
}

extend Extend <: Comparable {
    func compare(rhs: UInt32): std.core.Ordering

    operator func <(rhs: UInt32): Bool

    operator func >(rhs: UInt32): Bool

    operator func <=(rhs: UInt32): Bool

    operator func >=(rhs: UInt32): Bool

    operator func ==(rhs: UInt32): Bool

    operator func !=(rhs: UInt32): Bool
}

extend Extend <: Comparable {
    func compare(rhs: UInt16): std.core.Ordering

    operator func <(rhs: UInt16): Bool

    operator func >(rhs: UInt16): Bool

    operator func <=(rhs: UInt16): Bool

    operator func >=(rhs: UInt16): Bool

    operator func ==(rhs: UInt16): Bool

    operator func !=(rhs: UInt16): Bool
}

extend Extend <: Comparable {
    func compare(rhs: UInt8): std.core.Ordering

    operator func <(rhs: UInt8): Bool

    operator func >(rhs: UInt8): Bool

    operator func <=(rhs: UInt8): Bool

    operator func >=(rhs: UInt8): Bool

    operator func ==(rhs: UInt8): Bool

    operator func !=(rhs: UInt8): Bool
}

extend Extend <: Comparable {
    func compare(rhs: Float16): std.core.Ordering

    operator func <(rhs: Float16): Bool

    operator func >(rhs: Float16): Bool

    operator func <=(rhs: Float16): Bool

    operator func >=(rhs: Float16): Bool

    operator func ==(rhs: Float16): Bool

    operator func !=(rhs: Float16): Bool
}

extend Extend <: Comparable {
    func compare(rhs: Float32): std.core.Ordering

    operator func <(rhs: Float32): Bool

    operator func >(rhs: Float32): Bool

    operator func <=(rhs: Float32): Bool

    operator func >=(rhs: Float32): Bool

    operator func ==(rhs: Float32): Bool

    operator func !=(rhs: Float32): Bool
}

extend Extend <: Comparable {
    func compare(rhs: Float64): std.core.Ordering

    operator func <(rhs: Float64): Bool

    operator func >(rhs: Float64): Bool

    operator func <=(rhs: Float64): Bool

    operator func >=(rhs: Float64): Bool

    operator func ==(rhs: Float64): Bool

    operator func !=(rhs: Float64): Bool
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): IntNative
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): Int64
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): Int32
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): Int16
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): Int8
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): UIntNative
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): UInt64
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): UInt32
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): UInt16
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): UInt8
}

extend Extend <: Countable {
    func position: Int64

    func next(right: Int64): Rune
}

let MAX_NS_FLOAT64: Float64

let MIN_NS_FLOAT64: Float64

func durMul(secs: UInt64, nanos: UInt32, multiplier: UInt64, sign: Bool): std.core.Option<std.core.Duration>

func u32MulU64(thisInt: UInt32, thatInt: UInt64): (UInt64, UInt32)

func tryU64Mul(left: UInt64, right: UInt64): std.core.Option<UInt64>

func tryU64Add(left: UInt64, right: UInt64): std.core.Option<UInt64>

func checkSub(left: Int64, right: Int64): Bool

func toSecNsBase(num: UInt64): (UInt32, UInt32, UInt32)

func durDiv(secs: UInt64, nanos: UInt32, divisor: UInt64): (Int64, Int64)

func getDifference(num3: Int64, num2: Int64, num1: Int64, num0: Int64): (Int64, Int64, Int64, Int64)

func getDifference(num2: Int64, num1: Int64, num0: Int64): (UInt32, UInt32, UInt32)

func u32TwoMul(num1: UInt32, num0: UInt32, multiplier: UInt32): (UInt32, UInt32, UInt32)

func u32ThreeMul(num2: UInt32, num1: UInt32, num0: UInt32, multiplier: UInt32): (UInt32, UInt32, UInt32, UInt32)

func u32DividendMul(num3: UInt32, num2: UInt32, num1: UInt32, num0: UInt32, times: UInt32): (UInt32, UInt32, UInt32, UInt32, UInt32)

func divAndMod(dividend: UInt64, dividend2: UInt32, divisor2: UInt32, divisor1: UInt32): UInt32

extend Extend {
    operator func *(r: std.core.Duration): std.core.Duration
}

extend Extend {
    operator func -(r: UInt32): Int64
}

extend Extend {
    operator func *(r: UInt32): UInt64

    operator func /(r: UInt32): UInt64

    operator func %(r: UInt32): UInt32
}

extend Extend {
    operator func *(r: UInt64): UInt64
}

extend Extend {
    operator func *(r: std.core.Duration): std.core.Duration
}

extend StringBuilder {
    func append(num: Int64, unit: std.core.String): std.core.StringBuilder
}

func isSumOverflow(left: Int64, right: Int64): Bool

enum Endian {
    | Big

    | Little

    prop Platform: std.core.Endian
}

func CJ_PlatformEndian: UInt8

interface NotEqual {
    operator func !=(rhs: T): Bool
}

extend Extend <: Equatable {
    operator func ==(right: Bool): Bool
}

extend Extend <: Equatable {
    operator func ==(right: Unit): Bool
}

extend Range <: Equatable {
    operator func ==(that: std.core.Range<T>): Bool
}

class Error <: std.core.Object & std.core.ToString {
    var detailMessage: std.core.String

    var pcArray: std.core.Array<UInt64>

    var traceElementArray: std.core.Array<std.core.StackTraceElement>

    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String

    prop message: std.core.String

    func toString: std.core.String

    func printStackTrace: Unit

    func getStackTraceMessage: std.core.String

    func getStackTrace: std.core.Array<std.core.StackTraceElement>
}

class StackTraceElement {
    let declaringClass: std.core.String

    let methodName: std.core.String

    let fileName: std.core.String

    let lineNumber: Int64

    init(declaringClass: std.core.String, methodName: std.core.String, fileName: std.core.String, lineNumber: Int64) { /* compiled code */ }
}

func fillInStackTrace(pc: std.core.Array<UInt64>, message: std.core.Array<UInt8>): std.core.Array<UInt64>

func decodeStackTrace(pc: UInt64, funcStart: UInt64, funcDesc: UInt64): std.core.StackTraceData

struct StackTraceData {
    let className: std.core.Array<UInt8>

    let methodName: std.core.Array<UInt8>

    let fileName: std.core.Array<UInt8>

    let lineNumber: Int64

    init() { /* compiled code */ }
}

func getStackTraceImpl(pcArray: std.core.Array<UInt64>): std.core.Array<std.core.StackTraceElement>

func bitCast(val: From): To

interface BitCastRange {
}

extend Extend {
    func fromBits(bits: UInt64): Float64

    func toBits: UInt64
}

extend Extend {
    func fromBits(bits: UInt32): Float32

    func toBits: UInt32
}

extend Extend {
    func fromBits(bits: UInt16): Float16

    func toBits: UInt16
}

extend Extend <: BitCastRange {
}

extend Extend <: BitCastRange {
}

extend Extend <: BitCastRange {
}

extend Extend <: BitCastRange {
}

extend Extend <: BitCastRange {
}

extend Extend <: BitCastRange {
}

func composition(f: (T1) -> T2, g: (T2) -> T3): (T1) -> T3

func futureInit(future: std.core.Future<T>): Unit

class Future {
    let _thread: std.core.Thread

    var result: std.core.FutureResult

    var executeFn: () -> T

    init(fn: () -> T) { /* compiled code */ }

    prop thread: std.core.Thread

    func `get`: T

    func tryGet: std.core.Option<T>

    func `get`(timeout: std.core.Duration): T

    func cancel: Unit

    func execute: Unit

    func executeClosure(fn: () -> T): Unit
}

func futureIsComplete(future: std.core.Future<T>): Bool

func futureWait(future: std.core.Future<T>, timeout: Int64): Unit

func futureNotifyAll(future: std.core.Future<T>): Unit

func CJ_CORE_PrintOomHint: Unit

let INT64_MAX: Int64

class FutureOk <: std.core.FutureResult {
    var value: T

    init(v: T) { /* compiled code */ }
}

class FutureErr <: std.core.FutureResult {
    let cachedResult: std.core.FutureErr

    var err: std.core.Error

    init(e: std.core.Error) { /* compiled code */ }
}

class FutureExcept <: std.core.FutureResult {
    var except: std.core.Exception

    init(e: std.core.Exception) { /* compiled code */ }
}

interface ThreadContext {
    func end: Unit

    func hasEnded: Bool
}

extend Ordering <: Hashable {
    func hashCode: Int64
}

extend Extend <: Hashable {
    func hashCode: Int64
}

extend Extend <: Hashable {
    func hashCode: Int64
}

extend Extend <: Hashable {
    func hashCode: Int64
}

func i32tof32(num: Int32): Float32

func f32toi32(num: Float32): Int32

func i64tof64(num: Int64): Float64

func f642i64(num: Float64): Int64

extend Extend <: Hashable {
    func hashCode: Int64

    prop Max: IntNative

    prop Min: IntNative
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop Max: Int64

    prop Min: Int64
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop Max: Int32

    prop Min: Int32
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop Max: Int16

    prop Min: Int16
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop Max: Int8

    prop Min: Int8
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop Max: UIntNative

    prop Min: UIntNative
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop Max: UInt64

    prop Min: UInt64
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop Max: UInt32

    prop Min: UInt32
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop Max: UInt16

    prop Min: UInt16
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop Max: UInt8

    prop Min: UInt8
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop NaN: Float64

    prop Inf: Float64

    prop Max: Float64

    prop Min: Float64

    prop MinDenormal: Float64

    prop MinNormal: Float64

    func isInf: Bool

    func isNaN: Bool

    func isNormal: Bool

    func max(a: Float64, b: Float64, others: std.core.Array<Float64>): Float64

    func min(a: Float64, b: Float64, others: std.core.Array<Float64>): Float64
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop NaN: Float32

    prop Inf: Float32

    prop Max: Float32

    prop Min: Float32

    prop MinDenormal: Float32

    prop MinNormal: Float32

    func isInf: Bool

    func isNaN: Bool

    func isNormal: Bool

    func max(a: Float32, b: Float32, others: std.core.Array<Float32>): Float32

    func min(a: Float32, b: Float32, others: std.core.Array<Float32>): Float32
}

extend Extend <: Hashable {
    func hashCode: Int64

    prop NaN: Float16

    prop Inf: Float16

    prop Max: Float16

    prop Min: Float16

    prop MinDenormal: Float16

    prop MinNormal: Float16

    func isInf: Bool

    func isNaN: Bool

    func isNormal: Bool

    func max(a: Float16, b: Float16, others: std.core.Array<Float16>): Float16

    func min(a: Float16, b: Float16, others: std.core.Array<Float16>): Float16
}

extend Range <: Hashable {
    func hashCode: Int64
}

interface Hasher {
    func finish: Int64

    func reset: Unit

    func write(value: Bool): Unit

    func write(value: Rune): Unit

    func write(value: Int8): Unit

    func write(value: Int16): Unit

    func write(value: Int32): Unit

    func write(value: Int64): Unit

    func write(value: UInt8): Unit

    func write(value: UInt16): Unit

    func write(value: UInt32): Unit

    func write(value: UInt64): Unit

    func write(value: Float16): Unit

    func write(value: Float32): Unit

    func write(value: Float64): Unit

    func write(value: std.core.String): Unit
}

struct DefaultHasher <: std.core.Hasher {
    var res: Int64

    func finish: Int64

    func reset: Unit

    func write(value: Bool): Unit

    func write(value: Rune): Unit

    func write(value: Int8): Unit

    func write(value: Int16): Unit

    func write(value: Int32): Unit

    func write(value: Int64): Unit

    func write(value: UInt8): Unit

    func write(value: UInt16): Unit

    func write(value: UInt32): Unit

    func write(value: UInt64): Unit

    func write(value: Float16): Unit

    func write(value: Float32): Unit

    func write(value: Float64): Unit

    func write(value: std.core.String): Unit

    init(res: Int64) { /* compiled code */ }
}

func wyrand(seed: Int64): Int64

func mix(A: UInt64, B: UInt64): UInt64

func r4(arr: std.core.Array<UInt8>, pos: Int64): UInt64

func r8(arr: std.core.Array<UInt8>, pos: Int64): UInt64

class IllegalFormatException <: std.core.IllegalArgumentException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class IllegalMemoryException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class IllegalStateException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class IncompatiblePackageException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

extend Iterator {
    func contains(element: T): Bool
}

extend Iterator {
    func max: std.core.Option<T>

    func min: std.core.Option<T>
}

extend Iterator {
    func zip(it: std.core.Iterator<R>): std.core.Iterator<(T, R)>

    func enumerate: std.core.Iterator<(Int64, T)>

    func filter(predicate: (T) -> Bool): std.core.Iterator<T>

    func filterMap(transform: (T) -> std.core.Option<R>): std.core.Iterator<R>

    func map(transform: (T) -> R): std.core.Iterator<R>

    func step(count: Int64): std.core.Iterator<T>

    func skip(count: Int64): std.core.Iterator<T>

    func take(count: Int64): std.core.Iterator<T>

    func concat(other: std.core.Iterator<T>): std.core.Iterator<T>

    func inspect(action: (T) -> Unit): std.core.Iterator<T>

    func flatMap(transform: (T) -> std.core.Iterator<R>): std.core.Iterator<R>

    func intersperse(separator: T): std.core.Iterator<T>

    func all(predicate: (T) -> Bool): Bool

    func any(predicate: (T) -> Bool): Bool

    func none(predicate: (T) -> Bool): Bool

    func isEmpty: Bool

    func count: Int64

    func forEach(action: (T) -> Unit): Unit

    func first: std.core.Option<T>

    func last: std.core.Option<T>

    func at(n: Int64): std.core.Option<T>

    func reduce(operation: (T, T) -> T): std.core.Option<T>

    func fold(initial: R, operation: (R, T) -> R): R
}

func sizeOfType: UIntNative

func alignOfType: UIntNative

func malloc(size: UIntNative): CPointer<Unit>

func free(p: CPointer<Unit>): Unit

let SECUREC_MEM_MAX_LEN: Int64

func sizeOf: UIntNative

func alignOf: UIntNative

func getCommandLineArgsFromRuntime: CPointer<CPointer<UInt8>>

func getCommandLineArgs: std.core.Array<std.core.String>

func CJ_CORE_Float64ToHash(x: Float64): Int64

func CJ_CORE_Float32ToHash(x: Float32): Int64

func CJ_CONSOLE_Readline: CPointer<std.core.StructureString>

struct StructureString {
    let buffer: CPointer<UInt8>

    let length: UIntNative

    init() { /* compiled code */ }
}

func CJ_CONSOLE_ClearstdInerr: Unit

func CJ_CORE_StringSize(str: CPointer<UInt8>, len: Int64): Int64

func CJ_CORE_IndexOfChar(orgStr: CPointer<UInt8>, patC: UInt32, orgSize: Int64, start: Int64): Int64

func CJ_CORE_LastIndexOfChar(orgStr: CPointer<UInt8>, patC: UInt32, orgSize: Int64, start: Int64): Int64

func CJ_CORE_CountChar(orgStr: CPointer<UInt8>, patC: UInt32, orgSize: Int64): Int64

func CJ_MRT_GetCommandLineArgs: CPointer<CPointer<UInt8>>

class NegativeArraySizeException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

interface Any {
}

func refEq(a: std.core.Object, b: std.core.Object): Bool

func intrinsicRefEq(a: std.core.Object, b: std.core.Object): Bool

func ifSome(o: std.core.Option<T>, action: (T) -> Unit): Unit

func ifNone(o: std.core.Option<T>, action: () -> Unit): Unit

extend Option <: Equatable {
    operator func ==(that: std.core.Option<T>): Bool

    operator func !=(that: std.core.Option<T>): Bool
}

extend Option <: ToString {
    func toString: std.core.String
}

extend Option <: Hashable {
    func hashCode: Int64
}

extend Option {
    func flatten: std.core.Option<T>
}

let MAX_INT_NATIVE: IntNative

let MIN_INT_NATIVE: IntNative

let MAX_UINT_NATIVE: UIntNative

class OverflowException <: std.core.ArithmeticException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

func readln: std.core.String

var globalOutOfMemoryError: std.core.OutOfMemoryError

func throwImplicitException(id: Int64, args: CPointer<Unit>): Unit

func throwArithmeticException: Unit

func createArithmeticExceptionMsg(msg: std.core.String): std.core.ArithmeticException

func throwArithmeticExceptionMsg(msg: std.core.String): Nothing

func createOverflowExceptionMsg(msg: std.core.String): std.core.OverflowException

func throwOverflowException: Unit

func throwIndexOutOfBoundsException: Unit

func throwNegativeArraySizeException: Unit

func callToString(e: std.core.ToString): std.core.String

func checkIsError(e: std.core.Object): Bool

func throwError(msg: std.core.String): Unit

func callPrintStackTrace(e: std.core.Exception): Unit

func callPrintStackTraceError(e: std.core.Error): Unit

let MAX_TIMEOUT_DURATION: std.core.Duration

func intrinsicSleep(ns: Int64): Unit

func sleep(dur: std.core.Duration): Unit

class SpawnException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class StackOverflowError <: std.core.Error {
    let NotFolded: UInt64

    let TopFolded: UInt64

    let BottomFolded: UInt64

    var FoldedFlag: UInt64

    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String

    func printStackTrace: Unit
}

let digits100: std.core.Array<UInt8>

let EMPTY_INT64_ARRAY: std.core.Array<Int64>

func checkUtf8Of2Bytes(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Rune

func checkUtf8Of3Bytes(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Rune

func checkUtf8Of4Bytes(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Rune

func isThreadObjectInited: Bool

func getThreadObject: std.core.Thread

func CJ_MRT_SetCJThreadName(`handle`: CPointer<Unit>, name: CPointer<UInt8>, len: Int64): Unit

func CJ_MRT_GetCJThreadId(`handle`: CPointer<Unit>): Int64

func CJ_MRT_GetCurrentCJThread: CPointer<Unit>

class ThreadLocalMap {
    var entries: std.core.Array<std.core.Entry>

    var size: Int64

    init() { /* compiled code */ }

    func `get`(tlKey: std.core.ThreadLocalKey): std.core.Option<T>

    func `set`(tlKey: std.core.ThreadLocalKey, value: T): Unit

    func remove(tlKey: std.core.ThreadLocalKey): Unit
}

func setThreadObject(thread: std.core.Thread): Unit

func handleException(ex: std.core.Exception): Unit

func handleError(er: std.core.Error): Unit

let EMPTY_KEY: std.core.ThreadLocalKey

let DELETED_KEY: std.core.ThreadLocalKey

class ThreadLocal {
    let key: std.core.ThreadLocalKey

    func `get`: std.core.Option<T>

    func `set`(value: std.core.Option<T>): Unit

    init() { /* compiled code */ }
}

func getThreadLocalMap: std.core.ThreadLocalMap

struct Entry {
    let tlKey: std.core.ThreadLocalKey

    let boxedValue: std.core.Object

    init(k: std.core.ThreadLocalKey, v: std.core.Object) { /* compiled code */ }

    func isValid: Bool
}

let EMPTY_ENTRY: std.core.Entry

let DELETED_ENTRY: std.core.Entry

class TimeoutException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

extend Extend <: ToString {
    func toString: std.core.String
}

type Byte = UInt8

type Int = Int64

type UInt = UInt64

class UnsupportedException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}
