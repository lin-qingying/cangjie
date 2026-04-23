// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func foo(): Int64 {
    let v = spawn {
        var c = 11
        {
            =>
            c = 0
            c
        }()
    }
    return v.get()
}

main(): Int64 {
    return foo()
}
