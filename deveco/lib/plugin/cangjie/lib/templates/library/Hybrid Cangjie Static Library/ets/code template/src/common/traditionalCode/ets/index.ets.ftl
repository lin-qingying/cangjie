<#if apiType == "stageMode">
export {${"${pagePackageName}"?cap_first}} from './src/main/ets/components/${pagePackageName}'
<#else>
export {${"${pagePackageName}"?cap_first}} from './src/main/ets/components/${pagePackageName}'
</#if>