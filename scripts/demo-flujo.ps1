# Demo NexusMarket: carrito con 3 productos de 3 vendedores, pago, envio, devolucion y reporte en Mongo.
# Uso:  .\demo-flujo.ps1   (con pausas)     .\demo-flujo.ps1 -SinPausas

param(
    [string]$BaseUrl = 'http://localhost:8080',
    [switch]$SinPausas
)

$ErrorActionPreference = 'Stop'
$inicioMs = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$ids = New-Object 'System.Collections.Generic.HashSet[string]'

function VarEnv([string]$Nombre) {
    $v = [Environment]::GetEnvironmentVariable($Nombre)
    if ($v) { return $v }
    if (Test-Path '.env') {
        foreach ($l in Get-Content '.env') {
            if ($l -match "^\s*$Nombre\s*=\s*(.*)$") { return $Matches[1].Trim() }
        }
    }
    return $null
}

function Llamar {
    param([string]$Metodo, [string]$Ruta, $Cuerpo = $null, [string]$Token = $null, [switch]$Silencioso)
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $p = @{ Method = $Metodo; Uri = "$BaseUrl$Ruta"; Headers = $headers; ContentType = 'application/json' }
    if ($null -ne $Cuerpo) { $p['Body'] = ($Cuerpo | ConvertTo-Json -Depth 6) }
    try { return Invoke-RestMethod @p }
    catch {
        if (-not $Silencioso) {
            Write-Host "ERROR $Metodo $Ruta -> $($_.Exception.Message)" -ForegroundColor Red
            if ($_.ErrorDetails -and $_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message -ForegroundColor Red }
        }
        throw
    }
}

function Entrar([string]$Email, [string]$Clave) {
    return Llamar 'POST' '/api/v1/public/auth/login' @{ email = $Email; password = $Clave }
}
function Reg($o) { if ($o -and $o.id) { [void]$ids.Add([string]$o.id) }; return $o }
function Titulo([string]$t) { Write-Host ''; Write-Host "=== $t ===" -ForegroundColor Cyan }
function Pausa { if (-not $SinPausas) { Read-Host 'Pulsa Enter para continuar' | Out-Null } }

function MostrarCarrito([string]$Token) {
    $c = Llamar 'GET' '/api/v1/buyer/cart' $null $Token
    $filas = @()
    foreach ($i in $c.items) {
        $prod = Llamar 'GET' "/api/v1/public/products/$($i.productId)"
        $filas += [pscustomobject]@{
            Producto = $i.productName; Vendedor = $prod.sellerId; Cantidad = $i.quantity
            Precio = $i.unitPrice.amount; Subtotal = $i.subtotal.amount
        }
    }
    $filas | Format-Table -AutoSize | Out-String | Write-Host
    $vend = @($filas | Select-Object -ExpandProperty Vendedor -Unique).Count
    Write-Host "Items: $(@($c.items).Count)   Vendedores distintos: $vend   Total: $($c.total.amount) $($c.total.currency)" -ForegroundColor Green
}

$AdminEmail = VarEnv 'NEXUS_BOOTSTRAP_ADMIN_EMAIL'
$AdminPassword = VarEnv 'NEXUS_BOOTSTRAP_ADMIN_PASSWORD'
if (-not $AdminEmail) { $AdminEmail = Read-Host 'Correo del administrador' }
if (-not $AdminPassword) { $AdminPassword = Read-Host 'Clave del administrador' }

$ts = Get-Date -Format 'yyyyMMddHHmmss'
$clave = 'Demo12345'
$direccion = @{ street = 'Calle 10 # 5-20'; city = 'Medellin'; region = 'Antioquia'; postalCode = '050001'; country = 'CO' }
$catalogo = @(
    @{ nombre = 'Auriculares inalambricos'; precio = 120000; cantidad = 1 },
    @{ nombre = 'Teclado mecanico'; precio = 250000; cantidad = 2 },
    @{ nombre = 'Libro de programacion'; precio = 80000; cantidad = 1 }
)

Titulo '0. Sesion del administrador'
$admin = Entrar $AdminEmail $AdminPassword
$tAdmin = $admin.token

Titulo '1. Usuarios internos: operador logistico y supervisor'
$emailOp = "operador.$ts@example.com"
$operador = Reg (Llamar 'POST' '/api/v1/admin/users' @{
    fullName = 'Operador Demo'; email = $emailOp; identityDocument = "DEMO-$ts-OP"
    role = 'LOGISTIC_OPERATOR'; password = $clave } $tAdmin)
