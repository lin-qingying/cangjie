// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

public interface I1<V> {
    func containsValue(value: V, fn: (V, V) -> Bool): Bool {
        false
    }
}

public interface I2<V> {
    func containsValue(value: V, fn: (V, V) -> Bool): Bool {
        false
    }
    func containsValue(value: V): Bool {
        false
    }
}

extend Int64 <: I1<Int64> {}

extend Int64 <: I2<Int64> {}

main() {
    0
}

/* SCAN
error: interface function 'containsValue' must be implemented in 'extend Int64'
  ==> main_int64.cj:24:1:
   | 
24 | extend Int64 <: I1<Int64> {}
   | ^ 
   | 

error: interface function 'containsValue' must be implemented in 'extend Int64'
  ==> main_int64.cj:26:1:
   | 
26 | extend Int64 <: I2<Int64> {}
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
