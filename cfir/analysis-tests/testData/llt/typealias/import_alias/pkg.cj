// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public let ga = 1

type TT = Int64

var FLOAT32_SIZE: Int64 = 4

public interface Number<T> {
    static func size(): Int64
    static func InitArrayTensorByValue(value: Array<T>, shapeSize: Int64): Int64
}

extend Float32 <: Number<Float32> {
    public static func size(): Int64 {
        return FLOAT32_SIZE
    }
    public static func InitArrayTensorByValue(value: Array<Float32>, shapeSize: Int64): Int64 {
        return shapeSize
    }
}

func InitArrayTensorByValue<T>(value: Array<T>, shape: Array<Int64>): TT where T <: Number<T> {
    let handle = T.InitArrayTensorByValue(value, shape.size)
    return handle
}

public class ConcreteTensorG<T> where T <: Number<T> {
    private var tensorHandle: Int64 // = 0

    public init(value: Array<T>) {
        this.tensorHandle = InitArrayTensorByValue<T>(value, [1, 2])
    }
}

public func test() {
    1
}
