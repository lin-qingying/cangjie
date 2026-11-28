// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.time

import std.core.*
import std.convert.*
import std.collection.ArrayList

let MIN_INT64: Int64 = COMPILED_CODE /* compiled code */

let MAX_INT64: Int64 = COMPILED_CODE /* compiled code */

let MAX_YEAR: Int64 = COMPILED_CODE /* compiled code */

let MIN_YEAR: Int64 = COMPILED_CODE /* compiled code */

let MAX_NANOSECOND: Int64 = COMPILED_CODE /* compiled code */

let MONTHS_PER_YEAR: Int64 = COMPILED_CODE /* compiled code */

let DAYS_PER_WEEK: Int64 = COMPILED_CODE /* compiled code */

let DAYS_OF_NORMAL_YEAR: Int64 = COMPILED_CODE /* compiled code */

let DAYS_OF_LEAP_YEAR: Int64 = COMPILED_CODE /* compiled code */

let DAYS_PER_400YEARS: Int64 = COMPILED_CODE /* compiled code */

let DAYS_PER_100YEARS: Int64 = COMPILED_CODE /* compiled code */

let DAYS_PER_4YEARS: Int64 = COMPILED_CODE /* compiled code */

let HOURS_PER_DAY: Int64 = COMPILED_CODE /* compiled code */

let MINUTES_PER_HOUR: Int64 = COMPILED_CODE /* compiled code */

let SECS_PER_MINUTE: Int64 = COMPILED_CODE /* compiled code */

let SECS_PER_HOUR: Int64 = COMPILED_CODE /* compiled code */

let SECS_PER_DAY: Int64 = COMPILED_CODE /* compiled code */

let SECS_PER_WEEK: Int64 = COMPILED_CODE /* compiled code */

let MS_PER_SEC: Int64 = COMPILED_CODE /* compiled code */

let US_PER_MS: Int64 = COMPILED_CODE /* compiled code */

let NS_PER_US: Int64 = COMPILED_CODE /* compiled code */

let US_PER_SEC: Int64 = COMPILED_CODE /* compiled code */

let NS_PER_MS: Int64 = COMPILED_CODE /* compiled code */

let NS_PER_SEC: Int64 = COMPILED_CODE /* compiled code */

let NS_PER_MINUTE: Int64 = COMPILED_CODE /* compiled code */

let NS_PER_HOUR: Int64 = COMPILED_CODE /* compiled code */

let NS_PER_DAY: Int64 = COMPILED_CODE /* compiled code */

let SECS_OF_NORMAL_YEAR: Int64 = COMPILED_CODE /* compiled code */

let SECS_OF_LEAP_YEAR: Int64 = COMPILED_CODE /* compiled code */

let HOUR_12_INTERVAL: Int64 = COMPILED_CODE /* compiled code */

let HOUR_12_START: Int64 = COMPILED_CODE /* compiled code */

let HOUR_12_END: Int64 = COMPILED_CODE /* compiled code */

let HOUR_24_START: Int64 = COMPILED_CODE /* compiled code */

let HOUR_24_NOON: Int64 = COMPILED_CODE /* compiled code */

let START_AD_YEAR: Int64 = COMPILED_CODE /* compiled code */

let DAYS_OF_UNIX_TO_AD1: Int64 = COMPILED_CODE /* compiled code */

let SECS_OF_UNIX_TO_AD1: Int64 = COMPILED_CODE /* compiled code */

let DAYS_OF_MAX_TO_AD1: Int64 = COMPILED_CODE /* compiled code */

let SECS_OF_MAX_TO_AD1: Int64 = COMPILED_CODE /* compiled code */

let DAYS_OF_MIN_TO_AD1: Int64 = COMPILED_CODE /* compiled code */

let SECS_OF_MIN_TO_AD1: Int64 = COMPILED_CODE /* compiled code */

let DAYS_OF_JAN_TO_MARCH_LEAP: Int64 = COMPILED_CODE /* compiled code */

let MAX_OFFSET: Int64 = COMPILED_CODE /* compiled code */

