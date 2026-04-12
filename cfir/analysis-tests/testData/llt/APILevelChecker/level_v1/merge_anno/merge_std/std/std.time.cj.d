// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.












package std.time

import std.convert.*
import std.collection.ArrayList

/**
* A DateTime represents an instance in time with nanosecond precision.
*
* DateTime instances can be compared using common comparison operators.
* DateTime instances can subtract other DateTime instances, producing a Duration.
* DateTime instances can add or sub a Duration, producing a new DateTime.
*
* Each DateTime is associated with a time zone. When calculating the dateTime representation,
* the TimeZone is involved in it, such as in the toString function.
* The methods inLocal, inUTC, and inTimeZone return a new DateTime with a specified TimeZone.
* Every function returns a dateTime will create a new instance.
*
* The comparison results between two DateTimes determined by their current UTC time.
* For example, 2023-04-10T08:00:00 in CST（China Standard Time）is equal to
* 2023-04-10T00:00:00 in UTC.
*/
@!APILevel[12, atomicservice : true]
public struct DateTime <: ToString & Hashable & Comparable<DateTime> & Formattable & Parsable<DateTime> {
    /* Unix epoch time. */
    @!APILevel[12, atomicservice : true]
    static public prop UnixEpoch: DateTime
    
    /**
    * Get the year of DateTime instance.
    * Ranges in [-999,999,999, 999,999,999].
    */
    @!APILevel[12, atomicservice : true]
    public prop year: Int64
    
    /**
    * Get the year of DateTime instance.
    * Values are in Enum Month.
    */
    @!APILevel[12, atomicservice : true]
    public prop month: Month
    
    /**
    * Get the month of DateTime instance.
    * Ranges in [1, 12].
    */
    @Deprecated[message: "Use `public prop month: Month` instead."]
    @!APILevel[12, atomicservice : true]
    public prop monthValue: Int64
    
    /**
    * Get the day of DateTime instance.
    * Ranges in [1, 31], the maximum depends on month and year.
    */
    @!APILevel[12, atomicservice : true]
    public prop dayOfMonth: Int64
    
    /**
    * Get the day of week of DateTime instance.
    * Values are in Enum DayOfWeek.
    */
    @!APILevel[12, atomicservice : true]
    public prop dayOfWeek: DayOfWeek
    
    /**
    * Get the day of year of DateTime instance.
    * Ranges in [1, 365] in non leap year and [1, 366] in leap year.
    */
    @!APILevel[12, atomicservice : true]
    public prop dayOfYear: Int64
    
    /**
    * Get the hour of DateTime instance.
    * Ranges in [0, 23].
    */
    @!APILevel[12, atomicservice : true]
    public prop hour: Int64
    
    /**
    * Get the minute of DateTime instance.
    * Ranges in [0, 59].
    */
    @!APILevel[12, atomicservice : true]
    public prop minute: Int64
    
    /**
    * Get the second of DateTime instance.
    * Ranges in [0, 59].
    */
    @!APILevel[12, atomicservice : true]
    public prop second: Int64
    
    /**
    * Get the nanosecond of DateTime instance.
    * Ranges in [0, 59].
    */
    @!APILevel[12, atomicservice : true]
    public prop nanosecond: Int64
    
    /**
    * Get the year and week tuple of DateTime instance based on ISO-8601.
    * Ranges in [0, 59].
    */
    @!APILevel[12, atomicservice : true]
    public prop isoWeek: (Int64, Int64)
    
    /**
    * Get the timeZone of DateTime instance.
    */
    @!APILevel[12, atomicservice : true]
    public prop zone: TimeZone
    
    /**
    * Get the zone id of DateTime instance.
    */
    @!APILevel[12, atomicservice : true]
    public prop zoneId: String
    
    /**
    * Get the zone offset of DateTime instance.
    */
    @!APILevel[12, atomicservice : true]
    public prop zoneOffset: Duration
    
    @!APILevel[12, atomicservice : true]
    public prop date: (Int64, Month, Int64)
    
    @!APILevel[12, atomicservice : true]
    public prop time: (Int64, Int64, Int64)
    
