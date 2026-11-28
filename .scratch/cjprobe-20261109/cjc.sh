#!/bin/bash
# 用法: cjc.sh <version: 1.0.0|1.0.5|1.1.3> <cjc args...>
v=$1; shift
case $v in
  1.0.0) H=/c/Users/lin17/.cangjie/sdks/cangjie-1.0.0;;
  1.0.5) H=/c/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.0.5/cangjie;;
  1.1.3) H=/c/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie;;
esac
T=$(cygpath -w /d/code/intellij/cangjie/.scratch/cjprobe-20261109/tmp)
export CANGJIE_HOME=$(cygpath -w $H) TMP=$T TEMP=$T
export PATH="$H/bin:$H/runtime/lib/windows_x86_64_llvm:$H/lib/windows_x86_64_llvm:$H/tools/bin:$H/third_party/llvm/bin:$PATH"
"$H/bin/cjc.exe" "$@"
