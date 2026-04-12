// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs

// Interface
public interface A {
    func default_foo(): Int32 {
        1
    }
}

public interface B<X> <: A {
    func default_foo(): Int32 {
        2
    }
}

public class C <: B<Int32> {}

// Class
public class G<T> {
    public let count: T
    public init(x: T) {
        count = x
    }
}

public let obj: G<Float64> = G<Float64>(1.00)

// Record
public struct Data<T> {
    public let width: T
    public let length: T
    public init(width1: T, length1: T) {
        width = width1
        length = length1
    }
    public func area(): T {
        return width
    }
}

public let data: Data<Int32> = Data<Int32>(3, 4)
