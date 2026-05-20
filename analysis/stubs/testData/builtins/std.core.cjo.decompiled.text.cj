// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.core

import std.core

func arrayInitByCollection(array: std.core.Array<T>, elements: std.core.Collection<T>): std.core.Array<T> { /* compiled code */ }

interface Collection <: std.core.Iterable<T> {
    abstract prop size: Int64

    abstract func isEmpty(): Bool

    func toArray(): std.core.Array<T> { /* compiled code */ }
}

class Iterator <: std.core.Object & std.core.Iterable<T> {
    abstract func next(): std.core.Option<T>

    init() { /* compiled code */ }

    func iterator(): std.core.Iterator<T> { /* compiled code */ }
}

interface Iterable {
    abstract func iterator(): std.core.Iterator<E>
}

enum Option {
    | Some(T)

    | None

    func getOrThrow(): T { /* compiled code */ }

    func getOrThrow(exception: () -> std.core.Exception): T { /* compiled code */ }

    func getOrDefault(other: () -> T): T { /* compiled code */ }

    func isNone(): Bool { /* compiled code */ }

    func isSome(): Bool { /* compiled code */ }

    func map(transform: (T) -> R): std.core.Option<R> { /* compiled code */ }

    func filter(predicate: (T) -> Bool): std.core.Option<T> { /* compiled code */ }

    func flatMap(transform: (T) -> std.core.Option<R>): std.core.Option<R> { /* compiled code */ }
}

func arraySet(rawArr: std.core.Array<T>, index: Int64, element: T): Unit { /* compiled code */ }

func arrayInitByFunction(array: std.core.Array<T>, initElement: (Int64) -> T): std.core.Array<T> { /* compiled code */ }

func arraySize(rawArr: std.core.Array<T>): Int64 { /* compiled code */ }

