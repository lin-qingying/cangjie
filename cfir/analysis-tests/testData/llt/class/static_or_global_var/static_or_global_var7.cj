// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class A {
    static init() {
        C.fun()
    }
}

func getNum() {
    8
}

class C {
    static let NUM = getNum()
    public static func fun(): Int64 {
        println(NUM)
        NUM
    }
}

main(): Int64 {
    0
}

// ASSERT: scan global/static variable 'NUM' is used before initialization during initializing 'A.init'