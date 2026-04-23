// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func test(a: (Int64) -> Int64): (Int64) -> Int64 {
    return a
}

main() {
    let x = test <!TYPE_MISMATCH!>{i: Int8 => test {i => i}}<!>
    return 0
}
