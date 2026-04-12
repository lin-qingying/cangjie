// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.



package std.unicode
import std.collection.*


@!APILevel[since: "12", atomicservice : true]
public enum CasingOption {
    @!APILevel[since: "12", atomicservice : true]
    // Turkish
    TR |
    @!APILevel[since: "12", atomicservice : true]
    // Azeri
    AZ |
    @!APILevel[since: "12", atomicservice : true]
    // Lithuanian
    LT |
    @!APILevel[since: "12", atomicservice : true]
    Other
}

/* Methods for  Unicode. */
@!APILevel[since: "12", atomicservice : true]
public interface UnicodeRuneExtension {
}

extend Rune <: UnicodeRuneExtension {
    /**
    * Returns true if this Unicode character is Letter.
    * In Unicode, Letter includes Uppercase_Letter, Lowercase_Letter, Titlecase_Letter, Modifier_Letter and Other_Letter.
    * Definition is placed in https://www.unicode.org/reports/tr44/#GC_Values_Table
    */
    @!APILevel[since: "12", atomicservice : true]
    public func isLetter(): Bool
    
    /**
    * Returns true if this Unicode character is Number.
    * In Unicode, Number includes Decimal_Number, Letter_Number and Other_Number.
    * Definition is placed in https://www.unicode.org/reports/tr44/#GC_Values_Table
    */
    @!APILevel[since: "12", atomicservice : true]
    public func isNumber(): Bool
    
    /** Returns true if this Unicode character is Lowercase. */
    @!APILevel[since: "12", atomicservice : true]
    public func isLowerCase(): Bool
    
    /** Returns true if this Unicode character is Uppercase. */
    @!APILevel[since: "12", atomicservice : true]
    public func isUpperCase(): Bool
    
    /** Returns true if this Unicode character is Titlecase. */
    @!APILevel[since: "12", atomicservice : true]
    public func isTitleCase(): Bool
    
    /** Returns true if this Unicode character is whitespace. */
    @!APILevel[since: "12", atomicservice : true]
    public func isWhiteSpace(): Bool
    
    /** Returns the uppercase of this Unicode character. */
    @!APILevel[since: "12", atomicservice : true]
    public func toUpperCase(): Rune
    
    /** Returns the lowercase of this Unicode character. */
    @!APILevel[since: "12", atomicservice : true]
    public func toLowerCase(): Rune
    
    /** Returns the titlecase of this Unicode character. */
    @!APILevel[since: "12", atomicservice : true]
    public func toTitleCase(): Rune
    
    /** Returns the uppercase of this Unicode character. */
    @!APILevel[since: "12", atomicservice : true]
    public func toUpperCase(opt: CasingOption): Rune
    
    /** Returns the lowercase of this Unicode character. */
    @!APILevel[since: "12", atomicservice : true]
    public func toLowerCase(opt: CasingOption): Rune
    
    /** Returns the titlecase of this Unicode character. */
    @!APILevel[since: "12", atomicservice : true]
    public func toTitleCase(opt: CasingOption): Rune
}

@!APILevel[since: "12", atomicservice : true]
public interface UnicodeStringExtension {
}

extend String <: UnicodeStringExtension {
    /**
    * Returns true if this string is empty or contains only whitespace Unicode, otherwise `false`.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func isBlank(): Bool
    
    /**
    * @return a string which is the result of converting every character in this
    *         string to its lower case.
    * @throws IllegalArgumentException if there is an invalid utf8 leading code
    *         in array `itemBytes`.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func toLower(): String
    
    /**
    * @return a string which is the result of converting every character in
    *         this string to its upper case.
    * @throws IllegalArgumentException if there is an invalid utf8 leading code
    *         in array `itemBytes`.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func toUpper(): String
    
    /**
    * @return a string which is the result of converting every character in
    *         this string to its title case.
    * @throws IllegalArgumentException if there is an invalid utf8 leading code
    *         in array `itemBytes`.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func toTitle(): String
    
    /**
    * @return a string which is the result of converting every character in this
    *         string to its lower case.
    * @throws IllegalArgumentException if there is an invalid utf8 leading code
    *         in array `itemBytes`.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func toLower(opt: CasingOption): String
    
    /**
    * @return a string which is the result of converting every character in
    *         this string to its upper case.
    * @throws IllegalArgumentException if there is an invalid utf8 leading code
    *         in array `itemBytes`.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func toUpper(opt: CasingOption): String
    
    /**
    * @return a string which is the result of converting every character in
    *         this string to its title case.
    * @throws IllegalArgumentException if there is an invalid utf8 leading code
    *         in array `itemBytes`.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func toTitle(opt: CasingOption): String
    
    /**
    * @return a string which is the result of removing the leading and trailing
    *         whitespace of this string.
    * @throws IllegalArgumentException if there is no valid utf8 code in array `myData`.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func trim(): String
    
    /**
    * @return a string which is the result of removing the leading whitespace of this string.
    * @throws IllegalArgumentException if there is no valid utf8 leading code in array `myData`.
    */
    @Deprecated[message: "Use member function ` public func trimStart(): String` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func trimLeft(): String
    
    /**
    * @return a string which is the result of removing the trailing whitespace of this string.
    * @throws IllegalArgumentException if there is no valid utf8 code in array `myData`.
    */
    @Deprecated[message: "Use member function ` public func trimEnd(): String` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func trimRight(): String
    
    /**
    * @return a string which is the result of removing the leading whitespace of this string.
    * @throws IllegalArgumentException if there is no valid utf8 leading code in array `myData`.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func trimStart(): String
    
    /**
    * @return a string which is the result of removing the trailing whitespace of this string.
    * @throws IllegalArgumentException if there is no valid utf8 code in array `myData`.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func trimEnd(): String
}