    /**
    * Get current DateTime in specified time zone.
    *
    * @param timeZone specified time zone. the default value is set to TimeZone.Local.
    * @return current DateTime in specified time zone.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public static func now(timeZone!: TimeZone = TimeZone.Local): DateTime
    
    /**
    * Get current DateTime, with time zone set to UTC.
    *
    * @return current DateTime of UTC.
    */
    @!APILevel[12, atomicservice : true]
    public static func nowUTC(): DateTime
    
    /**
    * Construct DateTime after d since unix epoch time.
    *
    * @param d duration.
    * @return DateTime after d since unix epoch time.
    */
    @!APILevel[12, atomicservice : true]
    public static func fromUnixTimeStamp(d: Duration): DateTime
    
    /**
    * Construct a DateTime instance with unix second and unix nanosecond.
    * The start time is set to unix epoch time.
    *
    * @param second unix second.
    * @param nanosecond unix nanosecond.
    * @return DateTime after second seconds and nanosecond nanoseconds since unix epoch time.
    *
    * @throws IllegalArgumentException if the value of nanosecond is outside the range [0, 999,999,999].
    * @throws ArithmeticException if the result of year is outside the range [-999,999,999, 999,999,999].
    */
    @!APILevel[12, atomicservice : true]
    public static func ofEpoch(second!: Int64, nanosecond!: Int64): DateTime
    
    /**
    * Construct a instance of DateTime with specified parameters.
    * The start time is set to unix epoch time.
    *
    * @param year year A.D, ranges in [-999,999,999, 999,999,999].
    * @param month month value of year, ranges in [1, 12].
    * @param dayOfMonth days of month, ranges in [1, 31], the maximum depends on month and year.
    * @param hour hours of day, ranges in [0, 23].
    * @param minute minutes of hour, ranges in [0, 59].
    * @param second seconds of minute, ranges in [0, 59].
    * @param nanosecond nanoseconds of second, ranges in [0, 999,999,999].
    * @param timeZone time zone.
    * @return DateTime with specified parameters.
    *
    * @throws IllegalArgumentException if the value of any parameter is out of the range, or the datetime does not exist.
    */
    @!APILevel[12, atomicservice : true]
    public static func of(
        year!: Int64,
        month!: Int64,
        dayOfMonth!: Int64,
        hour!: Int64 = 0,
        minute!: Int64 = 0,
        second!: Int64 = 0,
        nanosecond!: Int64 = 0,
        timeZone!: TimeZone = TimeZone.Local
    ): DateTime
    
    /**
    * Construct a instance of DateTime with specified parameters.
    * The start time is set to unix epoch time.
    *
    * @param year year A.D, ranges in [-999,999,999, 999,999,999].
    * @param month month of year.
    * @param dayOfMonth days of month, ranges in [1, 31], the maximum depends on month and year.
    * @param hour hours of day, ranges in [0, 23].
    * @param minute minutes of hour, ranges in [0, 59].
    * @param second seconds of minute, ranges in [0, 59].
    * @param nanosecond nanoseconds of second, ranges in [0, 999,999,999].
    * @param timeZone time zone.
    * @return DateTime with specified parameters.
    *
    * @throws IllegalArgumentException if the value of any parameter is out of the range, or the datetime does not exist.
    */
    @!APILevel[12, atomicservice : true]
    public static func of(
        year!: Int64,
        month!: Month,
        dayOfMonth!: Int64,
        hour!: Int64 = 0,
        minute!: Int64 = 0,
        second!: Int64 = 0,
        nanosecond!: Int64 = 0,
        timeZone!: TimeZone = TimeZone.Local
    ): DateTime
    
    /**
    * Construct a instance of DateTime with specified parameters.
    * The start time is set to unix epoch time.
    *
    * @param year year A.D, ranges in [-999,999,999, 999,999,999].
    * @param month month value of year, ranges in [1, 12].
    * @param dayOfMonth days of month, ranges in [1, 31], the maximum depends on month and year.
    * @param hour hours of day, ranges in [0, 23].
    * @param minute minutes of hour, ranges in [0, 59].
    * @param second seconds of minute, ranges in [0, 59].
    * @param nanosecond nanoseconds of second, ranges in [0, 999,999,999].
    * @return DateTime with specified parameters.
    *
    * @throws IllegalArgumentException if the value of any parameter is out of the range.
    */
    @!APILevel[12, atomicservice : true]
    public static func ofUTC(
        year!: Int64,
        month!: Int64,
        dayOfMonth!: Int64,
        hour!: Int64 = 0,
        minute!: Int64 = 0,
        second!: Int64 = 0,
        nanosecond!: Int64 = 0
    ): DateTime
    
