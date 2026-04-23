{
  "rootOut":"${modulePath}",
  "addObfuscationConfig": true,
  "uiSyntax": "ets",
  "isCrossPlatformProject": <#if category?? && (category == "cross ability" || category == "cross library") || (isArkUIXProject?? && isArkUIXProject)>true<#else>false</#if>,
  "shouldCreateOhosTest": <#if (createOhosTest?? && createOhosTest) && apiType?? && apiType == "stageMode">true<#else>false</#if>
}