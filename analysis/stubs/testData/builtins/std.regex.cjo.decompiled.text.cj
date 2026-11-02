// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.regex

import std.core.*
import std.collection.*
import std.sync.*

let UNCAPTURED_GROUP_INDEX: Int64 = COMPILED_CODE /* compiled code */

let REGEX_NORMAL: UInt32 = COMPILED_CODE /* compiled code */

let REGEX_IGNORECASE: UInt32 = COMPILED_CODE /* compiled code */

let REGEX_MULTILINE: UInt32 = COMPILED_CODE /* compiled code */

let REGEX_UNICODE: UInt32 = COMPILED_CODE /* compiled code */

public struct Position {
    internal static let empty: std.regex.Position

    init(start: UIntNative, end: UIntNative) { /* compiled code */ }

    public let start: Int64

    public let end: Int64

    Position(start: Int64, end: Int64) { /* compiled code */ }
}

public struct MatchData {
    public func matchString(): std.core.String { /* compiled code */ }

    public func matchString(group: Int64): std.core.String { /* compiled code */ }

    public func matchString(group: std.core.String): std.core.String { /* compiled code */ }

    public func matchPosition(): std.regex.Position { /* compiled code */ }

    public func matchPosition(group: Int64): std.regex.Position { /* compiled code */ }

    public func matchPosition(group: std.core.String): std.regex.Position { /* compiled code */ }

    @Deprecated[message: "Use member function `public func groupCount(): Int64` instead."]
    public func groupNumber(): Int64 { /* compiled code */ }

    public func groupCount(): Int64 { /* compiled code */ }

    internal let input: std.core.String

    internal let positions: std.core.Array<std.regex.Position>

    internal let rc: Int64

    internal let nameToIndex: (std.core.String) -> Int64

