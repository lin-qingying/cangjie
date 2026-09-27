#!/bin/bash
SDK="C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe"
BASE="D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113"
export TMP="$BASE/tmp" TEMP="$BASE/tmp"
mkdir -p "$TMP" "$BASE/p"
cd "$BASE/p" || exit 1

run_case() {
  local name="$1"
  echo "=================== CASE $name ==================="
  echo "--- common.cj ---"; cat common.cj
  echo "--- specific.cj ---"; cat specific.cj
  rm -f *.cjo *.chir *.dll *.a 2>/dev/null
  echo ">>> compile common (chir mode, --experimental)"
  "$SDK" common.cj --experimental --output-type=chir -o common_part.chir 2>&1 | head -50
  echo ">>> files after common: $(ls | tr '\n' ' ')"
  local CJO=$(ls *.cjo 2>/dev/null | head -1)
  echo ">>> cjo=$CJO"
  if [ -n "$CJO" ]; then
    echo ">>> compile specific (--common-part-cjo)"
    "$SDK" specific.cj --experimental --common-part-cjo "$CJO" -o out_spec.dll --output-type=dylib 2>&1 | head -50
    echo ">>> files after specific: $(ls | tr '\n' ' ')"
  fi
}

BASE_COMMON='package cjmp_p

public common func Platform(): String {
    "Common"
}
'
BASE_SPECIFIC='package cjmp_p

public specific func Platform(): String {
    "Spec"
}
'

printf '%s' "$BASE_COMMON" > common.cj; printf '%s' "$BASE_SPECIFIC" > specific.cj
run_case base

printf '%s\npublic specific func Pf(x: Int64 = 1): Int64 {\n    x\n}\n' "$BASE_SPECIFIC" > specific.cj
run_case spec_param_default

printf '%s\npublic common let greeting = "hello"\n' "$BASE_COMMON" > common.cj
printf '%s' "$BASE_SPECIFIC" > specific.cj
run_case common_let_no_type

printf '%s\npublic common var counter: Int64\n' "$BASE_COMMON" > common.cj
run_case common_var_no_init

printf '%s\n@Frozen\npublic common func G<T>(x: T): T {\n    x\n}\n' "$BASE_COMMON" > common.cj
run_case common_generic_frozen

printf '%s\npublic common func G2<T>(x: T): T {\n    x\n}\n' "$BASE_COMMON" > common.cj
run_case common_generic_plain

printf '%s\npublic common func NoRet() {}\n' "$BASE_COMMON" > common.cj
run_case common_func_no_rettype_withbody

printf '%s\npublic specific func SInCommon(): String {\n    "s"\n}\n' "$BASE_COMMON" > common.cj
run_case specific_in_common_file

printf '%s\npublic common func CInSpecific(): String {\n    "c"\n}\n' "$BASE_SPECIFIC" > specific.cj
run_case common_in_specific_file

echo "=================== DONE ==================="
