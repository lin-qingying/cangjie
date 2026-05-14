// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.time

import std.core.*
import std.convert.*
import std.collection.ArrayList

let MIN_INT64: Int64

let MAX_INT64: Int64

let MAX_YEAR: Int64

let MIN_YEAR: Int64

let MAX_NANOSECOND: Int64

let MONTHS_PER_YEAR: Int64

let DAYS_PER_WEEK: Int64

let DAYS_OF_NORMAL_YEAR: Int64

let DAYS_OF_LEAP_YEAR: Int64

let DAYS_PER_400YEARS: Int64

let DAYS_PER_100YEARS: Int64

let DAYS_PER_4YEARS: Int64

let HOURS_PER_DAY: Int64

let MINUTES_PER_HOUR: Int64

let SECS_PER_MINUTE: Int64

let SECS_PER_HOUR: Int64

let SECS_PER_DAY: Int64

let SECS_PER_WEEK: Int64

let MS_PER_SEC: Int64

let US_PER_MS: Int64

let NS_PER_US: Int64

let US_PER_SEC: Int64

let NS_PER_MS: Int64

let NS_PER_SEC: Int64

let NS_PER_MINUTE: Int64

let NS_PER_HOUR: Int64

let NS_PER_DAY: Int64

let SECS_OF_NORMAL_YEAR: Int64

let SECS_OF_LEAP_YEAR: Int64

let HOUR_12_INTERVAL: Int64

let HOUR_12_START: Int64

let HOUR_12_END: Int64

let HOUR_24_START: Int64

let HOUR_24_NOON: Int64

let START_AD_YEAR: Int64

let DAYS_OF_UNIX_TO_AD1: Int64

let SECS_OF_UNIX_TO_AD1: Int64

let DAYS_OF_MAX_TO_AD1: Int64

let SECS_OF_MAX_TO_AD1: Int64

let DAYS_OF_MIN_TO_AD1: Int64

let SECS_OF_MIN_TO_AD1: Int64

let DAYS_OF_JAN_TO_MARCH_LEAP: Int64

let MAX_OFFSET: Int64

let MIN_OFFSET: Int64

let YEAR_OUT_OF_RANGE: std.core.String

let MAX_UTC_SECS: Int64

let MIN_UTC_SECS: Int64

let SEPARATOR: std.core.String

let SLASH_CHAR: std.core.String

let TZIF_SOUTCE_LINUX: std.core.String

let LOCALTIME_SRC: std.core.String

let TZPATH: std.core.String

let MAXPATHLEN: Int64

class TimeZone <: std.core.Object & std.core.ToString & std.core.Equatable<std.time.TimeZone> {
    let UTC: std.time.TimeZone

    let Local: std.time.TimeZone

    let zoneId: std.core.String

    let localTimes: std.core.Array<std.time.LocalTime>

    let transTimes: std.core.Array<std.time.TransTime>

    let footer: std.core.String

    var cache: std.core.Option<(Int64, Int64, std.time.LocalTime)>

    prop id: std.core.String {
        get() { /* compiled code */ }
    }

    init(id: std.core.String, offset: Int32) { /* compiled code */ }

    init(id: std.core.String, offset: std.core.Duration) { /* compiled code */ }

    static func load(id: std.core.String): std.time.TimeZone { /* compiled code */ }

    static func loadFromPaths(id: std.core.String, tzpaths: std.core.Array<std.core.String>): std.time.TimeZone { /* compiled code */ }

    static func loadFromTZData(id: std.core.String, data: std.core.Array<UInt8>): std.time.TimeZone { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    operator func ==(r: std.time.TimeZone): Bool { /* compiled code */ }

    operator func !=(r: std.time.TimeZone): Bool { /* compiled code */ }

    func lookup(epochSecond: Int64): (std.core.String, Int64, Int64, Int64) { /* compiled code */ }

    func inferZoneIndex(): Int64 { /* compiled code */ }

    func lookupName(name: std.core.String, unix: Int64): Int64 { /* compiled code */ }
}

struct DateTime <: std.core.ToString & std.core.Hashable & std.core.Comparable<std.time.DateTime> & std.convert.Formattable & std.convert.Parsable<std.time.DateTime> {
    let d: std.core.Duration