    /**
    * Construct a instance of DateTime with specified parameters.
    * The start time is set to unix epoch time.
    *
    * @param year year A.D, ranges in [-999,999,999, 999,999,999].
    * @param month month of year.
    * @param dayOfMonth days of month, ranges in [1, 31], the maximum depends on month and year.
    * @param hour hours of day, ranges in [0, 23].
    * @param minute minutes of hour, ranges in [0, 59].
    * @param second seconds of minute, ranges in [0, 59].
    * @param nanosecond nanoseconds of second, ranges in [0, 999,999,999].
    * @return DateTime with specified parameters.
    *
    * @throws IllegalArgumentException if the value of any parameter is out of the range.
    */
    @!APILevel[12, atomicservice : true]
    public static func ofUTC(
        year!: Int64,
        month!: Month,
        dayOfMonth!: Int64,
        hour!: Int64 = 0,
        minute!: Int64 = 0,
        second!: Int64 = 0,
        nanosecond!: Int64 = 0
    ): DateTime
    
    /**
    * Parse a DateTime from string with RFC3339 date-time format.
    *
    * @param str DateTime string.
    * @return a DateTime if the parsing succeeds.
    *
    * @throws TimeParseException if the parsing is incorrect.
    */
    @!APILevel[12, atomicservice : true]
    public static func parse(str: String): DateTime
    
    @!APILevel[12, atomicservice : true]
    public static func tryParse(str: String): Option<DateTime>
    
    /**
    * Parse a time from string with specified format.
    *
    * @param str DateTime string.
    * @param format of @p str, for example, "yyyy/MM/dd HH:mm:ss OOOO".
    * @return a DateTime if the parsing succeeds.
    *
    * @throws TimeParseException if the parsing is incorrect.
    * @throws IllegalArgumentException if the format is illegal.
    */
    @!APILevel[12, atomicservice : true]
    public static func parse(str: String, format: String): DateTime
    
    @Deprecated[message: "Use member funtion `public static func parse(str: String, format: String): DateTime` instead."]
    @!APILevel[12, atomicservice : true]
    public static func parse(str: String, format: DateTimeFormat): DateTime
    
    /**
    * Return DateTime with the time zone set to UTC.
    *
    * @return a DateTime with the time zone set to UTC.
    *
    * @throws ArithmeticException if the result of year in UTC is outside the range [-999,999,999, 999,999,999].
    */
    @!APILevel[12, atomicservice : true]
    public func inUTC(): DateTime
    
    /**
    * Return DateTime with the time zone set to Local.
    *
    * @return a DateTime with the time zone set to Local.
    *
    * @throws ArithmeticException if the result of year in Local is outside the range [-999,999,999, 999,999,999].
    */
    @!APILevel[12, atomicservice : true]
    public func inLocal(): DateTime
    
    /**
    * Return DateTime with the time zone set to specified time zone.
    *
    * @param timeZone time zone
    * @return a DateTime with the time zone set to Local.
    *
    * @throws ArithmeticException if the result of year in Local is outside the range [-999,999,999, 999,999,999]
    */
    @!APILevel[12, atomicservice : true]
    public func inTimeZone(timeZone: TimeZone): DateTime
    
    /**
    * Return the duration elapsed since January 1, 1970 UTC.
    * The result does not depend on the time zone associated with time,
    * in other words, base on the UTC time zone.
    *
    * @return duration of unix nanosecond.
    *
    * @throws ArithmeticException if the timestamp is out of range for 'Duration'.
    */
    @!APILevel[12, atomicservice : true]
    public func toUnixTimeStamp(): Duration
    
    /**
    * Return a String that represents the DateTime instance, the format defaults to RFC3339.
    *
    * @return a DateTime string formatted in RFC3339.
    */
    @!APILevel[12, atomicservice : true]
    public func toString(): String
    