func toArray(raw: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

struct Array {
    let rawptr: std.core.Array<T>

    let start: Int64

    let len: Int64

    func toRawArray(): std.core.Array<T> { /* compiled code */ }

    init() { /* compiled code */ }

    init(size: Int64, repeat: T) { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    init(data: std.core.Array<T>, start: Int64, len: Int64) { /* compiled code */ }

    prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    func slice(start: Int64, len: Int64): std.core.Array<T> { /* compiled code */ }

    func `get`(index: Int64): std.core.Option<T> { /* compiled code */ }

    operator func [](index: Int64): T { /* compiled code */ }

    operator func [](index: Int64, value: T): Unit { /* compiled code */ }

    operator func [](range: std.core.Range<Int64>): std.core.Array<T> { /* compiled code */ }

    func fill(value: T): Unit { /* compiled code */ }

    operator func [](range: std.core.Range<Int64>, value: std.core.Array<T>): Unit { /* compiled code */ }

    func reverse(): Unit { /* compiled code */ }

    func rangeSlice(range: std.core.Range<Int64>, _: Int64): std.core.Array<T> { /* compiled code */ }

    func isLegal(start_: Int64, end_: Int64): Bool { /* compiled code */ }

    func normalize(range: std.core.Range<Int64>): (Int64, Int64) { /* compiled code */ }

    func clone(): std.core.Array<T> { /* compiled code */ }

    func clone(range: std.core.Range<Int64>): std.core.Array<T> { /* compiled code */ }

    func copyTo(dst: std.core.Array<T>, srcStart: Int64, dstStart: Int64, copyLen: Int64): Unit { /* compiled code */ }

    func copyTo(dst: std.core.Array<T>): Unit { /* compiled code */ }

    func concat(other: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

    func getUnchecked(index: Int64): T { /* compiled code */ }

    func setUnchecked(index: Int64, element: T): Unit { /* compiled code */ }

    func swap(index1: Int64, index2: Int64): Unit { /* compiled code */ }

    func splitAt(mid: Int64): (std.core.Array<T>, std.core.Array<T>) { /* compiled code */ }

    func repeat(n: Int64): std.core.Array<T> { /* compiled code */ }

    func map(transform: (T) -> R): std.core.Array<R> { /* compiled code */ }
}

func acquireArrayRawData(arr: std.core.Array<T>): std.core.CPointerHandle<T> { /* compiled code */ }

interface CType {
}

struct CPointerHandle {
    let pointer: CPointer<T>

    let array: std.core.Array<T>

    init() { /* compiled code */ }

    init(ptr: CPointer<T>, arr: std.core.Array<T>) { /* compiled code */ }
}

func acquireRawData(rawArr: std.core.Array<T>): CPointer<T> { /* compiled code */ }

func releaseArrayRawData(`handle`: std.core.CPointerHandle<T>): Unit { /* compiled code */ }

func releaseRawData(rawArr: std.core.Array<T>, ptr: CPointer<T>): Unit { /* compiled code */ }

let ASCII_UPPER_TO_LOWER_DIFF: UInt8

func max(a: T, b: T, others: std.core.Array<T>): T { /* compiled code */ }

interface Comparable <: std.core.Equatable<T> & std.core.Less<T> & std.core.Greater<T> & std.core.LessOrEqual<T> & std.core.GreaterOrEqual<T> {
    abstract func compare(that: T): std.core.Ordering

    operator func ==(rhs: T): Bool { /* compiled code */ }

    operator func <(rhs: T): Bool { /* compiled code */ }

    operator func >(rhs: T): Bool { /* compiled code */ }

    operator func <=(rhs: T): Bool { /* compiled code */ }

    operator func >=(rhs: T): Bool { /* compiled code */ }
}

func min(a: T, b: T, others: std.core.Array<T>): T { /* compiled code */ }

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

func print(str: std.core.String, flush: Bool): Unit { /* compiled code */ }

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

    func `get`(index: Int64): std.core.Option<UInt8> { /* compiled code */ }

    func acquireRaw(): CPointer<UInt8> { /* compiled code */ }

    func releaseRaw(ptr: CPointer<UInt8>): Unit { /* compiled code */ }

    func toRuneArray(): std.core.Array<Rune> { /* compiled code */ }

    func toArray(): std.core.Array<UInt8> { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func iterator(): std.core.Iterator<UInt8> { /* compiled code */ }

    func runes(): std.core.Iterator<Rune> { /* compiled code */ }

    func lines(): std.core.Iterator<std.core.String> { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func isAscii(): Bool { /* compiled code */ }

    func isAsciiBlank(): Bool { /* compiled code */ }

    func clone(): std.core.String { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    static func fromUtf8(utf8Data: std.core.Array<UInt8>): std.core.String { /* compiled code */ }

    func rawData(): std.core.Array<UInt8> { /* compiled code */ }

    static func fromUtf8Unchecked(utf8Data: std.core.Array<UInt8>): std.core.String { /* compiled code */ }

    func indexOf(b: UInt8): std.core.Option<Int64> { /* compiled code */ }

    func indexOf(b: UInt8, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    func indexOfImpl(b: UInt8, fromIndex: Int64): Int64 { /* compiled code */ }

    func indexOfCommon(b: UInt8, fromIndex: Int64): Int64 { /* compiled code */ }

    func indexOfFast(b: UInt8, fromIndex: Int64): Int64 { /* compiled code */ }

    func indexOf(str: std.core.String): std.core.Option<Int64> { /* compiled code */ }

    func indexOf(str: std.core.String, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    func lastIndexOf(b: UInt8): std.core.Option<Int64> { /* compiled code */ }

    func lastIndexOf(b: UInt8, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    func lastIndexOf(str: std.core.String): std.core.Option<Int64> { /* compiled code */ }

    func lastIndexOf(str: std.core.String, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    func count(str: std.core.String): Int64 { /* compiled code */ }

    func split(str: std.core.String, removeEmpty: Bool): std.core.Array<std.core.String> { /* compiled code */ }

    func split(str: std.core.String, maxSplits: Int64, removeEmpty: Bool): std.core.Array<std.core.String> { /* compiled code */ }

    func lazySplit(str: std.core.String, removeEmpty: Bool): std.core.Iterator<std.core.String> { /* compiled code */ }

    func lazySplit(str: std.core.String, maxSplits: Int64, removeEmpty: Bool): std.core.Iterator<std.core.String> { /* compiled code */ }

    func replace(old: std.core.String, new: std.core.String): std.core.String { /* compiled code */ }

    func insertString(new: std.core.String): std.core.String { /* compiled code */ }

    func replaceString(old: std.core.String, new: std.core.String): std.core.String { /* compiled code */ }

    func toAsciiLower(): std.core.String { /* compiled code */ }

    func toAsciiUpper(): std.core.String { /* compiled code */ }

    func toAsciiTitle(): std.core.String { /* compiled code */ }

    func trimAscii(): std.core.String { /* compiled code */ }

    func trimAsciiStart(): std.core.String { /* compiled code */ }

    func trimAsciiEnd(): std.core.String { /* compiled code */ }

    func trimStart(`set`: std.core.Array<Rune>): std.core.String { /* compiled code */ }

    func trimEnd(`set`: std.core.Array<Rune>): std.core.String { /* compiled code */ }

    func trimStart(`set`: std.core.String): std.core.String { /* compiled code */ }

    func trimEnd(`set`: std.core.String): std.core.String { /* compiled code */ }

    func trimStart(predicate: (Rune) -> Bool): std.core.String { /* compiled code */ }

    func trimEnd(predicate: (Rune) -> Bool): std.core.String { /* compiled code */ }

    func removePrefix(prefix: std.core.String): std.core.String { /* compiled code */ }

    func removeSuffix(suffix: std.core.String): std.core.String { /* compiled code */ }

    func contains(str: std.core.String): Bool { /* compiled code */ }

    func startsWith(prefix: std.core.String): Bool { /* compiled code */ }

    func endsWith(suffix: std.core.String): Bool { /* compiled code */ }

    func padStart(totalWidth: Int64, padding: std.core.String): std.core.String { /* compiled code */ }

    func padEnd(totalWidth: Int64, padding: std.core.String): std.core.String { /* compiled code */ }

    func equalsIgnoreAsciiCase(that: std.core.String): Bool { /* compiled code */ }

    func byteEqualsIgnoreAsciiCase(b1: UInt8, b2: UInt8): Bool { /* compiled code */ }

    static func compareBytes(ptr1: std.core.Array<UInt8>, start1: Int64, ptr2: std.core.Array<UInt8>, start2: Int64, num: Int64): Int64 { /* compiled code */ }

    static func compareBytesCommon(ptr1: std.core.Array<UInt8>, start1: Int64, ptr2: std.core.Array<UInt8>, start2: Int64, num: Int64): Int64 { /* compiled code */ }

    static func compareBytesFast(ptr1: std.core.Array<UInt8>, start1: Int64, ptr2: std.core.Array<UInt8>, start2: Int64, num: Int64): Int64 { /* compiled code */ }

    func compare(str: std.core.String): std.core.Ordering { /* compiled code */ }

    operator func +(right: std.core.String): std.core.String { /* compiled code */ }

    operator func *(count: Int64): std.core.String { /* compiled code */ }

    operator func <(right: std.core.String): Bool { /* compiled code */ }

    operator func <=(right: std.core.String): Bool { /* compiled code */ }

    operator func >(right: std.core.String): Bool { /* compiled code */ }

    operator func >=(right: std.core.String): Bool { /* compiled code */ }

    operator func ==(right: std.core.String): Bool { /* compiled code */ }

    operator func !=(right: std.core.String): Bool { /* compiled code */ }

    operator func [](index: Int64): UInt8 { /* compiled code */ }

    func sliceUnchecked(startIndex: Int64, len: Int64): std.core.String { /* compiled code */ }

    operator func [](range: std.core.Range<Int64>): std.core.String { /* compiled code */ }

    static func join(strArray: std.core.Array<std.core.String>, delimiter: std.core.String): std.core.String { /* compiled code */ }

    func indexOfString(str: std.core.String, startIndex: Int64, fromLast: Bool): Int64 { /* compiled code */ }

    func splitOfString(str: std.core.String, maxSplits: Int64, removeEmpty: Bool): std.core.Array<std.core.String> { /* compiled code */ }

    func splitOfStringForOneRune(maxSplits: Int64): std.core.Array<std.core.String> { /* compiled code */ }

    func getRealSplitLenWithRemoveEmpty(indexes: CPointer<Int64>, total: Int64, separatorLen: Int64): Int64 { /* compiled code */ }

    func splitAsStringArray(indexes: CPointer<Int64>, total: Int64, separatorLen: Int64, maxSplits: Int64, removeEmpty: Bool): std.core.Array<std.core.String> { /* compiled code */ }

    static func withRawData(rawData: std.core.Array<UInt8>): std.core.String { /* compiled code */ }

    static func checkUtf8Encoding(data: std.core.Array<UInt8>): Bool { /* compiled code */ }
}

foreign func CJ_CORE_PrintUTF8(c: CPointer<UInt8>, len: Int64, newLine: Bool, flush: Bool): Int64

class OutOfMemoryError <: std.core.Error {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

func println(): Unit { /* compiled code */ }

foreign func CJ_CORE_PrintChar(c: UInt32, newLine: Bool, flush: Bool): Unit

func println(str: std.core.String): Unit { /* compiled code */ }

func eprint(str: std.core.String, flush: Bool): Unit { /* compiled code */ }

foreign func CJ_CORE_ErrorPrintUTF8(c: CPointer<UInt8>, len: Int64, newLine: Bool, flush: Bool): Int64

func eprintln(str: std.core.String): Unit { /* compiled code */ }

func eprint(arg: T, flush: Bool): Unit { /* compiled code */ }

interface ToString {
    abstract func toString(): std.core.String
}

func eprintln(arg: T): Unit { /* compiled code */ }

func print(arg: T, flush: Bool): Unit { /* compiled code */ }

func println(arg: T): Unit { /* compiled code */ }

func print(b: Bool, flush: Bool): Unit { /* compiled code */ }

foreign func CJ_CORE_PrintBool(b: Bool, newLine: Bool, flush: Bool): Unit

func println(b: Bool): Unit { /* compiled code */ }

func print(c: Rune, flush: Bool): Unit { /* compiled code */ }

func println(c: Rune): Unit { /* compiled code */ }

func print(f: Float16, flush: Bool): Unit { /* compiled code */ }

foreign func CJ_CORE_PrintFloat(f: Float64, newLine: Bool, flush: Bool): Unit

func println(f: Float16): Unit { /* compiled code */ }

func print(f: Float32, flush: Bool): Unit { /* compiled code */ }

func println(f: Float32): Unit { /* compiled code */ }

func print(f: Float64, flush: Bool): Unit { /* compiled code */ }

func println(f: Float64): Unit { /* compiled code */ }

func print(i: Int8, flush: Bool): Unit { /* compiled code */ }

foreign func CJ_CORE_PrintSigned(i: Int64, newLine: Bool, flush: Bool): Unit

func print(i: Int16, flush: Bool): Unit { /* compiled code */ }

func print(i: Int32, flush: Bool): Unit { /* compiled code */ }

func print(i: Int64, flush: Bool): Unit { /* compiled code */ }

func print(i: UInt8, flush: Bool): Unit { /* compiled code */ }

foreign func CJ_CORE_PrintUnsigned(i: UInt64, newLine: Bool, flush: Bool): Unit

func print(i: UInt16, flush: Bool): Unit { /* compiled code */ }

func print(i: UInt32, flush: Bool): Unit { /* compiled code */ }

func print(i: UInt64, flush: Bool): Unit { /* compiled code */ }

func println(i: Int8): Unit { /* compiled code */ }

func println(i: Int16): Unit { /* compiled code */ }

func println(i: Int32): Unit { /* compiled code */ }

func println(i: Int64): Unit { /* compiled code */ }

func println(i: UInt8): Unit { /* compiled code */ }

func println(i: UInt16): Unit { /* compiled code */ }

func println(i: UInt32): Unit { /* compiled code */ }

func println(i: UInt64): Unit { /* compiled code */ }

let MAX_TIMEOUT_NS: Int64

let UNICODE_TO_UTF8_MAX_LEN: Int64

let IS_SIMD_SUPPORTED: Bool

foreign func CJ_CORE_CanUseSIMD(): Bool

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

func zeroValue(): T { /* compiled code */ }

func intrinsicBuiltInCopyTo(src: std.core.Array<T>, dst: std.core.Array<T>, srcStart: Int64, dstStart: Int64, copyLen: Int64): Unit { /* compiled code */ }

class IndexOutOfBoundsException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
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

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    func toString(): std.core.String { /* compiled code */ }

    func reset(capacity: std.core.Option<Int64>): Unit { /* compiled code */ }

    func append(r: Rune): Unit { /* compiled code */ }

    func append(str: std.core.String): Unit { /* compiled code */ }

    func append(sb: std.core.StringBuilder): Unit { /* compiled code */ }

    func appendFromUtf8(arr: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func appendFromUtf8Unchecked(arr: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func append(runeArr: std.core.Array<Rune>): Unit { /* compiled code */ }

    func append(cstr: CString): Unit { /* compiled code */ }

    func append(v: T): Unit { /* compiled code */ }

    func append(val: std.core.Array<T>): Unit { /* compiled code */ }

    func append(b: Bool): Unit { /* compiled code */ }

    func append(n: Int64): Unit { /* compiled code */ }

    func append(n: Int32): Unit { /* compiled code */ }

    func append(n: Int16): Unit { /* compiled code */ }

    func append(n: Int8): Unit { /* compiled code */ }

    func append(n: UInt64): Unit { /* compiled code */ }

    func append(n: UInt32): Unit { /* compiled code */ }

    func append(n: UInt16): Unit { /* compiled code */ }

    func append(n: UInt8): Unit { /* compiled code */ }

    func append(n: Float64): Unit { /* compiled code */ }

    func append(n: Float32): Unit { /* compiled code */ }

    func append(n: Float16): Unit { /* compiled code */ }

    func reserve(additional: Int64): Unit { /* compiled code */ }

    func grow(minCapacity: Int64): Unit { /* compiled code */ }

    func append(cp: CPointer<UInt8>): Unit { /* compiled code */ }
}

func arrayGetUnchecked(rawArr: std.core.Array<T>, index: Int64): T { /* compiled code */ }

func arraySetUnchecked(rawArr: std.core.Array<T>, index: Int64, element: T): Unit { /* compiled code */ }

struct Range <: std.core.Iterable<T> {
    let start: T

    let end: T

    let step: Int64

    let hasStart: Bool

    let hasEnd: Bool

    let isClosed: Bool

    init(start: T, end: T, step: Int64, hasStart: Bool, hasEnd: Bool, isClosed: Bool) { /* compiled code */ }

    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    func last(): T { /* compiled code */ }
}

class IllegalArgumentException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

func arrayGet(rawArr: std.core.Array<T>, index: Int64): T { /* compiled code */ }

class ArrayIterator <: std.core.Iterator<T> {
    var myData: std.core.Array<T>

    let end: Int64

    var myPosition: Int64

    init(data: std.core.Array<T>) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

interface Equatable <: std.core.Equal<T> & std.core.NotEqual<T> {
    operator func !=(rhs: T): Bool { /* compiled code */ }
}

interface Equal {
    abstract operator func ==(rhs: T): Bool
}

func readPointer(c: CPointer<T>, idx: Int64): T { /* compiled code */ }

func writePointer(c: CPointer<T>, idx: Int64, value: T): Unit { /* compiled code */ }

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

    static func of(sec: Int64, ns: Int64): std.core.Duration { /* compiled code */ }

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

    func toNanoseconds(): Int64 { /* compiled code */ }

    func toMicroseconds(): Int64 { /* compiled code */ }

    func toMilliseconds(): Int64 { /* compiled code */ }

    func toSeconds(): Int64 { /* compiled code */ }

    func toMinutes(): Int64 { /* compiled code */ }

    func toHours(): Int64 { /* compiled code */ }

    func toDays(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func abs(): std.core.Duration { /* compiled code */ }

    operator func +(r: std.core.Duration): std.core.Duration { /* compiled code */ }

    operator func -(r: std.core.Duration): std.core.Duration { /* compiled code */ }

    operator func *(r: Int64): std.core.Duration { /* compiled code */ }

    operator func *(r: Float64): std.core.Duration { /* compiled code */ }

    func toFloat64Nanoseconds(): Float64 { /* compiled code */ }

    operator func /(r: Int64): std.core.Duration { /* compiled code */ }

    operator func /(r: Float64): std.core.Duration { /* compiled code */ }

    operator func /(r: std.core.Duration): Float64 { /* compiled code */ }

    operator func ==(r: std.core.Duration): Bool { /* compiled code */ }

    operator func !=(r: std.core.Duration): Bool { /* compiled code */ }

    operator func >=(r: std.core.Duration): Bool { /* compiled code */ }

    operator func >(r: std.core.Duration): Bool { /* compiled code */ }

    operator func <=(r: std.core.Duration): Bool { /* compiled code */ }

    operator func <(r: std.core.Duration): Bool { /* compiled code */ }

    func compare(rhs: std.core.Duration): std.core.Ordering { /* compiled code */ }

    func toUnsignedNum(r: Int64): (UInt64, UInt32, UInt64, Bool) { /* compiled code */ }
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

    func setRuntimeCJThreadHandle(`handle`: CPointer<Unit>): Unit { /* compiled code */ }

    func waitForCJThreadHandle(): Unit { /* compiled code */ }

    func clearRuntimeCJThreadHandle(): Unit { /* compiled code */ }

    static prop currentThread: std.core.Thread {
        get() { /* compiled code */ }
    }

    prop id: Int64 {
        get() { /* compiled code */ }
    }

    mut prop name: std.core.String {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop hasPendingCancellation: Bool {
        get() { /* compiled code */ }
    }

    func setCancellationRequest(): Unit { /* compiled code */ }

    static func handleUncaughtExceptionBy(exHandler: (std.core.Thread, std.core.Exception) -> Unit): Unit { /* compiled code */ }
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

    func next(): std.core.Option<R> { /* compiled code */ }
}

class ZipIterator <: std.core.Iterator<(T, R)> {
    let itt: std.core.Iterator<T>

    let itr: std.core.Iterator<R>

    init(itt: std.core.Iterator<T>, itr: std.core.Iterator<R>) { /* compiled code */ }

    func next(): std.core.Option<(T, R)> { /* compiled code */ }
}

class EnumerateIterator <: std.core.Iterator<(Int64, T)> {
    var index: Int64

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>) { /* compiled code */ }

    func next(): std.core.Option<(Int64, T)> { /* compiled code */ }
}

class FilterIterator <: std.core.Iterator<T> {
    let predicate: (T) -> Bool

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, predicate: (T) -> Bool) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

class FilterMapIterator <: std.core.Iterator<R> {
    let transform: (T) -> std.core.Option<R>

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, transform: (T) -> std.core.Option<R>) { /* compiled code */ }

    func next(): std.core.Option<R> { /* compiled code */ }
}

class StepIterator <: std.core.Iterator<T> {
    let _count: Int64

    let it: std.core.Iterator<T>

    var index: Int64

    init(it: std.core.Iterator<T>, count: Int64) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

class SkipIterator <: std.core.Iterator<T> {
    var _count: Int64

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, count: Int64) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

class TakeIterator <: std.core.Iterator<T> {
    var _count: Int64

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, count: Int64) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

class ConcatIterator <: std.core.Iterator<T> {
    let it: std.core.Iterator<T>

    let other: std.core.Iterator<T>

    var itOver: Bool

    init(it: std.core.Iterator<T>, other: std.core.Iterator<T>) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

class InspectIterator <: std.core.Iterator<T> {
    let action: (T) -> Unit

    let it: std.core.Iterator<T>

    init(it: std.core.Iterator<T>, action: (T) -> Unit) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

class FlatMapIterator <: std.core.Iterator<R> {
    var subIt: std.core.Option<std.core.Iterator<R>>

    let transform: (T) -> std.core.Iterable<R>

    let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>, transform: (T) -> std.core.Iterable<R>) { /* compiled code */ }

    func next(): std.core.Option<R> { /* compiled code */ }
}

class IntersperseIterator <: std.core.Iterator<T> {
    let separator: T

    let it: std.core.Iterator<T>

    var item: std.core.Option<T>

    var isInsert: Bool

    init(it: std.core.Iterator<T>, separator: T) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

class NoneValueException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class Exception <: std.core.Object & std.core.ToString {
    var detailMessage: std.core.String

    var pcArray: std.core.Array<UInt64>

    var traceElementArray: std.core.Array<std.core.StackTraceElement>

    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName(): std.core.String { /* compiled code */ }

    prop message: std.core.String {
        get() { /* compiled code */ }
    }

    func toString(): std.core.String { /* compiled code */ }

    func printStackTrace(): Unit { /* compiled code */ }

    func getStackTrace(): std.core.Array<std.core.StackTraceElement> { /* compiled code */ }
}

interface Countable {
    abstract func next(right: Int64): T

    abstract func position(): Int64
}

class RangeIterator <: std.core.Iterator<T> {
    let _last: T

    let _step: Int64

    var cur: T

    var hasNext: Bool

    init(first: T, last: T, step: Int64, isEmpty: Bool) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

struct RunesToUtf8Bytes {
    static func getBytes(runes: std.core.Array<Rune>): std.core.Array<UInt8> { /* compiled code */ }

    static func getBytes(elements: std.core.Collection<Rune>): std.core.Array<UInt8> { /* compiled code */ }

    static func getSize(runes: std.core.Array<Rune>): Int64 { /* compiled code */ }

    init() { /* compiled code */ }
}

foreign func memcpy_s(dest: CPointer<UInt8>, destMax: UIntNative, src: CPointer<UInt8>, count: UIntNative): Int32

func utf8RuneSize(arr: std.core.Array<UInt8>, start: Int64, length: Int64): Int64 { /* compiled code */ }

class StringByteIterator <: std.core.Iterator<UInt8> {
    func next(): std.core.Option<UInt8> { /* compiled code */ }

    let iteratorData: std.core.StringIteratorData

    StringByteIterator(iteratorData: std.core.StringIteratorData) { /* compiled code */ }
}

class StringIteratorData {
    let myData: std.core.Array<UInt8>

    var end: Int64

    var nextPos: Int64

    init(data: std.core.Array<UInt8>) { /* compiled code */ }

    init(data: std.core.Array<UInt8>) { /* compiled code */ }
}

class StringRuneIterator <: std.core.Iterator<Rune> {
    func next(): std.core.Option<Rune> { /* compiled code */ }

    let iteratorData: std.core.StringIteratorData

    StringRuneIterator(iteratorData: std.core.StringIteratorData) { /* compiled code */ }
}

class StringLineIterator <: std.core.Iterator<std.core.String> {
    func next(): std.core.Option<std.core.String> { /* compiled code */ }

    let iteratorData: std.core.StringIteratorData

    StringLineIterator(iteratorData: std.core.StringIteratorData) { /* compiled code */ }
}

func wyhash(arr: std.core.Array<UInt8>, start: Int64, size: Int64, see: UInt64): UInt64 { /* compiled code */ }

func checkInvalid(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Unit { /* compiled code */ }

func vectorIndexByte32(arr: std.core.Array<UInt8>, offset: Int64, target: UInt8): Int64 { /* compiled code */ }

foreign func CJ_CORE_CountString(orgStr: CPointer<UInt8>, subStr: CPointer<UInt8>, orgSize: Int64, subSize: Int64): Int64

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

    func next(): std.core.Option<std.core.String> { /* compiled code */ }
}

foreign func CJ_CORE_CountAndIndexString(orgStr: CPointer<UInt8>, subStr: CPointer<UInt8>, orgSize: Int64, subSize: Int64): CPointer<Int64>

struct LibC {
    static func malloc(count: Int64): CPointer<T> { /* compiled code */ }

    static func free(p: CPointer<T>): Unit { /* compiled code */ }

    static func mallocCString(str: std.core.String): CString { /* compiled code */ }

    static func free(cstr: CString): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

foreign func CJ_CORE_StringMemcmp(str1: CPointer<UInt8>, str2: CPointer<UInt8>, n: UIntNative): Int64

func vectorCompare32(arr1: std.core.Array<UInt8>, offset1: Int64, arr2: std.core.Array<UInt8>, offset2: Int64): Int64 { /* compiled code */ }

func intrinsicRefEqRawArray(a: std.core.Array<T>, b: std.core.Array<T>): Bool { /* compiled code */ }

foreign func CJ_CORE_LastIndexOfString(orgStr: CPointer<UInt8>, subStr: CPointer<UInt8>, orgSize: Int64, subSize: Int64, start: Int64): Int64

foreign func CJ_CORE_IndexOfString(orgStr: CPointer<UInt8>, subStr: CPointer<UInt8>, orgSize: Int64, subSize: Int64, start: Int64): Int64

func checkUtf8Of2BytesWithoutException(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Bool { /* compiled code */ }

func checkUtf8Of3BytesWithoutException(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Bool { /* compiled code */ }

func checkUtf8Of4BytesWithoutException(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Bool { /* compiled code */ }

func calcNumLen(num: UInt64): Int64 { /* compiled code */ }

func numberToString(number: UInt64, endIndex: Int64, buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

foreign func CJ_CORE_Float64ToCPointer(num: Float64): CPointer<UInt8>

foreign func strlen(str: CPointer<UInt8>): UIntNative

class ThreadLocalKey <: std.core.Object & std.core.Hashable {
    let ALPHA: Int64

    let CURR: std.core.AtomicInt64

    static func nextHash(): Int64 { /* compiled code */ }

    let code: Int64

    func equals(another: std.core.ThreadLocalKey): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

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

    override func getClassName(): std.core.String { /* compiled code */ }
}

extend Array {
    func flatten(): std.core.Array<T> { /* compiled code */ }
}

extend Array <: Collection {
    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }
}

extend Array <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend Array <: Equatable {
    operator func ==(that: std.core.Array<T>): Bool { /* compiled code */ }

    operator func !=(that: std.core.Array<T>): Bool { /* compiled code */ }

    func contains(element: T): Bool { /* compiled code */ }

    func indexOf(element: T): std.core.Option<Int64> { /* compiled code */ }

    func indexOf(element: T, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    func indexOf(elements: std.core.Array<T>): std.core.Option<Int64> { /* compiled code */ }

    func indexOf(elements: std.core.Array<T>, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    func lastIndexOf(element: T): std.core.Option<Int64> { /* compiled code */ }

    func lastIndexOf(element: T, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    func lastIndexOf(elements: std.core.Array<T>): std.core.Option<Int64> { /* compiled code */ }

    func lastIndexOf(elements: std.core.Array<T>, fromIndex: Int64): std.core.Option<Int64> { /* compiled code */ }

    func trimStart(`set`: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

    func trimEnd(`set`: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

    func trimStart(predicate: (T) -> Bool): std.core.Array<T> { /* compiled code */ }

    func trimEnd(predicate: (T) -> Bool): std.core.Array<T> { /* compiled code */ }

    func removePrefix(prefix: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

    func removeSuffix(suffix: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }
}

func arrayClone(rawArr: std.core.Array<T>): std.core.Array<T> { /* compiled code */ }

foreign func CJ_CORE_AtExitCallbackListLock(): Unit

foreign func CJ_CORE_AtExitCallbackListUnlook(): Unit

class AtexitCallbackNode {
    var atexitCallback: () -> Unit

    var next: std.core.Option<std.core.AtexitCallbackNode>

    init(callback: () -> Unit) { /* compiled code */ }
}

var atexitCallbackHead: std.core.Option<std.core.AtexitCallbackNode>

var callbacksRunningFlag: Bool

func CJ_CORE_AddAtexitCallback(callback: () -> Unit): Unit { /* compiled code */ }

func CJ_CORE_ExecAtexitCallbacks(): Unit { /* compiled code */ }

enum MemoryOrder {
    | SeqCst
}

func load(atomicType: ATOMIC_TYPE, memoryOrder: std.core.MemoryOrder): TYPE { /* compiled code */ }

func store(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.core.MemoryOrder): Unit { /* compiled code */ }

func fetchAdd(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.core.MemoryOrder): TYPE { /* compiled code */ }

func compareAndSwap(atomicType: ATOMIC_TYPE, oldValue: TYPE, newValue: TYPE, successOrder: std.core.MemoryOrder, failureOrder: std.core.MemoryOrder): Bool { /* compiled code */ }

class AtomicBool {
    var value: Int8

    init(val: Bool) { /* compiled code */ }

    func load(): Bool { /* compiled code */ }

    func store(val: Bool): Unit { /* compiled code */ }
}

class AtomicInt64 {
    var value: Int64

    init(val: Int64) { /* compiled code */ }

    func fetchAdd(val: Int64): Int64 { /* compiled code */ }

    func load(): Int64 { /* compiled code */ }

    func store(val: Int64): Unit { /* compiled code */ }

    func compareAndSwap(old: Int64, new: Int64): Bool { /* compiled code */ }
}

class AtomicReference {
    var value: T

    init(val: T) { /* compiled code */ }

    func load(): T { /* compiled code */ }

    func store(val: T): Unit { /* compiled code */ }
}

class AtomicBox {
    let boxRef: std.core.AtomicReference<std.core.Box<T>>

    init(val: T) { /* compiled code */ }

    func load(): T { /* compiled code */ }

    func store(val: T): Unit { /* compiled code */ }
}

extend Box <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }
}

interface Hashable {
    abstract func hashCode(): Int64
}

extend Box <: Comparable {
    operator func ==(that: std.core.Box<T>): Bool { /* compiled code */ }

    operator func !=(that: std.core.Box<T>): Bool { /* compiled code */ }

    operator func >(that: std.core.Box<T>): Bool { /* compiled code */ }

    operator func <(that: std.core.Box<T>): Bool { /* compiled code */ }

    operator func >=(that: std.core.Box<T>): Bool { /* compiled code */ }

    operator func <=(that: std.core.Box<T>): Bool { /* compiled code */ }

    func compare(that: std.core.Box<T>): std.core.Ordering { /* compiled code */ }
}

extend Box <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` {
    func isAsciiLetter(): Bool { /* compiled code */ }

    func isAsciiNumber(): Bool { /* compiled code */ }

    func isAsciiHex(): Bool { /* compiled code */ }

    func isAsciiOct(): Bool { /* compiled code */ }

    func isAsciiPunctuation(): Bool { /* compiled code */ }

    func isAsciiGraphic(): Bool { /* compiled code */ }

    func isAsciiControl(): Bool { /* compiled code */ }

    func isAsciiNumberOrLetter(): Bool { /* compiled code */ }

    func isAsciiLowerCase(): Bool { /* compiled code */ }

    func isAsciiUpperCase(): Bool { /* compiled code */ }

    func isAscii(): Bool { /* compiled code */ }

    func isAsciiWhiteSpace(): Bool { /* compiled code */ }

    func toAsciiUpperCase(): UInt8 { /* compiled code */ }

    func toAsciiLowerCase(): UInt8 { /* compiled code */ }
}

func getPointerAddress(c: CPointer<T>): UIntNative { /* compiled code */ }

func addPointer(c: CPointer<T>, offset: Int64): CPointer<T> { /* compiled code */ }

extend `Unit` {
    func isNull(): Bool { /* compiled code */ }

    func isNotNull(): Bool { /* compiled code */ }

    func toUIntNative(): UIntNative { /* compiled code */ }

    func read(): T { /* compiled code */ }

    func write(value: T): Unit { /* compiled code */ }

    func read(idx: Int64): T { /* compiled code */ }

    func write(idx: Int64, value: T): Unit { /* compiled code */ }

    operator func +(offset: Int64): CPointer<T> { /* compiled code */ }

    operator func -(offset: Int64): CPointer<T> { /* compiled code */ }

    func asResource(): std.core.CPointerResource<T> { /* compiled code */ }
}

struct CPointerResource <: std.core.Resource {
    let value: CPointer<T>

    let isFree: std.core.Box<Bool>

    init(p: CPointer<T>) { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    func close(): Unit { /* compiled code */ }
}

interface Resource {
    abstract func isClosed(): Bool

    abstract func close(): Unit
}

foreign func strcmp(source: CPointer<UInt8>, target: CPointer<UInt8>): Int32

foreign func strncmp(source: CPointer<UInt8>, target: CPointer<UInt8>, cmpSize: UIntNative): Int32

foreign func strcasecmp(source: CPointer<UInt8>, target: CPointer<UInt8>): Int32

func convertCStr2Ptr(cs: CString): CPointer<UInt8> { /* compiled code */ }

func createStrFromCPointer(cp: CPointer<UInt8>): std.core.String { /* compiled code */ }

extend `Unit` <: ToString {
    func getChars(): CPointer<UInt8> { /* compiled code */ }

    func isNull(): Bool { /* compiled code */ }

    func size(): Int64 { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    func isNotEmpty(): Bool { /* compiled code */ }

    func startsWith(prefix: CString): Bool { /* compiled code */ }

    func endsWith(suffix: CString): Bool { /* compiled code */ }

    func equals(rhs: CString): Bool { /* compiled code */ }

    func equalsLower(rhs: CString): Bool { /* compiled code */ }

    func subCString(beginIndex: UIntNative): CString { /* compiled code */ }

    func subCString(beginIndex: UIntNative, subLen: UIntNative): CString { /* compiled code */ }

    func compare(str: CString): Int32 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func asResource(): std.core.CStringResource { /* compiled code */ }
}

struct CStringResource <: std.core.Resource {
    let value: CString

    let isFree: std.core.Box<Bool>

    init(p: CString) { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    func close(): Unit { /* compiled code */ }
}

extend `Unit` {
    func isAsciiLetter(): Bool { /* compiled code */ }

    func isAsciiNumber(): Bool { /* compiled code */ }

    func isAsciiHex(): Bool { /* compiled code */ }

    func isAsciiOct(): Bool { /* compiled code */ }

    func isAsciiPunctuation(): Bool { /* compiled code */ }

    func isAsciiGraphic(): Bool { /* compiled code */ }

    func isAsciiControl(): Bool { /* compiled code */ }

    func isAsciiNumberOrLetter(): Bool { /* compiled code */ }

    func isAsciiLowerCase(): Bool { /* compiled code */ }

    func isAsciiUpperCase(): Bool { /* compiled code */ }

    func isAscii(): Bool { /* compiled code */ }

    func isAsciiWhiteSpace(): Bool { /* compiled code */ }

    func toAsciiUpperCase(): Rune { /* compiled code */ }

    func toAsciiLowerCase(): Rune { /* compiled code */ }

    static func utf8Size(arr: std.core.Array<UInt8>, index: Int64): Int64 { /* compiled code */ }

    static func utf8SizeUnchecked(byte: UInt8): Int64 { /* compiled code */ }

    static func utf8Size(c: Rune): Int64 { /* compiled code */ }

    static func fromUtf8(arr: std.core.Array<UInt8>, index: Int64): (Rune, Int64) { /* compiled code */ }

    static func fromUtf8(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): (Rune, Int64) { /* compiled code */ }

    static func getPreviousFromUtf8(arr: std.core.Array<UInt8>, index: Int64): (Rune, Int64) { /* compiled code */ }

    static func intoUtf8Array(c: Rune, arr: std.core.Array<UInt8>, index: Int64): Int64 { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: Rune): std.core.Ordering { /* compiled code */ }
}

func Int64Less(a: Int64, b: Int64): Bool { /* compiled code */ }

func Int64Greater(a: Int64, b: Int64): Bool { /* compiled code */ }

func Int64LessOrEqual(a: Int64, b: Int64): Bool { /* compiled code */ }

func Int64GreaterOrEqual(a: Int64, b: Int64): Bool { /* compiled code */ }

func Int64Equal(a: Int64, b: Int64): Bool { /* compiled code */ }

func Int64NotEqual(a: Int64, b: Int64): Bool { /* compiled code */ }

func Int32Less(a: Int32, b: Int32): Bool { /* compiled code */ }

func Int32Greater(a: Int32, b: Int32): Bool { /* compiled code */ }

func Int32LessOrEqual(a: Int32, b: Int32): Bool { /* compiled code */ }

func Int32GreaterOrEqual(a: Int32, b: Int32): Bool { /* compiled code */ }

func Int32Equal(a: Int32, b: Int32): Bool { /* compiled code */ }

func Int32NotEqual(a: Int32, b: Int32): Bool { /* compiled code */ }

func Int16Less(a: Int16, b: Int16): Bool { /* compiled code */ }

func Int16Greater(a: Int16, b: Int16): Bool { /* compiled code */ }

func Int16LessOrEqual(a: Int16, b: Int16): Bool { /* compiled code */ }

func Int16GreaterOrEqual(a: Int16, b: Int16): Bool { /* compiled code */ }

func Int16Equal(a: Int16, b: Int16): Bool { /* compiled code */ }

func Int16NotEqual(a: Int16, b: Int16): Bool { /* compiled code */ }

func Int8Less(a: Int8, b: Int8): Bool { /* compiled code */ }

func Int8Greater(a: Int8, b: Int8): Bool { /* compiled code */ }

func Int8LessOrEqual(a: Int8, b: Int8): Bool { /* compiled code */ }

func Int8GreaterOrEqual(a: Int8, b: Int8): Bool { /* compiled code */ }

func Int8Equal(a: Int8, b: Int8): Bool { /* compiled code */ }

func Int8NotEqual(a: Int8, b: Int8): Bool { /* compiled code */ }

func UInt64Less(a: UInt64, b: UInt64): Bool { /* compiled code */ }

func UInt64Greater(a: UInt64, b: UInt64): Bool { /* compiled code */ }

func UInt64LessOrEqual(a: UInt64, b: UInt64): Bool { /* compiled code */ }

func UInt64GreaterOrEqual(a: UInt64, b: UInt64): Bool { /* compiled code */ }

func UInt64Equal(a: UInt64, b: UInt64): Bool { /* compiled code */ }

func UInt64NotEqual(a: UInt64, b: UInt64): Bool { /* compiled code */ }

func UInt32Less(a: UInt32, b: UInt32): Bool { /* compiled code */ }

func UInt32Greater(a: UInt32, b: UInt32): Bool { /* compiled code */ }

func UInt32LessOrEqual(a: UInt32, b: UInt32): Bool { /* compiled code */ }

func UInt32GreaterOrEqual(a: UInt32, b: UInt32): Bool { /* compiled code */ }

func UInt32Equal(a: UInt32, b: UInt32): Bool { /* compiled code */ }

func UInt32NotEqual(a: UInt32, b: UInt32): Bool { /* compiled code */ }

func UInt16Less(a: UInt16, b: UInt16): Bool { /* compiled code */ }

func UInt16Greater(a: UInt16, b: UInt16): Bool { /* compiled code */ }

func UInt16LessOrEqual(a: UInt16, b: UInt16): Bool { /* compiled code */ }

func UInt16GreaterOrEqual(a: UInt16, b: UInt16): Bool { /* compiled code */ }

func UInt16Equal(a: UInt16, b: UInt16): Bool { /* compiled code */ }

func UInt16NotEqual(a: UInt16, b: UInt16): Bool { /* compiled code */ }

func UInt8Less(a: UInt8, b: UInt8): Bool { /* compiled code */ }

func UInt8Greater(a: UInt8, b: UInt8): Bool { /* compiled code */ }

func UInt8LessOrEqual(a: UInt8, b: UInt8): Bool { /* compiled code */ }

func UInt8GreaterOrEqual(a: UInt8, b: UInt8): Bool { /* compiled code */ }

func UInt8Equal(a: UInt8, b: UInt8): Bool { /* compiled code */ }

func UInt8NotEqual(a: UInt8, b: UInt8): Bool { /* compiled code */ }

func Float16Less(a: Float16, b: Float16): Bool { /* compiled code */ }

func Float16Greater(a: Float16, b: Float16): Bool { /* compiled code */ }

func Float16LessOrEqual(a: Float16, b: Float16): Bool { /* compiled code */ }

func Float16GreaterOrEqual(a: Float16, b: Float16): Bool { /* compiled code */ }

func Float16Equal(a: Float16, b: Float16): Bool { /* compiled code */ }

func Float16NotEqual(a: Float16, b: Float16): Bool { /* compiled code */ }

func Float32Less(a: Float32, b: Float32): Bool { /* compiled code */ }

func Float32Greater(a: Float32, b: Float32): Bool { /* compiled code */ }

func Float32LessOrEqual(a: Float32, b: Float32): Bool { /* compiled code */ }

func Float32GreaterOrEqual(a: Float32, b: Float32): Bool { /* compiled code */ }

func Float32Equal(a: Float32, b: Float32): Bool { /* compiled code */ }

func Float32NotEqual(a: Float32, b: Float32): Bool { /* compiled code */ }

func Float64Less(a: Float64, b: Float64): Bool { /* compiled code */ }

func Float64Greater(a: Float64, b: Float64): Bool { /* compiled code */ }

func Float64LessOrEqual(a: Float64, b: Float64): Bool { /* compiled code */ }

func Float64GreaterOrEqual(a: Float64, b: Float64): Bool { /* compiled code */ }

func Float64Equal(a: Float64, b: Float64): Bool { /* compiled code */ }

func Float64NotEqual(a: Float64, b: Float64): Bool { /* compiled code */ }

interface Less {
    abstract operator func <(rhs: T): Bool
}

interface Greater {
    abstract operator func >(rhs: T): Bool
}

interface LessOrEqual {
    abstract operator func <=(rhs: T): Bool
}

interface GreaterOrEqual {
    abstract operator func >=(rhs: T): Bool
}

extend Ordering <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend Ordering <: Comparable {
    func compare(that: std.core.Ordering): std.core.Ordering { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: IntNative): std.core.Ordering { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: Int64): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: Int64): Bool { /* compiled code */ }

    operator func >(rhs: Int64): Bool { /* compiled code */ }

    operator func <=(rhs: Int64): Bool { /* compiled code */ }

    operator func >=(rhs: Int64): Bool { /* compiled code */ }

    operator func ==(rhs: Int64): Bool { /* compiled code */ }

    operator func !=(rhs: Int64): Bool { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: Int32): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: Int32): Bool { /* compiled code */ }

    operator func >(rhs: Int32): Bool { /* compiled code */ }

    operator func <=(rhs: Int32): Bool { /* compiled code */ }

    operator func >=(rhs: Int32): Bool { /* compiled code */ }

    operator func ==(rhs: Int32): Bool { /* compiled code */ }

    operator func !=(rhs: Int32): Bool { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: Int16): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: Int16): Bool { /* compiled code */ }

    operator func >(rhs: Int16): Bool { /* compiled code */ }

    operator func <=(rhs: Int16): Bool { /* compiled code */ }

    operator func >=(rhs: Int16): Bool { /* compiled code */ }

    operator func ==(rhs: Int16): Bool { /* compiled code */ }

    operator func !=(rhs: Int16): Bool { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: Int8): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: Int8): Bool { /* compiled code */ }

    operator func >(rhs: Int8): Bool { /* compiled code */ }

    operator func <=(rhs: Int8): Bool { /* compiled code */ }

    operator func >=(rhs: Int8): Bool { /* compiled code */ }

    operator func ==(rhs: Int8): Bool { /* compiled code */ }

    operator func !=(rhs: Int8): Bool { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: UIntNative): std.core.Ordering { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: UInt64): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: UInt64): Bool { /* compiled code */ }

    operator func >(rhs: UInt64): Bool { /* compiled code */ }

    operator func <=(rhs: UInt64): Bool { /* compiled code */ }

    operator func >=(rhs: UInt64): Bool { /* compiled code */ }

    operator func ==(rhs: UInt64): Bool { /* compiled code */ }

    operator func !=(rhs: UInt64): Bool { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: UInt32): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: UInt32): Bool { /* compiled code */ }

    operator func >(rhs: UInt32): Bool { /* compiled code */ }

    operator func <=(rhs: UInt32): Bool { /* compiled code */ }

    operator func >=(rhs: UInt32): Bool { /* compiled code */ }

    operator func ==(rhs: UInt32): Bool { /* compiled code */ }

    operator func !=(rhs: UInt32): Bool { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: UInt16): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: UInt16): Bool { /* compiled code */ }

    operator func >(rhs: UInt16): Bool { /* compiled code */ }

    operator func <=(rhs: UInt16): Bool { /* compiled code */ }

    operator func >=(rhs: UInt16): Bool { /* compiled code */ }

    operator func ==(rhs: UInt16): Bool { /* compiled code */ }

    operator func !=(rhs: UInt16): Bool { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: UInt8): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: UInt8): Bool { /* compiled code */ }

    operator func >(rhs: UInt8): Bool { /* compiled code */ }

    operator func <=(rhs: UInt8): Bool { /* compiled code */ }

    operator func >=(rhs: UInt8): Bool { /* compiled code */ }

    operator func ==(rhs: UInt8): Bool { /* compiled code */ }

    operator func !=(rhs: UInt8): Bool { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: Float16): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: Float16): Bool { /* compiled code */ }

    operator func >(rhs: Float16): Bool { /* compiled code */ }

    operator func <=(rhs: Float16): Bool { /* compiled code */ }

    operator func >=(rhs: Float16): Bool { /* compiled code */ }

    operator func ==(rhs: Float16): Bool { /* compiled code */ }

    operator func !=(rhs: Float16): Bool { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: Float32): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: Float32): Bool { /* compiled code */ }

    operator func >(rhs: Float32): Bool { /* compiled code */ }

    operator func <=(rhs: Float32): Bool { /* compiled code */ }

    operator func >=(rhs: Float32): Bool { /* compiled code */ }

    operator func ==(rhs: Float32): Bool { /* compiled code */ }

    operator func !=(rhs: Float32): Bool { /* compiled code */ }
}

extend `Unit` <: Comparable {
    func compare(rhs: Float64): std.core.Ordering { /* compiled code */ }

    operator func <(rhs: Float64): Bool { /* compiled code */ }

    operator func >(rhs: Float64): Bool { /* compiled code */ }

    operator func <=(rhs: Float64): Bool { /* compiled code */ }

    operator func >=(rhs: Float64): Bool { /* compiled code */ }

    operator func ==(rhs: Float64): Bool { /* compiled code */ }

    operator func !=(rhs: Float64): Bool { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): IntNative { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): Int64 { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): Int32 { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): Int16 { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): Int8 { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): UIntNative { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): UInt64 { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): UInt32 { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): UInt16 { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): UInt8 { /* compiled code */ }
}

extend `Unit` <: Countable {
    func position(): Int64 { /* compiled code */ }

    func next(right: Int64): Rune { /* compiled code */ }
}

let MAX_NS_FLOAT64: Float64

let MIN_NS_FLOAT64: Float64

func durMul(secs: UInt64, nanos: UInt32, multiplier: UInt64, sign: Bool): std.core.Option<std.core.Duration> { /* compiled code */ }

func u32MulU64(thisInt: UInt32, thatInt: UInt64): (UInt64, UInt32) { /* compiled code */ }

func tryU64Mul(left: UInt64, right: UInt64): std.core.Option<UInt64> { /* compiled code */ }

func tryU64Add(left: UInt64, right: UInt64): std.core.Option<UInt64> { /* compiled code */ }

func checkSub(left: Int64, right: Int64): Bool { /* compiled code */ }

func toSecNsBase(num: UInt64): (UInt32, UInt32, UInt32) { /* compiled code */ }

func durDiv(secs: UInt64, nanos: UInt32, divisor: UInt64): (Int64, Int64) { /* compiled code */ }

func getDifference(num3: Int64, num2: Int64, num1: Int64, num0: Int64): (Int64, Int64, Int64, Int64) { /* compiled code */ }

func getDifference(num2: Int64, num1: Int64, num0: Int64): (UInt32, UInt32, UInt32) { /* compiled code */ }

func u32TwoMul(num1: UInt32, num0: UInt32, multiplier: UInt32): (UInt32, UInt32, UInt32) { /* compiled code */ }

func u32ThreeMul(num2: UInt32, num1: UInt32, num0: UInt32, multiplier: UInt32): (UInt32, UInt32, UInt32, UInt32) { /* compiled code */ }

func u32DividendMul(num3: UInt32, num2: UInt32, num1: UInt32, num0: UInt32, times: UInt32): (UInt32, UInt32, UInt32, UInt32, UInt32) { /* compiled code */ }

func divAndMod(dividend: UInt64, dividend2: UInt32, divisor2: UInt32, divisor1: UInt32): UInt32 { /* compiled code */ }

extend `Unit` {
    operator func *(r: std.core.Duration): std.core.Duration { /* compiled code */ }
}

extend `Unit` {
    operator func -(r: UInt32): Int64 { /* compiled code */ }
}

extend `Unit` {
    operator func *(r: UInt32): UInt64 { /* compiled code */ }

    operator func /(r: UInt32): UInt64 { /* compiled code */ }

    operator func %(r: UInt32): UInt32 { /* compiled code */ }
}

extend `Unit` {
    operator func *(r: UInt64): UInt64 { /* compiled code */ }
}

extend `Unit` {
    operator func *(r: std.core.Duration): std.core.Duration { /* compiled code */ }
}

extend StringBuilder {
    func append(num: Int64, unit: std.core.String): std.core.StringBuilder { /* compiled code */ }
}

func isSumOverflow(left: Int64, right: Int64): Bool { /* compiled code */ }

enum Endian {
    | Big

    | Little

    static prop Platform: std.core.Endian {
        get() { /* compiled code */ }
    }
}

foreign func CJ_PlatformEndian(): UInt8

interface NotEqual {
    abstract operator func !=(rhs: T): Bool
}

extend `Unit` <: Equatable {
    operator func ==(right: Bool): Bool { /* compiled code */ }
}

extend `Unit` <: Equatable {
    operator func ==(right: Unit): Bool { /* compiled code */ }
}

extend Range <: Equatable {
    operator func ==(that: std.core.Range<T>): Bool { /* compiled code */ }
}

class Error <: std.core.Object & std.core.ToString {
    var detailMessage: std.core.String

    var pcArray: std.core.Array<UInt64>

    var traceElementArray: std.core.Array<std.core.StackTraceElement>

    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName(): std.core.String { /* compiled code */ }

    prop message: std.core.String {
        get() { /* compiled code */ }
    }

    func toString(): std.core.String { /* compiled code */ }

    func printStackTrace(): Unit { /* compiled code */ }

    func getStackTraceMessage(): std.core.String { /* compiled code */ }

    func getStackTrace(): std.core.Array<std.core.StackTraceElement> { /* compiled code */ }
}

class StackTraceElement {
    let declaringClass: std.core.String

    let methodName: std.core.String

    let fileName: std.core.String

    let lineNumber: Int64

    init(declaringClass: std.core.String, methodName: std.core.String, fileName: std.core.String, lineNumber: Int64) { /* compiled code */ }
}

func fillInStackTrace(pc: std.core.Array<UInt64>, message: std.core.Array<UInt8>): std.core.Array<UInt64> { /* compiled code */ }

func decodeStackTrace(pc: UInt64, funcStart: UInt64, funcDesc: UInt64): std.core.StackTraceData { /* compiled code */ }

struct StackTraceData {
    let className: std.core.Array<UInt8>

    let methodName: std.core.Array<UInt8>

    let fileName: std.core.Array<UInt8>

    let lineNumber: Int64

    init() { /* compiled code */ }
}

func getStackTraceImpl(pcArray: std.core.Array<UInt64>): std.core.Array<std.core.StackTraceElement> { /* compiled code */ }

func bitCast(val: From): To { /* compiled code */ }

interface BitCastRange {
}

extend `Unit` {
    static func fromBits(bits: UInt64): Float64 { /* compiled code */ }

    func toBits(): UInt64 { /* compiled code */ }
}

extend `Unit` {
    static func fromBits(bits: UInt32): Float32 { /* compiled code */ }

    func toBits(): UInt32 { /* compiled code */ }
}

extend `Unit` {
    static func fromBits(bits: UInt16): Float16 { /* compiled code */ }

    func toBits(): UInt16 { /* compiled code */ }
}

extend `Unit` <: BitCastRange {
}

extend `Unit` <: BitCastRange {
}

extend `Unit` <: BitCastRange {
}

extend `Unit` <: BitCastRange {
}

extend `Unit` <: BitCastRange {
}

extend `Unit` <: BitCastRange {
}

func composition(f: (T1) -> T2, g: (T2) -> T3): (T1) -> T3 { /* compiled code */ }

func futureInit(future: std.core.Future<T>): Unit { /* compiled code */ }

class Future {
    let _thread: std.core.Thread

    var result: std.core.FutureResult

    var executeFn: () -> T

    init(fn: () -> T) { /* compiled code */ }

    prop thread: std.core.Thread {
        get() { /* compiled code */ }
    }

    func `get`(): T { /* compiled code */ }

    func tryGet(): std.core.Option<T> { /* compiled code */ }

    func `get`(timeout: std.core.Duration): T { /* compiled code */ }

    func cancel(): Unit { /* compiled code */ }

    func execute(): Unit { /* compiled code */ }

    static func executeClosure(fn: () -> T): Unit { /* compiled code */ }
}

func futureIsComplete(future: std.core.Future<T>): Bool { /* compiled code */ }

func futureWait(future: std.core.Future<T>, timeout: Int64): Unit { /* compiled code */ }

func futureNotifyAll(future: std.core.Future<T>): Unit { /* compiled code */ }

foreign func CJ_CORE_PrintOomHint(): Unit

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
    abstract func end(): Unit

    abstract func hasEnded(): Bool
}

extend Ordering <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }
}

func i32tof32(num: Int32): Float32 { /* compiled code */ }

func f32toi32(num: Float32): Int32 { /* compiled code */ }

func i64tof64(num: Int64): Float64 { /* compiled code */ }

func f642i64(num: Float64): Int64 { /* compiled code */ }

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop Max: IntNative {
        get() { /* compiled code */ }
    }

    static prop Min: IntNative {
        get() { /* compiled code */ }
    }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop Max: Int64 {
        get() { /* compiled code */ }
    }

    static prop Min: Int64 {
        get() { /* compiled code */ }
    }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop Max: Int32 {
        get() { /* compiled code */ }
    }

    static prop Min: Int32 {
        get() { /* compiled code */ }
    }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop Max: Int16 {
        get() { /* compiled code */ }
    }

    static prop Min: Int16 {
        get() { /* compiled code */ }
    }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop Max: Int8 {
        get() { /* compiled code */ }
    }

    static prop Min: Int8 {
        get() { /* compiled code */ }
    }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop Max: UIntNative {
        get() { /* compiled code */ }
    }

    static prop Min: UIntNative {
        get() { /* compiled code */ }
    }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop Max: UInt64 {
        get() { /* compiled code */ }
    }

    static prop Min: UInt64 {
        get() { /* compiled code */ }
    }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop Max: UInt32 {
        get() { /* compiled code */ }
    }

    static prop Min: UInt32 {
        get() { /* compiled code */ }
    }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop Max: UInt16 {
        get() { /* compiled code */ }
    }

    static prop Min: UInt16 {
        get() { /* compiled code */ }
    }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop Max: UInt8 {
        get() { /* compiled code */ }
    }

    static prop Min: UInt8 {
        get() { /* compiled code */ }
    }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop NaN: Float64 {
        get() { /* compiled code */ }
    }

    static prop Inf: Float64 {
        get() { /* compiled code */ }
    }

    static prop Max: Float64 {
        get() { /* compiled code */ }
    }

    static prop Min: Float64 {
        get() { /* compiled code */ }
    }

    static prop MinDenormal: Float64 {
        get() { /* compiled code */ }
    }

    static prop MinNormal: Float64 {
        get() { /* compiled code */ }
    }

    func isInf(): Bool { /* compiled code */ }

    func isNaN(): Bool { /* compiled code */ }

    func isNormal(): Bool { /* compiled code */ }

    static func max(a: Float64, b: Float64, others: std.core.Array<Float64>): Float64 { /* compiled code */ }

    static func min(a: Float64, b: Float64, others: std.core.Array<Float64>): Float64 { /* compiled code */ }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop NaN: Float32 {
        get() { /* compiled code */ }
    }

    static prop Inf: Float32 {
        get() { /* compiled code */ }
    }

    static prop Max: Float32 {
        get() { /* compiled code */ }
    }

    static prop Min: Float32 {
        get() { /* compiled code */ }
    }

    static prop MinDenormal: Float32 {
        get() { /* compiled code */ }
    }

    static prop MinNormal: Float32 {
        get() { /* compiled code */ }
    }

    func isInf(): Bool { /* compiled code */ }

    func isNaN(): Bool { /* compiled code */ }

    func isNormal(): Bool { /* compiled code */ }

    static func max(a: Float32, b: Float32, others: std.core.Array<Float32>): Float32 { /* compiled code */ }

    static func min(a: Float32, b: Float32, others: std.core.Array<Float32>): Float32 { /* compiled code */ }
}

extend `Unit` <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    static prop NaN: Float16 {
        get() { /* compiled code */ }
    }

    static prop Inf: Float16 {
        get() { /* compiled code */ }
    }

    static prop Max: Float16 {
        get() { /* compiled code */ }
    }

    static prop Min: Float16 {
        get() { /* compiled code */ }
    }

    static prop MinDenormal: Float16 {
        get() { /* compiled code */ }
    }

    static prop MinNormal: Float16 {
        get() { /* compiled code */ }
    }

    func isInf(): Bool { /* compiled code */ }

    func isNaN(): Bool { /* compiled code */ }

    func isNormal(): Bool { /* compiled code */ }

    static func max(a: Float16, b: Float16, others: std.core.Array<Float16>): Float16 { /* compiled code */ }

    static func min(a: Float16, b: Float16, others: std.core.Array<Float16>): Float16 { /* compiled code */ }
}

extend Range <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }
}

interface Hasher {
    abstract func finish(): Int64

    abstract mut func reset(): Unit

    abstract mut func write(value: Bool): Unit

    abstract mut func write(value: Rune): Unit

    abstract mut func write(value: Int8): Unit

    abstract mut func write(value: Int16): Unit

    abstract mut func write(value: Int32): Unit

    abstract mut func write(value: Int64): Unit

    abstract mut func write(value: UInt8): Unit

    abstract mut func write(value: UInt16): Unit

    abstract mut func write(value: UInt32): Unit

    abstract mut func write(value: UInt64): Unit

    abstract mut func write(value: Float16): Unit

    abstract mut func write(value: Float32): Unit

    abstract mut func write(value: Float64): Unit

    abstract mut func write(value: std.core.String): Unit
}

struct DefaultHasher <: std.core.Hasher {
    var res: Int64

    func finish(): Int64 { /* compiled code */ }

    mut func reset(): Unit { /* compiled code */ }

    mut func write(value: Bool): Unit { /* compiled code */ }

    mut func write(value: Rune): Unit { /* compiled code */ }

    mut func write(value: Int8): Unit { /* compiled code */ }

    mut func write(value: Int16): Unit { /* compiled code */ }

    mut func write(value: Int32): Unit { /* compiled code */ }

    mut func write(value: Int64): Unit { /* compiled code */ }

    mut func write(value: UInt8): Unit { /* compiled code */ }

    mut func write(value: UInt16): Unit { /* compiled code */ }

    mut func write(value: UInt32): Unit { /* compiled code */ }

    mut func write(value: UInt64): Unit { /* compiled code */ }

    mut func write(value: Float16): Unit { /* compiled code */ }

    mut func write(value: Float32): Unit { /* compiled code */ }

    mut func write(value: Float64): Unit { /* compiled code */ }

    mut func write(value: std.core.String): Unit { /* compiled code */ }

    init(res: Int64) { /* compiled code */ }
}

func wyrand(seed: Int64): Int64 { /* compiled code */ }

func mix(A: UInt64, B: UInt64): UInt64 { /* compiled code */ }

func r4(arr: std.core.Array<UInt8>, pos: Int64): UInt64 { /* compiled code */ }

func r8(arr: std.core.Array<UInt8>, pos: Int64): UInt64 { /* compiled code */ }

class IllegalFormatException <: std.core.IllegalArgumentException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class IllegalMemoryException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class IllegalStateException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class IncompatiblePackageException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

extend Iterator {
    func contains(element: T): Bool { /* compiled code */ }
}

extend Iterator {
    func max(): std.core.Option<T> { /* compiled code */ }

    func min(): std.core.Option<T> { /* compiled code */ }
}

extend Iterator {
    func zip(it: std.core.Iterator<R>): std.core.Iterator<(T, R)> { /* compiled code */ }

    func enumerate(): std.core.Iterator<(Int64, T)> { /* compiled code */ }

    func filter(predicate: (T) -> Bool): std.core.Iterator<T> { /* compiled code */ }

    func filterMap(transform: (T) -> std.core.Option<R>): std.core.Iterator<R> { /* compiled code */ }

    func map(transform: (T) -> R): std.core.Iterator<R> { /* compiled code */ }

    func step(count: Int64): std.core.Iterator<T> { /* compiled code */ }

    func skip(count: Int64): std.core.Iterator<T> { /* compiled code */ }

    func take(count: Int64): std.core.Iterator<T> { /* compiled code */ }

    func concat(other: std.core.Iterator<T>): std.core.Iterator<T> { /* compiled code */ }

    func inspect(action: (T) -> Unit): std.core.Iterator<T> { /* compiled code */ }

    func flatMap(transform: (T) -> std.core.Iterator<R>): std.core.Iterator<R> { /* compiled code */ }

    func intersperse(separator: T): std.core.Iterator<T> { /* compiled code */ }

    func all(predicate: (T) -> Bool): Bool { /* compiled code */ }

    func any(predicate: (T) -> Bool): Bool { /* compiled code */ }

    func none(predicate: (T) -> Bool): Bool { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    func count(): Int64 { /* compiled code */ }

    func forEach(action: (T) -> Unit): Unit { /* compiled code */ }

    func first(): std.core.Option<T> { /* compiled code */ }

    func last(): std.core.Option<T> { /* compiled code */ }

    func at(n: Int64): std.core.Option<T> { /* compiled code */ }

    func reduce(operation: (T, T) -> T): std.core.Option<T> { /* compiled code */ }

    func fold(initial: R, operation: (R, T) -> R): R { /* compiled code */ }
}

func sizeOfType(): UIntNative { /* compiled code */ }

func alignOfType(): UIntNative { /* compiled code */ }

foreign func malloc(size: UIntNative): CPointer<Unit>

foreign func free(p: CPointer<Unit>): Unit

let SECUREC_MEM_MAX_LEN: Int64

func sizeOf(): UIntNative { /* compiled code */ }

func alignOf(): UIntNative { /* compiled code */ }

func getCommandLineArgsFromRuntime(): CPointer<CPointer<UInt8>> { /* compiled code */ }

func getCommandLineArgs(): std.core.Array<std.core.String> { /* compiled code */ }

foreign func CJ_CORE_Float64ToHash(x: Float64): Int64

foreign func CJ_CORE_Float32ToHash(x: Float32): Int64

foreign func CJ_CONSOLE_Readline(): CPointer<std.core.StructureString>

struct StructureString {
    let buffer: CPointer<UInt8>

    let length: UIntNative

    init() { /* compiled code */ }
}

foreign func CJ_CONSOLE_ClearstdInerr(): Unit

foreign func CJ_CORE_StringSize(str: CPointer<UInt8>, len: Int64): Int64

foreign func CJ_CORE_IndexOfChar(orgStr: CPointer<UInt8>, patC: UInt32, orgSize: Int64, start: Int64): Int64

foreign func CJ_CORE_LastIndexOfChar(orgStr: CPointer<UInt8>, patC: UInt32, orgSize: Int64, start: Int64): Int64

foreign func CJ_CORE_CountChar(orgStr: CPointer<UInt8>, patC: UInt32, orgSize: Int64): Int64

foreign func CJ_MRT_GetCommandLineArgs(): CPointer<CPointer<UInt8>>

class NegativeArraySizeException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

interface Any {
}

func refEq(a: std.core.Object, b: std.core.Object): Bool { /* compiled code */ }

func intrinsicRefEq(a: std.core.Object, b: std.core.Object): Bool { /* compiled code */ }

func ifSome(o: std.core.Option<T>, action: (T) -> Unit): Unit { /* compiled code */ }

func ifNone(o: std.core.Option<T>, action: () -> Unit): Unit { /* compiled code */ }

extend Option <: Equatable {
    operator func ==(that: std.core.Option<T>): Bool { /* compiled code */ }

    operator func !=(that: std.core.Option<T>): Bool { /* compiled code */ }
}

extend Option <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend Option <: Hashable {
    func hashCode(): Int64 { /* compiled code */ }
}

extend Option {
    func flatten(): std.core.Option<T> { /* compiled code */ }
}

let MAX_INT_NATIVE: IntNative

let MIN_INT_NATIVE: IntNative

let MAX_UINT_NATIVE: UIntNative

class OverflowException <: std.core.ArithmeticException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

func readln(): std.core.String { /* compiled code */ }

var globalOutOfMemoryError: std.core.OutOfMemoryError

func throwImplicitException(id: Int64, args: CPointer<Unit>): Unit { /* compiled code */ }

func throwArithmeticException(): Unit { /* compiled code */ }

func createArithmeticExceptionMsg(msg: std.core.String): std.core.ArithmeticException { /* compiled code */ }

func throwArithmeticExceptionMsg(msg: std.core.String): Nothing { /* compiled code */ }

func createOverflowExceptionMsg(msg: std.core.String): std.core.OverflowException { /* compiled code */ }

func throwOverflowException(): Unit { /* compiled code */ }

func throwIndexOutOfBoundsException(): Unit { /* compiled code */ }

func throwNegativeArraySizeException(): Unit { /* compiled code */ }

func callToString(e: std.core.ToString): std.core.String { /* compiled code */ }

func checkIsError(e: std.core.Object): Bool { /* compiled code */ }

func throwError(msg: std.core.String): Unit { /* compiled code */ }

func callPrintStackTrace(e: std.core.Exception): Unit { /* compiled code */ }

func callPrintStackTraceError(e: std.core.Error): Unit { /* compiled code */ }

let MAX_TIMEOUT_DURATION: std.core.Duration

func intrinsicSleep(ns: Int64): Unit { /* compiled code */ }

func sleep(dur: std.core.Duration): Unit { /* compiled code */ }

class SpawnException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class StackOverflowError <: std.core.Error {
    let NotFolded: UInt64

    let TopFolded: UInt64

    let BottomFolded: UInt64

    var FoldedFlag: UInt64

    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }

    override func printStackTrace(): Unit { /* compiled code */ }
}

let digits100: std.core.Array<UInt8>

let EMPTY_INT64_ARRAY: std.core.Array<Int64>

func checkUtf8Of2Bytes(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Rune { /* compiled code */ }

func checkUtf8Of3Bytes(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Rune { /* compiled code */ }

func checkUtf8Of4Bytes(arr: std.core.Array<UInt8>, startIndex: Int64, endIndex: Int64): Rune { /* compiled code */ }

func isThreadObjectInited(): Bool { /* compiled code */ }

func getThreadObject(): std.core.Thread { /* compiled code */ }

foreign func CJ_MRT_SetCJThreadName(`handle`: CPointer<Unit>, name: CPointer<UInt8>, len: Int64): Unit

foreign func CJ_MRT_GetCJThreadId(`handle`: CPointer<Unit>): Int64

foreign func CJ_MRT_GetCurrentCJThread(): CPointer<Unit>

class ThreadLocalMap {
    var entries: std.core.Array<std.core.Entry>

    var size: Int64

    init() { /* compiled code */ }

    func `get`(tlKey: std.core.ThreadLocalKey): std.core.Option<T> { /* compiled code */ }

    func `set`(tlKey: std.core.ThreadLocalKey, value: T): Unit { /* compiled code */ }

    func remove(tlKey: std.core.ThreadLocalKey): Unit { /* compiled code */ }
}

func setThreadObject(thread: std.core.Thread): Unit { /* compiled code */ }

func handleException(ex: std.core.Exception): Unit { /* compiled code */ }

func handleError(er: std.core.Error): Unit { /* compiled code */ }

let EMPTY_KEY: std.core.ThreadLocalKey

let DELETED_KEY: std.core.ThreadLocalKey

class ThreadLocal {
    let key: std.core.ThreadLocalKey

    func `get`(): std.core.Option<T> { /* compiled code */ }

    func `set`(value: std.core.Option<T>): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

func getThreadLocalMap(): std.core.ThreadLocalMap { /* compiled code */ }

struct Entry {
    let tlKey: std.core.ThreadLocalKey

    let boxedValue: std.core.Object

    init(k: std.core.ThreadLocalKey, v: std.core.Object) { /* compiled code */ }

    func isValid(): Bool { /* compiled code */ }
}

let EMPTY_ENTRY: std.core.Entry

let DELETED_ENTRY: std.core.Entry

class TimeoutException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend `Unit` <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

type Byte = UInt8

type Int = Int64

type UInt = UInt64

class UnsupportedException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}
