#!/bin/bash
SDK="C:/Users/lin17/.cangjie/sdks/cangjie-sdk-windows-x64-1.1.3/cangjie/bin/cjc.exe"
BASE="D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113"
export TMP="$BASE/tmp" TEMP="$BASE/tmp"
mkdir -p "$TMP" "$BASE/p4"
cd "$BASE/p4" || exit 1

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

printf '%s\npublic common func Pc(x!: Int64): Int64 {\n    x\n}\n' "$BC" > common.cj
printf '%s\npublic specific func Pc(x!: Int64 = 1): Int64 {\n    x\n}\n' "$SP" > specific.cj
run_pair matched_spec_default

printf '%s\npublic common func Pc(x!: Int64 = 1): Int64 {\n    x\n}\n' "$BC" > common.cj
printf '%s\npublic specific func Pc(x!: Int64): Int64 {\n    x\n}\n' "$SP" > specific.cj
run_pair matched_readthrough

printf '%s\npublic common func Pc(x!: Int64 = 1): Int64 {\n    x\n}\n' "$BC" > common.cj
printf '%s\npublic specific func Pc(x!: Int64 = 2): Int64 {\n    x\n}\n' "$SP" > specific.cj
run_pair matched_both_defaults

printf '%s\npublic common class CM {\n    public CM() {}\n    public common var mv = 1\n}\n' "$BC" > common.cj
printf '%s' "$SP" > specific.cj
run_pair common_class_member_var_implicit

printf '%s\npublic common var gv: Int64\n' "$BC" > common.cj
printf '%s\npublic specific var gv = 5\n' "$SP" > specific.cj
run_pair matched_var_spec_implicit

printf '%s\npublic common class WithCtor {\n    public WithCtor() {}\n}\n' "$BC" > common.cj
printf '%s' "$SP" > specific.cj
run_pair common_class_with_ctor_ok

printf '%s\npublic common class CF {\n    public CF() {}\n    public common func m()\n}\n' "$BC" > common.cj
printf '%s' "$SP" > specific.cj
run_pair common_class_member_no_body

echo "=================== DONE ==================="
