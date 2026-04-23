// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func test(a: (Int64) -> Int64, b: Int64): (Int64) -> Int64 {
    return a
}

main() {
    let x = <!NO_VALUE_FOR_PARAMETER!>test {i: Int8 => {i => i}}<!>
    return 0
}
