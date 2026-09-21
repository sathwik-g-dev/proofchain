# ProofChain

> **Java Project Source Code Analyzer & Skill Scoring Platform**  
> An MCA portfolio project built with Java 21, Spring Boot 3.3, JavaParser AST, Spring Data JPA, MySQL 8, and Vanilla JavaScript.

---

## 1. Project Overview

**ProofChain** is a Java project analysis tool that examines a software project's source code and generates an explainable technical analysis report.

Instead of relying on self-reported, manually entered metrics or inflated resume bullet points, ProofChain inspects the actual Abstract Syntax Tree (AST) of a Java codebase. It automatically detects classes, interfaces, inheritance hierarchies, Stream and Collections usage, Spring Boot components (controllers, services, repositories, entities), REST endpoints, unit tests, and build dependencies.

These detected metrics feed deterministically into ProofChain's **ProofScoringEngine** to generate a transparent **Technical Project Score** (0–100) with an itemized point breakdown and file-level evidence linking each metric directly to contributing source files.

---

## 2. Primary Workflow

```
                  ┌───────────────────────────────────────────────┐
                  │              PROJECT INPUT METHOD             │
                  │   Uploaded ZIP Archive OR Public GitHub URL   │
                  └───────────────────────┬───────────────────────┘
                                          │
                                          ▼
                  ┌───────────────────────────────────────────────┐
                  │           SAFE SANDBOX EXTRACTION             │
                  │   • Path traversal (Zip Slip) protection     │
                  │   • Zip Bomb limits (size & entry bounds)    │
                  │   • Strictly never executes uploaded code     │
                  └───────────────────────┬───────────────────────┘
                                          │
                                          ▼
                  ┌───────────────────────────────────────────────┐
                  │             AST & BUILD ANALYZER              │
                  │   • JavaParser AST: syntax, classes, methods  │
                  │   • Spring Boot & REST route detection        │
                  │   • JUnit test methods & Mockito detection    │
                  │   • Maven/Gradle build dependency inspection  │
                  └───────────────────────┬───────────────────────┘
                                          │
                                          ▼
                  ┌───────────────────────────────────────────────┐
                  │         EXPLAINABLE SCORING ENGINE            │
                  │   • Deterministic rule evaluation             │
                  │   • Transparent point formula rationale       │
                  │   • File-level evidence attribution           │
                  └───────────────────────┬───────────────────────┘
                                          │
                                          ▼
                  ┌───────────────────────────────────────────────┐
                  │            TECHNICAL PROJECT REPORT           │
                  │   • Overall Technical Project Score           │
                  │   • Code Statistics Grid & Tech Summary       │
                  │   • Expandable File-Level Evidence list       │
                  └───────────────────────────────────────────────┘
```

---

## 3. Technology Stack