    MatchData(input: std.core.String, positions: std.core.Array<std.regex.Position>, rc: Int64, nameToIndex!: (std.core.String) -> Int64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

@Deprecated[message: "APIs in Matcher have been moved to Regex."]
public class Matcher {
    private var input: std.core.String

    private var regex: std.regex.Regex

    private var begin: Int64

    private var end: Int64

    private var iterator: std.regex.FindIterator

    private var replacedOption: std.core.Option<std.core.String>

    private var replaceRegexOption: std.core.Option<std.regex.Regex>

    private var replaceIteraorOption: std.core.Option<std.regex.FindIterator>

    private var replaceOffsetOption: std.core.Option<Int64>

    init(regex: std.regex.Regex, input: std.core.String) { /* compiled code */ }

    public func fullMatch(): std.core.Option<std.regex.MatchData> { /* compiled code */ }

    public func matchStart(): std.core.Option<std.regex.MatchData> { /* compiled code */ }

    public func find(): std.core.Option<std.regex.MatchData> { /* compiled code */ }

    public func find(index: Int64): std.core.Option<std.regex.MatchData> { /* compiled code */ }

    public func findAll(): std.core.Option<std.core.Array<std.regex.MatchData>> { /* compiled code */ }

    public func allCount(): Int64 { /* compiled code */ }

    public func replace(replacement: std.core.String): std.core.String { /* compiled code */ }

    public func replace(replacement: std.core.String, index: Int64): std.core.String { /* compiled code */ }

    public func replaceAll(replacement: std.core.String): std.core.String { /* compiled code */ }

    public func replaceAll(replacement: std.core.String, limit: Int64): std.core.String { /* compiled code */ }

    public func split(): std.core.Array<std.core.String> { /* compiled code */ }

    public func split(limit: Int64): std.core.Array<std.core.String> { /* compiled code */ }

    public func setRegion(beginIndex: Int64, endIndex: Int64): std.regex.Matcher { /* compiled code */ }

    public func region(): std.regex.Position { /* compiled code */ }

    public func resetRegion(): std.regex.Matcher { /* compiled code */ }

    public func resetString(input: std.core.String): std.regex.Matcher { /* compiled code */ }

    public func getString(): std.core.String { /* compiled code */ }
}

public class Regex {
    internal let re: std.core.CPointer<Unit>

    internal var pattern: std.core.String

    internal var flags: std.core.Array<std.regex.RegexFlag>

    private var namedGroups: std.core.Option<std.collection.HashMap<std.core.String, Int64>>

    init(pattern: std.core.String, flags: std.core.Array<std.regex.RegexFlag>) { /* compiled code */ }

    @Deprecated[message: "Use member function `public Regex(let pattern: String, flags: Array<RegexFlag>)` instead."]
    init(pattern: std.core.String, option: std.regex.RegexOption) { /* compiled code */ }

    ~init() { /* compiled code */ }

    @Deprecated[message: "`Matcher` has been marked as deprecated."]
    public func matcher(input: std.core.String): std.regex.Matcher { /* compiled code */ }

    public func matches(input: std.core.String): Bool { /* compiled code */ }

    public func getNamedGroups(): std.collection.Map<std.core.String, Int64> { /* compiled code */ }

    public func find(input: std.core.String, group!: Bool = COMPILED_CODE /* compiled code */): std.core.Option<std.regex.MatchData> { /* compiled code */ }

    internal func find(input: std.core.String, inputCString: CString, pcre2md: std.core.CPointer<Unit>, offset: Int64, group!: Bool = COMPILED_CODE /* compiled code */): std.core.Option<std.regex.MatchData> { /* compiled code */ }

    internal func allCount(input: std.core.String, begin: Int64, end: Int64): Int64 { /* compiled code */ }

    public func findAll(input: std.core.String, group!: Bool = COMPILED_CODE /* compiled code */): std.core.Array<std.regex.MatchData> { /* compiled code */ }

    public func lazyFindAll(input: std.core.String, group!: Bool = COMPILED_CODE /* compiled code */): std.core.Iterator<std.regex.MatchData> { /* compiled code */ }

    public func replace(input: std.core.String, replacement: std.core.String): std.core.String { /* compiled code */ }

    public func replace(input: std.core.String, replacement: std.core.String, index: Int64): std.core.String { /* compiled code */ }

    public func replaceAll(input: std.core.String, replacement: std.core.String): std.core.String { /* compiled code */ }

    public func replaceAll(input: std.core.String, replacement: std.core.String, limit: Int64): std.core.String { /* compiled code */ }

    public func split(input: std.core.String): std.core.Array<std.core.String> { /* compiled code */ }

    public func split(input: std.core.String, limit: Int64): std.core.Array<std.core.String> { /* compiled code */ }

    public func string(): std.core.String { /* compiled code */ }
}

internal class FindIterator <: std.core.Iterator<std.regex.MatchData> {
    internal var offset: Int64

    private let regex: std.regex.Regex

    private let input: std.core.String

    private let inputCString: CString

    private let group: Bool

    private let pcre2md: std.core.CPointer<Unit>

    init(regex: std.regex.Regex, input: std.core.String, group!: Bool = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    ~init() { /* compiled code */ }

    public func next(): std.core.Option<std.regex.MatchData> { /* compiled code */ }
}

internal type Pcre2CodePtr = std.core.CPointer<Unit>

internal type Pcre2MatchDataPtr = std.core.CPointer<Unit>

internal type Pcre2OvectorPtr = std.core.CPointer<UIntNative>

internal struct CompileResult {
    internal let re: std.core.CPointer<Unit>

    internal let errorCode: Int32

    internal let errorOffset: UIntNative

    CompileResult(re: std.core.CPointer<Unit>, errorCode: Int32, errorOffset: UIntNative) { /* compiled code */ }
}

internal struct NameTableInfo {
    internal let nameCount: UInt32

    internal let nameEntrySize: UInt32

    internal let nameTable: std.core.CPointer<UInt8>

    NameTableInfo(nameCount: UInt32, nameEntrySize: UInt32, nameTable: std.core.CPointer<UInt8>) { /* compiled code */ }
}

internal foreign func CJ_REGEX_Compile(pattern: CString, options: UInt32): std.core.CPointer<std.regex.CompileResult>

internal foreign func CJ_REGEX_CreateMatchData(re: std.core.CPointer<Unit>): std.core.CPointer<Unit>

internal foreign func CJ_REGEX_Match(re: std.core.CPointer<Unit>, cinput: CString, length: UIntNative, offset: UIntNative, matchData: std.core.CPointer<Unit>): Int32

internal foreign func CJ_REGEX_GetOvector(matchData: std.core.CPointer<Unit>): std.core.CPointer<UIntNative>

internal foreign func CJ_REGEX_GetErrorMsg(errorCode: Int32): CString

internal foreign func CJ_REGEX_GetNameTableInfo(re: std.core.CPointer<Unit>): std.core.CPointer<std.regex.NameTableInfo>

internal foreign func CJ_REGEX_FreeCode(re: std.core.CPointer<Unit>): Unit

internal foreign func CJ_REGEX_FreeMatchData(md: std.core.CPointer<Unit>): Unit

internal foreign func CJ_REGEX_Count(re: std.core.CPointer<Unit>, cinput: CString, length: UIntNative, offset: UIntNative, end: UIntNative, matchData: std.core.CPointer<Unit>): Int64

internal func release(cString: CString): Unit { /* compiled code */ }

internal func release(pcre2md: std.core.CPointer<Unit>): Unit { /* compiled code */ }

public enum RegexFlag {
    | IgnoreCase

    | MultiLine

    | Unicode

    internal func value(): UInt32 { /* compiled code */ }

    internal static func getValue(flags: std.core.Array<std.regex.RegexFlag>): UInt32 { /* compiled code */ }
}

@Deprecated[message: "Use `public enum RegexFlag` instead."]
public class RegexOption <: std.core.Object & std.core.ToString {
    private var option: UInt32

    init() { /* compiled code */ }

    public func ignoreCase(): std.regex.RegexOption { /* compiled code */ }

    public func multiLine(): std.regex.RegexOption { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    internal func toFlags(): std.core.Array<std.regex.RegexFlag> { /* compiled code */ }
}

let _phantom: std.core.Object

let _rf: std.sync.AtomicReference<std.core.Object>

internal func keepAlive(o: std.core.Object): Unit { /* compiled code */ }

public class RegexException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}
