<#if uiSyntax?lower_case == "ets">
    <instantiate src="ets/code template/src/common/traditionalCode/ets/ets/main_page/main_page.ets.ftl"
          dest="${rootOut}/src/main/ets${pageMidDir}/${pagePackageName}.ets" />
    <open file="${rootOut}/src/main/ets${pageMidDir}/${pagePackageName}.ets"/>
<#elseif uiSyntax?lower_case == "js" || uiSyntax?lower_case == "hml">
    <copy src="ets/code template/src/common/traditionalCode/js/js/page/index"
          dest="${rootOut}/src/main/js/${pageMidDir}/${pagePackageName}" />
    <open file="${rootOut}/src/main/js/${pageMidDir}/${pagePackageName}/index.js"/>
</#if>