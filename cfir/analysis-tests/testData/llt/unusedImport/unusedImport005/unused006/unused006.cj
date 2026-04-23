// DEPENDENCE: ./other.cj

import a

@a.Derive[Equatable]
class A{
    var val = 1
    init(temp:Int64){
        val = temp
    }
}

main() {
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
