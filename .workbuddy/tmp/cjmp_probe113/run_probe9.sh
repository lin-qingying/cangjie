#!/bin/bash
# 覆盖面补齐探针（十轮）：C* = 仅 common 编译；T* = 两段式（common → specific）
SDK="C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe"
BASE="D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113"
export CANGJIE_HOME="C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie"
export TMP="$BASE/tmp" TEMP="$BASE/tmp"
mkdir -p "$TMP"
for dir in "$BASE"/p9/*/; do
  name=$(basename "$dir")
  cd "$dir" || continue
  echo "=================== CASE $name ==================="
  rm -f *.cjo *.chir *.dll *.a 2>/dev/null
  echo ">>> common compile"
  "$SDK" common.cj --experimental --output-type=chir 2>&1 | head -60
  if [ -f specific.cj ]; then
    if [ -f cjmp_p.chir ] && [ -f cjmp_p.cjo ]; then
      echo ">>> specific compile"
      "$SDK" specific.cj cjmp_p.chir --experimental --common-part-cjo=cjmp_p.cjo -o out.dll --output-type=dylib 2>&1 | head -80
    else
      echo ">>> common compile failed, skip specific"
    fi
  fi
done
echo "=================== DONE ==================="
