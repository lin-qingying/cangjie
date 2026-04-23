// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// Should reporting type mismatch, not just invalid subscript expr
func test(a: Array<Int8>): Unit {}

main() {
    var arr: Array<Array<UInt64>> = [[255u64, 0u64, 0u64, 0u64]]
    var a = test(<!TYPE_MISMATCH!>arr[0]<!>)
}