    /**
    * Return a String that represents the DateTime instance, the format is set to specified format.
    *
    * @param fmt of func format, for example, "yyyy/MM/dd HH:mm:ss OOOO".
    *
    * @return a DateTime string formatted in the specified format.
    *
    * @throws IllegalArgumentException if the fmt is illegal.
    */
    @!APILevel[12, atomicservice : true]
    public func format(fmt: String): String
    
    @Deprecated[message: "Use member funtion `public func format(fmt: String): String` instead."]
    @!APILevel[12, atomicservice : true]
    public func toString(format: DateTimeFormat): String
    
    /**
    * Returns hash value of DateTime instance
    *
    * @return Int64 the hash value of the current DateTime instance.
    */
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    /**
    * Adds specified years to DateTime instance, returns a new DateTime to represent the new DateTime.
    * Specifically, if the day is outside the range of month,
    * it will shift to the last day of the month due to the impact of leap year.
    * For example, (2020-02-29).addYears(1) will return (2021-02-28)
    *
    * @param n the number of year.
    * @return a new DateTime instance after adding @p n years.
    *
    * @throws ArithmeticException if result of year is outside [-999,999,999, 999,999,999], or the result datetime does not exist.
    */
    @!APILevel[12, atomicservice : true]
    public func addYears(n: Int64): DateTime
    
    /**
    * Adds specified months to DateTime instance, returns a new DateTime to represent the new DateTime.
    * Specifically, if the day is outside the range of month,
    * it will shift to the last day of the month.
    * For example, (March 31st).addMonths(1) will return (April 30th).
    *
    * @param n the number of month.
    * @return a new DateTime instance after adding @p n months.
    *
    * @throws ArithmeticException if result of year is outside [-999,999,999, 999,999,999], or the result datetime does not exist.
    */
    @!APILevel[12, atomicservice : true]
    public func addMonths(n: Int64): DateTime
    
    /**
    * Adds specified weeks to DateTime instance, returns a new DateTime to represent the new DateTime.
    *
    * @param n the number of week.
    * @return a new DateTime instance after adding @p n weeks.
    *
    * @throws ArithmeticException if result of year is outside [-999,999,999, 999,999,999], or the result datetime does not exist.
    */
    @!APILevel[12, atomicservice : true]
    public func addWeeks(n: Int64): DateTime
    
    /**
    * Adds specified days to DateTime instance, returns a new DateTime to represent the new DateTime.
    *
    * @param n the number of day.
    * @return a new DateTime instance after adding @p n days.
    *
    * @throws ArithmeticException if result of year is outside [-999,999,999, 999,999,999], or the result datetime does not exist.
    */
    @!APILevel[12, atomicservice : true]
    public func addDays(n: Int64): DateTime
    
    /**
    * Adds specified hours to DateTime instance, returns a new DateTime to represent the new DateTime.
    *
    * @param n the number of hour.
    * @return a new DateTime instance after adding @p n hours.
    *
    * @throws ArithmeticException if result of year is outside [-999,999,999, 999,999,999], or the result datetime does not exist.
    */
    @!APILevel[12, atomicservice : true]
    public func addHours(n: Int64): DateTime
    
    /**
    * Adds specified minutes to DateTime instance, returns a new DateTime to represent the new DateTime.
    *
    * @param n the number of minute.
    * @return a new DateTime instance after adding @p n minutes.
    *
    * @throws ArithmeticException if result of year is outside [-999,999,999, 999,999,999], or the result datetime does not exist.
    */
    @!APILevel[12, atomicservice : true]
    public func addMinutes(n: Int64): DateTime
    
    /**
    * Adds specified seconds to DateTime instance, returns a new DateTime to represent the new DateTime.
    *
    * @param n the number of second.
    * @return a new DateTime instance after adding @p n seconds.
    *
    * @throws ArithmeticException if result of year is outside [-999,999,999, 999,999,999], or the result datetime does not exist.
    */
    @!APILevel[12, atomicservice : true]
    public func addSeconds(n: Int64): DateTime
    
    /**
    * Adds specified nanoseconds to DateTime instance, returns a new DateTime to represent the new DateTime.
    *
    * @param n the number of nanosecond.
    * @return a new DateTime instance after adding @p n nanoseconds.
    *
    * @throws ArithmeticException if result of year is outside [-999,999,999, 999,999,999]
    */
    @!APILevel[12, atomicservice : true]
    public func addNanoseconds(n: Int64): DateTime
    
