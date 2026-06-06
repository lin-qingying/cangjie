// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct Data<T1, T2, T3> {
    public func area(width: T1, length: T2, height: T3): T3{
        return  height
}
<!parse_expected_decl!>return<!> 1;}

<!parse_expected_decl!>return<!> 1;<!parse_expected_character!>func<!> run(): Int64 {
    var a: Int64 = 1
    var b: Float64 = 2.2
    var c: Int32 = 2
    var data: Data<Int64, Float64, Int32> = Data<Int64, Float64, Int32>()
    var res: Int32 = data.area(a, b, c)
    if (res == 2) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}
