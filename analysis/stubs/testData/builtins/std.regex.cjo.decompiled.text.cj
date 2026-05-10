// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.regex

import std.core.*
import std.collection.*
import std.sync.*

let UNCAPTURED_GROUP_INDEX: Int64

let REGEX_NORMAL: UInt32

let REGEX_IGNORECASE: UInt32

let REGEX_MULTILINE: UInt32

let REGEX_UNICODE: UInt32

struct Position {
    let empty: std.regex.Position

    init(start: UIntNative, end: UIntNative) { /* compiled code */ }

    let start: Int64

    let end: Int64

    init(start: Int64, end: Int64) { /* compiled code */ }
}

struct MatchData {
    func matchString: std.core.String

    func matchString(group: Int64): std.core.String

    func matchString(group: std.core.String): std.core.String

    func matchPosition: std.regex.Position

    func matchPosition(group: Int64): std.regex.Position

    func matchPosition(group: std.core.String): std.regex.Position

    func groupNumber: Int64

    func groupCount: Int64

    let input: std.core.String

    let positions: std.core.Array<std.regex.Position>

    let rc: Int64

    let nameToIndex: (std.core.String) -> Int64

    init(input: std.core.String, positions: std.core.Array<std.regex.Position>, rc: Int64, nameToIndex: (std.core.String) -> Int64) { /* compiled code */ }
}

class Matcher {
    var input: std.core.String

    var regex: std.regex.Regex

    var begin: Int64

    var end: Int64

    var iterator: std.regex.FindIterator

    var replacedOption: std.core.Option<std.core.String>

    var replaceRegexOption: std.core.Option<std.regex.Regex>

    var replaceIteraorOption: std.core.Option<std.regex.FindIterator>

    var replaceOffsetOption: std.core.Option<Int64>

    init(regex: std.regex.Regex, input: std.core.String) { /* compiled code */ }

    func fullMatch: std.core.Option<std.regex.MatchData>

    func matchStart: std.core.Option<std.regex.MatchData>

    func find: std.core.Option<std.regex.MatchData>

    func find(index: Int64): std.core.Option<std.regex.MatchData>

    func findAll: std.core.Option<std.core.Array<std.regex.MatchData>>

    func allCount: Int64

    func replace(replacement: std.core.String): std.core.String

    func replace(replacement: std.core.String, index: Int64): std.core.String

    func replaceAll(replacement: std.core.String): std.core.String

    func replaceAll(replacement: std.core.String, limit: Int64): std.core.String

    func split: std.core.Array<std.core.String>

    func split(limit: Int64): std.core.Array<std.core.String>

    func setRegion(beginIndex: Int64, endIndex: Int64): std.regex.Matcher

    func region: std.regex.Position

    func resetRegion: std.regex.Matcher

    func resetString(input: std.core.String): std.regex.Matcher

    func getString: std.core.String
}

class Regex {
    let re: CPointer<Unit>

    var pattern: std.core.String

    var flags: std.core.Array<std.regex.RegexFlag>

    var namedGroups: std.core.Option<std.collection.HashMap<std.core.String, Int64>>

    init(pattern: std.core.String, flags: std.core.Array<std.regex.RegexFlag>) { /* compiled code */ }

    init(pattern: std.core.String, option: std.regex.RegexOption) { /* compiled code */ }

    ~init()

    func matcher(input: std.core.String): std.regex.Matcher

    func matches(input: std.core.String): Bool

    func getNamedGroups: std.collection.Map<std.core.String, Int64>

    func find(input: std.core.String, group: Bool): std.core.Option<std.regex.MatchData>

    func find(input: std.core.String, inputCString: CString, pcre2md: CPointer<Unit>, offset: Int64, group: Bool): std.core.Option<std.regex.MatchData>

    func allCount(input: std.core.String, begin: Int64, end: Int64): Int64

    func findAll(input: std.core.String, group: Bool): std.core.Array<std.regex.MatchData>

    func lazyFindAll(input: std.core.String, group: Bool): std.core.Iterator<std.regex.MatchData>

    func replace(input: std.core.String, replacement: std.core.String): std.core.String

    func replace(input: std.core.String, replacement: std.core.String, index: Int64): std.core.String

    func replaceAll(input: std.core.String, replacement: std.core.String): std.core.String

    func replaceAll(input: std.core.String, replacement: std.core.String, limit: Int64): std.core.String

    func split(input: std.core.String): std.core.Array<std.core.String>

    func split(input: std.core.String, limit: Int64): std.core.Array<std.core.String>

    func string: std.core.String
}

class FindIterator <: std.core.Iterator<std.regex.MatchData> {
    var offset: Int64

    let regex: std.regex.Regex

    let input: std.core.String

    let inputCString: CString

    let group: Bool

    let pcre2md: CPointer<Unit>

    init(regex: std.regex.Regex, input: std.core.String, group: Bool) { /* compiled code */ }

    ~init()

    func next: std.core.Option<std.regex.MatchData>
}

type Pcre2CodePtr = CPointer<Unit>

type Pcre2MatchDataPtr = CPointer<Unit>

type Pcre2OvectorPtr = CPointer<UIntNative>

struct CompileResult {
    let re: CPointer<Unit>

    let errorCode: Int32

    let errorOffset: UIntNative

    init(re: CPointer<Unit>, errorCode: Int32, errorOffset: UIntNative) { /* compiled code */ }
}

struct NameTableInfo {
    let nameCount: UInt32

    let nameEntrySize: UInt32

    let nameTable: CPointer<UInt8>

    init(nameCount: UInt32, nameEntrySize: UInt32, nameTable: CPointer<UInt8>) { /* compiled code */ }
}

func CJ_REGEX_Compile(pattern: CString, options: UInt32): CPointer<std.regex.CompileResult>

func CJ_REGEX_CreateMatchData(re: CPointer<Unit>): CPointer<Unit>

func CJ_REGEX_Match(re: CPointer<Unit>, cinput: CString, length: UIntNative, offset: UIntNative, matchData: CPointer<Unit>): Int32

func CJ_REGEX_GetOvector(matchData: CPointer<Unit>): CPointer<UIntNative>

func CJ_REGEX_GetErrorMsg(errorCode: Int32): CString

func CJ_REGEX_GetNameTableInfo(re: CPointer<Unit>): CPointer<std.regex.NameTableInfo>

func CJ_REGEX_FreeCode(re: CPointer<Unit>): Unit

func CJ_REGEX_FreeMatchData(md: CPointer<Unit>): Unit

func CJ_REGEX_Count(re: CPointer<Unit>, cinput: CString, length: UIntNative, offset: UIntNative, end: UIntNative, matchData: CPointer<Unit>): Int64

func release(cString: CString): Unit

func release(pcre2md: CPointer<Unit>): Unit

enum RegexFlag {
    | IgnoreCase

    | MultiLine

    | Unicode

    func value: UInt32

    func getValue(flags: std.core.Array<std.regex.RegexFlag>): UInt32
}

class RegexOption <: std.core.Object & std.core.ToString {
    var option: UInt32

    init() { /* compiled code */ }

    func ignoreCase: std.regex.RegexOption

    func multiLine: std.regex.RegexOption

    func toString: std.core.String

    func toFlags: std.core.Array<std.regex.RegexFlag>
}

let _phantom: std.core.Object

let _rf: std.sync.AtomicReference<std.core.Object>

func keepAlive(o: std.core.Object): Unit

class RegexException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}