    /**
    * Override the + operator, adds with a Duration instance and
    * returns a new DateTime instance.
    *
    * @param r a duration instance.
    * @return a new DateTime after adding @p r.
    *
    * @throws ArithmeticException if result of year is outside [-999,999,999, 999,999,999]
    */
    @!APILevel[12, atomicservice : true]
    public operator func +(r: Duration): DateTime
    
    /**
    * Override the - operator, subtracts with a Duration instance and
    * returns a new DateTime instance.
    *
    * @param r a duration instance.
    * @return a new DateTime after subtracting @p r.
    *
    * @throws ArithmeticException if result of year is outside [-999,999,999, 999,999,999]
    */
    @!APILevel[12, atomicservice : true]
    public operator func -(r: Duration): DateTime
    
    /**
    * Override the - operator, subtract another DateTime instance and
    * returns a Duration instance.
    * A return value of negative indicates that the time subtracted is later than this time,
    * otherwise is equal to or earlier than this time.
    *
    * @param r another time.
    * @return a duration as a result of this - @p r.
    */
    @!APILevel[12, atomicservice : true]
    public operator func -(r: DateTime): Duration
    
    /**
    * Override the == operator, determines whether this DateTime equals another DateTime.
    *
    * @param r another DateTime to be compared to.
    * @return true if the DateTime is equal to @p r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func ==(r: DateTime): Bool
    
    /**
    * Override the != operator, determines whether this time is not equal another DateTime.
    *
    * @param r another DateTime to be compared to.
    * @return true if the DateTime is not equal to @p r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func !=(r: DateTime): Bool
    
    /**
    * Override the >= operator, determines whether this time is later than or equal to another DateTime.
    *
    * @param r another DateTime to be compared to.
    * @return true if the DateTime is later than or equal to @p r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func >=(r: DateTime): Bool
    
    /**
    * Override the > operator, determines whether this time is later than another time.
    *
    * @param r another DateTime to be compared to.
    * @return true if the time is later than @p r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func >(r: DateTime): Bool
    
    /**
    * Override the <= operator, determines whether this time is earlier than or equal to another DateTime.
    *
    * @param r another DateTime to be compared to.
    * @return true if the DateTime is earlier than or equal to @p r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func <=(r: DateTime): Bool
    
    /**
    * Override the < operator, determines whether this time is earlier than another DateTime.
    *
    * @param r another DateTime to be compared to.
    * @return true if the DateTime is earlier than @p r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func <(r: DateTime): Bool
    
    /**
    * Compare the relationship between two instance of DateTime.
    *
    * @param rhs Instance of DateTime compared with this.
    * @return Value indicating the relationship between two instance of DateTime.
    */
    @!APILevel[12, atomicservice : true]
    public func compare(rhs: DateTime): Ordering
}

/**
* The TimeParseException class
*/
@!APILevel[12, atomicservice : true]
public class TimeParseException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

@!APILevel[12, atomicservice : true]
public class DateTimeFormat {
    @!APILevel[12, atomicservice : true]
    public static const RFC1123: String = "www, dd MMM yyyy HH:mm:ss z"
    
    @!APILevel[12, atomicservice : true]
    public static const RFC3339: String = "yyyy-MM-ddTHH:mm:ssOOOO"
    
    @!APILevel[12, atomicservice : true]
    public static const RFC822: String = "ww dd MMM yy HH:mm:ss z"
    
    @!APILevel[12, atomicservice : true]
    public static const RFC850: String = "wwww, dd-MMM-yy HH:mm:ss z"
    
    @Deprecated["Use member function `init(formatString: String)` instead."]
    @!APILevel[12, atomicservice : true]
    public static func of(format: String): DateTimeFormat
    
    @Deprecated["The prop is deprecated, no substitutions."]
    @!APILevel[12, atomicservice : true]
    public prop format: String
}

