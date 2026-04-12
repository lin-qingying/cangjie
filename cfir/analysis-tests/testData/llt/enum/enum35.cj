// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {}

class B {}

func Do1(a: Option<A>) {
    return match (a) {
        case Some(y) => 1
        case None => 0
    }
}

func Do2(a: Option<B>) {
    return match (a) {
        case Some(y) => 1
        case None => 0
    }
}

main(): Int64 {
    let a: Option<String> = Option.None
    if (Do1(None) != 0) {
        return 1
    }
    if (Do2(Option.None) != 0) {
        return 1
    }
    return 0
}
