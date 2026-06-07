// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    func f(a : Int64) {
        func g(h : (Int64) -> Int64) {
            h(0) - a
        }
        g
    }
    var t = <!parse_trailing_closure_only_follow_name!>(if (true) {f(1)} else {f(2)})<!>{ t => t + 1 }
    return 0
}
