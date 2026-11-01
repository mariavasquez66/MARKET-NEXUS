# Adaptadores de casos de uso

> Paquete: `application.adapters.useCases`.
> Implementan los seis puertos de entrada y **solo orquestan** servicios de
> dominio. Sin lógica de negocio, sin acceso directo a repositorios y sin
> conocimiento de HTTP.

## 1. Clases

| Caso de uso | Implementa | Servicios inyectados |
|---|---|---|
| `PublicAccessUseCase` | `PublicAccessPort` | `AuthService`, `CatalogService` |
| `NaturalCustomerUseCase` | `NaturalCustomerPort` | `CartService`, `OrderService`, `InvoiceService`, `ReturnService`, `RefundService`, `BuyerService`, `LogisticsService` (consulta de envíos), `InventoryService` (consulta) |
| `BusinessCustomerUseCase` | `BusinessCustomerPort` | `CatalogService`, `InventoryService`, `OrderService` |
| `BusinessOperatorUseCase` | `BusinessOperatorPort` | `InventoryService`, `LogisticsService`, `OrderService`, `ReturnService` |
| `BusinessSupervisorUseCase` | `BusinessSupervisorPort` | `ReportService`, `OrderService` |
| `TellerEmployeeUseCase` | `TellerEmployeePort` | `SellerService`, `WarehouseService`, `UserService`, `ReturnService`, `RefundService` |

## 2. Contrato de implementación

```java
@Component
public class NaturalCustomerUseCase implements NaturalCustomerPort {

    private final CartService cartService;
    private final OrderService orderService;
    // ...

    public NaturalCustomerUseCase(CartService cartService,
                                  OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @Override
    public Cart getCart(User actor) {
        return cartService.getCart(actor);
    }
    // delegación 1:1 con el servicio
}
```

Reglas:

1. **Delegación 1:1.** Cada método del puerto llama al método de servicio
   documentado en `SDD/Domain/services/`. No se añaden reglas propias.
2. **Actor.** El `User` llega como primer parámetro desde el controlador
   (reconstruido por el filtro JWT); el caso de uso no lo fabrica ni lo busca.
3. **Sin DTOs.** Los casos de uso trabajan con tipos de dominio; los DTOs y su
   mapeo viven en `adapters/rest`.
4. **Transacciones.** Las anotaciones `@Transactional` (si se requieren) se
   colocan en el caso de uso o en el controlador, nunca en el dominio. Las
   operaciones de solo lectura pueden usar `@Transactional(readOnly = true)`.
5. **Composición.** Si un caso de uso necesita varios servicios, los recibe por
   constructor; no crea instancias con `new`.
6. **Errores.** No captura excepciones de dominio: se propagan al
   `GlobalExceptionHandler`.

## 3. Mapa puerto → caso de uso → controlador

| Puerto | Caso de uso | Controlador | Prefijo |
|---|---|---|---|
| `PublicAccessPort` | `PublicAccessUseCase` | `PublicController` | `/api/v1/public` |
| `NaturalCustomerPort` | `NaturalCustomerUseCase` | `BuyerController` | `/api/v1/buyer` |
| `BusinessCustomerPort` | `BusinessCustomerUseCase` | `SellerController` | `/api/v1/seller` |
| `BusinessOperatorPort` | `BusinessOperatorUseCase` | `LogisticsController` | `/api/v1/logistics` |
| `BusinessSupervisorPort` | `BusinessSupervisorUseCase` | `SupervisorController` | `/api/v1/supervisor` |
| `TellerEmployeePort` | `TellerEmployeeUseCase` | `AdminController` | `/api/v1/admin` |

## 4. Pruebas previstas (Fase 7)

- Un test por caso de uso con los servicios mockeados
  (`PublicAccessUseCaseTest`, `NaturalCustomerUseCaseTest`, …), verificando que
  cada método delega con los argumentos correctos y no añade lógica.
