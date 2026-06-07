// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import ops.*

main(): Int64 {
    let input = Tensor<Int32>(0)
    let input2: Int32 = 1
    let value = MaximumOp<Int32>(input, input2)
    return 0
}