$emailSup = "supervisor.$ts@example.com"
$supervisor = Reg (Llamar 'POST' '/api/v1/admin/users' @{
    fullName = 'Supervisor Demo'; email = $emailSup; identityDocument = "DEMO-$ts-SU"
    role = 'SUPERVISOR'; password = $clave } $tAdmin)
Write-Host 'Operador y supervisor creados.' -ForegroundColor Green

Titulo '2. Tres vendedores, cada uno con su bodega, stock y un producto publicado'
$productos = @()
for ($n = 1; $n -le 3; $n++) {
    $item = $catalogo[$n - 1]
    $emailV = "vendedor$n.$ts@example.com"
    $usuario = Reg (Llamar 'POST' '/api/v1/admin/users' @{
        fullName = "Vendedor $n Demo"; email = $emailV; identityDocument = "DEMO-$ts-V$n"
        role = 'SELLER'; password = $clave } $tAdmin)
    $vendedor = Reg (Llamar 'POST' '/api/v1/admin/sellers' @{
        userId = $usuario.id; taxId = "$($ts.Substring(2))$n" } $tAdmin)
    $bodega = Reg (Llamar 'POST' '/api/v1/admin/warehouses' @{
        name = "Bodega Vendedor $n"; location = $direccion; type = 'SELLER'
        ownerSellerId = $vendedor.id; responsibleUserId = $operador.id } $tAdmin)
    try { Llamar 'POST' "/api/v1/admin/sellers/$($vendedor.id)/warehouses/$($bodega.id)" $null $tAdmin -Silencioso | Out-Null } catch { }
    $tV = (Entrar $emailV $clave).token
    $producto = Reg (Llamar 'POST' '/api/v1/seller/products' @{
        name = $item.nombre; description = 'Producto de demostracion'; type = 'PHYSICAL'; price = $item.precio } $tV)
    Reg (Llamar 'POST' '/api/v1/seller/inventory' @{
        productId = $producto.id; warehouseId = $bodega.id; initialQuantity = 20 } $tV) | Out-Null
    Llamar 'POST' "/api/v1/seller/products/$($producto.id)/publish" $null $tV | Out-Null
    $productos += [pscustomobject]@{
        Vendedor = $vendedor.id; Producto = $item.nombre; ProductoId = $producto.id
        Precio = $item.precio; Cantidad = $item.cantidad; BodegaId = $bodega.id
    }
    Write-Host "  vendedor $n listo: $($item.nombre)" -ForegroundColor Yellow
}
$productos | Format-Table Vendedor, Producto, BodegaId -AutoSize | Out-String | Write-Host
Pausa

Titulo '3. Comprador y carrito con 3 productos de 3 vendedores distintos'
$emailC = "comprador.$ts@example.com"
Llamar 'POST' '/api/v1/public/buyers/register' @{
    fullName = 'Comprador Demo'; email = $emailC; identityDocument = "DEMO-$ts-C"
    password = $clave; mainAddress = $direccion } | Out-Null
$tComprador = (Entrar $emailC $clave).token
foreach ($p in $productos) {
    Llamar 'POST' '/api/v1/buyer/cart/items' @{ productId = $p.ProductoId; quantity = $p.Cantidad } $tComprador | Out-Null
    Write-Host "  agregado al carrito: $($p.Producto)" -ForegroundColor Yellow
}
MostrarCarrito $tComprador
Pausa

Titulo '4. Checkout: se crea el pedido y se reserva el stock'
$orden = Reg (Llamar 'POST' '/api/v1/buyer/orders' @{ deliveryAddress = $direccion } $tComprador)
Write-Host "Pedido $($orden.id)  estado: $($orden.status)  total: $($orden.totalAmount.amount)  bodegas: $(@($orden.allocations).Count)" -ForegroundColor Green
Pausa

Titulo '5. Pago del pedido (se emite la factura)'
$orden = Llamar 'POST' "/api/v1/buyer/orders/$($orden.id)/pay" $null $tComprador
if ($orden.invoiceId) { [void]$ids.Add([string]$orden.invoiceId) }
Write-Host "Estado: $($orden.status)  pago: $($orden.paymentStatus)  factura: $($orden.invoiceId)" -ForegroundColor Green
Pausa

