// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func f(x: Float32): Float32 {
    x + 1.0
}

func g(x: Float32): Float32 {
    x * 2.0
}

func run() {
    let x: Float32 = 1.0
    let fgfg = f ~> g ~> f ~> g
    let res = fgfg(x)
    print("res: ${res}")
    return res
}

main(): Int64 {
    let res = run()
    if (res == 10.0) {
        return 0
    }
    return 1
}
