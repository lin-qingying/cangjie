// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../src

internal import pkga.*

func run(): Int64 {
    if (B<Int32>() != 2) {
        return 1
    }
    return 0
}

main(): Int64 {
    return run()
}
