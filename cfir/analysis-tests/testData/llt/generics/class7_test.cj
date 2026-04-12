// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

struct Tensor<T> {
    public var no: LazyOp = LazyOp()
}

open class LazyOp{
    public func execute () { return this }
}

class OP<T> <: LazyOp {
    public func CallAsFunction(x : Tensor<T>) : Tensor<T>
    {
        return x
    }
}

func testop<T>(input : Tensor<T>) : Tensor<T> {
    var op = OP<T>();
    var res = op.CallAsFunction(input)
    return res
}

main() : Int64 {
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
