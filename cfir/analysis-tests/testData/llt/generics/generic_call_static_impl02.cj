// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {
    mut static prop a: Int64
}

interface I2 <: I1 {}

func f<T>(): Unit where T <: I2 {
    T.a = 1
}

main() {
    f<<!sema_cannot_instantiated_by_incomplete_type!>I<!>2>()
}
