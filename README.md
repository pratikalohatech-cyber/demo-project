# HR Demo — Legacy Java Stack + JCR Reference Project

A small, deliberately-simple project wiring together every technology named in the
JD: **Servlets, Filters, JSP, Ant, Tomcat, Apache Jackrabbit (JCR)**, plus
**Fabric.js, GSAP, and jQuery/$.ajax()** on the frontend.

This was written and reviewed for correctness, but **not compiled or run** —
my sandbox has no internet access, so I couldn't download the actual
Jackrabbit/Servlet-API jars or a Tomcat install to test it end to end.
Treat this as a reference to read, trace through, and run yourself locally —
not something to trust blindly. Expect to fix a small dependency-version
issue or two when you actually build it; that's normal and honestly good
practice for the "navigate an unfamiliar Java project" skill this whole
prep has been about.

## What maps to what (for interview review)

| Interview topic | File |
|---|---|
| Servlet lifecycle (init/service/destroy), doGet/doPost | `src/com/hrdemo/servlet/EmployeeServlet.java` |
| Filter chain, cross-cutting concerns | `src/com/hrdemo/filter/LoggingFilter.java` |
| Filter short-circuiting (auth) | `src/com/hrdemo/filter/AuthFilter.java` |
| web.xml — servlet mappings, filter order, session config | `web/WEB-INF/web.xml` |
| Ant build lifecycle — targets, WAR packaging, Tomcat deploy | `build.xml` |
| JSP (incl. the old scriptlet pattern, shown deliberately) | `web/index.jsp` |
| JCR repository startup/shutdown lifecycle | `src/com/hrdemo/jcr/RepositoryStartupListener.java` |
| JCR node hierarchy, versioning (checkin/checkout), binaries, locking, JCR-SQL2 queries | `src/com/hrdemo/jcr/DocumentRepository.java` |
| Servlet bridging HTTP → JCR operations | `src/com/hrdemo/servlet/DocumentServlet.java` |
| Fabric.js object model, custom data, `object:modified` event | `web/js/shift-planner.js` |
| GSAP tweening a non-DOM (Fabric) object + `requestRenderAll()` | `web/js/shift-planner.js` |
| jQuery `$.ajax()` — vanilla REST, manual DOM update in callback | `web/js/shift-planner.js` |
| Thread-safety on shared servlet instance state | `EmployeeServlet.java` (synchronized block), `ShiftServlet.java` (ConcurrentHashMap) |

## How to actually get this running (do this yourself to really learn it)

### 1. Install prerequisites
- JDK 8 or 11
- Apache Ant (`apt install ant` / `brew install ant`)
- Apache Tomcat 9 (download from tomcat.apache.org, unzip anywhere)

### 2. Get the dependency jars
This project needs jars that aren't included here (no internet in my
sandbox to fetch them). Download these into the `lib/` folder:

- **Jackrabbit** (JCR reference implementation) — get `jackrabbit-core`
  and its transitive dependencies. Easiest path: create a throwaway Maven
  project with this dependency and copy the resolved jars out:
  ```xml
  <dependency>
      <groupId>org.apache.jackrabbit</groupId>
      <artifactId>jackrabbit-core</artifactId>
      <version>2.21.24</version>
  </dependency>
  ```
- **Servlet API** — don't bundle this in `lib/`; it's provided by Tomcat
  itself at `$TOMCAT_HOME/lib/servlet-api.jar`. The `build.xml` already
  points at `${tomcat.home}/lib` for this — just set that property.

### 3. Configure build.xml
Edit the top of `build.xml`:
```xml
<property name="tomcat.home" value="/path/to/your/tomcat" />
<property name="tomcat.webapps" value="/path/to/your/tomcat/webapps" />
```

### 4. Build and deploy
```bash
ant clean deploy
```
This compiles, packages a WAR, and copies it into Tomcat's `webapps/`
folder. Start Tomcat (`$TOMCAT_HOME/bin/startup.sh`) and it'll auto-expand
and deploy the WAR.

### 5. Try it
- `http://localhost:8080/hr-demo/` — landing page with usage examples
- `http://localhost:8080/hr-demo/shift-planner.jsp` — drag a shift block,
  watch GSAP animate it into place, check your browser's Network tab to see
  the `$.ajax()` POST firing
- `curl http://localhost:8080/hr-demo/employee` — plain servlet, JSON out
- Document/JCR endpoints need the demo auth header:
  ```bash
  curl -X POST "http://localhost:8080/hr-demo/document?action=save&employeeId=E001&docName=cert.txt&docType=certification&content=Hello&uploadedBy=admin" \
       -H "X-Demo-Auth-Token: demo-secret-token"

  curl "http://localhost:8080/hr-demo/document?action=history&employeeId=E001&docName=cert.txt" \
       -H "X-Demo-Auth-Token: demo-secret-token"
  ```
  Save the same `docName` twice with different `content` and check
  `action=history` — you should see two versions.

## What's deliberately left out (kept the demo focused)

- **Hibernate/PostgreSQL** — `Employee` is an in-memory map here, not a
  real entity, to avoid requiring a database just to read this. The
  interview prep's Hibernate section stands on its own — this project is
  about the Servlet/JCR/frontend wiring specifically.
- **Keycloak/SSO** — `AuthFilter` uses a hardcoded demo token instead of
  real token validation, to keep the auth flow's *shape* (filter blocks
  unauthenticated requests before they reach the servlet) visible without
  requiring a running Keycloak instance.
- **CKEditor** — not wired in; conceptually it would just be another
  content source feeding into `DocumentServlet`'s `save` action as the
  `content` parameter.

## A note on being honest in the interview about this

If asked whether you've built something like this yourself: this project
was assembled as study material to see the pieces work together — say
that plainly if asked directly. What's genuinely valuable to take from
it: actually running it, breaking it on purpose (comment out
`chain.doFilter()` and watch every request die; remove `mix:versionable`
and watch `getVersionHistory` fail), and fixing the dependency setup
yourself. That hands-on debugging is what will actually make the JCR and
Servlet answers in your prep doc sound like lived experience rather than
memorized theory.