Titulo '6. Envios (operador logistico): un envio por bodega'
$tOp = (Entrar $emailOp $clave).token
$bodegas = @($orden.allocations | Select-Object -ExpandProperty warehouseId -Unique)
$envios = @()
foreach ($b in $bodegas) {
    $envios += Reg (Llamar 'POST' '/api/v1/logistics/shipments' @{ orderId = $orden.id; warehouseId = $b } $tOp)
}
$k = 0
foreach ($e in $envios) {
    $k++
    Llamar 'POST' "/api/v1/logistics/shipments/$($e.id)/start" @{ trackingNumber = "TRK-$ts-$k" } $tOp | Out-Null
    Write-Host "  envio $k despachado (TRK-$ts-$k)" -ForegroundColor Yellow
}
$estado = (Llamar 'GET' "/api/v1/buyer/orders/$($orden.id)" $null $tComprador).status
Write-Host "Estado del pedido tras despachar: $estado" -ForegroundColor Green
foreach ($e in $envios) {
    Llamar 'POST' "/api/v1/logistics/shipments/$($e.id)/deliver" $null $tOp | Out-Null
}
$estado = (Llamar 'GET' "/api/v1/buyer/orders/$($orden.id)" $null $tComprador).status
Write-Host "Estado del pedido tras entregar: $estado" -ForegroundColor Green
Pausa

Titulo '7. Devolucion de UNO de los productos (reembolso parcial)'
$devuelto = $productos[2]
$monto = $devuelto.Precio * $devuelto.Cantidad
$dev = Reg (Llamar 'POST' "/api/v1/buyer/orders/$($orden.id)/returns" @{
    reason = "Devolucion del producto: $($devuelto.Producto)"
    productId = $devuelto.ProductoId
    quantity = $devuelto.Cantidad } $tComprador)
Llamar 'POST' "/api/v1/admin/returns/$($dev.id)/approve" $null $tAdmin | Out-Null
$reem = Reg (Llamar 'POST' "/api/v1/admin/returns/$($dev.id)/refunds" @{ amount = $monto } $tAdmin)
Llamar 'POST' "/api/v1/admin/refunds/$($reem.id)/process" $null $tAdmin | Out-Null
Write-Host "Devolucion parcial aprobada y reembolso procesado por $monto (producto: $($devuelto.Producto), cantidad: $($dev.quantity))" -ForegroundColor Green
Pausa

Titulo '8. Reporte de auditoria desde la API (datos guardados en MongoDB)'
$tSup = (Entrar $emailSup $clave).token
$logs = @(Llamar 'GET' '/api/v1/supervisor/audit-logs' $null $tSup)
$mios = @($logs | Where-Object { $ids.Contains([string]$_.entityId) } | Sort-Object occurredAt)
$mios | Select-Object @{ n = 'Hora'; e = { ([datetime]$_.occurredAt).ToLocalTime().ToString('HH:mm:ss') } }, action, entityType, role, @{ n = 'Entidad'; e = { ([string]$_.entityId).Substring(0, 8) } } | Format-Table -AutoSize | Out-String | Write-Host
Write-Host "Eventos de esta demo: $($mios.Count)" -ForegroundColor Green
$mios | Group-Object action | Sort-Object Name | Format-Table Name, Count -AutoSize | Out-String | Write-Host
Pausa

Titulo '9. Consulta directa a MongoDB (coleccion audit_logs)'
$mu = VarEnv 'MONGO_ROOT_USERNAME'
$mp = VarEnv 'MONGO_ROOT_PASSWORD'
if ($mu -and $mp) {
    $js = 'db.audit_logs.find({occurredAt:{$gte:new Date(' + $inicioMs + ')}},{_id:0,actionCode:1,entityTypeCode:1,roleCode:1,occurredAt:1}).sort({occurredAt:1}).forEach(printjson)'
    docker exec nexusmarket-mongo mongo -u $mu -p $mp --authenticationDatabase admin nexus_audit_db --quiet --eval $js
}
else {
    Write-Host 'No encontre MONGO_ROOT_USERNAME/PASSWORD en el .env; usa el reporte de la API del paso 8.' -ForegroundColor Yellow
}
Write-Host ''
Write-Host "Pedido: $($orden.id)  Devolucion: $($dev.id)  Reembolso: $($reem.id)" -ForegroundColor Cyan
