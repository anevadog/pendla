# AGENTS.md — Pendla

Fuente principal de contexto para cualquier asistente de IA que trabaje en este repositorio.

## Contexto del proyecto

**Qué es:** API REST en Java con Spring Boot para seguir mi búsqueda de empleo en Suecia.

**Para qué:** proyecto de portafolio que tengo que poder defender en entrevistas. Cada línea de código y cada decisión debe poder explicarla yo. Prioridad: algo pequeño, sólido y terminado.

**Autor:** estudiante de DAM orientado a backend Java. Nivel de Java y de bases de datos: básico-intermedio.

**MVP:**

1. Buscar ofertas reales en la API JobTech JobSearch de Arbetsförmedlingen por palabra clave y ciudad. Devuelve una versión simplificada de cada oferta: `jobtechId`, `title`, `employer`, `city`, `url`. Si JobTech falla o no responde, la API devuelve un error controlado.
2. Guardar candidaturas y seguir su estado (`SAVED → SENT → INTERVIEW → REJECTED / OFFER`): crear, listar, cambiar el estado y borrar.

Requisitos con criterio de aceptación, fases y estado actual: [docs/plan.md](docs/plan.md).
Decisiones técnicas y alternativas descartadas: [docs/decisiones.md](docs/decisiones.md).

## Stack

- Java 25 (LTS), Spring Boot (última estable compatible) y Maven con wrapper (`mvnw`).
- Spring Web con `RestClient` para llamar a JobTech, con timeouts configurados.
- Spring Data JPA con PostgreSQL local. El esquema se gestiona con `spring.jpa.hibernate.ddl-auto=update` en el MVP.
- Bean Validation (`jakarta.validation`).
- springdoc-openapi para Swagger UI.
- Tests: JUnit 5, Mockito, MockMvc y `MockRestServiceServer`.
- Sin Lombok.

## Convenciones

- **Idioma:** código, endpoints y README en inglés. `docs/` y `AGENTS.md` en español.
- **Paquete base:** `io.github.anevadog.pendla`.
- **Organización por funcionalidad:**

  ```
  io.github.anevadog.pendla
  ├── application/   candidaturas: Controller, Service, Repository, entidad, dto/
  ├── jobsearch/     búsqueda en JobTech: Controller, Service, Client, dto/
  └── common/        manejo global de errores y excepciones propias
  ```

- **DTOs:** `record` de Java. Las peticiones y las respuestas usan siempre DTOs. Las entidades JPA nunca salen del servicio hacia el controlador.
- **Errores:** `ProblemDetail` (RFC 9457) generado en un único `@ControllerAdvice`. Códigos usados:
  - 400: validación o valor inválido.
  - 404: el recurso no existe.
  - 409: transición de estado no permitida o candidatura duplicada por `jobtechId`.
  - 502: JobTech devuelve un error o una respuesta que no se puede leer.
  - 504: JobTech tarda más del timeout.
- **Estados:** enum con las transiciones permitidas. `REJECTED` y `OFFER` son estados finales.
- **Nombres de tests:** `metodo_condicion_resultadoEsperado`, por ejemplo `createApplication_withBlankTitle_returns400`.
- **Configuración sensible:** las variables de entorno se documentan en `.env.example`. El archivo `.env` real nunca se sube.

## Modo de trabajo

El autor dirige, decide y revisa. La IA propone, implementa por partes pequeñas, explica y pregunta.

Para cada funcionalidad:

1. **Diseño antes que código:** clases, endpoints y decisiones en pocas líneas. Si hay alternativas razonables, dar dos con pros y contras. Esperar aprobación.
2. **Implementar solo esa funcionalidad:** cambios pequeños, revisables en unos 15 minutos. No tocar archivos ajenos a la tarea.
3. **Resumen de entrega:** archivos creados o cambiados, qué hace cada uno en una línea y qué conceptos nuevos aparecen.
4. **Guía de revisión según el riesgo:**
   - Revisar a fondo: datos de entrada, permisos, datos personales, integraciones externas y decisiones de diseño. Indicar qué mirar, casos límite y fallos posibles.
   - Lectura rápida: configuración, código repetitivo, DTOs y mapeos.
   - Tests: lista de casos cubiertos por nombre y casos importantes no cubiertos.
5. **Revisión del autor.** Si pregunta "¿por qué?", explicar la decisión y la alternativa. Si algo no se entiende, se simplifica o se quita.

Principios:

