Add-Type -AssemblyName System.Drawing

function Create-SimpleIcon {
    param($size, $outputPath)

    $bitmap = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = 'AntiAlias'

    # Background - green
    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(76, 175, 80))
    $graphics.FillRectangle($bgBrush, 0, 0, $size, $size)

    # F letter - white
    $fgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $font = New-Object System.Drawing.Font('Arial', [int]($size * 0.6), [System.Drawing.FontStyle]::Bold)
    $stringFormat = New-Object System.Drawing.StringFormat
    $stringFormat.Alignment = 'Center'
    $stringFormat.LineAlignment = 'Center'
    $rect = New-Object System.Drawing.RectangleF(0, 0, $size, $size)
    $graphics.DrawString('F', $font, $fgBrush, $rect, $stringFormat)

    $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}

$basePath = 'D:\00_Study\FitLog\app\src\main\res'

Create-SimpleIcon 48 "$basePath\mipmap-mdpi\ic_launcher.png"
Create-SimpleIcon 72 "$basePath\mipmap-hdpi\ic_launcher.png"
Create-SimpleIcon 96 "$basePath\mipmap-xhdpi\ic_launcher.png"
Create-SimpleIcon 144 "$basePath\mipmap-xxhdpi\ic_launcher.png"
Create-SimpleIcon 192 "$basePath\mipmap-xxxhdpi\ic_launcher.png"

Create-SimpleIcon 48 "$basePath\mipmap-mdpi\ic_launcher_round.png"
Create-SimpleIcon 72 "$basePath\mipmap-hdpi\ic_launcher_round.png"
Create-SimpleIcon 96 "$basePath\mipmap-xhdpi\ic_launcher_round.png"
Create-SimpleIcon 144 "$basePath\mipmap-xxhdpi\ic_launcher_round.png"
Create-SimpleIcon 192 "$basePath\mipmap-xxxhdpi\ic_launcher_round.png"

Write-Host 'Icons created successfully'
