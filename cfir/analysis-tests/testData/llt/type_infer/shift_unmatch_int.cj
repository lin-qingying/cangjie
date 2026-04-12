// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%compiler %cmp_opt %f -o %output
// EXEC:%run %run_opt %output %run_args

main(): Int64 {
    var a1: Int8 = 1
    var b1: UInt8 = 1
    a1 << b1
    a1 >> b1

    var a2: Int16 = 1
    var b2: UInt8 = 1
    a2 << b2
    a2 >> b2

    var a3: UInt8 = 1
    var b3: Int16 = 1
    a3 << b3
    a3 >> b3

    var a4: UInt64 = 1
    var b4: UInt32 = 1
    a4 << b4
    a4 >> b4
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
