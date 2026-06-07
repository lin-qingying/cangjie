// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func test(a: Any) {}

main(): Int64 {
    test <!TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION!>{=> 1}<!>
    return 0
}
