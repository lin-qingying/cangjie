<merge src="code template/src/faMode/config.json.ftl" dest="${rootOut}/src/main/config.json" />
<#if !superVisualEnable>
<#--  传统开发模式  -->
    <#if uiSyntax?lower_case == "ets">
        <merge src="ets/code template/src/faMode/common/ets/jsConfig.json.ftl" dest="${rootOut}/src/main/config.json" />
        <copy src="ets/code template/src/faMode/traditionalCode/ets/src" dest="${rootOut}/src/main/ets/${abilityName}"/>
        <open file="${rootOut}/src/main/ets/${abilityName}/app.ets" />
    <#elseif uiSyntax?lower_case == "js" || uiSyntax?lower_case == "hml">
        <merge src="ets/code template/src/faMode/common/js/jsConfig.json.ftl" dest="${rootOut}/src/main/config.json" />
        <copy src="ets/code template/src/faMode/traditionalCode/js/src" dest="${rootOut}/src/main/js/${abilityName}"/>
        <open file="${rootOut}/src/main/js/${abilityName}/app.js" />
    </#if>
<#else>
<#--  低代码开发模式  -->
    <#if uiSyntax?lower_case == "ets">
        <merge src="ets/code template/src/faMode/common/ets/jsConfig.json.ftl" dest="${rootOut}/src/main/config.json" />
        <copy src="ets/code template/src/faMode/superVisual/ets/src" dest="${rootOut}/src/main/ets/${abilityName}"/>
        <#if supportHos!false>
            <copy src="ets/code template/src/faMode/superVisual/ets/supervisual/index-hos.visual"
                  dest="${rootOut}/src/main/supervisual/${abilityName}/pages/index.visual" />
        <#else>
            <copy src="ets/code template/src/faMode/superVisual/ets/supervisual/index-ohos.visual"
                  dest="${rootOut}/src/main/supervisual/${abilityName}/pages/index.visual" />
        </#if>
        <open file="${rootOut}/src/main/ets/${abilityName}/app.ets" />
        <open file="${rootOut}/src/main/supervisual/${abilityName}/pages/index.visual" />

    <#elseif uiSyntax?lower_case == "js" || uiSyntax?lower_case == "hml">
        <merge src="ets/code template/src/faMode/common/js/jsConfig.json.ftl" dest="${rootOut}/src/main/config.json" />
        <copy src="ets/code template/src/faMode/superVisual/js/src" dest="${rootOut}/src/main/js/${abilityName}" />
        <copy src="ets/code template/src/faMode/superVisual/js/supervisual/" dest="${rootOut}/src/main/supervisual/${abilityName}/pages/" />
        <open file="${rootOut}/src/main/js/${abilityName}/app.js" />
        <open file="${rootOut}/src/main/supervisual/${abilityName}/pages/index/index.visual" />
    </#if>
</#if>