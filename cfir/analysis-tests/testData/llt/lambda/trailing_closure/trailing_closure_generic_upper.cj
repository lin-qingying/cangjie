// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func test<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(a: T) where T <: (Int64) -> Int64 {
    a(1)
}

main(): Int64 {
    return test {i => i}
}
