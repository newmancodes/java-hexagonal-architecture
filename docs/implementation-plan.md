# Plan: Implement Hexagonal Architecture — Product Catalogue Demo

## Context

The project is a clean-slate Spring Boot 4.0.3 app scaffolded with the right dependencies (Web MVC, JPA, Validation, Actuator, Lombok, Testcontainers) but zero domain logic. The goal is to build a concrete, complete example of Hexagonal Architecture using a **Product catalogue** domain — simple enough to follow, rich enough to demonstrate all layers: domain business rules, application use cases, a persistence adapter, and a REST web adapter, each with appropriate tests.

---

## Package Structure

```
digital.newman.hexagonal/
├── domain/                         # Pure Java — no Spring, no JPA
├── application/
│   ├── port/in/                    # Driving ports (use case interfaces)
│   ├── port/out/                   # Driven ports (persistence interfaces)
│   └── service/                    # Application service + Spring @Configuration
└── adapter/
    ├── in/web/                     # REST controller (inbound adapter)
    └── out/persistence/            # JPA adapter (outbound adapter)
```

**Dependency rule:** arrows point inward only. `adapter` → `application.port` → `domain`. Neither `domain` nor `application` imports Spring or JPA.

---

## Phase 1 — Domain Layer (no Spring required)

**Files to create** in `src/main/java/digital/newman/hexagonal/domain/`:

| File | Description |
|------|-------------|
| `ProductId.java` | `record ProductId(UUID value)` — wraps UUID, `generate()` + `of()` factories |
| `Money.java` | `record Money(BigDecimal amount, Currency currency)` — validates non-negative amount |
| `Product.java` | Plain class with two factories: `create()` (generates ID, validates) and `reconstitute()` (restores from DB, accepts existing ID). Business method: `adjustStock(int delta)` throws `InsufficientStockException` if stock goes negative. Also `updateDetails(name, description, price)`. |
| `ProductNotFoundException.java` | `RuntimeException` — takes `UUID id` |
| `DuplicateSkuException.java` | `RuntimeException` — takes `String sku` |
| `InsufficientStockException.java` | `RuntimeException` — takes `String message` |

**Test to write:** `src/test/java/.../domain/ProductTest.java` — pure JUnit 5, zero Spring, covers: ID generation, stock adjust (positive/negative/below-zero), blank name validation.

---

## Phase 2 — Ports (interfaces only)

**Driving ports** in `application/port/in/`:

| Interface | Methods | Command record |
|-----------|---------|----------------|
| `CreateProductUseCase` | `createProduct(CreateProductCommand)` | `name, sku, description, price, currencyCode` |
| `GetProductUseCase` | `getProduct(UUID)`, `getAllProducts()` | — |
| `UpdateProductUseCase` | `updateProduct(UpdateProductCommand)` | `id, name, description, price, currencyCode` |
| `DeleteProductUseCase` | `deleteProduct(UUID)` | — |
| `AdjustStockUseCase` | `adjustStock(AdjustStockCommand)` | `productId, delta` |

Command objects are nested `record`s inside each interface.

**Driven ports** in `application/port/out/`:

| Interface | Methods |
|-----------|---------|
| `SaveProductPort` | `save(Product)` → `Product` |
| `LoadProductPort` | `loadById(UUID)` → `Optional<Product>`, `loadAll()` → `List<Product>`, `existsBySku(String)` → `boolean` |
| `DeleteProductPort` | `deleteById(UUID)` |

---

## Phase 3 — Application Service

**`application/service/ProductService.java`** — plain Java class, no annotations. Implements all 5 use case interfaces. Constructor-injected with the 3 driven ports. Orchestrates: load → call domain method → save.

**`application/service/ProductServiceConfiguration.java`** — `@Configuration` class (the only Spring annotation in this package) that declares `ProductService` as a `@Bean` wired with the 3 port implementations.

**Test:** `application/service/ProductServiceTest.java` — JUnit 5 + Mockito only (`@ExtendWith(MockitoExtension.class)`). Mocks all 3 ports. Verifies orchestration: duplicate SKU check, not-found throws, stock delegation.

---

## Phase 4 — Persistence Adapter

