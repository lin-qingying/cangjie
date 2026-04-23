// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let a: VArray<Int64, $3> = [1, 2, <!TYPE_MISMATCH!>"3"<!>] //error
    let a1: VArray<Int64, $1> = <!VARRAY_SIZE_MISMATCH!>[1, 2, 3]<!> //error
    0
}
