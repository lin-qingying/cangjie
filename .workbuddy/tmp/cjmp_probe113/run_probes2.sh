#!/bin/bash
SDK="C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe"
BASE="D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113"
export TMP="$BASE/tmp" TEMP="$BASE/tmp"
mkdir -p "$TMP" "$BASE/p2"
cd "$BASE/p2" || exit 1

run_case() {
  local name="$1"
  echo "=================== CASE $name ==================="
  echo "--- common.cj ---"; cat common.cj
  echo "--- specific.cj ---"; cat specific.cj
  rm -f *.cjo *.chir *.dll *.a 2>/dev/null
  echo ">>> common compile"
  "$SDK" common.cj --experimental --output-type=chir 2>&1 | head -40
  if [ -f cjmp_p.chir ] && [ -f cjmp_p.cjo ]; then
    echo ">>> specific compile"
    "$SDK" specific.cj cjmp_p.chir --experimental --common-part-cjo cjmp_p.cjo -o out_spec.dll --output-type=dylib 2>&1 | head -40
    echo ">>> dll: $(ls *.dll 2>/dev/null)"
  else
    echo ">>> common compile failed, skip specific"
  fi
}

H='package cjmp_p

'
BASE_COMMON="${H}public common func Platform(): String {
    \"Common\"
}
"
BASE_SPECIFIC="${H}public specific func Platform(): String {
    \"Spec\"
}
"

printf '%s' "$BASE_COMMON" > common.cj; printf '%s' "$BASE_SPECIFIC" > specific.cj
run_case base

printf '%s\npublic specific func Pf(x: Int64 = 1): Int64 {\n    x\n}\n' "$BASE_SPECIFIC" > specific.cj
run_case spec_param_default

printf '%s\npublic common func Pc(x: Int64 = 1): Int64 {\n    x\n}\n' "$BASE_COMMON" > common.cj
printf '%s' "$BASE_SPECIFIC" > specific.cj
run_case common_param_default

printf '%s\npublic common var x\n' "$BASE_COMMON" > common.cj
run_case common_var_no_type_no_init

printf '%s\npublic common var z: Int64\n' "$BASE_COMMON" > common.cj
run_case common_var_type_only

printf '%s\npublic common let y = 1\n' "$BASE_COMMON" > common.cj
run_case common_let_init_only

printf '%s\npublic common interface I {\n    func m()\n}\n' "$BASE_COMMON" > common.cj
run_case common_iface_member_no_body

printf '%s\npublic specific interface I2 {\n    func m(): Unit\n}\n' "$BASE_SPECIFIC" > specific.cj
printf '%s' "$BASE_COMMON" > common.cj
run_case spec_iface_member_no_body

printf '%s\npublic common class NoCtor {}\n' "$BASE_COMMON" > common.cj
run_case common_class_no_ctor

printf '%s\npublic common struct NoCtorS {}\n' "$BASE_COMMON" > common.cj
run_case common_struct_no_ctor

printf '%s\npublic common let (a, b) = (1, 2)\n' "$BASE_COMMON" > common.cj
run_case common_pattern_decl

printf '%s\npublic class Plain {\n    common static init() {}\n}\n' "$BASE_COMMON" > common.cj
run_case common_static_init_in_plain

printf '%s\npublic class Plain2 {\n    common func f(): Unit {}\n}\n' "$BASE_COMMON" > common.cj
run_case common_member_in_plain

printf '%s\npublic abstract class AbsC {\n    public abstract func f(): Unit\n}\n' "$BASE_COMMON" > common.cj
run_case explicitly_abstract_non_cjmp

printf '%s\npublic common func NoRet()\n' "$BASE_COMMON" > common.cj
run_case common_func_no_body_no_rettype

echo "=================== DONE ==================="
