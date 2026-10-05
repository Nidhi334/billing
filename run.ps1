$compileOnly = $args -contains "-CompileOnly"
$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$srcPath = Join-Path $projectRoot "src"
$libPath = Join-Path $projectRoot "lib"
$binPath = Join-Path $projectRoot "bin"

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Error "javac was not found. Install a JDK and add its bin folder to PATH."
    exit 1
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "java was not found. Install a JDK and add its bin folder to PATH."
    exit 1
}

$javaFiles = Get-ChildItem -Path $srcPath -Filter "*.java" -Recurse |
    Where-Object { $_.Name -ne "tempCodeRunnerFile.java" } |
    ForEach-Object { $_.FullName }

if ($javaFiles.Count -eq 0) {
    Write-Error "No Java source files were found under $srcPath."
    exit 1
}

New-Item -ItemType Directory -Force -Path $binPath | Out-Null

Write-Host "Compiling SmartBilling Pro..."
& javac -cp "$libPath\*;$srcPath" -d $binPath $javaFiles
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed. Fix the errors above and run this script again."
    exit $LASTEXITCODE
}

if ($compileOnly) {
    Write-Host "Compilation successful."
    exit 0
}

Write-Host "Compilation successful. Starting the application..."
& java -cp "$binPath;$libPath\*" App
exit $LASTEXITCODE
