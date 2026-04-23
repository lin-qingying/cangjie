// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func test(): Int64 {
    let x: Int64 = 1;
    let r: Int64 = match (x){
        case 0 => 1
        case 1 => 0
        case _ => 2
    }
    return r
}

main(): Int64 {
    return test()
}