@!APILevel[12, atomicservice : true]
public enum DayOfWeek <: ToString & Equatable<DayOfWeek> {
    @!APILevel[12, atomicservice : true]
    Sunday |
    @!APILevel[12, atomicservice : true]
    Monday |
    @!APILevel[12, atomicservice : true]
    Tuesday |
    @!APILevel[12, atomicservice : true]
    Wednesday |
    @!APILevel[12, atomicservice : true]
    Thursday |
    @!APILevel[12, atomicservice : true]
    Friday |
    @!APILevel[12, atomicservice : true]
    Saturday
    /**
    * Generates a instance of DayOfWeek according to a Int64 value.
    *
    * @param wday a number represents a DayOfWeek.
    * @return a Result if success to generate a DayOfWeek based on wday, otherwise Failure.
    *
    * @throws IllegalArgumentException if dayOfWeek is out of range [0, 6]
    *
    * @since 0.18.4
    */
    @!APILevel[12, atomicservice : true]
    public static func of(dayOfWeek: Int64): DayOfWeek
    
    /**
    * Return Int64 value of the DayOfWeek instance.
    *
    * @return a number represents the DayOfWeek.
    *
    * @since 0.18.4
    */
    @Deprecated[message: "Use member function `public func toInteger(): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func value(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toInteger(): Int64
    
    /**
    * Return a String that represents the DayOfWeek, for example, Friday.
    *
    * @return a DayOfWeek name.
    *
    * @since 0.18.4
    */
    @!APILevel[12, atomicservice : true]
    public func toString(): String
    
    /**
    * Override the == operator, determines whether this DayOfWeek equals another.
    *
    * @param r another DayOfWeek.
    * @return true if the DayOfWeek is equal to @p r, otherwise false.
    *
    * @since 0.18.4
    */
    @!APILevel[12, atomicservice : true]
    public operator func ==(r: DayOfWeek): Bool
    
    /**
    * Override the != operator, determines whether this DayOfWeek is not equal to another.
    *
    * @param r another DayOfWeek.
    * @return true if the DayOfWeek is not equal to @p r, otherwise false.
    *
    * @since 0.18.4
    */
    @!APILevel[12, atomicservice : true]
    public operator func !=(r: DayOfWeek): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func +(n: Int64): DayOfWeek
    
    @!APILevel[12, atomicservice : true]
    public operator func -(n: Int64): DayOfWeek
}

@!APILevel[12, atomicservice : true]
public enum Month <: ToString & Equatable<Month> {
    @!APILevel[12, atomicservice : true]
    January |
    @!APILevel[12, atomicservice : true]
    February |
    @!APILevel[12, atomicservice : true]
    March |
    @!APILevel[12, atomicservice : true]
    April |
    @!APILevel[12, atomicservice : true]
    May |
    @!APILevel[12, atomicservice : true]
    June |
    @!APILevel[12, atomicservice : true]
    July |
    @!APILevel[12, atomicservice : true]
    August |
    @!APILevel[12, atomicservice : true]
    September |
    @!APILevel[12, atomicservice : true]
    October |
    @!APILevel[12, atomicservice : true]
    November |
    @!APILevel[12, atomicservice : true]
    December
    /**
    * Generates a instance of Month according to a Int64 value.
    *
    * @param mon a number represents a Month.
    * @return a Result if success to generate a Month based on mon, otherwise Failure.
    *
    * @throws IllegalArgumentException if mon is out of range [1, 12]
    *
    * @since 0.18.4
    */
    @!APILevel[12, atomicservice : true]
    public static func of(mon: Int64): Month
    
