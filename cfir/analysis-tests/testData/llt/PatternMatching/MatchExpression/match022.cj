// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

// _EXEC: %compi I<T,K>{
  func a(a:A<T,K>){a}
  func b(){1}
}

extend<T,K> A<T,K> <: I<T,K>{
  public func a(a:A<T,K>){a}
  public func b(){1}
}

struct B{}

extend B{
  public func a(a:A<Int64,Float64>){
    var b : Int64 = match(a){
      case C(x) => x
      case _ => 2
    }
    var d : Float64 = match(A<Int64,Float64>.C(3).a(B(5.02))){
      case B(x) => x
      case _ => 3.002
    }

    return b + Int64(d)
  }
}

main(): Int64  ler %cmp_opt %f -o %output
// _EXEC: %run %run_opt %output %run_args

type A<T,K> = enumA<T,K>

enum enumA<T,K>{
  C(T) | B(K)
}

interface I<T,K>{
  func a(a:A<T,K>){a}
  func b(){1}
}

extend<T,K> A<T,K> <: I<T,K>{
  public func a(a:A<T,K>){a}
  public func b(){1}
}

struct B{}

extend B{
  public func a(a:A<Int64,Float64>){
    var b : Int64 = match(a){
      case C(x) => x
      case _ => 2
    }
    var d : Float64 = match(A<Int64,Float64>.C(3).a(B(5.02))){
      case B(x) => x
      case _ => 3.002
    }

    return b + Int64(d)
  }
}

main(): Int64  {
  if(B().a(B(5.02)) == 7 && B().a(A<Int64,Float64>.C(3).a(C(3))) == 8){
    return 0
  }else{
    return 1
  }
}
