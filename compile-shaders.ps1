param()

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$target = Join-Path $root 'target'
$classpathFile = Join-Path $target 'shader-tools.classpath'

Push-Location -LiteralPath $root
try
{
    & (Join-Path $root 'mvnw.cmd') -q -Pshader-tools clean compile
    if ($LASTEXITCODE -ne 0) { throw "Shader tool compilation failed with exit code $LASTEXITCODE." }
    & (Join-Path $root 'mvnw.cmd') -q -Pshader-tools dependency:build-classpath "-Dmdep.outputFile=$classpathFile"
    if ($LASTEXITCODE -ne 0) { throw "Shader tool classpath resolution failed with exit code $LASTEXITCODE." }
    $classpath = (Join-Path $target 'classes') + ';' + ((Get-Content -LiteralPath $classpathFile -Raw).Trim())
    foreach ($shader in @(
        @('quad.vert', 'vert', 'quad.vert.spv'),
        @('quad-srgb.frag', 'frag', 'quad-srgb.frag.spv'),
        @('quad-unorm.frag', 'frag', 'quad-unorm.frag.spv')
    ))
    {
        & java --enable-native-access=ALL-UNNAMED -cp $classpath br.com.engine.platform.lwjgl.ShaderAssetCompiler `
            (Join-Path $root ("src/resources/shaders/" + $shader[0])) $shader[1] (Join-Path $root ("src/resources/shaders/" + $shader[2]))
        if ($LASTEXITCODE -ne 0) { throw "Shader compilation failed for $($shader[0]) with exit code $LASTEXITCODE." }
    }
}
finally { Pop-Location }