    let tz: std.time.TimeZone

    static prop UnixEpoch: std.time.DateTime {
        get() { /* compiled code */ }
    }

    prop year: Int64 {
        get() { /* compiled code */ }
    }

    prop month: std.time.Month {
        get() { /* compiled code */ }
    }

    prop monthValue: Int64 {
        get() { /* compiled code */ }
    }

    prop dayOfMonth: Int64 {
        get() { /* compiled code */ }
    }

    prop dayOfWeek: std.time.DayOfWeek {
        get() { /* compiled code */ }
    }

    prop dayOfYear: Int64 {
        get() { /* compiled code */ }
    }

    prop hour: Int64 {
        get() { /* compiled code */ }
    }

    prop minute: Int64 {
        get() { /* compiled code */ }
    }

    prop second: Int64 {
        get() { /* compiled code */ }
    }

    prop nanosecond: Int64 {
        get() { /* compiled code */ }
    }

    prop isoWeek: (Int64, Int64) {
        get() { /* compiled code */ }
    }

    prop zone: std.time.TimeZone {
        get() { /* compiled code */ }
    }

    prop zoneId: std.core.String {
        get() { /* compiled code */ }
    }

    prop zoneOffset: std.core.Duration {
        get() { /* compiled code */ }
    }

    prop date: (Int64, std.time.Month, Int64) {
        get() { /* compiled code */ }
    }

    prop time: (Int64, Int64, Int64) {
        get() { /* compiled code */ }
    }

    init(secs: Int64, nanos: UInt32, tz: std.time.TimeZone) { /* compiled code */ }

    static func now(timeZone: std.time.TimeZone): std.time.DateTime { /* compiled code */ }

    static func nowUTC(): std.time.DateTime { /* compiled code */ }

    static func fromUnixTimeStamp(d: std.core.Duration): std.time.DateTime { /* compiled code */ }

    static func ofEpoch(second: Int64, nanosecond: Int64): std.time.DateTime { /* compiled code */ }

    static func of(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64, timeZone: std.time.TimeZone): std.time.DateTime { /* compiled code */ }

    static func of(year: Int64, month: std.time.Month, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64, timeZone: std.time.TimeZone): std.time.DateTime { /* compiled code */ }

    static func ofUTC(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64): std.time.DateTime { /* compiled code */ }

    static func ofUTC(year: Int64, month: std.time.Month, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64): std.time.DateTime { /* compiled code */ }

    static func parse(str: std.core.String): std.time.DateTime { /* compiled code */ }

    static func tryParse(str: std.core.String): std.core.Option<std.time.DateTime> { /* compiled code */ }

    static func parse(str: std.core.String, format: std.core.String): std.time.DateTime { /* compiled code */ }

    static func parse(str: std.core.String, format: std.time.DateTimeFormat): std.time.DateTime { /* compiled code */ }

    func inUTC(): std.time.DateTime { /* compiled code */ }

    func inLocal(): std.time.DateTime { /* compiled code */ }

    func inTimeZone(timeZone: std.time.TimeZone): std.time.DateTime { /* compiled code */ }

