// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package std.regex

import std.collection.*
import std.sync.*

@!APILevel[since: "12", atomicservice : true]
public struct Position {
}

@!APILevel[since: "12", atomicservice : true]
public struct MatchData {
    /**
    * Retrive the whole matched string.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func matchString(): String
    
    /**
    * Retrive the subsequence of capture group by index.
    * 
    * @throws IndexOutOfBoundsException if capture group not enabled, or group is less than zero or larger than groupCount()
    */
    @!APILevel[since: "12", atomicservice : true]
    public func matchString(group: Int64): String
    
    /**
    * Retrive the subsequence of capture group by name.
    * 
    * @throws IllegalArgumentException if capture group not enabled or group name not found
    */
    @!APILevel[since: "12", atomicservice : true]
    public func matchString(group: String): String
    
    /**
    * Retrive the position of whole matched string.
    * 
    * @throws IndexOutOfBoundsException if the length of `position` is less than 1
    */
    @!APILevel[since: "12", atomicservice : true]
    public func matchPosition(): Position
    
    /**
    * Retrive the position of the capture group subsequence by index.
    * 
    * @throws IllegalArgumentException if capture group not enabled, or group is less than zero or larger than groupCount
    */
    @!APILevel[since: "12", atomicservice : true]
    public func matchPosition(group: Int64): Position
    
    /**
    * Retrive the position of the capture group subsequence by index.
    * 
    * @throws IllegalArgumentException if capture group not enabled or group name not found
    */
    @!APILevel[since: "12", atomicservice : true]
    public func matchPosition(group: String): Position
    
