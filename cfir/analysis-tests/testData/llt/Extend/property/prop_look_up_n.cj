// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

struct A<T> {}

extend<T> A<T> {
    public static prop p: Int64 {
        get() {
            return 0
        }
    }
}

main(): Int64 {
    return A.p
}

// ASSERT: regex generic type should be used with type argument
