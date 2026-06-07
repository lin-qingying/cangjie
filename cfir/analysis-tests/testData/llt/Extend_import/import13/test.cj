// FILE: pkg0.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg0

public interface CellUnary {
    operator func ()(input0: Float64): Float64
}

// FILE: pkg1.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg1

public import pkg0.*

public struct Conv2d {}

extend Conv2d <: CellUnary {
    public operator func ()(input: Float64): Float64 {
        input
    }
}

// FILE: test.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg1.*

main(): Int64 {
    let conv = Conv2d()
    let output = conv(1.0)
    return 0
}
