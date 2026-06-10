// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var myArray1: Array<Int32>
    <!ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED!>[<!>]
    var myArray2: Array<Array<Int32>> = [[]]
    return 0
}
