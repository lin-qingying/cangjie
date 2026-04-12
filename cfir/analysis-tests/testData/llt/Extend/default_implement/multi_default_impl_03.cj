// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-PIPE-0: %run %output

interface I1 {}

interface I2 <: I1 {}

interface I3 {}

interface I4 <: I3 {}

class A {}

extend A <: I1 & I3 {}

extend A <: I2 & I4 {}

main() {
    0
}