    /**
    * Return Int64 value of the Month instance.
    *
    * @return a nunber represents the Month.
    *
    * @since 0.18.4
    */
    @Deprecated[message: "Use member function `public func toInteger(): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func value(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toInteger(): Int64
    
    /**
    * Return a String that represents the Month, for example, June.
    *
    * @return a month name.
    *
    * @since 0.18.4
    */
    @!APILevel[12, atomicservice : true]
    public func toString(): String
    
    /**
    * Override the + operator.
    *
    * @param n delta.
    * @return a new Month after adding @p n to this Month.
    * @since 0.18.4
    */
    @!APILevel[12, atomicservice : true]
    public operator func +(n: Int64): Month
    
    /**
    * Override the - operator.
    *
    * @param n delta.
    * @return a new Month after subtracting @p n from this Month.
    * @since 0.18.4
    */
    @!APILevel[12, atomicservice : true]
    public operator func -(n: Int64): Month
    
    /**
    * Override the == operator, determines whether this Month equals another.
    *
    * @param r another Month.
    * @return true if the Month is equal to @p r, otherwise false
    *
    * @since 0.18.4
    */
    @!APILevel[12, atomicservice : true]
    public operator func ==(r: Month): Bool
    
    /**
    * Override the != operator, determines whether this Month is not equal to another.
    *
    * @param r another Month.
    * @return true if the Month is not equal to @p r, otherwise false.
    *
    * @since 0.18.4
    */
    @!APILevel[12, atomicservice : true]
    public operator func !=(r: Month): Bool
}

/**
* MonoTime is a kind of clock to measure the elapsed time, Like a constantly running stopwatch, independent of any other clock time.
*
* MonoTimes created by 'now' are always no less than any previously measured MonoTime created in the same way, and are
* commonly used in BenchMark or task queues on a first come first served basis.
*/
@!APILevel[12, atomicservice : true]
public struct MonoTime <: Hashable & Comparable<MonoTime> {
    /**
    * Obtain the elapsed time from the start of the system to current.
    *
    * @return a MonoTime corresponding to current.
    */
    @!APILevel[12, atomicservice : true]
    public static func now(): MonoTime
    
    /**
    * Override operator `+`, adds a Duration and returns a new MonoTime instance.
    *
    * @param r duration to add.
    * @return a MonoTime based on this MonoTime with the specified @r added.
    */
    @!APILevel[12, atomicservice : true]
    public operator func +(r: Duration): MonoTime
    
    /**
    * Override operator `-`, subtracts a Duration and returns a new MonoTime instance.
    *
    * @param r duration to subtract.
    * @return a MonoTime based on this MonoTime with the specified @r subtracted.
    */
    @!APILevel[12, atomicservice : true]
    public operator func -(r: Duration): MonoTime
    
    /**
    * Override operator `-`, subtracts a MonoTime and returns a Duration.
    *
    * @param r MonoTime to subtract.
    * @return a MonoTime based on this MonoTime with the specified @r subtracted.
    */
    @!APILevel[12, atomicservice : true]
    public operator func -(r: MonoTime): Duration
    
    /**
    * Override operator `==`, compares whether this MonoTime is equal to another MonoTime.
    *
    * @param r another MonoTime to be compared to.
    * @return true if the elapsed time of this MonoTime is equal to that of @r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func ==(r: MonoTime): Bool
    
    /**
    * Override operator `!=`, compares whether this MonoTime is not equal to another MonoTime.
    *
    * @param r another MonoTime to be compared to.
    * @return true if the elapsed time of this MonoTime is not equal to that of @r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func !=(r: MonoTime): Bool
    
    /**
    * Override operator `>=`, compares whether this MonoTime is greater than or equal to another MonoTime.
    *
    * @param r another MonoTime to be compared to.
    * @return true if the elapsed time of this MonoTime is greater than or equal to that of @r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func >=(r: MonoTime): Bool
    
    /**
    * Override operator `>`, compares whether this MonoTime is greater than another MonoTime.
    *
    * @param r another MonoTime to be compared to.
    * @return true if the elapsed time of this MonoTime is greater than that of @r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func >(r: MonoTime): Bool
    
    /**
    * Override operator `<=`, compares whether this MonoTime is less than or equal to another MonoTime.
    *
    * @param r another MonoTime to be compared to.
    * @return true if the elapsed time of this MonoTime is less than or equal to that of @r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func <=(r: MonoTime): Bool
    
    /**
    * Override operator `<`, compares whether this MonoTime is less than or equal to another MonoTime.
    *
    * @param r another MonoTime to be compared to.
    * @return true if the elapsed time of this MonoTime is less than that of @r, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func <(r: MonoTime): Bool
    
    /**
    * Compare the order between two instance of MonoTime.
    *
    * @param rhs instance of MonoTime compared with this.
    * @return a Ordering value indicating the relationship between two instance of MonoTime.
    */
    @!APILevel[12, atomicservice : true]
    public func compare(rhs: MonoTime): Ordering
    
