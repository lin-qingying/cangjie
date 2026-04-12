// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A<T> <: B<T> {}

interface B<T> {
    operator func +(b: B<T>) {
    }
}

main() {
    let a: B<Int32> = A<Int32>();
    let b: B<Int32> = A<Int32>();
    let c = a + b
    0
}
