# Supuestos no verificados — NexusMarket

> Estos supuestos **no** están respaldados por el PDF funcional. No se implementan como reglas obligatorias nuevas. Varios ya existen en el código por decisiones previas (D-12, D-15, etc.); se conservan mientras se confirman, pero no deben presentarse como requisitos verificados.

| ID | Supuesto | Estado en código | Pregunta para confirmar |
|---|---|---|---|
| SUP-01 | IVA 19% y precios con IVA incluido. | D-12, `TaxPolicyAdapter`, `InvoiceService`. | ¿La tasa y la base imponible son 19% e IVA incluido? |
| SUP-02 | Cancelación de pedidos, solo desde `PENDING_PAYMENT`. | `OrderStatus.CANCELLED`, `Order.cancel`. | ¿Se permite cancelar? ¿En qué estados y con qué efecto sobre inventario? |
| SUP-03 | Un envío por bodega y agrupación de envíos por pedido. | `Shipment` por pedido/bodega, sin regla explícita. | ¿Cuántos envíos se generan por pedido y por bodega? |
| SUP-04 | `DELIVERED` y `FINALIZED` como estados separados. | `OrderStatus` y `Order.finalizeOrder`. | ¿“Entregado / Finalizado” es un solo estado o dos pasos? |
| SUP-05 | Valores de `CommercialStatus`: `ELIGIBLE`, `RESTRICTED`, `SUSPENDED`. | `CommercialStatus`. | ¿El catálogo comercial es ese? ¿Qué estado impide comprar? |
| SUP-06 | Reportes administrativos solo para `SUPERVISOR`. | `ReportService`, `SupervisorController`. | ¿Qué roles consultan reportes? |
| SUP-07 | Aprobación de devoluciones por `ADMIN`; recepción por `LOGISTIC_OPERATOR`. | `ReturnService`, `TellerEmployeePort`, `BusinessOperatorPort`. | ¿Quién aprueba/rechaza y quién recibe una devolución? |
| SUP-08 | Procesamiento de reembolsos por `ADMIN`; comprador solo consulta. | `RefundService`, `TellerEmployeePort`. | ¿El comprador solicita reembolsos o solo los consulta? |
| SUP-09 | Estado de usuario `INACTIVE` además de `ACTIVE` y `BLOCKED`. | `UserStatus`. | ¿Se admite baja lógica/inactivo? |
| SUP-10 | Carrito como agregado separado del pedido. | `Cart`, `OrderStatus.CART` sin uso. | ¿El carrito es un agregado independiente o un estado del pedido? |
| SUP-11 | Rol `SELLER` registra existencias iniciales. | `InventoryService.registerInitialStock`. | ¿Quién registra el stock inicial: Vendedor, Operador Logístico o Admin? |
| SUP-12 | `TaxId` obligatorio para vendedor y propietario/responsable de bodega. | `Seller`, `Warehouse`. | ¿Estos atributos son obligatorios? |
| SUP-13 | Una factura por pedido pagado y reglas de redondeo. | `InvoiceService`, D-12. | ¿Se emite una factura por pedido? ¿Qué reglas fiscales aplican? |
| SUP-14 | Reglas de reembolso parcial y tope acumulado por devolución. | `RefundService`, D-15. | ¿Se permiten reembolsos parciales? |
| SUP-15 | Contenido de reportes (ventas, pedidos, inventario, devoluciones, usuarios). | `ReportService`. | ¿Qué reportes y métricas requiere el negocio? |
| SUP-16 | Devolución parcial por producto y cantidad en pedidos con varios vendedores. El PDF no detalla devoluciones parciales. | Implementado como `[REQUISITO-DOCENTE]`, no como supuesto del PDF: `Return.productId/quantity` y overload de `ReturnService`. | ¿La devolución parcial debe reembolsar solo el producto seleccionado y reingresar únicamente esa cantidad? |

## Estado

Ningún supuesto de esta tabla se usará para justificar una ampliación de servicios o endpoints. Si el PDF no lo respalda, la ampliación correspondiente se cancela o se deja documentada como propuesta.
