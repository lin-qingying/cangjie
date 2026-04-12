// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
// EXEC: %run %n.%suffix

type F<T, U> = (T) -> U

type U<X, Y> = F<X, Y>

func foo(): U<Int32, Bool> {
    return {i: Int32 => true}
}

main() {
    return 0
}
