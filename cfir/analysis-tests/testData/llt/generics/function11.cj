// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix

func g<A, B>(a: A): B {
    let b = g<A, B>(a)
    return b
}

main(): Int64 {
    return g<Bool, Int64>(false);
}
