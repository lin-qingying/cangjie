// ERRCHECK: %compiler %diag_json %f -o %output 
main():Int64 {
    return 0
}

open class Boo{
    public var a: Int32 = 1
    public let b: Int32 = 2
}
class Aoo <: Boo{
    public var c = if(true){
        func foo1(x!:Int32=a){return x}
    }

    let d = if(true) {
        func foo(_:Int32){
            func bar(x!:Int32=a) {
                x
            }
            bar()
        }
    }
}
// ASSERT: scan-2 sema_illegal_usage_of_super_member

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