    func toUnixTimeStamp(): std.core.Duration { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func format(fmt: std.core.String): std.core.String { /* compiled code */ }

    func toString(format: std.time.DateTimeFormat): std.core.String { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func addYears(n: Int64): std.time.DateTime { /* compiled code */ }

    func addMonths(n: Int64): std.time.DateTime { /* compiled code */ }

    func addWeeks(n: Int64): std.time.DateTime { /* compiled code */ }

    func addDays(n: Int64): std.time.DateTime { /* compiled code */ }

    func addHours(n: Int64): std.time.DateTime { /* compiled code */ }

    func addMinutes(n: Int64): std.time.DateTime { /* compiled code */ }

    func addSeconds(n: Int64): std.time.DateTime { /* compiled code */ }

    func addNanoseconds(n: Int64): std.time.DateTime { /* compiled code */ }

    operator func +(r: std.core.Duration): std.time.DateTime { /* compiled code */ }

    operator func -(r: std.core.Duration): std.time.DateTime { /* compiled code */ }

    operator func -(r: std.time.DateTime): std.core.Duration { /* compiled code */ }

    operator func ==(r: std.time.DateTime): Bool { /* compiled code */ }

    operator func !=(r: std.time.DateTime): Bool { /* compiled code */ }

    operator func >=(r: std.time.DateTime): Bool { /* compiled code */ }

    operator func >(r: std.time.DateTime): Bool { /* compiled code */ }

    operator func <=(r: std.time.DateTime): Bool { /* compiled code */ }

    operator func <(r: std.time.DateTime): Bool { /* compiled code */ }

    func compare(rhs: std.time.DateTime): std.core.Ordering { /* compiled code */ }

    func toYearString(length: Int64, isISOYear: Bool): std.core.String { /* compiled code */ }

    func toMonthString(length: Int64): std.core.String { /* compiled code */ }

    func toHourIn12String(length: Int64): std.core.String { /* compiled code */ }

    func toSecondFractionString(length: Int64): std.core.String { /* compiled code */ }

    func toDayOfWeekString(length: Int64): std.core.String { /* compiled code */ }

    func toEraString(length: Int64): std.core.String { /* compiled code */ }

    func toOffsetString(length: Int64): std.core.String { /* compiled code */ }

    func toZoneInfoString(length: Int64): std.core.String { /* compiled code */ }

    func toGMTOffsetString(length: Int64): std.core.String { /* compiled code */ }

    func toDayOfYearString(length: Int64): std.core.String { /* compiled code */ }

    func genDateTime(zoneId: std.core.String, zoneOffset: Int64): std.time.DateTime { /* compiled code */ }

    func checkExtraInfo(isoYear: Int64, isoWeek: Int64, dayOfWeek: Int64): Unit { /* compiled code */ }
}

func systemNow(): (Int64, Int64) { /* compiled code */ }

let DAYS_BEFORE: std.core.Array<Int64>

let DAYS_OF_MONTH: std.core.Array<Int64>

struct NativeClock {
    var sec: Int64

    var nanosec: Int64

    init(sec: Int64, nanosec: Int64) { /* compiled code */ }
}

enum Month <: std.core.ToString & std.core.Equatable<std.time.Month> {
    | January

    | February

    | March

    | April

    | May

    | June

    | July

    | August

    | September

    | October

    | November

    | December

    static func of(mon: Int64): std.time.Month { /* compiled code */ }

    func value(): Int64 { /* compiled code */ }

    func toInteger(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    operator func +(n: Int64): std.time.Month { /* compiled code */ }

    operator func -(n: Int64): std.time.Month { /* compiled code */ }

    operator func ==(r: std.time.Month): Bool { /* compiled code */ }

    operator func !=(r: std.time.Month): Bool { /* compiled code */ }
}

enum DayOfWeek <: std.core.ToString & std.core.Equatable<std.time.DayOfWeek> {
    | Sunday

    | Monday

    | Tuesday

    | Wednesday

    | Thursday

    | Friday

    | Saturday

    static func of(dayOfWeek: Int64): std.time.DayOfWeek { /* compiled code */ }

    func value(): Int64 { /* compiled code */ }

    func toInteger(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    operator func ==(r: std.time.DayOfWeek): Bool { /* compiled code */ }

    operator func !=(r: std.time.DayOfWeek): Bool { /* compiled code */ }

    operator func +(n: Int64): std.time.DayOfWeek { /* compiled code */ }

    operator func -(n: Int64): std.time.DayOfWeek { /* compiled code */ }
}

class DateTimeFormat {
    let RFC1123: std.core.String

    let RFC3339: std.core.String

    let RFC822: std.core.String

    let RFC850: std.core.String

    let data: std.collection.ArrayList<std.time.FormatType>

    let rawString: std.core.String

    init(formatString: std.core.String) { /* compiled code */ }

    static func of(format: std.core.String): std.time.DateTimeFormat { /* compiled code */ }

    prop format: std.core.String {
        get() { /* compiled code */ }
    }
}

func isSumOverflow(left: Int64, right: Int64): Bool { /* compiled code */ }

func checkSecAdd(left: Int64, right: Int64): Unit { /* compiled code */ }

func checkMul(left: Int64, right: Int64): Unit { /* compiled code */ }

func checkDateTime(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64): Unit { /* compiled code */ }

func toYearAndSecond(sec: Int64): (Int64, Int64) { /* compiled code */ }

func toEpochSeconds(year: Int64, sec: UInt32): Int64 { /* compiled code */ }

func getDate(year: Int64, sec: UInt32): (std.time.Month, Int64) { /* compiled code */ }

func getDayOfWeek(year: Int64, sec: UInt32): Int64 { /* compiled code */ }

func daysSinceUnix(year: Int64): Int64 { /* compiled code */ }

func daysSinceAD1(year: Int64): Int64 { /* compiled code */ }

func daysAD1ToYear(year: Int64): Int64 { /* compiled code */ }

func adjustYearAndSecond(year: Int64, second: UInt32, offset: Int64): (Int64, UInt32) { /* compiled code */ }

func isLeapYear(year: Int64): Bool { /* compiled code */ }

func daysIn(month: Int64, year: Int64): Int64 { /* compiled code */ }

class TimeParseException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

func parseTime(value: std.core.String, format: std.core.String, isRFC3339: Bool): std.time.DateTime { /* compiled code */ }

func checkResultYear(seconds: Int64): Unit { /* compiled code */ }

func toSecondsFromAD1(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64): Int64 { /* compiled code */ }

func setDefaultTime(hour: Int64, minute: Int64, second: Int64, nanosecond: Int64): (Int64, Int64, Int64, Int64) { /* compiled code */ }

func adjustHour(hour: Int64, is24Hour: Bool, apmFlag: Int64): Int64 { /* compiled code */ }

func adjustDate(year: Int64, month: Int64, dayOfMonth: Int64, dayOfYear: Int64): (Int64, Int64) { /* compiled code */ }

func toSecondInYear(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64): Int64 { /* compiled code */ }

func genZoneNameWithOffset(offset: Int64): std.core.String { /* compiled code */ }

func dayOfYearToDate(dayOfYear: Int64, year: Int64): (Int64, Int64) { /* compiled code */ }

func setDefault(num: Int64): Int64 { /* compiled code */ }

func addZeroPrefix(value: Int64, length: Int64): std.core.String { /* compiled code */ }

struct DateTimeParser {
    var value: std.core.Array<Rune>

    var index: Int64

    var parseLen: Int64

    init(value: std.core.Array<Rune>) { /* compiled code */ }

    func getLenToParse(): Int64 { /* compiled code */ }

    func isEnd(): Bool { /* compiled code */ }

    func isEnough(): Bool { /* compiled code */ }

    mut func move(len: Int64): Unit { /* compiled code */ }

    func `get`(offset: Int64): Rune { /* compiled code */ }

    mut func parseYear(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func parseMonth(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func parseDayOfWeek(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func matchValue(formats: std.core.Array<std.core.String>): Int64 { /* compiled code */ }

    mut func parseEra(value: Bool, length: Int64): Unit { /* compiled code */ }

    mut func parseDayOfMonth(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func parseDayOfYear(value: Int64): Int64 { /* compiled code */ }

    mut func parseISOWeek(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func parseHourIn12(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func parseHourIn24(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func parseMinute(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func parseSecond(value: Int64, length: Int64, isRFC3339: Bool): (Int64, Int64) { /* compiled code */ }

    mut func parseNanosecond(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func parseOffset(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func parseOffsetDate(length: Int64): Int64 { /* compiled code */ }

    mut func parseAmPm(flag: Int64): Int64 { /* compiled code */ }

    mut func parseZoneInfo(value: std.core.String, length: Int64): std.core.String { /* compiled code */ }

    mut func parseZoneId(): std.core.String { /* compiled code */ }

    mut func parseZoneName(): std.core.String { /* compiled code */ }

    mut func parseGMTOffset(value: Int64, length: Int64): Int64 { /* compiled code */ }

    mut func checkSeperator(r: Rune): Unit { /* compiled code */ }

    func checkEnd(): Unit { /* compiled code */ }

    func getInteger(): Int64 { /* compiled code */ }

    mut func parseExtraInteger(length: Int64): Int64 { /* compiled code */ }

    mut func parseInteger(length: Int64): Int64 { /* compiled code */ }

    mut func parseSecFraction(length: Int64): Int64 { /* compiled code */ }

    func checkOffsetLength(n: Int64): Unit { /* compiled code */ }
}

enum FormatType {
    | FormatYear(Int64)

    | FormatISOYear(Int64)

    | FormatMonth(Int64)

    | FormatDayOfMonth(Int64)

    | FormatHourIn24(Int64)

    | FormatHourIn12(Int64)

    | FormatMinute(Int64)

    | FormatSecond(Int64)

    | FormatSecondFraction(Int64)

    | FormatOffset(Int64)

    | FormatZoneInfo(Int64)

    | FormatGMTOffset(Int64)

    | FormatDayOfYear(Int64)

    | FormatAmPm(Int64)

    | FormatDayOfWeek(Int64)

    | FormatISOWeek(Int64)

    | FormatEra(Int64)

    | Separator(Rune)

    | Termination

    func checkLength(): Unit { /* compiled code */ }

    func checkFormatLength(length: Int64, maxLen: Int64, r: Rune): Unit { /* compiled code */ }
}

enum Text {
    | TxtMonth

    | TxtDayOfWeek

    | TxtEra
}

enum Numeric {
    | NumDay

    | NumDayOfYear

    | NumWeekOfYear

    | NumHour12

    | NumHour24

    | NumMinute

    | NumSecond
}

func parseFormat(formatString: std.core.String): std.collection.ArrayList<std.time.FormatType> { /* compiled code */ }

func parseFormat(layout: std.core.Array<Rune>, index: Int64): (Int64, std.time.FormatType) { /* compiled code */ }

func findFormat(start: Int64, layout: std.core.Array<Rune>): Int64 { /* compiled code */ }

let SHORT_DAY_OF_WEEK_FORM: std.core.Array<std.core.String>

let LONG_DAY_OF_WEEK_FORM: std.core.Array<std.core.String>

let SHORT_MONTH_FORM: std.core.Array<std.core.String>

let LONG_MONTH_FORM: std.core.Array<std.core.String>

struct MonoTime <: std.core.Hashable & std.core.Comparable<std.time.MonoTime> {
    let d: std.core.Duration

    static func now(): std.time.MonoTime { /* compiled code */ }

    operator func +(r: std.core.Duration): std.time.MonoTime { /* compiled code */ }

    operator func -(r: std.core.Duration): std.time.MonoTime { /* compiled code */ }

    operator func -(r: std.time.MonoTime): std.core.Duration { /* compiled code */ }

    operator func ==(r: std.time.MonoTime): Bool { /* compiled code */ }

    operator func !=(r: std.time.MonoTime): Bool { /* compiled code */ }

    operator func >=(r: std.time.MonoTime): Bool { /* compiled code */ }

    operator func >(r: std.time.MonoTime): Bool { /* compiled code */ }

    operator func <=(r: std.time.MonoTime): Bool { /* compiled code */ }

    operator func <(r: std.time.MonoTime): Bool { /* compiled code */ }

    func compare(rhs: std.time.MonoTime): std.core.Ordering { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }
}

let TZIF_SOURCE: std.core.Array<std.core.String>

foreign func CJ_TIME_GetEnvVariable(id: CString): CString

enum RuleType {
    | JulianDay

    | DayOfYear

    | MonthNthDayOfWeek
}

class ZoneRule {
    var rType: std.time.RuleType

    var rDay: Int64

    var rWeek: Int64

    var rMon: Int64

    var rTime: Int64

    func parse(footer: std.core.String): std.core.String { /* compiled code */ }

    init() { /* compiled code */ }
}

class LocalTime {
    var des: std.core.String

    var offset: Int32

    var isDST: Bool

    init() { /* compiled code */ }

    init(des: std.core.String, offset: Int32, isDST: Bool) { /* compiled code */ }
}

class TransTime {
    var trans: Int64

    var index: UInt8

    init() { /* compiled code */ }

    init(trans: Int64, index: UInt8) { /* compiled code */ }
}

func parseTZ(footer: std.core.String, end: Int64, epochSecond: Int64): (std.core.String, Int64, Int64, Int64) { /* compiled code */ }

func swap(left: T, right: T): (T, T) { /* compiled code */ }

func parseTZName(footer: std.core.String): (std.core.String, std.core.String) { /* compiled code */ }

func parseTZOffset(footer: std.core.String): (Int64, std.core.String) { /* compiled code */ }

func parseTZRule(footer: std.core.String): (std.time.ZoneRule, std.core.String) { /* compiled code */ }

func getNum(footer: std.core.String): (Int64, std.core.String) { /* compiled code */ }

func transToSecOfYear(year: Int64, rule: std.time.ZoneRule, off: Int64): Int64 { /* compiled code */ }

class InvalidDataException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

foreign func CJ_TIME_GetLocalTimeOffset(): Int64

func initLocal(): std.time.TimeZone { /* compiled code */ }

func initLocalDefault(): std.time.TimeZone { /* compiled code */ }

foreign func CJ_TIME_Now(): std.time.NativeClock

foreign func CJ_TIME_MonotonicNow(): std.time.NativeClock

func monoNow(): (Int64, Int64) { /* compiled code */ }

foreign func CJ_TIME_GetFileSize(path: CPointer<UInt8>, pathLen: Int64): Int64

foreign func CJ_TIME_ReadAllBytesFromFile(path: CPointer<UInt8>, pathLen: Int64, buf: CPointer<UInt8>, bufLen: Int64): Int64

func loadTimeZone(name: std.core.String, source: std.core.String): std.time.TimeZone { /* compiled code */ }

func loadTZifData(name: std.core.String, source: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

func loadTZifData(path: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

func parseTZinfoData(data: std.core.Array<UInt8>): (std.core.Array<std.time.LocalTime>, std.core.Array<std.time.TransTime>, std.core.String) { /* compiled code */ }

func parseHeader(arrReader: std.time.ArrReader): (Int64, std.core.Array<Int64>) { /* compiled code */ }

class ArrReader {
    var arr: std.core.Array<UInt8>

    var index: Int64

    init(arr: std.core.Array<UInt8>) { /* compiled code */ }

    func skip(len: Int64): Unit { /* compiled code */ }

    func getNext(len: Int64): std.core.Array<UInt8> { /* compiled code */ }

    func getLast(): std.core.Array<UInt8> { /* compiled code */ }
}

func parseFields(arrReader: std.time.ArrReader): std.core.Array<Int64> { /* compiled code */ }

func parseZones(zoneNum: Int64, typeRecords: std.core.Array<UInt8>, zoneDes: std.core.Array<UInt8>): std.core.Array<std.time.LocalTime> { /* compiled code */ }

func itos(arr: std.core.Array<UInt8>, canNul: Bool): std.core.String { /* compiled code */ }

func parseZoneTrans(timeNum: Int64, zoneNum: Int64, transTimes: std.core.Array<UInt8>, transTypes: std.core.Array<UInt8>, timeSize: Int64): std.core.Array<std.time.TransTime> { /* compiled code */ }
