var scriptOutputDataKeys = [];
var scriptOutputDataValues = [];

function addScriptOutputData(key, value) {
    scriptOutputDataKeys.push(key);
    scriptOutputDataValues.push(value);
}

function removeScriptOutputData(key) {
    for (i = 0; i < scriptOutputDataKeys.length; i++) {
        if (scriptOutputDataKeys[i] == key) {
            break;
        }
    }
    scriptOutputDataKeys.splice(i, 1);
    scriptOutputDataValues.splice(i, 1);
}

function prepareScriptOutputDataForSubmission() {
    assembledScriptOutputData='';
    for (i = 0; i < scriptOutputDataKeys.length; i++) {
        assembledScriptOutputData = assembledScriptOutputData + scriptOutputDataKeys[i] + equalsSymbol +
            scriptOutputDataValues[i];
        if (i < (scriptOutputDataKeys.length - 1)) {
            assembledScriptOutputData = assembledScriptOutputData + delimiterSymbol;
        }
    }
    document.forms[0].elements['clientScriptOutputData'].value = assembledScriptOutputData;
}