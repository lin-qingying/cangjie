// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f
// ASSERT: regex-not core dump

var input:Int32 = 1
var f = match ( input ){
    case x => var a: Int32 = 1;var x1 = {a}
}

main():Int64 {
    if ( f != () ){
        return 1
    }
    return 0
}
