// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f(x: Array<Int8>) {
    x.size
}

func g(x: Int64) {
    x + 1
}

main() {
    let gf = f ~> g
    println(gf(1, 2, 3))
}
