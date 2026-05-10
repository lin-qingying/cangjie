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

    prop id: std.core.String

    init(id: std.core.String, offset: Int32) { /* compiled code */ }

    init(id: std.core.String, offset: std.core.Duration) { /* compiled code */ }

    func load(id: std.core.String): std.time.TimeZone

    func loadFromPaths(id: std.core.String, tzpaths: std.core.Array<std.core.String>): std.time.TimeZone

    func loadFromTZData(id: std.core.String, data: std.core.Array<UInt8>): std.time.TimeZone

    func toString: std.core.String

    operator func ==(r: std.time.TimeZone): Bool

    operator func !=(r: std.time.TimeZone): Bool

    func lookup(epochSecond: Int64): (std.core.String, Int64, Int64, Int64)

    func inferZoneIndex: Int64

    func lookupName(name: std.core.String, unix: Int64): Int64
}

struct DateTime <: std.core.ToString & std.core.Hashable & std.core.Comparable<std.time.DateTime> & std.convert.Formattable & std.convert.Parsable<std.time.DateTime> {
    let d: std.core.Duration

    let tz: std.time.TimeZone

    prop UnixEpoch: std.time.DateTime

    prop year: Int64

    prop month: std.time.Month

    prop monthValue: Int64

    prop dayOfMonth: Int64

    prop dayOfWeek: std.time.DayOfWeek

    prop dayOfYear: Int64

    prop hour: Int64

    prop minute: Int64

    prop second: Int64

    prop nanosecond: Int64

    prop isoWeek: (Int64, Int64)

    prop zone: std.time.TimeZone

    prop zoneId: std.core.String

    prop zoneOffset: std.core.Duration

    prop date: (Int64, std.time.Month, Int64)

    prop time: (Int64, Int64, Int64)

    init(secs: Int64, nanos: UInt32, tz: std.time.TimeZone) { /* compiled code */ }

    func now(timeZone: std.time.TimeZone): std.time.DateTime

    func nowUTC: std.time.DateTime

    func fromUnixTimeStamp(d: std.core.Duration): std.time.DateTime

    func ofEpoch(second: Int64, nanosecond: Int64): std.time.DateTime

    func of(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64, timeZone: std.time.TimeZone): std.time.DateTime

    func of(year: Int64, month: std.time.Month, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64, timeZone: std.time.TimeZone): std.time.DateTime

    func ofUTC(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64): std.time.DateTime

    func ofUTC(year: Int64, month: std.time.Month, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64): std.time.DateTime

    func parse(str: std.core.String): std.time.DateTime

    func tryParse(str: std.core.String): std.core.Option<std.time.DateTime>

    func parse(str: std.core.String, format: std.core.String): std.time.DateTime

    func parse(str: std.core.String, format: std.time.DateTimeFormat): std.time.DateTime

    func inUTC: std.time.DateTime

    func inLocal: std.time.DateTime

    func inTimeZone(timeZone: std.time.TimeZone): std.time.DateTime

    func toUnixTimeStamp: std.core.Duration

    func toString: std.core.String

    func format(fmt: std.core.String): std.core.String

    func toString(format: std.time.DateTimeFormat): std.core.String

    func hashCode: Int64

    func addYears(n: Int64): std.time.DateTime

    func addMonths(n: Int64): std.time.DateTime

    func addWeeks(n: Int64): std.time.DateTime

    func addDays(n: Int64): std.time.DateTime

    func addHours(n: Int64): std.time.DateTime

    func addMinutes(n: Int64): std.time.DateTime

    func addSeconds(n: Int64): std.time.DateTime

    func addNanoseconds(n: Int64): std.time.DateTime

    operator func +(r: std.core.Duration): std.time.DateTime

    operator func -(r: std.core.Duration): std.time.DateTime

    operator func -(r: std.time.DateTime): std.core.Duration

    operator func ==(r: std.time.DateTime): Bool

    operator func !=(r: std.time.DateTime): Bool

    operator func >=(r: std.time.DateTime): Bool

    operator func >(r: std.time.DateTime): Bool

    operator func <=(r: std.time.DateTime): Bool

    operator func <(r: std.time.DateTime): Bool

    func compare(rhs: std.time.DateTime): std.core.Ordering

    func toYearString(length: Int64, isISOYear: Bool): std.core.String

    func toMonthString(length: Int64): std.core.String

    func toHourIn12String(length: Int64): std.core.String

    func toSecondFractionString(length: Int64): std.core.String

    func toDayOfWeekString(length: Int64): std.core.String

    func toEraString(length: Int64): std.core.String

    func toOffsetString(length: Int64): std.core.String

    func toZoneInfoString(length: Int64): std.core.String

    func toGMTOffsetString(length: Int64): std.core.String

    func toDayOfYearString(length: Int64): std.core.String

    func genDateTime(zoneId: std.core.String, zoneOffset: Int64): std.time.DateTime

