// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I {
    operator func [](index: Int64) {
        return this
    }
}

open class empty <: I {} // empty obj define. To replace big obj for gc free.

struct A0 <: I {}

func create_obj(a: Int64): I {
    match (a) {
        case 0 => A0()
        case _ => empty()
    }
}

main() {
    let ins = create_obj(0)[0];
    if (ins is A0) {
        return 0
    }
    return 1
}
