#!/bin/bash
SDK="C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe"
BASE="D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113"
export TMP="$BASE/tmp" TEMP="$BASE/tmp"
mkdir -p "$TMP" "$BASE/p3"
cd "$BASE/p3" || exit 1

run_pair() {
  local name="$1"
  echo "=================== CASE $name ==================="
  echo "--- common.cj ---"; cat common.cj
  echo "--- specific.cj ---"; cat specific.cj
  rm -f *.cjo *.chir *.dll *.a 2>/dev/null
  echo ">>> common compile"
  "$SDK" common.cj --experimental --output-type=chir 2>&1 | head -30
  if [ -f cjmp_p.chir ] && [ -f cjmp_p.cjo ]; then
    echo ">>> specific compile"
    "$SDK" specific.cj cjmp_p.chir --experimental --common-part-cjo=cjmp_p.cjo -o out.dll --output-type=dylib 2>&1 | head -30
    echo ">>> dll: $(ls *.dll 2>/dev/null)"
  else
    echo ">>> common compile failed, skip specific"
  fi
}

H='package cjmp_p

'
BC="${H}public common func Platform(): String {
    \"Common\"
}
"
SP="${H}public specific func Platform(): String {
    \"Spec\"
}
"

printf '%s' "$BC" > common.cj; printf '%s' "$SP" > specific.cj
run_pair base

printf '%s\npublic specific func Pf(x!: Int64 = 1): Int64 {\n    x\n}\n' "$SP" > specific.cj
run_pair spec_param_default_named

printf '%s\npublic common func Pc(x!: Int64 = 1): Int64 {\n    x\n}\n' "$BC" > common.cj
printf '%s' "$SP" > specific.cj
run_pair common_param_default_named

printf '%s\npublic specific interface I2 {\n    func m(): Unit\n}\n' "$SP" > specific.cj
run_pair spec_iface_member_no_body

printf '%s\npublic common class XC {}\n' "$SP" > specific.cj
run_pair common_decl_in_specific_mode

printf '%s\npublic specific class YS {}\n' "$BC" > common.cj
run_pair specific_decl_in_common_mode

printf '%s\npublic specific var sv = 1\n' "$SP" > specific.cj
printf '%s' "$BC" > common.cj
run_pair spec_var_init_only

printf '%s\npublic specific var sv2: Int64\n' "$SP" > specific.cj
run_pair spec_var_type_only

printf '%s\npublic specific let sl = 2\n' "$SP" > specific.cj
run_pair spec_let_init_only

printf '%s\npublic common abstract class CA {\n    public abstract func f(): Unit\n}\n' "$BC" > common.cj
printf '%s' "$SP" > specific.cj
run_pair common_abstract_member_ok

printf '%s\npublic specific class Solo {}\n' "$SP" > specific.cj
run_pair spec_class_no_ctor

echo "=================== DONE ==================="
