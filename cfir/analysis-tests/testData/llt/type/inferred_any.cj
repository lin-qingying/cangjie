// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {}

func test() {
    let a = if (true) {
        0
    } else <!chir_dce_unreachable_block_in_expression!>{
        A()
    }<!>
    return a as Int64 ?? -1
}

main() {
    return test()
}
