$ErrorActionPreference = 'Stop'

$sdkRoots = @(
  $env:ANDROID_HOME,
  $env:ANDROID_SDK_ROOT,
  $(if ($env:LOCALAPPDATA) { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }),
  $(if ($env:USERPROFILE) { Join-Path $env:USERPROFILE 'AppData\Local\Android\Sdk' })
) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

$adbPath = $null
foreach ($sdkRoot in $sdkRoots) {
  $candidate = Join-Path $sdkRoot 'platform-tools\adb.exe'
  if (Test-Path -LiteralPath $candidate -PathType Leaf) {
    $adbPath = (Resolve-Path -LiteralPath $candidate).Path
    break
  }
}

if (-not $adbPath) {
  $adbCommand = Get-Command adb.exe -ErrorAction SilentlyContinue
  if ($adbCommand) {
    $adbPath = $adbCommand.Source
    if ([string]::IsNullOrWhiteSpace($adbPath)) {
      $adbPath = $adbCommand.Path
    }
  }
}

if ([string]::IsNullOrWhiteSpace($adbPath)) {
  Write-Output '[ERROR] Không tìm thấy adb. Hãy cài Android SDK Platform-Tools hoặc đặt ANDROID_HOME/ANDROID_SDK_ROOT.'
  exit 1
}

Write-Output "adb: $adbPath"
$deviceOutput = @(& $adbPath devices 2>&1)
if ($LASTEXITCODE -ne 0) {
  Write-Output "[ERROR] Không thể chạy adb devices: $($deviceOutput -join ' ')"
  exit 1
}

$emulatorSerials = @()
foreach ($line in $deviceOutput) {
  if ($line -match '^(\S+)\s+device(?:\s+.*)?$') {
    $serial = $Matches[1]
    if ($serial -like 'emulator-*') {
      $emulatorSerials += $serial
    }
  }
}

if ($emulatorSerials.Count -eq 0) {
  Write-Output 'Không có Android emulator nào đang ở trạng thái device; bỏ qua adb reverse.'
  exit 0
}

$failedSerials = @()
foreach ($serial in $emulatorSerials) {
  $reverseOutput = @(& $adbPath -s $serial reverse tcp:8180 tcp:8180 2>&1)
  if ($LASTEXITCODE -eq 0) {
    Write-Output "[OK] ${serial}: localhost:8180 -> emulator tcp:8180"
  } else {
    Write-Output "[FAIL] ${serial} không cấu hình được adb reverse: $($reverseOutput -join ' ')"
    $failedSerials += $serial
  }
}

if ($failedSerials.Count -gt 0) {
  exit 1
}
