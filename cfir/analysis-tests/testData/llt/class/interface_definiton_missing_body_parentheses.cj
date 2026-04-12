// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// exec: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I1 { }
interface I2


main(): Int64 {        // expected: regex expected '{' in interface'}'
    return 0
}

// assert: regex-not core dumped
// assert: regex-not stack trace
