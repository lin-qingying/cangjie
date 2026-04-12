// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

main() {
    let f = C().test
    let f2 = C.test2
    let f3 = test3
}

class C {
    public func test() {
        ()
    }
    public static func test2() {
        ()
    }
}

func test3() {
    ()
}
