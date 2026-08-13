[CmdletBinding()]
param(
    [ValidateSet('all', 'user-service', 'restaurant-service', 'catalog-service', 'search-service')]
    [string]$Service = 'all',

    [string]$AccessToken = $env:OPENAPI_ACCESS_TOKEN
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$outputDirectory = Join-Path $repositoryRoot 'docs\api-specs'

$services = [ordered]@{
    'user-service'       = 'http://localhost:8101'
    'restaurant-service' = 'http://localhost:8102'
    'catalog-service'    = 'http://localhost:8103'
    'search-service'     = 'http://localhost:8104'
}

$selectedServices = if ($Service -eq 'all') { $services.Keys } else { @($Service) }
$headers = @{}
if (-not [string]::IsNullOrWhiteSpace($AccessToken)) {
    $headers.Authorization = "Bearer $AccessToken"
}

foreach ($serviceName in $selectedServices) {
    $url = "$($services[$serviceName])/v3/api-docs.yaml"
    $target = Join-Path $outputDirectory "$serviceName.yaml"
    $temporary = "$target.tmp"

    Write-Host "Exporting $serviceName from $url"
    try {
        Invoke-WebRequest -Uri $url -Headers $headers -OutFile $temporary -TimeoutSec 30
        $firstLine = Get-Content -LiteralPath $temporary -TotalCount 1
        if ($firstLine -notmatch '^openapi:\s') {
            $responseLength = (Get-Item -LiteralPath $temporary).Length
            $previewLength = [Math]::Min(300, $responseLength)
            $preview = (Get-Content -LiteralPath $temporary -Raw).Substring(0, $previewLength)
            throw "Response was not OpenAPI YAML. Preview: $preview"
        }
        Move-Item -LiteralPath $temporary -Destination $target -Force
        Write-Host "  wrote $target"
    }
    catch {
        if (Test-Path -LiteralPath $temporary) {
            Remove-Item -LiteralPath $temporary -Force
        }
        throw "Could not export $serviceName. Ensure it is running and supply a valid short-lived Bearer token through -AccessToken or OPENAPI_ACCESS_TOKEN. $($_.Exception.Message)"
    }
}