**Add to `pom.xml`:**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**Create `src/main/resources/db/migration/V1__create_products_table.sql`:**
```sql
CREATE TABLE products (
    id             UUID           NOT NULL,
    name           VARCHAR(255)   NOT NULL,
    sku            VARCHAR(100)   NOT NULL,
    description    TEXT,
    price_amount   NUMERIC(19,4)  NOT NULL,
    price_currency CHAR(3)        NOT NULL,
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT chk_products_price CHECK (price_amount >= 0),
    CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0)
);
```

**Update `application.yaml`:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hexagonal
    username: hexagonal
    password: hexagonal
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
```

**Files in `adapter/out/persistence/`** (all **package-private**):

| File | Key details |
|------|-------------|
| `ProductJpaEntity.java` | `@Entity @Table("products")`, Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`. Money stored as two columns: `priceAmount` + `priceCurrency`. |
| `ProductRepository.java` | `interface ProductRepository extends JpaRepository<ProductJpaEntity, UUID>` — adds `boolean existsBySku(String sku)` |
| `ProductPersistenceMapper.java` | `@Component`. `toJpaEntity(Product)` and `toDomain(ProductJpaEntity)`. The toDomain method calls `Product.reconstitute()`. |
| `ProductPersistenceAdapter.java` | `@Component @RequiredArgsConstructor @Transactional`. Implements `SaveProductPort`, `LoadProductPort`, `DeleteProductPort`. Read methods use `@Transactional(readOnly = true)`. |

**Test:** `adapter/out/persistence/ProductPersistenceAdapterTest.java` — `@DataJpaTest @Import({ProductPersistenceAdapter.class, ProductPersistenceMapper.class})` annotated with `@Import(TestcontainersConfiguration.class)` and `@AutoConfigureTestDatabase(replace = NONE)` to use the real Postgres container. Tests save/load round-trip and `existsBySku`.

---

## Phase 5 — Web Adapter

**Files in `adapter/in/web/`:**

| File | Key details |
|------|-------------|
| `CreateProductRequest.java` | `@Builder record` with Bean Validation: `@NotBlank name`, `@NotBlank @Pattern sku`, `@Positive price`, `@Size(3) currencyCode` |
| `UpdateProductRequest.java` | Same as above but no SKU field |
| `AdjustStockRequest.java` | `record` with `@NotNull Integer delta` |
| `ProductResponse.java` | `@Builder record` with `static ProductResponse from(Product)` factory |
| `GlobalExceptionHandler.java` | `@RestControllerAdvice` mapping domain exceptions to `ProblemDetail` (RFC 9457): `ProductNotFoundException` → 404, `DuplicateSkuException` → 409, `InsufficientStockException` → 422 |
| `ProductController.java` | `@RestController @RequestMapping("/api/products") @RequiredArgsConstructor`. Injects the 5 use case *interfaces* (not `ProductService` directly). Endpoints: `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `PATCH /{id}/stock` |

**Test:** `adapter/in/web/ProductControllerTest.java` — `@WebMvcTest(ProductController.class)`. Uses `@MockitoBean` (Spring Boot 4 replacement for deprecated `@MockBean`) on all 5 use case interfaces. Tests: 201 on create, 400 on missing name, 404 via exception handler.

---

## Phase 6 — Integration Test

**`src/test/java/.../ProductIntegrationTest.java`** — `@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)`. Full stack: HTTP → controller → service → persistence adapter → Postgres container. At minimum: create product, then GET all and assert it appears.

---

## Verification

```bash
# Run all tests (requires Docker for Testcontainers)
./mvnw verify

# Run just the fast unit tests (no Docker needed)
./mvnw test -Dtest="ProductTest,ProductServiceTest"

# Run the app locally with a real Postgres container
./mvnw spring-boot:test-run

# Then exercise the API
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Widget","sku":"WGT-001","price":9.99,"currencyCode":"GBP"}'

curl http://localhost:8080/api/products
```

---

## Critical Files

- `pom.xml` — add Flyway dependency
- `src/main/resources/application.yaml` — add datasource + JPA config
- `src/main/resources/db/migration/V1__create_products_table.sql` — new file
- All domain, port, service, adapter, and test files listed above (all new)
- `src/test/java/.../TestcontainersConfiguration.java` — reuse existing, import into adapter and integration tests