- **Backend**:
  - **Java 21**: Core language runtime (records, pattern matching, Stream API).
  - **Spring Boot 3.3.3**: Framework foundation (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`).
  - **JavaParser (`javaparser-core:3.26.2`)**: Real Abstract Syntax Tree (AST) parser for Java source code inspection. Deterministic and rule-based; no AI or external LLM dependencies.
  - **Spring Security Crypto**: `BCryptPasswordEncoder` for one-way password hashing.
  - **Hibernate 6**: Relational ORM mapping entities and derived queries.
- **Database**:
  - **MySQL 8.0**: Relational persistence with foreign keys and composite indexes.
- **Frontend**:
  - **HTML5 & Vanilla CSS**: Responsive dark-mode single-page application ("Nordic Slate & Warm Ochre" design system). Fully responsive from mobile viewports (360px+) to desktop.
  - **Vanilla JavaScript (ES6+)**: Drag-and-drop file upload, Fetch API, dynamic DOM updates, and non-blocking toast notifications.
- **Testing**:
  - **JUnit 5 & Mockito**: Comprehensive test suite (32 unit, service-mock, and application context tests).
- **Build & Dependency Management**:
  - **Apache Maven**: Dependency resolution and build lifecycle management.

---

## 4. Architecture & Module Design

The application follows a clean, layered Spring Boot architecture:

```
                            ┌────────────────────────────────────┐
                            │      Single-Page Web Interface     │
                            │      HTML5 · CSS · Vanilla JS      │
                            └─────────────────┬──────────────────┘
                                              │ HTTP / Multipart / JSON
                                              ▼
                            ┌────────────────────────────────────┐
                            │       REST Controller Layer        │
                            │  7 RestControllers, 25 Endpoints   │
                            └─────────────────┬──────────────────┘
                                              │ DTOs & Validation
                                              ▼
                            ┌────────────────────────────────────┐
                            │    Analysis & Service Engine       │
                            │  • ProjectAnalysisService          │
                            │  • JavaSourceAnalyzer (AST)        │
                            │  • SpringBootAnalyzer              │
                            │  • TestAnalyzer & BuildAnalyzer    │
                            │  • ZipProjectExtractor (Safe)      │
                            │  • GitHubProjectFetcher            │
                            │  • ProofScoringEngine              │
                            └─────────────────┬──────────────────┘
                                              │ JPA Entities
                                              ▼
                            ┌────────────────────────────────────┐
                            │          Repository Layer          │
                            │      5 Spring Data JPA Repos       │
                            └─────────────────┬──────────────────┘
                                              │ Hibernate SQL / JPQL
                                              ▼
                            ┌────────────────────────────────────┐
                            │           MySQL Database           │
                            │    6 Normalized Database Tables    │
                            └────────────────────────────────────┘
```

---

## 5. Key Analyzer Capabilities

### 1. Java & OOP Analysis
- Detects Java source files, concrete classes, interfaces, enums, and records.
- Inspects method declarations, constructors, and field definitions.
- Identifies inheritance hierarchies (`extends`) and interface implementations (`implements`).
- Identifies Collections Framework usage (`List`, `Set`, `Map`, `Queue`) and Stream API pipelines (`.stream()`, `.filter()`, `.map()`, `.collect()`).
- Detects custom exception hierarchies and structured `try-catch` blocks.

### 2. Spring Boot & Architecture Analysis
- Detects `@SpringBootApplication` roots.
- Identifies REST Controllers (`@RestController`, `@Controller`).
- Maps HTTP request endpoints (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`, `@RequestMapping`).
- Discovers transactional business service layer components (`@Service`).
- Detects persistence repositories (`@Repository`, `JpaRepository`).
- Inspects database entities (`@Entity`, `@Table`), relational associations (`@ManyToOne`, `@OneToMany`, `@ManyToMany`), and database indexes (`@Index`).
- Identifies input validation annotations (`@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`).
- Detects centralized error translation (`@RestControllerAdvice`, `@ExceptionHandler`).
- Recognizes Request/Response DTO patterns separating API contracts from entities.

### 3. Testing Analysis
- Discovers test source files and test classes.
- Counts executable JUnit test methods (`@Test`, `@ParameterizedTest`).
- Detects assertion statements (`assertEquals`, `assertTrue`, `assertThat`, etc.).
- Identifies Mockito dependency mocking (`@Mock`, `@InjectMocks`, `Mockito.when`, `verify`).

### 4. Build & Dependency Inspection
- Parses `pom.xml` and `build.gradle` build descriptors.
- Detects target Java versions (e.g., Java 21, 17, 11).
- Detects framework versions (e.g., Spring Boot 3.3.3) and active library dependencies (MySQL, Spring Data JPA, Spring Security, Validation, JUnit 5, Mockito, Lombok).

### 5. File-Level Evidence Attribution
- Every detected metric preserves the names of the specific source files that contributed to it (e.g., REST Controllers $\rightarrow$ `UserController.java`, `ProjectController.java`).
- The web interface presents an interactive accordion allowing users to expand each metric to see contributing files.

---

## 6. Security Considerations

1. **Untrusted Code Execution Guarantee**:
   - ProofChain **strictly never executes** uploaded projects or retrieved repositories.
   - It does NOT run Maven, Gradle, shell commands, unit test runners, or uploaded `.class`/`.jar`/`.exe` binaries.
   - Code is treated exclusively as static text files parsed into an AST.
2. **Path Traversal Protection (Zip Slip)**:
   - Every file entry in a ZIP archive is normalized and verified against the canonical target path (`destinationPath.startsWith(canonicalTarget)`). Any entry containing `../` or attempting to escape the temporary sandbox throws a `SecurityException` and aborts extraction.
3. **Zip Bomb Protection**:
   - Enforces a maximum file entry count (5,000 files) and a maximum uncompressed byte limit (50 MB) to prevent resource exhaustion attacks.
4. **Temporary Directory Lifecycle**:
   - Extracted projects reside in isolated temporary directories created via `Files.createTempDirectory`.
   - Cleanup is strictly guaranteed inside `finally` blocks, removing all extracted files immediately upon analysis completion.
5. **Ignored Directories**:
   - Irrelevant and generated directories (`.git`, `target`, `build`, `node_modules`, `.idea`, `.vscode`, `.mvn`, `.gradle`) and compiled binary formats (`.class`, `.jar`, `.war`, `.exe`) are bypassed during extraction and AST parsing.
