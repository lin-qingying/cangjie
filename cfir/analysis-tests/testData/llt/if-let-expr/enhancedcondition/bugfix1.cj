// (CJNATIVE) EXEC: %compiler %f %optimization_level_2 -o %output
// (CJNATIVE) RUN-EXEC-PIPE: %run %run_args %run_opt %output | compare %f

enum E {
    Foo(Int32, Int32)
}
main() {
    var i: Int32 = 0
    while (let Foo(0, x) <- Foo(i, i)) {
        println("see x=${x}")
        i++
    }
    println("done")
}

/* SCAN
see x=0
done
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