let MIN_OFFSET: Int64 = COMPILED_CODE /* compiled code */

let YEAR_OUT_OF_RANGE: std.core.String = COMPILED_CODE /* compiled code */

let MAX_UTC_SECS: Int64 = COMPILED_CODE /* compiled code */

let MIN_UTC_SECS: Int64 = COMPILED_CODE /* compiled code */

let SEPARATOR: std.core.String = COMPILED_CODE /* compiled code */

let SLASH_CHAR: std.core.String = COMPILED_CODE /* compiled code */

let TZIF_SOUTCE_LINUX: std.core.String = COMPILED_CODE /* compiled code */

let LOCALTIME_SRC: std.core.String = COMPILED_CODE /* compiled code */

let TZPATH: std.core.String = COMPILED_CODE /* compiled code */

let MAXPATHLEN: Int64 = COMPILED_CODE /* compiled code */

public class TimeZone <: std.core.Object & std.core.ToString & std.core.Equatable<std.time.TimeZone> {
    public static let UTC: std.time.TimeZone

    public static let Local: std.time.TimeZone

    internal let zoneId: std.core.String

    internal let localTimes: std.core.Array<std.time.LocalTime>

    internal let transTimes: std.core.Array<std.time.TransTime>

    internal let footer: std.core.String

    internal var cache: std.core.Option<(Int64, Int64, std.time.LocalTime)>

    public prop id: std.core.String {
        get() { /* compiled code */ }
    }

    init(id: std.core.String, offset: Int32) { /* compiled code */ }

    init(id: std.core.String, offset: std.core.Duration) { /* compiled code */ }

    public static func load(id: std.core.String): std.time.TimeZone { /* compiled code */ }

    public static func loadFromPaths(id: std.core.String, tzpaths: std.core.Array<std.core.String>): std.time.TimeZone { /* compiled code */ }

    public static func loadFromTZData(id: std.core.String, data: std.core.Array<UInt8>): std.time.TimeZone { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public operator func ==(r: std.time.TimeZone): Bool { /* compiled code */ }

    public operator func !=(r: std.time.TimeZone): Bool { /* compiled code */ }

    internal func lookup(epochSecond: Int64): (std.core.String, Int64, Int64, Int64) { /* compiled code */ }

    internal func inferZoneIndex(): Int64 { /* compiled code */ }

    internal func lookupName(name: std.core.String, unix: Int64): Int64 { /* compiled code */ }
}

public struct DateTime <: std.core.ToString & std.core.Hashable & std.core.Comparable<std.time.DateTime> & std.convert.Formattable & std.convert.Parsable<std.time.DateTime> {
    internal let d: std.core.Duration

    internal let tz: std.time.TimeZone

    public static prop UnixEpoch: std.time.DateTime {
        get() { /* compiled code */ }
    }

    public prop year: Int64 {
        get() { /* compiled code */ }
    }

