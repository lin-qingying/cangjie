// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg

internal import pkg.*

public class Integer {
    public init(val: Int32) {}
}

main() {
    InteropTest<Int32>.foo<Integer>(Integer(-123))
    return 0
}