    /**
    * Returns hash value of MonoTime instance
    *
    * @return the hash value of this MonoTime.
    */
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
}

/**
* The class TimeZone implements the basic functions of the time zone.
* The time zone data comes from the IANA time zone database file installed in the operating system.
* The local time zone is determined by the environment variable TZ.
* If the TZ does not exist, it is determined by the time zone stored in the /etc/localtime file.
* Otherwise, the host is regarded as the UTC time zone.
*
* @since 0.19.3
*/
@!APILevel[12, atomicservice : true]
public class TimeZone <: ToString & Equatable<TimeZone> {
    @!APILevel[12, atomicservice : true]
    public static let UTC = TimeZone("UTC", 0)
    
    @!APILevel[12, atomicservice : true]
    public static let Local = initLocal()
    
    @!APILevel[12, atomicservice : true]
    public prop id: String
    
    /**
    * Constructs a fixed TimeZone with given zone id and offset.
    *
    * @param id zone id.
    * @param offset zone offset.
    *
    * @throws IllegalArgumentException if TimeZone id is empty.
    * @since 0.19.3
    */
    @!APILevel[12, atomicservice : true]
    public init(id: String, offset: Duration)
    
    /**
    * Loads a TimeZone with the given id.
    * If the id is "UTC", it returns TimeZone.UTC. If the id is "Local", it returns TimeZone.Local.
    * Otherwise, the id is considered the TimeZone id corresponding to the file in the IANA
    * If the CJ_TZPATH environment variable exists, obtain the path from the environment variable first.
    * time zone database, such as "Asia/Shanghai".
    *
    * @param id zone id.
    * @return the TimeZone if success, otherwise an Exception.
    *
    * @throws IllegalArgumentException if TimeZone id is illegal or empty.
    * @throws InvalidDataException if the timezone data cannot be found when a valid environment variable is set
    * @throws InvalidDataException if the TimeZone file fails to be parsed.
    * @throws IllegalMemoryException if failed to call loadTimeZone(cjvm).
    *
    * @since 0.19.3
    */
    @!APILevel[12, atomicservice : true]
    public static func load(id: String): TimeZone
    
    /**
    * Obtains the TimeZone object based on the given root path and time zone id.
    * This function searches for the timezone data named <id> from the given root path in turn until it is found.
    
    * @param tzpaths Set of root paths for timezone datas.
    * @param id zone id.
    * @return the TimeZone if success, otherwise an Exception.
    *
    * @throws IllegalArgumentException if TimeZone id is empty.
    * @throws InvalidDataException if the timezone data cannot be found.
    * @throws InvalidDataException if the TimeZone file fails to be parsed.
    * @throws IllegalMemoryException if failed to call loadTimeZone(cjvm).
    *
    * @since 0.35.7
    */
    @!APILevel[12, atomicservice : true]
    public static func loadFromPaths(id: String, tzpaths: Array<String>): TimeZone
    
    /**
    * Loads a TimeZone with the given id initialized from the IANA Time LocalTime database format data.
    *
    * @param id zone id.
    * @param data IANA Time LocalTime database format data.
    * @return the TimeZone if success, otherwise an Exception.
    *
    * @throws IllegalArgumentException if id is empty.
    * @throws InvalidDataException if the TimeZone data fails to be parsed.
    * @throws IllegalMemoryException if failed to call runtimeNow(cjvm).
    * @since 0.19.3
    */
    @!APILevel[12, atomicservice : true]
    public static func loadFromTZData(id: String, data: Array<UInt8>): TimeZone
    
    /**
    * Return a String that represents the time zone, actually time zone id.
    *
    * @return time zone id.
    *
    * @since 0.19.3
    */
    @!APILevel[12, atomicservice : true]
    public func toString(): String
    
    /**
    * Override the == operator, determines whether this TimeZone equals another TimeZone.
    *
    * @param r another TimeZone.
    * @return true if the TimeZone is equal to @p r, otherwise false.
    *
    * @since 0.38.2
    */
    @!APILevel[12, atomicservice : true]
    public operator func ==(r: TimeZone): Bool
    
    /**
    * Override the == operator, determines whether this TimeZone is not equal another TimeZone.
    *
    * @param r another TimeZone.
    * @return true if the TimeZone is not equal to @p r, otherwise false.
    *
    * @since 0.38.2
    */
    @!APILevel[12, atomicservice : true]
    public operator func !=(r: TimeZone): Bool
}

@!APILevel[12, atomicservice : true]
public class InvalidDataException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