    public prop month: std.time.Month {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use `public prop month: Month` instead."]
    public prop monthValue: Int64 {
        get() { /* compiled code */ }
    }

    public prop dayOfMonth: Int64 {
        get() { /* compiled code */ }
    }

    public prop dayOfWeek: std.time.DayOfWeek {
        get() { /* compiled code */ }
    }

    public prop dayOfYear: Int64 {
        get() { /* compiled code */ }
    }

    public prop hour: Int64 {
        get() { /* compiled code */ }
    }

    public prop minute: Int64 {
        get() { /* compiled code */ }
    }

    public prop second: Int64 {
        get() { /* compiled code */ }
    }

    public prop nanosecond: Int64 {
        get() { /* compiled code */ }
    }

    public prop isoWeek: (Int64, Int64) {
        get() { /* compiled code */ }
    }

    public prop zone: std.time.TimeZone {
        get() { /* compiled code */ }
    }

    public prop zoneId: std.core.String {
        get() { /* compiled code */ }
    }

    public prop zoneOffset: std.core.Duration {
        get() { /* compiled code */ }
    }

    public prop date: (Int64, std.time.Month, Int64) {
        get() { /* compiled code */ }
    }

    public prop time: (Int64, Int64, Int64) {
        get() { /* compiled code */ }
    }

    init(secs: Int64, nanos: UInt32, tz: std.time.TimeZone) { /* compiled code */ }

    @Frozen
    public static func now(timeZone!: std.time.TimeZone = COMPILED_CODE /* compiled code */): std.time.DateTime { /* compiled code */ }

    public static func nowUTC(): std.time.DateTime { /* compiled code */ }

    public static func fromUnixTimeStamp(d: std.core.Duration): std.time.DateTime { /* compiled code */ }

    public static func ofEpoch(second!: Int64, nanosecond!: Int64): std.time.DateTime { /* compiled code */ }

    public static func of(year!: Int64, month!: Int64, dayOfMonth!: Int64, hour!: Int64 = COMPILED_CODE /* compiled code */, minute!: Int64 = COMPILED_CODE /* compiled code */, second!: Int64 = COMPILED_CODE /* compiled code */, nanosecond!: Int64 = COMPILED_CODE /* compiled code */, timeZone!: std.time.TimeZone = COMPILED_CODE /* compiled code */): std.time.DateTime { /* compiled code */ }

    public static func of(year!: Int64, month!: std.time.Month, dayOfMonth!: Int64, hour!: Int64 = COMPILED_CODE /* compiled code */, minute!: Int64 = COMPILED_CODE /* compiled code */, second!: Int64 = COMPILED_CODE /* compiled code */, nanosecond!: Int64 = COMPILED_CODE /* compiled code */, timeZone!: std.time.TimeZone = COMPILED_CODE /* compiled code */): std.time.DateTime { /* compiled code */ }

    public static func ofUTC(year!: Int64, month!: Int64, dayOfMonth!: Int64, hour!: Int64 = COMPILED_CODE /* compiled code */, minute!: Int64 = COMPILED_CODE /* compiled code */, second!: Int64 = COMPILED_CODE /* compiled code */, nanosecond!: Int64 = COMPILED_CODE /* compiled code */): std.time.DateTime { /* compiled code */ }

    public static func ofUTC(year!: Int64, month!: std.time.Month, dayOfMonth!: Int64, hour!: Int64 = COMPILED_CODE /* compiled code */, minute!: Int64 = COMPILED_CODE /* compiled code */, second!: Int64 = COMPILED_CODE /* compiled code */, nanosecond!: Int64 = COMPILED_CODE /* compiled code */): std.time.DateTime { /* compiled code */ }

    public static func parse(str: std.core.String): std.time.DateTime { /* compiled code */ }

    public static func tryParse(str: std.core.String): std.core.Option<std.time.DateTime> { /* compiled code */ }

    public static func parse(str: std.core.String, format: std.core.String): std.time.DateTime { /* compiled code */ }

    @Deprecated[message: "Use member function `public static func parse(str: String, format: String): DateTime` instead."]
    public static func parse(str: std.core.String, format: std.time.DateTimeFormat): std.time.DateTime { /* compiled code */ }

    public func inUTC(): std.time.DateTime { /* compiled code */ }

    public func inLocal(): std.time.DateTime { /* compiled code */ }

    public func inTimeZone(timeZone: std.time.TimeZone): std.time.DateTime { /* compiled code */ }

    public func toUnixTimeStamp(): std.core.Duration { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }

    @Deprecated[message: "Use member function `public func format(fmt: String): String` instead."]
    public func toString(format: std.time.DateTimeFormat): std.core.String { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func addYears(n: Int64): std.time.DateTime { /* compiled code */ }

    public func addMonths(n: Int64): std.time.DateTime { /* compiled code */ }

    public func addWeeks(n: Int64): std.time.DateTime { /* compiled code */ }

    public func addDays(n: Int64): std.time.DateTime { /* compiled code */ }

    public func addHours(n: Int64): std.time.DateTime { /* compiled code */ }

    public func addMinutes(n: Int64): std.time.DateTime { /* compiled code */ }

    public func addSeconds(n: Int64): std.time.DateTime { /* compiled code */ }

    public func addNanoseconds(n: Int64): std.time.DateTime { /* compiled code */ }

    public operator func +(r: std.core.Duration): std.time.DateTime { /* compiled code */ }

    public operator func -(r: std.core.Duration): std.time.DateTime { /* compiled code */ }

    public operator func -(r: std.time.DateTime): std.core.Duration { /* compiled code */ }

    public operator func ==(r: std.time.DateTime): Bool { /* compiled code */ }

    public operator func !=(r: std.time.DateTime): Bool { /* compiled code */ }

    public operator func >=(r: std.time.DateTime): Bool { /* compiled code */ }

    public operator func >(r: std.time.DateTime): Bool { /* compiled code */ }

    public operator func <=(r: std.time.DateTime): Bool { /* compiled code */ }

    public operator func <(r: std.time.DateTime): Bool { /* compiled code */ }

    public func compare(rhs: std.time.DateTime): std.core.Ordering { /* compiled code */ }

    internal func toYearString(length: Int64, isISOYear: Bool): std.core.String { /* compiled code */ }

    internal func toMonthString(length: Int64): std.core.String { /* compiled code */ }

    internal func toHourIn12String(length: Int64): std.core.String { /* compiled code */ }

    internal func toSecondFractionString(length: Int64): std.core.String { /* compiled code */ }

    internal func toDayOfWeekString(length: Int64): std.core.String { /* compiled code */ }

    internal func toEraString(length: Int64): std.core.String { /* compiled code */ }

    internal func toOffsetString(length: Int64): std.core.String { /* compiled code */ }

    internal func toZoneInfoString(length: Int64): std.core.String { /* compiled code */ }

    internal func toGMTOffsetString(length: Int64): std.core.String { /* compiled code */ }

    internal func toDayOfYearString(length: Int64): std.core.String { /* compiled code */ }

    internal func genDateTime(zoneId: std.core.String, zoneOffset: Int64): std.time.DateTime { /* compiled code */ }

    internal func checkExtraInfo(isoYear: Int64, isoWeek: Int64, dayOfWeek: Int64): Unit { /* compiled code */ }
}

internal func systemNow(): (Int64, Int64) { /* compiled code */ }

let DAYS_BEFORE: std.core.Array<Int64>

let DAYS_OF_MONTH: std.core.Array<Int64>

internal struct NativeClock {
    public var sec: Int64

    public var nanosec: Int64

    init(sec!: Int64, nanosec!: Int64) { /* compiled code */ }
}

public enum Month <: std.core.ToString & std.core.Equatable<std.time.Month> {
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

    public static func of(mon: Int64): std.time.Month { /* compiled code */ }

    @Deprecated[message: "Use member function `public func toInteger(): Int64` instead."]
    public func value(): Int64 { /* compiled code */ }

    public func toInteger(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public operator func +(n: Int64): std.time.Month { /* compiled code */ }

    public operator func -(n: Int64): std.time.Month { /* compiled code */ }

    public operator func ==(r: std.time.Month): Bool { /* compiled code */ }

    public operator func !=(r: std.time.Month): Bool { /* compiled code */ }
}

public enum DayOfWeek <: std.core.ToString & std.core.Equatable<std.time.DayOfWeek> {
    | Sunday

    | Monday

    | Tuesday

    | Wednesday

    | Thursday

    | Friday

    | Saturday

    public static func of(dayOfWeek: Int64): std.time.DayOfWeek { /* compiled code */ }

    @Deprecated[message: "Use member function `public func toInteger(): Int64` instead."]
    public func value(): Int64 { /* compiled code */ }

    public func toInteger(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public operator func ==(r: std.time.DayOfWeek): Bool { /* compiled code */ }

    public operator func !=(r: std.time.DayOfWeek): Bool { /* compiled code */ }

    public operator func +(n: Int64): std.time.DayOfWeek { /* compiled code */ }

    public operator func -(n: Int64): std.time.DayOfWeek { /* compiled code */ }
}

public class DateTimeFormat {
    public static const RFC1123: std.core.String = COMPILED_CODE /* compiled code */

    public static const RFC3339: std.core.String = COMPILED_CODE /* compiled code */

    public static const RFC822: std.core.String = COMPILED_CODE /* compiled code */

    public static const RFC850: std.core.String = COMPILED_CODE /* compiled code */

    internal let data: std.collection.ArrayList<std.time.FormatType>

    internal let rawString: std.core.String

    init(formatString: std.core.String) { /* compiled code */ }

    @Deprecated["Use member function `init(formatString: String)` instead."]
    public static func of(format: std.core.String): std.time.DateTimeFormat { /* compiled code */ }

    @Deprecated["The prop is deprecated, no substitutions."]
    public prop format: std.core.String {
        get() { /* compiled code */ }
    }
}

internal func isSumOverflow(left: Int64, right: Int64): Bool { /* compiled code */ }

internal func checkSecAdd(left: Int64, right: Int64): Unit { /* compiled code */ }

internal func checkMul(left: Int64, right: Int64): Unit { /* compiled code */ }

internal func checkDateTime(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64): Unit { /* compiled code */ }

internal func toYearAndSecond(sec: Int64): (Int64, Int64) { /* compiled code */ }

internal func toEpochSeconds(year: Int64, sec: UInt32): Int64 { /* compiled code */ }

internal func getDate(year: Int64, sec: UInt32): (std.time.Month, Int64) { /* compiled code */ }

internal func getDayOfWeek(year: Int64, sec: UInt32): Int64 { /* compiled code */ }

internal func daysSinceUnix(year: Int64): Int64 { /* compiled code */ }

internal func daysSinceAD1(year: Int64): Int64 { /* compiled code */ }

internal func daysAD1ToYear(year: Int64): Int64 { /* compiled code */ }

internal func adjustYearAndSecond(year: Int64, second: UInt32, offset: Int64): (Int64, UInt32) { /* compiled code */ }

internal func isLeapYear(year: Int64): Bool { /* compiled code */ }

internal func daysIn(month: Int64, year: Int64): Int64 { /* compiled code */ }

public class TimeParseException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

internal func parseTime(value: std.core.String, format: std.core.String, isRFC3339!: Bool = COMPILED_CODE /* compiled code */): std.time.DateTime { /* compiled code */ }

internal func checkResultYear(seconds: Int64): Unit { /* compiled code */ }

internal func toSecondsFromAD1(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64): Int64 { /* compiled code */ }

internal func setDefaultTime(hour: Int64, minute: Int64, second: Int64, nanosecond: Int64): (Int64, Int64, Int64, Int64) { /* compiled code */ }

internal func adjustHour(hour: Int64, is24Hour: Bool, apmFlag: Int64): Int64 { /* compiled code */ }

internal func adjustDate(year: Int64, month: Int64, dayOfMonth: Int64, dayOfYear: Int64): (Int64, Int64) { /* compiled code */ }

internal func toSecondInYear(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64): Int64 { /* compiled code */ }

internal func genZoneNameWithOffset(offset: Int64): std.core.String { /* compiled code */ }

internal func dayOfYearToDate(dayOfYear: Int64, year: Int64): (Int64, Int64) { /* compiled code */ }

internal func setDefault(num: Int64): Int64 { /* compiled code */ }

internal func addZeroPrefix(value: Int64, length: Int64): std.core.String { /* compiled code */ }

internal struct DateTimeParser {
    internal var value: std.core.Array<Rune>

    internal var index: Int64

    internal var parseLen: Int64

    init(value: std.core.Array<Rune>) { /* compiled code */ }

    internal func getLenToParse(): Int64 { /* compiled code */ }

    internal func isEnd(): Bool { /* compiled code */ }

    internal func isEnough(): Bool { /* compiled code */ }

    internal mut func move(len: Int64): Unit { /* compiled code */ }

    internal func get(offset: Int64): Rune { /* compiled code */ }

    internal mut func parseYear(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func parseMonth(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func parseDayOfWeek(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func matchValue(formats: std.core.Array<std.core.String>): Int64 { /* compiled code */ }

    internal mut func parseEra(value: Bool, length: Int64): Unit { /* compiled code */ }

    internal mut func parseDayOfMonth(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func parseDayOfYear(value: Int64): Int64 { /* compiled code */ }

    internal mut func parseISOWeek(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func parseHourIn12(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func parseHourIn24(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func parseMinute(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func parseSecond(value: Int64, length: Int64, isRFC3339: Bool): (Int64, Int64) { /* compiled code */ }

    internal mut func parseNanosecond(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func parseOffset(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func parseOffsetDate(length: Int64): Int64 { /* compiled code */ }

    internal mut func parseAmPm(flag: Int64): Int64 { /* compiled code */ }

    internal mut func parseZoneInfo(value: std.core.String, length: Int64): std.core.String { /* compiled code */ }

    internal mut func parseZoneId(): std.core.String { /* compiled code */ }

    internal mut func parseZoneName(): std.core.String { /* compiled code */ }

    internal mut func parseGMTOffset(value: Int64, length: Int64): Int64 { /* compiled code */ }

    internal mut func checkSeperator(r: Rune): Unit { /* compiled code */ }

    internal func checkEnd(): Unit { /* compiled code */ }

    internal func getInteger(): Int64 { /* compiled code */ }

    internal mut func parseExtraInteger(length: Int64): Int64 { /* compiled code */ }

    internal mut func parseInteger(length: Int64): Int64 { /* compiled code */ }

    internal mut func parseSecFraction(length: Int64): Int64 { /* compiled code */ }

    internal func checkOffsetLength(n: Int64): Unit { /* compiled code */ }
}

internal enum FormatType {
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

    internal func checkLength(): Unit { /* compiled code */ }

    internal func checkFormatLength(length: Int64, maxLen: Int64, r: Rune): Unit { /* compiled code */ }
}

internal enum Text {
    | TxtMonth

    | TxtDayOfWeek

    | TxtEra
}

internal enum Numeric {
    | NumDay

    | NumDayOfYear

    | NumWeekOfYear

    | NumHour12

    | NumHour24

    | NumMinute

    | NumSecond
}

internal func parseFormat(formatString: std.core.String): std.collection.ArrayList<std.time.FormatType> { /* compiled code */ }

internal func parseFormat(layout: std.core.Array<Rune>, index: Int64): (Int64, std.time.FormatType) { /* compiled code */ }

internal func findFormat(start: Int64, layout: std.core.Array<Rune>): Int64 { /* compiled code */ }

let SHORT_DAY_OF_WEEK_FORM: std.core.Array<std.core.String>

let LONG_DAY_OF_WEEK_FORM: std.core.Array<std.core.String>

let SHORT_MONTH_FORM: std.core.Array<std.core.String>

let LONG_MONTH_FORM: std.core.Array<std.core.String>

public struct MonoTime <: std.core.Hashable & std.core.Comparable<std.time.MonoTime> {
    internal let d: std.core.Duration

    public static func now(): std.time.MonoTime { /* compiled code */ }

    public operator func +(r: std.core.Duration): std.time.MonoTime { /* compiled code */ }

    public operator func -(r: std.core.Duration): std.time.MonoTime { /* compiled code */ }

    public operator func -(r: std.time.MonoTime): std.core.Duration { /* compiled code */ }

    public operator func ==(r: std.time.MonoTime): Bool { /* compiled code */ }

    public operator func !=(r: std.time.MonoTime): Bool { /* compiled code */ }

    public operator func >=(r: std.time.MonoTime): Bool { /* compiled code */ }

    public operator func >(r: std.time.MonoTime): Bool { /* compiled code */ }

    public operator func <=(r: std.time.MonoTime): Bool { /* compiled code */ }

    public operator func <(r: std.time.MonoTime): Bool { /* compiled code */ }

    public func compare(rhs: std.time.MonoTime): std.core.Ordering { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }
}

let TZIF_SOURCE: std.core.Array<std.core.String>

internal foreign func CJ_TIME_GetEnvVariable(id: CString): CString

internal enum RuleType {
    | JulianDay

    | DayOfYear

    | MonthNthDayOfWeek
}

internal class ZoneRule {
    internal var rType: std.time.RuleType

    internal var rDay: Int64

    internal var rWeek: Int64

    internal var rMon: Int64

    internal var rTime: Int64

    internal func parse(footer: std.core.String): std.core.String { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class LocalTime {
    internal var des: std.core.String

    internal var offset: Int32

    internal var isDST: Bool

    init() { /* compiled code */ }

    init(des: std.core.String, offset: Int32, isDST: Bool) { /* compiled code */ }
}

internal class TransTime {
    internal var trans: Int64

    internal var index: UInt8

    init() { /* compiled code */ }

    init(trans: Int64, index: UInt8) { /* compiled code */ }
}

internal func parseTZ(footer: std.core.String, end: Int64, epochSecond: Int64): (std.core.String, Int64, Int64, Int64) { /* compiled code */ }

internal func swap<T>(left: T, right: T): (T, T) { /* compiled code */ }

internal func parseTZName(footer: std.core.String): (std.core.String, std.core.String) { /* compiled code */ }

internal func parseTZOffset(footer: std.core.String): (Int64, std.core.String) { /* compiled code */ }

internal func parseTZRule(footer: std.core.String): (std.time.ZoneRule, std.core.String) { /* compiled code */ }

internal func getNum(footer: std.core.String): (Int64, std.core.String) { /* compiled code */ }

internal func transToSecOfYear(year: Int64, rule: std.time.ZoneRule, off: Int64): Int64 { /* compiled code */ }

public class InvalidDataException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

internal foreign func CJ_TIME_GetLocalTimeOffset(): Int64

internal func initLocal(): std.time.TimeZone { /* compiled code */ }

internal func initLocalDefault(): std.time.TimeZone { /* compiled code */ }

internal foreign func CJ_TIME_Now(): std.time.NativeClock

internal foreign func CJ_TIME_MonotonicNow(): std.time.NativeClock

internal func monoNow(): (Int64, Int64) { /* compiled code */ }

internal foreign func CJ_TIME_GetFileSize(path: std.core.CPointer<UInt8>, pathLen: Int64): Int64

internal foreign func CJ_TIME_ReadAllBytesFromFile(path: std.core.CPointer<UInt8>, pathLen: Int64, buf: std.core.CPointer<UInt8>, bufLen: Int64): Int64

internal func loadTimeZone(name: std.core.String, source: std.core.String): std.time.TimeZone { /* compiled code */ }

internal func loadTZifData(name: std.core.String, source: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

internal func loadTZifData(path: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

internal func parseTZinfoData(data: std.core.Array<UInt8>): (std.core.Array<std.time.LocalTime>, std.core.Array<std.time.TransTime>, std.core.String) { /* compiled code */ }

internal func parseHeader(arrReader: std.time.ArrReader): (Int64, std.core.Array<Int64>) { /* compiled code */ }

internal class ArrReader {
    internal var arr: std.core.Array<UInt8>

    internal var index: Int64

    init(arr: std.core.Array<UInt8>) { /* compiled code */ }

    internal func skip(len: Int64): Unit { /* compiled code */ }

    internal func getNext(len: Int64): std.core.Array<UInt8> { /* compiled code */ }

    internal func getLast(): std.core.Array<UInt8> { /* compiled code */ }
}

internal func parseFields(arrReader: std.time.ArrReader): std.core.Array<Int64> { /* compiled code */ }

internal func parseZones(zoneNum: Int64, typeRecords: std.core.Array<UInt8>, zoneDes: std.core.Array<UInt8>): std.core.Array<std.time.LocalTime> { /* compiled code */ }

internal func itos(arr: std.core.Array<UInt8>, canNul: Bool): std.core.String { /* compiled code */ }

internal func parseZoneTrans(timeNum: Int64, zoneNum: Int64, transTimes: std.core.Array<UInt8>, transTypes: std.core.Array<UInt8>, timeSize: Int64): std.core.Array<std.time.TransTime> { /* compiled code */ }
