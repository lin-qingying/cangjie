// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-0: %compiler %n.cj -o %output
// EXEC-0: %output 2>&1 | compare %f
// ASSERT: scan error: the input is SomeTensor.

public class LazyTensorHandle {
    public var lazyTensorHandleId: Int32
    public init(lazyTensorHandleId: Int32) {
        this.lazyTensorHandleId = lazyTensorHandleId
    }
}

public struct Tensor {
    let handle_: Option<LazyTensorHandle>
    public init() {
        this.handle_ = None<LazyTensorHandle>
    }
    public init(handle_: LazyTensorHandle) {
        this.handle_ = handle_
    }
}

public func SwitchOp(input0: Tensor, input1: Tensor, input2: Tensor) {
    match (input0.handle_) {
        case Some(v) => print("error: the input is SomeTensor.\n")
        case None => print("error: the input is NoneTensor.\n")
    }
}

public interface TensorOperator {
    operator func >=(right: Tensor): Tensor
}

public interface FloatTensorOperator <: TensorOperator {
    operator func /(right: Tensor): Tensor
}

extend Int32 <: FloatTensorOperator {
    public operator func >=(right: Tensor): Tensor {
        return right
    }
    public operator func /(right: Tensor): Tensor {
        return right
    }
}

main() {
    let x = Tensor(LazyTensorHandle(1))
    SwitchOp(Int32(10) >= x, x, x)
    return 0
}
