#!/bin/bash
export PATH="/usr/bin:/bin:$PATH"
cd /d/code/intellij/cangjie || exit 1
QJ="java -jar gradle-queue-cli/build/libs/gradle-queue-cli.jar --project-dir D:/code/intellij/cangjie"

ok=0
for i in $(seq 1 25); do
  $QJ :cfir:providers:compileKotlin :cfir:checkers:compileKotlin --console=plain > /tmp/wait_cmp.log 2>&1
  if grep -q "BUILD SUCCESSFUL" /tmp/wait_cmp.log; then
    echo "[$(date +%H:%M:%S)] COMPILE OK (attempt $i)"
    ok=1
    break
  fi
  echo "[$(date +%H:%M:%S)] attempt $i: $(grep -m1 '^e: ' /tmp/wait_cmp.log | cut -c1-140)"
  sleep 40
done

if [ "$ok" = "1" ]; then
  echo "[$(date +%H:%M:%S)] running verification slice"
  $QJ :cfir:analysis-tests:test \
    --tests '*CfirAnalysisLLTTestGenerated$Class*' \
    --tests '*CfirAnalysisLLTPsiTestGenerated$Class*' \
    --tests '*CfirAnalysisLLTTestGenerated$Interface*' \
    --tests '*CfirAnalysisLLTPsiTestGenerated$Interface*' \
    --tests '*testUpperBoundsMemberAndMethodRich' \
    --tests '*CfirAnalysisLLTTestGenerated$ConstraintCheck*' \
    --tests '*CfirAnalysisLLTPsiTestGenerated$ConstraintCheck*' \
    --console=plain > /tmp/cc_final.log 2>&1
  echo "[$(date +%H:%M:%S)] slice done EXIT=$?"
else
  echo "[$(date +%H:%M:%S)] gave up waiting for a buildable tree"
fi
