{
  "apiType": "<#if apiType??>${apiType}<#else>stageMode</#if>",
  "buildOption": {
<#if enableNative>
    "externalNativeOptions": {
      "path": "./src/main/cpp/CMakeLists.txt",
      "arguments": "",
      "cppFlags": ""
    },
</#if>
  },
<#if addObfuscationConfig>
  "buildOptionSet": [
    {
      "name": "release",
      "arkOptions": {
        "obfuscation" : {
          "ruleOptions":{
            "enable": false,
            "files": ["./obfuscation-rules.txt"]
          },
          "consumerFiles": ["./consumer-rules.txt"]
          }
      },
  <#if enableNative>
	  "nativeLib": {
        "debugSymbol": {
          "strip": true,
          "exclude": []
        }
      }
  </#if>
    },
  ],
</#if>
  "targets": [
    {
      "name": "default"
    <#if (supportHos && !compileSdkVersion?contains("."))!false>
      ,
      "runtimeOS":"HarmonyOS"
    </#if>
    }
    <#if shouldCreateOhosTest>
    ,
    {
      "name": "ohosTest"
    }
    </#if>
  ]
}
