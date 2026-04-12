// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs0018001a

public interface Add<T> {
    func add(a: T): T
}

extend Int32 <: Add<Int32> {
    public func add(a: Int32): Int32 {
        return a
    }
}

extend Int64 <: Add<Int64> {
    public func add(a: Int64): Int64 {
        return a
    }
}

extend Float32 <: Add<Float32> {
    public func add(a: Float32): Float32 {
        return a
    }
}

extend Float64 <: Add<Float64> {
    public func add(a: Float64): Float64 {
        return a
    }
}

public open class C1<T> where T <: Add<T> {
    public var a: T
    public init(a: T) {
        this.a = a
    }
    public func test(): T {
        return this.a
    }
    public func test1(t: T): T {
        return t
    }
}

public class C2<T> where T <: Add<T> {
    public var a: T
    public init(x: Int32, b: T) {
        a = b
    }
    public init(b: T) {
        a = b
    }
}

