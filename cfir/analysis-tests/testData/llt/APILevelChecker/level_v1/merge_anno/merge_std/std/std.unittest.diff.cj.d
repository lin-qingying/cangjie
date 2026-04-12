// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package std.unittest.diff
import std.collection.*
import std.convert.*
import std.math.*
import std.unittest.common.*


@!APILevel[12, atomicservice : true]
public interface AssertPrintable<T> {
}

extend<T> HashSet<T> <: AssertPrintableCollection<HashSet<T>> where T <: Equatable<T> {
    @!APILevel[12, atomicservice : true]
    public func pprint(right: HashSet<T>, pp: PrettyPrinter, leftPrefix: String, rightPrefix: String, _: Int64): PrettyPrinter
}

@!APILevel[12, atomicservice : true]
extend<K, V> HashMap<K, V> <: AssertPrintableCollection<HashMap<K, V>> where K <: Equatable<K> & Hashable, V <: Equatable<V> {
    @!APILevel[12, atomicservice : true]
    public func pprint(right: HashMap<K, V>, pp: PrettyPrinter, leftPrefix: String, rightPrefix: String, level: Int64): PrettyPrinter
}

extend<K, V> TreeMap<K, V> <: AssertPrintableCollection<TreeMap<K, V>> where K <: Equatable<K> & Hashable, V <: Equatable<V> {
    @!APILevel[12, atomicservice : true]
    public func pprint(right: TreeMap<K, V>, pp: PrettyPrinter, leftPrefix: String, rightPrefix: String, level: Int64): PrettyPrinter
}

@!APILevel[12, atomicservice : true]
extend Float16 <: AssertPrintable<Float16> {
    @!APILevel[12, atomicservice : true]
    public prop hasNestedDiff: Bool
    
    @!APILevel[12, atomicservice : true]
    public func pprintForAssertion(pp: PrettyPrinter, right: Float16, leftPrefix: String, rightPrefix: String, level: Int64): PrettyPrinter
}

@!APILevel[12, atomicservice : true]
extend Float32 <: AssertPrintable<Float32> {
    @!APILevel[12, atomicservice : true]
    public prop hasNestedDiff: Bool
    
    @!APILevel[12, atomicservice : true]
    public func pprintForAssertion(pp: PrettyPrinter, right: Float32, leftPrefix: String, rightPrefix: String, level: Int64): PrettyPrinter
}

@!APILevel[12, atomicservice : true]
extend Float64 <: AssertPrintable<Float64> {
    @!APILevel[12, atomicservice : true]
    public prop hasNestedDiff: Bool
    
    @!APILevel[12, atomicservice : true]
    public func pprintForAssertion(pp: PrettyPrinter, right: Float64, leftPrefix: String, rightPrefix: String, level: Int64): PrettyPrinter
}

@!APILevel[12, atomicservice : true]
extend<T> Option<T> <: AssertPrintable<Option<T>> where T <: Equatable<T> {
    @!APILevel[12, atomicservice : true]
    public prop hasNestedDiff: Bool
    
    @!APILevel[12, atomicservice : true]
    public func pprintForAssertion(pp: PrettyPrinter, right: Option<T>, leftPrefix: String, rightPrefix: String, level: Int64): PrettyPrinter
}

@!APILevel[12, atomicservice : true]
extend String <: AssertPrintable<String> {
    @!APILevel[12, atomicservice : true]
    public prop hasNestedDiff: Bool
    
    @!APILevel[12, atomicservice : true]
    public func pprintForAssertion(pp: PrettyPrinter, right: String, leftPrefix: String, rightPrefix: String, level: Int64): PrettyPrinter
}

