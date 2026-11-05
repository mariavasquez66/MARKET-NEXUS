# Pasada 4 — Matriz de responsabilidades

- Fuente: `SDD/Especificación Funcional del Negocio - NexusMarket.pdf`, página 8 §12.
- Extracción repetida desde el PDF: PDFBox 2.0.31.
- SHA-256 del texto extraído: `1C994B68FE6A237F7C67AA1AB98D5A47373E4E24D1690EDE082DB9C1C13109F5`
- SHA-256 de palabras con coordenadas de la página 8: `BE06373B543CA4C2DFF00EA71156EDE60BD36E1CC81F688614FFFD436DB99175`
- Método de lectura: columnas identificadas por coordenada X de los encabezados y de cada marca, no por inferencia textual.

## Matriz verificada

| Proceso | Comprador | Vendedor | Op. Logístico | Admin | Etiqueta RVI |
|---|:---:|:---:|:---:|:---:|---|
| Registro de Vendedores |  |  |  | ✔ | [SPEC p.8 §12] |
| Registro de Productos |  | ✔ |  |  | [SPEC p.8 §12] |
| Administración de Inventario |  | ✔ | ✔ |  | [SPEC p.8 §12] |
| Gestión de Pedidos | ✔ | ✔ | ✔ |  | [SPEC p.8 §12] |
| Gestión de Reembolsos | ✔ |  |  | ✔ | [SPEC p.8 §12] |

## Hallazgos etiquetados

| ID | Hallazgo | Etiqueta RVI |
|---|---|---|
| P4-01 | La página 8 contiene cuatro columnas de rol: Comprador, Vendedor, Op. Logístico y Admin. | [SPEC p.8 §12] |
| P4-02 | Registro de Vendedores corresponde únicamente al Admin. | [SPEC p.8 §12] |
| P4-03 | Registro de Productos corresponde únicamente al Vendedor. | [SPEC p.8 §12] |
| P4-04 | Administración de Inventario corresponde a Vendedor y Operador Logístico. | [SPEC p.8 §12] |
| P4-05 | Gestión de Pedidos corresponde a Comprador, Vendedor y Operador Logístico. | [SPEC p.8 §12] |
| P4-06 | Gestión de Reembolsos corresponde a Comprador y Admin. | [SPEC p.8 §12] |
| P4-07 | La matriz no incluye filas de devoluciones, reportes ni Supervisor. | [SPEC p.8 §12] + [SUPUESTO: responsables no especificados] |
| P4-08 | El Supervisor solo aparece como “perfil de consulta y seguimiento operativo”; no se le asigna proceso en la matriz. | [SPEC p.4 §5] + [SPEC p.8 §12] + [SUPUESTO] |

## Corrección de una suposición previa

La decisión D-09 afirmaba que la matriz tenía “columnas perdidas”. La extracción posicional demuestra que las cuatro columnas están presentes y que las marcas son recuperables. Se corrige D-09: la matriz base es verificable; permanecen como supuestos las filas y roles que el PDF no incluye (devoluciones, reportes, Supervisor).