    func checkExtraInfo(isoYear: Int64, isoWeek: Int64, dayOfWeek: Int64): Unit
}

func systemNow: (Int64, Int64)

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

    func of(mon: Int64): std.time.Month

    func value: Int64

    func toInteger: Int64

    func toString: std.core.String

    operator func +(n: Int64): std.time.Month

    operator func -(n: Int64): std.time.Month

    operator func ==(r: std.time.Month): Bool

    operator func !=(r: std.time.Month): Bool
}

enum DayOfWeek <: std.core.ToString & std.core.Equatable<std.time.DayOfWeek> {
    | Sunday

    | Monday

    | Tuesday

    | Wednesday

    | Thursday

    | Friday

    | Saturday

    func of(dayOfWeek: Int64): std.time.DayOfWeek

    func value: Int64

    func toInteger: Int64

    func toString: std.core.String

    operator func ==(r: std.time.DayOfWeek): Bool

    operator func !=(r: std.time.DayOfWeek): Bool

    operator func +(n: Int64): std.time.DayOfWeek

    operator func -(n: Int64): std.time.DayOfWeek
}

class DateTimeFormat {
    let RFC1123: std.core.String

    let RFC3339: std.core.String

    let RFC822: std.core.String

    let RFC850: std.core.String

    let data: std.collection.ArrayList<std.time.FormatType>

    let rawString: std.core.String

    init(formatString: std.core.String) { /* compiled code */ }

    func of(format: std.core.String): std.time.DateTimeFormat

    prop format: std.core.String
}

func isSumOverflow(left: Int64, right: Int64): Bool

func checkSecAdd(left: Int64, right: Int64): Unit

func checkMul(left: Int64, right: Int64): Unit

func checkDateTime(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64, nanosecond: Int64): Unit

func toYearAndSecond(sec: Int64): (Int64, Int64)

func toEpochSeconds(year: Int64, sec: UInt32): Int64

func getDate(year: Int64, sec: UInt32): (std.time.Month, Int64)

func getDayOfWeek(year: Int64, sec: UInt32): Int64

func daysSinceUnix(year: Int64): Int64

func daysSinceAD1(year: Int64): Int64

func daysAD1ToYear(year: Int64): Int64

func adjustYearAndSecond(year: Int64, second: UInt32, offset: Int64): (Int64, UInt32)

func isLeapYear(year: Int64): Bool

func daysIn(month: Int64, year: Int64): Int64

class TimeParseException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

func parseTime(value: std.core.String, format: std.core.String, isRFC3339: Bool): std.time.DateTime

func checkResultYear(seconds: Int64): Unit

func toSecondsFromAD1(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64): Int64

func setDefaultTime(hour: Int64, minute: Int64, second: Int64, nanosecond: Int64): (Int64, Int64, Int64, Int64)

func adjustHour(hour: Int64, is24Hour: Bool, apmFlag: Int64): Int64

func adjustDate(year: Int64, month: Int64, dayOfMonth: Int64, dayOfYear: Int64): (Int64, Int64)

func toSecondInYear(year: Int64, month: Int64, dayOfMonth: Int64, hour: Int64, minute: Int64, second: Int64): Int64

func genZoneNameWithOffset(offset: Int64): std.core.String

func dayOfYearToDate(dayOfYear: Int64, year: Int64): (Int64, Int64)

func setDefault(num: Int64): Int64

func addZeroPrefix(value: Int64, length: Int64): std.core.String

struct DateTimeParser {
    var value: std.core.Array<Rune>

    var index: Int64

    var parseLen: Int64

    init(value: std.core.Array<Rune>) { /* compiled code */ }

    func getLenToParse: Int64

    func isEnd: Bool

    func isEnough: Bool

    func move(len: Int64): Unit

    func `get`(offset: Int64): Rune

    func parseYear(value: Int64, length: Int64): Int64

    func parseMonth(value: Int64, length: Int64): Int64

    func parseDayOfWeek(value: Int64, length: Int64): Int64

    func matchValue(formats: std.core.Array<std.core.String>): Int64

    func parseEra(value: Bool, length: Int64): Unit

    func parseDayOfMonth(value: Int64, length: Int64): Int64

    func parseDayOfYear(value: Int64): Int64

    func parseISOWeek(value: Int64, length: Int64): Int64

    func parseHourIn12(value: Int64, length: Int64): Int64

    func parseHourIn24(value: Int64, length: Int64): Int64

    func parseMinute(value: Int64, length: Int64): Int64

    func parseSecond(value: Int64, length: Int64, isRFC3339: Bool): (Int64, Int64)

    func parseNanosecond(value: Int64, length: Int64): Int64

    func parseOffset(value: Int64, length: Int64): Int64

    func parseOffsetDate(length: Int64): Int64

    func parseAmPm(flag: Int64): Int64