    @Deprecated[message: "Use member function `public func groupCount(): Int64` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func groupNumber(): Int64
    
    /**
    * Get the count of capture groups.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func groupCount(): Int64
}

/**
* An engine that performs match operations on a CharSequence
* by interpreting a pattern
*/
@Deprecated[message: "APIs in Matcher have been moved to Regex."]
@!APILevel[since: "12", atomicservice : true]
public class Matcher {
    @!APILevel[since: "12", atomicservice : true]
    public init(regex: Regex, input: String)
    
    /**
    * Attempts to match the entire region against the pattern.
    * @return MatchData's Option where the entire region matches this matcher's pattern.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func fullMatch(): Option<MatchData>
    
    /**
    * Attempts to match the region against the pattern from beginning.
    * @return MatchData's Option where the entire region matches this matcher's pattern.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func matchStart(): Option<MatchData>
    
    /**
    * Attempts to find the next subsequence of the input sequence that matches
    * the pattern.
    *
    * This method starts at the beginning of this matcher's region, or, if
    * a previous invocation of the method was successful and the matcher has
    * not since been reset, at the first character not matched by the previous
    * match.
    *
    * @return MatchData's Option where the entire region  matches this matcher's pattern.
    *
    */
    @!APILevel[since: "12", atomicservice : true]
    public func find(): Option<MatchData>
    
    /**
    * Resets this matcher and then attempts to find the next subsequence of
    * the input sequence that matches the pattern, starting at the specified.
    * @param index The the input sequence that matches from index.
    * @return MatchData's Option where the entire region  matches this matcher's pattern.
    *
    * @throws IndexOutOfBoundsException if index is less than 0 or index is greater than or equal to input.size.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func find(index: Int64): Option<MatchData>
    
    /**
    * Reset the region and find all match data in range of this matcher's region.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func findAll(): Option<Array<MatchData>>
    
    /**
    * Get number of records that match the regular expression.
    * @return number of records.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func allCount(): Int64
    
    /**
    * Replaces the first subsequence of the input sequence that matches the
    * pattern with the given replacement string.
    *
    * This method will not resets this matcher,it then scans the input
    * sequence looking for a match of the pattern,at the first character not matched by the previous
    * match.
    * @param replacement The character sequence to be replaced.
    * @return The character that has been replaced.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func replace(replacement: String): String
    
    /**
    * Resets this matcher and then attempts to find the next subsequence of
    * the input sequence that matches from index.
    *
    * This method will resets this matcher,Characters that are not
    * part of the match are appended directly to the result string; the match
    * is replaced in the result by the replacement string.
    * 
    * @param replacement The character sequence to be replaced.
    * @param index The the input sequence that matches from index.
    * @return The character that has been replaced.
    *
    * @throws IndexOutOfBoundsException if index is less than 0 or index is greater than or equal to input.size.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func replace(replacement: String, index: Int64): String
    
    /**
    * @param replacement The character sequence to be replaced.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func replaceAll(replacement: String): String
    
    /**
    * if `limit` is greater than zero then it will replace the string at most `limit` times
    * if `limit` is non-positive, it will replace the string as many times as possible
    * if `limit` is zero, return the string
    *
    * @param replacement The character sequence to be replaced.
    * @param limit The limit of replace.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func replaceAll(replacement: String, limit: Int64): String
    
    /**
    * Split the input sequence by all subsequence that matches the pattern as the delimiters.
    *
    * @return The array of strings computed by splitting the input
    * around matches of this pattern
    */
    @!APILevel[since: "12", atomicservice : true]
    public func split(): Array<String>
    
    /**
    * Split the input sequence by all subsequence that matches the pattern as the delimiters
    *
    * if limit n is greater than zero then it will separate the string at most n times
    * if limit n is non-positive, it will separate the string as many times as possible
    *
    * @param input The the input sequence.
    * @param limit The limit of split
    * @return The array of strings computed by splitting the input
    * around matches of this pattern
    */
    @!APILevel[since: "12", atomicservice : true]
    public func split(limit: Int64): Array<String>
    
    /**
    * Sets the region of string that will be searched to find a match.
    * @param start The index to start searching at (inclusive).
    * @param end The index to end searching at (exclusive).
    * @return  this matcher.
    *
    * @throws IndexOutOfBoundsException if beginIndex less than 0 or beginIndex greater than input.size
    * @throws IndexOutOfBoundsException if endIndex less than 0 or endIndex greater than input.size
    * @throws IndexOutOfBoundsException if beginIndex greater than endIndex
    */
    @!APILevel[since: "12", atomicservice : true]
    public func setRegion(beginIndex: Int64, endIndex: Int64): Matcher
    
    /**
    * Reports the start index and end index of the matcher's region.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func region(): Position
    
    /**
    * Sets the limits of this matcher's region. The region is the part of the
    * input sequence that will be searched to find a match. Invoking this
    * method resets the matcher.
    * @return this matcher.
    *
    * @throws RegexException if set region failed.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func resetRegion(): Matcher
    
    /**
    * Resets the Matcher's with a new string.
    *
    * @param input The new input character sequence.
    *
    * @return This matcher.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func resetString(input: String): Matcher
    
    /**
    * get this Matcher's input.
    *
    * @return This matcher's input.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func getString(): String
}

@!APILevel[since: "12", atomicservice : true]

@!APILevel[since: "12", atomicservice : true]

@!APILevel[since: "12", atomicservice : true]

/**
* Regular Expression.
* Description: Used to retrieve and replace text that conforms to a certain pattern.
*/
@!APILevel[since: "12", atomicservice : true]
public class Regex {
    @!APILevel[since: "12", atomicservice : true]
    public init(pattern: String, flags: Array<RegexFlag>)
    
    @Deprecated[message: "Use member funtion `public Regex(let pattern: String, flags: Array<RegexFlag>)` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public init(pattern: String, option: RegexOption)
    
    @Deprecated[message: "`Matcher` has been marked as deprecated."]
    @!APILevel[since: "12", atomicservice : true]
    public func matcher(input: String): Matcher
    
    /**
    * Attempts to check whether input matches the pattern.
    * @param input The character sequence to be matched.
    * @return Bool indicates the check result
    */
    @!APILevel[since: "12", atomicservice : true]
    public func matches(input: String): Bool
    
    /**
    * lazy getter of namedGroups, which only depending on the compiled pattern.
    * So this method can be invoked as soon as `Regex` is initialized. 
    * return Map<String, Int64> indicates the mapping from group name to group index
    */
    @!APILevel[since: "12", atomicservice : true]
    public func getNamedGroups(): Map<String, Int64>
    
    /**
    * Attempts to find the first match of the input sequence from the beginning.
    * @param input The the input sequence.
    * @param group Whether enable capture group or not. Disabled by default. 
    * @return Option<MatchData> indicates the match.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func find(input: String, group!: Bool = false): Option<MatchData>
    
    /**
    * Find all matches of the input sequence from the beginning.
    * @param input The the input sequence.
    * @param group Whether enable capture group or not. Disabled by default. 
    * @return Array<MatchData> indicates the matches.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func findAll(input: String, group!: Bool = false): Array<MatchData>
    
    /**
    * Get an iterator for finding all matches of the input sequence from the beginning.
    * @param input The the input sequence.
    * @param group Whether enable capture group or not. Disabled by default. 
    * @return Iterator<MatchData> indicates the iterator.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func lazyFindAll(input: String, group!: Bool = false): Iterator<MatchData>
    
    /**
    * Replaces the first subsequence of the input sequence that matches the
    * pattern with the given replacement string from the beginning.
    *
    * @param input The the input sequence.
    * @param replacement The character sequence to be replaced.
    * @return The character that has been replaced.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func replace(input: String, replacement: String): String
    
    /**
    * Replaces the first subsequence of the input sequence that matches the
    * pattern with the given replacement string from index.
    * 
    * @param input The the input sequence.
    * @param replacement The character sequence to be replaced.
    * @param index The the input sequence that matches from index.
    * @return The character that has been replaced.
    *
    * @throws IndexOutOfBoundsException if index is less than 0 or index is greater than or equal to input.size.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func replace(input: String, replacement: String, index: Int64): String
    
    /**
    * Replaces all subsequence of the input sequence that matches the
    * pattern with the given replacement string from the beginning.
    *
    * @param input The the input sequence.
    * @param replacement The character sequence to be replaced.
    * @return The character that has been replaced.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func replaceAll(input: String, replacement: String): String
    
    /**
    * Replaces all subsequence of the input sequence that matches the
    * pattern with the given replacement string from the beginning.
    *
    * if `limit` is greater than zero then it will replace the string at most `limit` times
    * if `limit` is non-positive, it will replace the string as many times as possible
    * if `limit` is zero, return the string
    *
    * @param input The the input sequence.
    * @param replacement The character sequence to be replaced.
    * @param limit The replacement limit. 
    * @return The character that has been replaced.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func replaceAll(input: String, replacement: String, limit: Int64): String
    
    /**
    * Split the input sequence by all subsequence that matches the pattern as the delimiters.
    *
    * @param input The the input sequence.
    * @return The array of strings computed by splitting the input
    * around matches of this pattern
    */
    @!APILevel[since: "12", atomicservice : true]
    public func split(input: String): Array<String>
    
    /**
    * Split the input sequence by all subsequence that matches the pattern as the delimiters
    *
    * if limit n is greater than zero then it will separate the string at most n times
    * if limit n is non-positive, it will separate the string as many times as possible
    *
    * @param input The the input sequence.
    * @param limit The limit of split
    * @return The array of strings computed by splitting the input
    * around matches of this pattern
    */
    @!APILevel[since: "12", atomicservice : true]
    public func split(input: String, limit: Int64): Array<String>
    
    @!APILevel[since: "12", atomicservice : true]
    public func string(): String
}

@!APILevel[since: "12", atomicservice : true]
public class RegexException <: Exception {
    @!APILevel[since: "12", atomicservice : true]
    public init()
    
    @!APILevel[since: "12", atomicservice : true]
    public init(message: String)
}

@!APILevel[since: "12", atomicservice : true]
public enum RegexFlag {
    @!APILevel[since: "12", atomicservice : true]
    IgnoreCase |
    @!APILevel[since: "12", atomicservice : true]
    MultiLine |
    @!APILevel[since: "12", atomicservice : true]
    Unicode
}

@Deprecated[message: "Use `public enum RegexFlag` instead."]
@!APILevel[since: "12", atomicservice : true]
public class RegexOption <: ToString {
    @!APILevel[since: "12", atomicservice : true]
    public init()
    
    @!APILevel[since: "12", atomicservice : true]
    public func ignoreCase(): RegexOption
    
    @!APILevel[since: "12", atomicservice : true]
    public func multiLine(): RegexOption
    
    @!APILevel[since: "12", atomicservice : true]
    public func toString(): String
}

