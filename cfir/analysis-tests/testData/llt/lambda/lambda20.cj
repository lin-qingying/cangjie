// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func test(): Int64 {
    let assign = {a: Int64 => <!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = 0}
    return 0
}

main() {
    return test()
}
