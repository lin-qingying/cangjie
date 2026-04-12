// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix | compare %f
// ASSERT: regex-auto matched

enum E<T> {
    | e(E<T>)
    | e
}

func test<T>(i_: T) {
    var a: E<T> = e(e<T>)
    var i = e<T>
    var res = match (a) {
        case e(v) =>
            print("matched");
            i
        case e =>
            print("fail");
            i
    }
    match (res) {
        case e => 1
        case e(_) => 0
    }
}

main(): Int64 {
    test(1)
}
