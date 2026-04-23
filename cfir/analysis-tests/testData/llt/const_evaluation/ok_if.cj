// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


const func f(x: Int64) {
    if (x < 0) {
        "negative"
    } else if (x == 0) {
        "zero"
    } else {
        return "positive"
    }
}

const x = f(0)

main() {
    println(x)
}
