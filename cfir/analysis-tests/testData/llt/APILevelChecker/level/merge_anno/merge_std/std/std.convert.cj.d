// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.





package std.convert


@!APILevel[since: "12", atomicservice : true]
public interface Formattable {
}

extend Int64 <: Formattable {
    /**
    * Formatting the Int64 Type.
    *
    * @param fmt formatting string.
    * @return formatted string.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected int format pattern
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend Int32 <: Formattable {
    /**
    * Formatting the Int32 Type.
    *
    * @param fmt formatting string.
    * @return formatted string.
    *
    * @throws IllegalArgumentException if fmt is not a valid format pattern.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected int format pattern
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend Int16 <: Formattable {
    /**
    * Formatting the Int16 Type.
    *
    * @param fmt formatting string.
    * @return formatted string.
    *
    * @throws IllegalArgumentException if fmt is not a valid format pattern.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected int format pattern
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend Int8 <: Formattable {
    /**
    * Formatting the Int8 Type.
    *
    * @param fmt formatting string.
    * @return formatted string.
    *
    * @throws IllegalArgumentException if fmt is not a valid format pattern.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected int format pattern
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend UInt64 <: Formattable {
    /**
    * Formatting the UInt64 Type
    *
    * @param fmt formatting string.
    * @return formatted string
    *
    * @throws IllegalArgumentException
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected unit format pattern
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend UInt32 <: Formattable {
    /**
    * Formatting the UInt32 Type
    *
    * @param fmt formatting string.
    * @return formatted string.
    *
    * @throws IllegalArgumentException if fmt is not a valid format pattern.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected unit format pattern
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend UInt16 <: Formattable {
    /**
    * Formatting the UInt16 Type.
    *
    * @param fmt formatting string.
    * @return formatted string.
    *
    * @throws IllegalArgumentException if fmt is not a valid format pattern.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected unit format pattern
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend UInt8 <: Formattable {
    /**
    * Formatting the UInt8 Type.
    *
    * @param fmt formatting string.
    * @return formatted string.
    *
    * @throws IllegalArgumentException if fmt is not a valid format pattern.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected unit format pattern
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend Float64 <: Formattable {
    /**
    * Formatting the Float64 Type.
    *
    * @param fmt formatting string.
    * @return formatted string.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected float format pattern
    */
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend Float32 <: Formattable {
    /**
    * Formatting the Float32 Type.
    *
    * @param fmt formatting string.
    * @return formatted string.
    *
    * @throws IllegalArgumentException if fmt is not a valid format pattern.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected float format pattern
    */
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend Float16 <: Formattable {
    /**
    * Formatting the Float16 Type
    *
    * @param fmt formatting string.
    * @return formatted string
    *
    * @throws IllegalArgumentException if fmt is not a valid format pattern.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected float format pattern
    */
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

extend Rune <: Formattable {
    /**
    * Formatting the Rune Type.
    *
    * @param fmt formatting string.
    * @return formatted string.
    *
    * @since 0.17.4
    *
    * @throws IllegalArgumentException if the `fmt` does not match the expected char format pattern
    */
    @!APILevel[since: "12", atomicservice : true]
    public func format(fmt: String): String
}

/**
* @Description Provide an unified abstraction to support specific types of parsing from strings.
*
* @since 0.17.4
*/
@!APILevel[since: "12", atomicservice : true]
public interface Parsable<T> {
}

@!APILevel[since: "12", atomicservice : true]
public interface RadixConvertible<T> {
}

/*
* @Description The Int8 is extended to provide the functions of parsing a string into Int8 or Option<Int8> type.
*
* @since 0.17.4
*/
extend Int8 <: Parsable<Int8> {
    /**
    * @Description Convert a string of Int8 literals to an Int8 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of Int8.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string is empty, or if the string is starting with "+",
    * or if failing to convert to Int8, or if the value after conversion is out of the Int8 range,
    * or if there is an invalid utf8 code in the string.
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): Int8
    
    /**
    * @Description Convert a string of Int8 literals to an Option<Int8> value. If the failure occurs, Option<Int8>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<Int8>.
    * @throws IllegalArgumentException if there is an invalid utf8 code in the string
    *
    * @since 0.31.2
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<Int8>
}

extend Int8 <: RadixConvertible<Int8> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(value: String, radix!: Int): Int8
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(value: String, radix!: Int): Option<Int8>
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func toString(radix!: Int): String
}

/*
* @Description The UInt8 is extended to provide the functions of parsing a string into UInt8 type or Option<UInt8> type.
*
* @since 0.17.4
*/
extend UInt8 <: Parsable<UInt8> {
    /**
    * @Description Convert a string of UInt8 literals to a UInt8 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of UInt8.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string is empty, or if the string is starting with "+" or "-",
    * or if failing to convert to UInt8, or if the value after conversion is out of the UInt8 range,
    * or if there is an invalid utf8 code in the string.
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): UInt8
    
    /**
    * @Description Convert a string of UInt8 literals to an Option<UInt8> value. If the failure occurs, Option<UInt8>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<UInt8>.
    * @throws IllegalArgumentException if there is an invalid utf8 code in the string
    *
    * @since 0.31.2
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<UInt8>
}

extend UInt8 <: RadixConvertible<UInt8> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(value: String, radix!: Int): UInt8
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(value: String, radix!: Int): Option<UInt8>
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func toString(radix!: Int): String
}

/*
* @Description The Int16 is extended to provide the functions of parsing a character string into Int16 type or Option<Int16> type.
*
* @since 0.17.4
*/
extend Int16 <: Parsable<Int16> {
    /**
    * @Description Convert a string of Int16 literals to an Int16 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of Int16.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string is empty, or if the string is starting with "+",
    * or if failing to convert to Int16, or if the value after conversion is out of the Int16 range,
    * @or if there is an invalid utf8 code in the string.
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): Int16
    
    /**
    * @Description Convert a string of Int16 literals to an Option<Int16> value. If the failure occurs, Option<Int16>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<Int16>.
    * @throws IllegalArgumentException if there is an invalid utf8 code in the string
    *
    * @since 0.31.2
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<Int16>
}

extend Int16 <: RadixConvertible<Int16> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(value: String, radix!: Int): Int16
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(value: String, radix!: Int): Option<Int16>
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func toString(radix!: Int): String
}

/*
* @Description The UInt16 is extended to provide the functions of parsing character strings to UInt16 type or Option<UInt16> type.
*
* @since 0.17.4
*/
extend UInt16 <: Parsable<UInt16> {
    /**
    * @Description Convert a string of UInt16 literals to a UInt16 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of UInt16.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string is empty, or if the string is starting with "+" or "-",
    * or if failing to convert to UInt16, or if the value after conversion is out of the UInt16 range,
    * or if there is an invalid utf8 code in the string.
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): UInt16
    
    /**
    * @Description Convert a string of UInt16 literals to an Option<UInt16> value. If the failure occurs, Option<UInt16>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<UInt16>.
    * @throws IllegalArgumentException if there is an invalid utf8 code in the string
    *
    * @since 0.31.2
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<UInt16>
}

extend UInt16 <: RadixConvertible<UInt16> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(value: String, radix!: Int): UInt16
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(value: String, radix!: Int): Option<UInt16>
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func toString(radix!: Int): String
}

/*
* @Description The Int32 is extended to provide the function of parsing a character string into Int32 type or Option<Int32> type.
*
* @since 0.17.4
*/
extend Int32 <: Parsable<Int32> {
    /**
    * @Description Convert a string of Int32 literals to an Int32 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of Int32.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string is empty, or if the string is starting with "+",
    * or if failing to convert to Int32, or if the value after conversion is out of the Int32 range,
    * or if there is an invalid utf8 code in the string.
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): Int32
    
    /**
    * @Description Convert a string of Int32 literals to an Option<Int32> value. If the failure occurs, Option<Int32>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<Int32>.
    * @throws IllegalArgumentException if there is an invalid utf8 code in the string
    *
    * @since 0.31.2
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<Int32>
}

extend Int32 <: RadixConvertible<Int32> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(value: String, radix!: Int): Int32
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(value: String, radix!: Int): Option<Int32>
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func toString(radix!: Int): String
}

/*
* @Description The UInt32 is extended to provide the functions of parsing a character string into UInt32 type or Option<UInt32> type.
*
* @since 0.17.4.
*/
extend UInt32 <: Parsable<UInt32> {
    /**
    * @Description Convert a string of UInt32 literals to a UInt32 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of UInt32.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string is empty, or if the string is starting with "+" or "-",
    * or if failing to convert to UInt32, or if the value after conversion is out of the UInt32 range,
    * or if there is an invalid utf8 code in the string.
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): UInt32
    
    /**
    * @Description Convert a string of UInt32 literals to an Option<UInt32> value. If the failure occurs, Option<UInt32>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<UInt32>.
    * @throws IllegalArgumentException if there is an invalid utf8 code in the string
    *
    * @since 0.31.2
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<UInt32>
}

extend UInt32 <: RadixConvertible<UInt32> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(value: String, radix!: Int): UInt32
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(value: String, radix!: Int): Option<UInt32>
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func toString(radix!: Int): String
}

/*
* @Description The Int64 is extended to provide the functions of parsing a character string to Int64 type or Option<Int64> type.
*
* @since 0.17.4
*/
extend Int64 <: Parsable<Int64> {
    /**
    * @Description Convert a string of Int64 literals to an Int64 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of Int64.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string is empty, or if the string is starting with "+",
    * or if failing to convert to Int64, or if the value after conversion is out of the Int64 range,
    * or if there is an invalid utf8 code in the string.
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): Int64
    
    /**
    * @Description Convert a string of Int64 literals to an Option<Int64> value. If the failure occurs, Option<Int64>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<Int64>.
    * @throws IllegalArgumentException if there is an invalid utf8 code in the string
    *
    * @since 0.31.2
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<Int64>
}

extend Int64 <: RadixConvertible<Int64> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(value: String, radix!: Int): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(value: String, radix!: Int): Option<Int64>
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func toString(radix!: Int): String
}

/*
* @Description The UInt64 is extended to provide the functions of parsing character strings to UInt64 type or Option<UInt64> type.
*
* @since 0.17.4.
*/
extend UInt64 <: Parsable<UInt64> {
    /**
    * @Description Convert a string of UInt64 literals to a UInt64 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of UInt64.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string is empty, or if the string is starting with "+" or "-",
    * or if failing to convert to UInt64, or if the value after conversion is out of the UInt64 range,
    * or if there is an invalid utf8 code in the string.
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): UInt64
    
    /**
    * @Description Convert a string of UInt64 literals to an Option<UInt64> value. If the failure occurs, Option<UInt64>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<UInt64>.
    * @throws IllegalArgumentException if there is an invalid utf8 code in the string
    *
    * @since 0.31.2
    */
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<UInt64>
}

extend UInt64 <: RadixConvertible<UInt64> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(value: String, radix!: Int): UInt64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(value: String, radix!: Int): Option<UInt64>
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func toString(radix!: Int): String
}

extend Float16 <: FloatParsable<Float16> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(n: Float64): Float16
}

extend Float32 <: FloatParsable<Float32> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(n: Float64): Float32
}

extend Float64 <: FloatParsable<Float64> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(n: Float64): Float64
}

/*
* @Description The Float16 is extended to provide the functions of parsing a character string into Float16 type or Option<Float16> type.
*
* @since 0.17.4
*/
extend Float16 <: Parsable<Float16> {
    /**
    * @Description Convert a string of Float16 literals to a Float16 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of Float16.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string does not comply with the floating point number syntax.
    */
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): Float16
    
    /**
    * @Description Convert a string of Float16 literals to an Option<Float16> value. If the failure occurs, Option<Float16>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<Float16>.
    *
    * @since 0.31.2
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<Float16>
}

/*
* @Description The Float32 is extended to provide the functions of parsing a character string to Float32 type or Option<Float32> type.
*
* @since 0.17.4
*/
extend Float32 <: Parsable<Float32> {
    /**
    * @Description Convert a string of Float32 literals to a Float32 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of Float32.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string does not comply with the floating point number syntax.
    */
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): Float32
    
    /**
    * @Description Convert a string of Float32 literals to an Option<Float32> value. If the failure occurs, Option<Float32>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<Float32>.
    *
    * @since 0.31.2
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<Float32>
}

/*
* @Description The Float64 is extended to provide the functions of parsing a character string to Float64 type or Option<Float64> type.
*
* @since 0.17.4
*/
extend Float64 <: Parsable<Float64> {
    /**
    * @Description Convert a string of Float64 literals to a Float64 value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of Float64.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string does not comply with the floating point number syntax.
    */
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): Float64
    
    /**
    * @Description Convert a string of Float64 literals to an Option<Float64> value. If the failure occurs, Option<Float64>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<Float64>.
    * @throws IllegalArgumentException if there is an invalid utf8 code in the string
    *
    * @since 0.31.2
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<Float64>
}

/*
* @Description The Rune is extended to provide the functions of parsing a character string into Rune type or Option<Rune> type.
*
* @since 0.17.4
*/
extend Rune <: Parsable<Rune> {
    /**
    * @Description Convert a string of Rune literals to a Rune value. If the failure occurs, Exception will be throwed.
    *
    * @param data of String.
    * @return Parameters of Rune.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string is empty, or if failing to convert to Rune,
    * or if there is an invalid utf8 code in the string.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): Rune
    
    /**
    * @Description Convert a string of Rune literals to an Option<Rune> value. If the failure occurs, Option<Rune>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<Rune>.
    * @throws IllegalArgumentException if there is an invalid utf8 code in the string
    *
    * @since 0.31.2
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<Rune>
}

/*
* @Description The Bool is extended to provide the functions of parsing a character string into Bool type or Option<Bool> type.
*
* @since 0.17.4
*/
extend Bool <: Parsable<Bool> {
    /**
    * @Description Convert a string of Boolean literals to a Boolean value. If the failure occurs, IllegalArgumentException will be throwed.
    *
    * @param data of String.
    * @return Parameters of Bool.
    *
    * @since 0.31.2
    *
    * @throws IllegalArgumentException if the string is empty, or if failing to convert to Bool,
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func parse(data: String): Bool
    
    /**
    * @Description Convert a string of Boolean literals to an Option<Bool> value. If the failure occurs, Option<Rune>.None will be returned.
    *
    * @param data of String.
    * @return Parameters of Option<Bool>.
    *
    * @since 0.31.2
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func tryParse(data: String): Option<Bool>
}