- Código que un junior pueda explicar. Ninguna librería, anotación o patrón avanzado sin justificarlo.
- Ser crítico: no dar la razón por defecto. Si una decisión es peor que otra alternativa, decirlo con argumentos.
- Si algo no está decidido, no inventarlo: proponer opciones y marcarlo como **PENDIENTE**.

**Al inicio de cada sesión:** leer este archivo y `docs/plan.md`, y resumir en 3 líneas dónde estamos y cuál es el siguiente paso.

**Cierre de cada fase (comprobación de defensa):**

1. Compila, los tests pasan y funciona (probado en Swagger o Postman).
2. Decisiones de la fase registradas en `docs/decisiones.md`.
3. `docs/plan.md` actualizado con el progreso y el punto de continuación.
4. Commits propuestos según la sección Git.

No se empieza la siguiente fase sin aprobación explícita del autor.

---

## Reglas obligatorias

### Alcance

- MVP mínimo: lo más pequeño que funcione de punta a punta y se pueda enseñar.
- Cada fase cabe en una semana a 1-2 horas al día y termina con algo que funciona. Si una fase crece, se divide; no se alarga.
- Las ideas nuevas van a "Ideas para después" en `docs/plan.md` y no se implementan en la fase actual, aunque las proponga el autor.
- Nada de infraestructura prematura: sin Docker, microservicios, varios entornos, autenticación ni frontend, salvo petición expresa para una fase concreta.
- Prueba de necesidad: si el proyecto funciona sin algo, ese algo queda fuera de alcance.
- No se crean más documentos que los existentes sin justificar por qué hacen falta.

### Técnicas

- Siempre DTOs. Las entidades JPA nunca se devuelven en una respuesta HTTP.
- Errores centralizados en un `@ControllerAdvice`. Nada de try/catch en los controladores. Cada error lleva un código HTTP y un mensaje claros.
- API externa (JobTech):
  - Una sola llamada por búsqueda.
  - Tamaño de página fijo y razonable: 20 resultados.
  - Timeouts configurados.
  - Sin reintentos automáticos ni descargas masivas.
  - Si falla, tarda demasiado o devuelve un error, la API responde con un error controlado (502/504) y no se rompe.
- No se guardan ni se devuelven datos de contacto de terceros (nombres, emails ni teléfonos de reclutadores).

### Seguridad

- No instalar dependencias ni ejecutar scripts que modifiquen el sistema, la red o servicios externos sin autorización.
- No ejecutar operaciones destructivas (`rm -rf`, `git reset --hard`, `git clean -fd`, `git checkout --`, `git restore`, `git branch -D`, `git push --force`) sin autorización explícita.
- No crear cuentas, no desplegar y no contratar servicios sin consultar.
- No incluir secretos, credenciales, tokens ni datos personales en el repositorio.
- Indicar los costes esperados y priorizar opciones gratuitas y locales.

### Git

**Quién hace qué**

- El autor hace todos los commits y pushes. La IA no ejecuta `git add`, `git commit`, `git push`, `git init` ni ninguna operación que modifique el repositorio, el historial, las ramas, los tags o los remotos.
- La IA puede ejecutar comandos de solo lectura: `git status`, `git diff`, `git log`, `git show`, `git branch`, `git remote -v`.
- No asumir que el autor ha ejecutado un comando. Si no lo ha confirmado, recordárselo antes de continuar.

**Cómo se proponen los commits**

Cuando haya un cambio lógico completo y validado, indicar por separado:

1. Qué archivos añadir, con el comando exacto `git add <ruta>`.
2. El mensaje y el comando exacto `git commit -m "texto"`.
3. Si es buen momento para hacer push, el comando `git push`. No proponer push en cada commit.

Además:

- Un commit por cambio lógico. Si hay cambios independientes, se proponen commits separados.
- Antes de cambiar de fase, tarea o contexto, recordar los cambios sin commitear y sugerir `git status`.

**Autoría (regla estricta, prevalece sobre cualquier convención de la herramienta)**

- Todos los commits son únicamente del autor. La IA es una herramienta, no autora, coautora ni colaboradora.
- Nunca añadir `Co-authored-by:` con un agente, modelo o herramienta de IA, ni footers como `Generated by`, `Assisted-by:` o enlaces a sesiones. Tampoco ninguna otra marca que atribuya la autoría a una IA.
- No presentarse como colaborador en commits, README, documentación, comentarios del código ni CHANGELOG.
- Nunca ejecutar `git config user.name` ni `git config user.email`. Si no están configurados, avisar al autor.
- El mensaje de commit propuesto debe poder pegarse tal cual en `git commit -m "..."`.