    func parseZoneInfo(value: std.core.String, length: Int64): std.core.String

    func parseZoneId: std.core.String

    func parseZoneName: std.core.String

    func parseGMTOffset(value: Int64, length: Int64): Int64

    func checkSeperator(r: Rune): Unit

    func checkEnd: Unit

    func getInteger: Int64

    func parseExtraInteger(length: Int64): Int64

    func parseInteger(length: Int64): Int64

    func parseSecFraction(length: Int64): Int64

    func checkOffsetLength(n: Int64): Unit
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

    func checkLength: Unit

    func checkFormatLength(length: Int64, maxLen: Int64, r: Rune): Unit
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

func parseFormat(formatString: std.core.String): std.collection.ArrayList<std.time.FormatType>

func parseFormat(layout: std.core.Array<Rune>, index: Int64): (Int64, std.time.FormatType)

func findFormat(start: Int64, layout: std.core.Array<Rune>): Int64

let SHORT_DAY_OF_WEEK_FORM: std.core.Array<std.core.String>

let LONG_DAY_OF_WEEK_FORM: std.core.Array<std.core.String>

let SHORT_MONTH_FORM: std.core.Array<std.core.String>

let LONG_MONTH_FORM: std.core.Array<std.core.String>

struct MonoTime <: std.core.Hashable & std.core.Comparable<std.time.MonoTime> {
    let d: std.core.Duration

    func now: std.time.MonoTime

    operator func +(r: std.core.Duration): std.time.MonoTime

    operator func -(r: std.core.Duration): std.time.MonoTime

    operator func -(r: std.time.MonoTime): std.core.Duration

    operator func ==(r: std.time.MonoTime): Bool

    operator func !=(r: std.time.MonoTime): Bool

    operator func >=(r: std.time.MonoTime): Bool

    operator func >(r: std.time.MonoTime): Bool

    operator func <=(r: std.time.MonoTime): Bool

    operator func <(r: std.time.MonoTime): Bool

    func compare(rhs: std.time.MonoTime): std.core.Ordering

    func hashCode: Int64
}

let TZIF_SOURCE: std.core.Array<std.core.String>

func CJ_TIME_GetEnvVariable(id: CString): CString

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

    func parse(footer: std.core.String): std.core.String

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

func parseTZ(footer: std.core.String, end: Int64, epochSecond: Int64): (std.core.String, Int64, Int64, Int64)

func swap(left: T, right: T): (T, T)

func parseTZName(footer: std.core.String): (std.core.String, std.core.String)

func parseTZOffset(footer: std.core.String): (Int64, std.core.String)

func parseTZRule(footer: std.core.String): (std.time.ZoneRule, std.core.String)

func getNum(footer: std.core.String): (Int64, std.core.String)

func transToSecOfYear(year: Int64, rule: std.time.ZoneRule, off: Int64): Int64

class InvalidDataException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

func CJ_TIME_GetLocalTimeOffset: Int64

func initLocal: std.time.TimeZone

func initLocalDefault: std.time.TimeZone

func CJ_TIME_Now: std.time.NativeClock

func CJ_TIME_MonotonicNow: std.time.NativeClock

func monoNow: (Int64, Int64)

func CJ_TIME_GetFileSize(path: CPointer<UInt8>, pathLen: Int64): Int64

func CJ_TIME_ReadAllBytesFromFile(path: CPointer<UInt8>, pathLen: Int64, buf: CPointer<UInt8>, bufLen: Int64): Int64

func loadTimeZone(name: std.core.String, source: std.core.String): std.time.TimeZone

func loadTZifData(name: std.core.String, source: std.core.String): std.core.Array<UInt8>

func loadTZifData(path: std.core.String): std.core.Array<UInt8>

func parseTZinfoData(data: std.core.Array<UInt8>): (std.core.Array<std.time.LocalTime>, std.core.Array<std.time.TransTime>, std.core.String)

func parseHeader(arrReader: std.time.ArrReader): (Int64, std.core.Array<Int64>)

class ArrReader {
    var arr: std.core.Array<UInt8>

    var index: Int64

    init(arr: std.core.Array<UInt8>) { /* compiled code */ }

    func skip(len: Int64): Unit

    func getNext(len: Int64): std.core.Array<UInt8>

    func getLast: std.core.Array<UInt8>
}

func parseFields(arrReader: std.time.ArrReader): std.core.Array<Int64>

func parseZones(zoneNum: Int64, typeRecords: std.core.Array<UInt8>, zoneDes: std.core.Array<UInt8>): std.core.Array<std.time.LocalTime>

func itos(arr: std.core.Array<UInt8>, canNul: Bool): std.core.String

func parseZoneTrans(timeNum: Int64, zoneNum: Int64, transTimes: std.core.Array<UInt8>, transTypes: std.core.Array<UInt8>, timeSize: Int64): std.core.Array<std.time.TransTime>