6. **Public GitHub Repositories Only**:
   - GitHub retrieval is restricted to public repository zipball downloads without storing access tokens in code.

---

## 7. REST API Endpoints

### Project Analysis & Upload
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/projects/analyze/upload` | Upload and analyze a project ZIP file (multipart form data) |
| `POST` | `/api/projects/analyze/github` | Fetch and analyze a public GitHub repository (JSON request) |
| `GET` | `/api/projects/analyze/{projectId}` | Retrieve the comprehensive analysis report for a project |

### Project Management
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/projects` | List all registered projects |
| `GET` | `/api/projects/{id}` | Get project details by ID |
| `DELETE` | `/api/projects/{id}` | Permanently delete project and its associated metrics/scores |
| `POST` | `/api/projects` | Register project metadata manually |

### Scoring & Reports
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/projects/{id}/report` | Generate technical report with overall score and skill breakdowns |
| `POST` | `/api/projects/{id}/analyze` | Recalculate proof score based on existing evidence |
| `GET` | `/api/projects/{id}/scores` | Get skill scores for a project |

### Dashboard & Analytics
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/dashboard/{userId}` | Get aggregate statistics, verified skills, and project portfolio |
| `GET` | `/api/skills` | List all registered competency skills |

---

## 8. Running the Application Locally

### Prerequisites
- **Java Development Kit (JDK) 21** or higher
- **MySQL Server 8.0+** running on `localhost:3306`

### Database Setup
```sql
CREATE DATABASE IF NOT EXISTS proofchain;
```

### Environment Configuration (Optional)
The application provides sensible local defaults in `application.properties`:
```properties
DB_URL=jdbc:mysql://localhost:3306/proofchain?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=Root
```
Override any value using standard environment variables if needed:
```bash
export DB_USERNAME=myuser
export DB_PASSWORD=mypassword
```

### Build & Run
```bash
# Clone the repository
git clone https://github.com/username/proofchain.git
cd proofchain

# Run all unit and integration tests (32 tests)
./mvnw clean test

# Start the Spring Boot application
./mvnw spring-boot:run
```

Access the web application in your browser at:  
👉 **`http://localhost:8080/`**

---

## 9. Testing & Quality Assurance

The project contains **32 comprehensive automated tests** run via Maven Surefire:

```text
Results:
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- **`ProofchainApplicationTests`**: Spring Boot application context initialization.
- **`JavaSourceAnalyzerTest`**: Verifies AST class/interface detection, inheritance, Collections, Stream API, exceptions, and malformed syntax recovery.
- **`SpringBootAnalyzerTest`**: Verifies controller, REST endpoint, service, repository, entity, and transaction detection.
- **`TestAnalyzerTest`**: Verifies JUnit `@Test` methods, test classes, assertions, and Mockito mocking detection.
- **`BuildFileAnalyzerTest`**: Verifies Maven `pom.xml` and Gradle `build.gradle` dependency parsing.
- **`ZipProjectExtractorTest`**: Verifies valid ZIP extraction, Zip Slip path traversal rejection (`../`), and empty ZIP handling.
- **`GitHubProjectFetcherTest`**: Verifies GitHub URL validation and owner/repo extraction.
- **`ProjectAnalysisServiceTest`**: End-to-end orchestration tests for Java and non-Java ZIP archives.
- **`EvidenceServiceTest`**, **`ProjectServiceTest`**, **`ProofScoringEngineTest`**, **`UserServiceTest`**: Service and rule-engine unit/mock tests.

---

## 10. Honest Limitations & Design Boundaries

ProofChain is designed as a realistic, explainable developer portfolio tool. In adherence to honest engineering principles:

1. **Static Source Analysis**: The analyzer examines source code syntax and structural characteristics. It does NOT compile or execute code, measure runtime performance, or evaluate algorithmic time/space complexity.
2. **Indicator Score, Not Quality Guarantee**: The **Technical Project Score** reflects detected technical characteristics and patterns. It does not certify that the application is production-ready or free from functional bugs.
3. **Language Scope**: AST analysis is tailored for Java projects (with Spring Boot, JPA, JUnit, Maven, and Gradle). Non-Java projects or unsupported languages will be reported truthfully as lacking supported Java source code rather than generating fabricated scores.
4. **Public GitHub Repositories**: The current GitHub fetcher supports public repositories. Private repository OAuth authentication is intentionally omitted to avoid credential complexity in a portfolio scope.
5. **No AI / Generative Hallucinations**: Scoring is 100% deterministic and rule-based. Scores can be reproduced and explained down to individual detected code elements.
