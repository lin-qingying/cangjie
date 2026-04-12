// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface Add<T> {
    operator func +(right: T): Int64
}

extend Int32 <: Add<Int32> {}

// ASSERT: regex-auto return type of '+' is not identical or not a subtype of the overridden/redefined/implement function
