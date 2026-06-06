// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

var aa = 0

class Data{
    public Data(var a!: Int64 = 6){
        aa++
    }
}

extend Data{
    public func f1(): <!parse_this_type_not_allow!>This<!>{
        this
    }
}

main():Int64{
    var data = Data(a: 8)
    if (data.f1().a == 8 && aa == 1) {
        return 0
    }
    return 1
}
