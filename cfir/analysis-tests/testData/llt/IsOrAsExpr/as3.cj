// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class E {
    public func fn() {
        println("hello")
    }
}

class F {}

func test(t: Object): Any {
    if (t is E) {
        return t as E
    } else {
        return t as F
    }
}

main() {
    let x = test(E()) as Option<E>

    match (x) {
        case Some(Some(e)) => e.fn()
        case _ => ()
    }
}
