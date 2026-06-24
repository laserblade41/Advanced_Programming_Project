# Computational Graph Dashboard

An advanced, multi-threaded Java-based HTTP Server and computational graph dashboard developed as part of the Advanced Programming course (Exercise 6). 

This system allows you to upload network configurations dynamically, trace execution nodes (Topics and Agents), publish message updates, and witness the graph recalculating in real-time on a beautifully designed dark-mode dashboard interface.

---

## Project Structure

Below is the verified structural overview of the project root and its components:

```text
Project_Root/
├── config_files/
│   └── simple.conf                        # Sample configuration files for testing agent pipelines
├── html_files/
│   ├── index.html                         # Multi-frame dashboard layout (Control Panel, Visualization, Values Monitor)
│   ├── form.html                          # Control Panel form to deploy configurations and publish values
│   ├── graph.html                         # Template script for vis.js visualization
│   └── temp.html                          # Awaiting-action placeholder page
├── configs/
│   └── src/configs/
│       ├── BinOpAgent.java                # Binary operation agent (supports add, sub, mul, div)
│       ├── Config.java                    # Configuration loader interface
│       ├── GenericConfig.java             # Dynamic reflection-based configuration loader
│       ├── Graph.java                     # Graph model built from topics and agents (supports cycle detection)
│       ├── IncAgent.java                  # Increment agent (output = input + 1)
│       ├── MathExampleConfig.java         # Example configuration loader
│       ├── Node.java                      # Graph node representation
│       └── PlusAgent.java                 # Addition agent (output = inputA + inputB)
├── graph/
│   └── src/graph/
│       ├── Agent.java                     # Agent functional interface
│       ├── Message.java                   # Wrapper for message values (string, double)
│       ├── ParallelAgent.java             # Decorator for run-time asynchronous agent processing
│       ├── Topic.java                     # Pub-sub subject holding topic subscribers and publishers
│       └── TopicManagerSingleton.java     # Singleton directory manager for topics
├── server/
│   └── src/server/
│       ├── HTTPServer.java                # HTTP server interface definition
│       ├── MyHTTPServer.java              # Thread-pooled socket server implementation
│       └── RequestParser.java             # Parsing engine for HTTP requests, query parameters, and multipart forms
├── servlets/
│   └── src/servlets/
│       ├── Servlet.java                   # Handler interface for HTTP requests
│       ├── ConfLoader.java                # Servlet managing config file uploads (POST) and graph lookups (GET)
│       ├── HtmlLoader.java                # Servlet serving static resources from the template directory
│       └── TopicDisplayer.java            # Servlet managing message publication (GET) and values table updates
├── views/
│   └── src/views/
│       └── HtmlGraphWriter.java           # Graph visualization HTML builder injecting live datasets
├── doc/
│   └── api/                               # Generated Javadoc HTML (open index.html in a browser)
│       ├── index.html                     # API overview — entry point for all packages
│       ├── configs/                       # Config, GenericConfig, Graph, agents, etc.
│       ├── graph/                         # Agent, Topic, Message, ParallelAgent, etc.
│       ├── server/                        # HTTPServer, MyHTTPServer, RequestParser
│       ├── servlets/                      # Servlet, ConfLoader, HtmlLoader, TopicDisplayer
│       └── views/                         # HtmlGraphWriter
├── Main.java                              # Project entry-point boots server and registers routes
├── Advanced_Programming_Project.iml       # IntelliJ project metadata
├── README.md                              # Project documentation
├── link.txt                               # Git repository and author submission details
└── demo_video.mp4                         # Live video presentation of the system
```

---

## System Architecture & Design

The application is structured strictly according to the **Model-View-Controller (MVC)** paradigm:

### 1. The Controller Layer (`server`, `servlets`)

- `**MyHTTPServer`**: A concurrent HTTP server utilizing an `ExecutorService` thread pool to dispatch client handlers asynchronously.
- `**RequestParser**`: Processes HTTP request headers, extracts URL parameters, parses multipart form data (for `.conf` file uploads), and segments URIs.
- **Servlets**:
  - `**HtmlLoader`**: Resolves static HTML file requests under `/app/`.
  - `**ConfLoader**`: Stores uploaded configurations, instantiates reflection-based agents via `GenericConfig`, builds the graph, and outputs visualization HTML.
  - `**TopicDisplayer**`: Publishes user messages, outputs the dynamic HTML table of current topic values, and orchestrates live dashboard reloading.

### 2. The Model Layer (`graph`, `configs`)

- **Pub-Sub Graph Engine**:
  - `**Topic`**: Tracks registered `Agent` subscribers and publishes values.
  - `**TopicManagerSingleton**`: A thread-safe catalog storing all active channels.
  - `**ParallelAgent**`: Wraps any standard `Agent` implementation with an internal thread-safe `BlockingQueue` and a dedicated worker thread, allowing asynchronous execution of callback calculations without blocking HTTP requests.
- **Configuration & Graph Building**:
  - `**GenericConfig`**: Uses dynamic ClassLoader loading and Java reflection to construct agents on-the-fly from file schemas.
  - `**Graph**`: Generates a bipartite representation showing data flow paths (`Topic` -> `Agent` -> `Topic`) and performs DFS cycle detection.

### 3. The View Layer (`views`, `html_files`)

- `**HtmlGraphWriter**`: Dynamically binds the current graph configuration and active topic values into the Vis.js network visualizer template.
- **Dashboard UI**:
  - HTML frames are configured to form a responsive panel dashboard.
  - Custom dark-theme styling, glassmorphism overlays, animated gradients, and interactive hover feedback are applied to deliver a modern visual experience.

---

## SOLID Principles Applied

- **Single Responsibility (SRP)**: Each class has one focus. For example, `RequestParser` handles incoming request bytes, `HtmlGraphWriter` constructs the visualization string, and `ConfLoader` controls application setup.
- **Open/Closed (OCP)**: New agents (e.g., custom processors) can be added simply by implementing the `Agent` interface without changing core graph building logic.
- **Liskov Substitution (LSP)**: `ParallelAgent` implements `Agent` and can decorate any underlying agent subtype seamlessly.
- **Interface Segregation (ISP)**: Clear interfaces like `Servlet`, `Agent`, `Config`, and `HTTPServer` keep system modules decoupled.
- **Dependency Inversion (DIP)**: High-level servlet modules communicate with `HTTPServer` and `Agent` abstractions rather than tight concrete configurations.

---

## Installation & Execution Guide

### Prerequisites

- Java Development Kit (JDK) 17 or higher.
- A modern web browser.

### 1. Building the Project

#### Option A: Using an IDE (e.g., IntelliJ IDEA)

1. Import the project root directory as a new project.
2. Open **File** -> **Project Structure** -> **Project** and set the SDK to **JDK 17** (or higher).
3. The IDE will automatically detect the modules and configure the classpaths.
4. Select **Build** -> **Rebuild Project** to compile all modules.

#### Option B: Using the CLI (Command Line Interface)

Open a terminal in the project root directory and run the compilation commands.

- **On Windows (PowerShell)**:
  ```powershell
  # Create the output directory
  mkdir -Force out/production/Advanced_Programming_Project

  # Find and compile all Java source files
  Get-ChildItem -Recurse -Filter *.java | Select-Object -ExpandProperty FullName | Out-File -FilePath java_sources.txt -Encoding utf8
  javac -d out/production/Advanced_Programming_Project -sourcepath ".;configs/src;graph/src;server/src;servlets/src;views/src" $java_sources.txt
  Remove-Item java_sources.txt
  ```
- **On Linux / macOS (Bash)**:
  ```bash
  # Create the output directory
  mkdir -p out/production/Advanced_Programming_Project

  # Find and compile all Java source files
  find . -name "*.java" > java_sources.txt
  javac -d out/production/Advanced_Programming_Project -sourcepath ".:configs/src:graph/src:server/src:servlets/src:views/src" @java_sources.txt
  rm java_sources.txt
  ```

### 2. Running the Server

#### Option A: Using an IDE

- Right-click [Main.java](file:///c:/Users/USER/Documents/uni/year5/advanced%20programming/Advanced_Programming_Project/Main.java) and select **Run 'Main.main()'**.

#### Option B: Using the CLI

Run the following command from the project root directory:

```bash
java -cp "out/production/Advanced_Programming_Project" Main
```

### 3. Accessing the Dashboard

Open your browser and navigate to:

```text
http://localhost:8080/app/index.html
```

### 4. Step-by-Step Demo Guide

1. **Load Configuration**: In the **Config Deployer** section on the left panel, select a configuration file (e.g., `config_files/simple.conf`) and click **Deploy**. The central panel will render your computational graph structure.
2. **Publish values**: In the **Topic Publisher** section, enter a topic name (e.g. `A`) and a value (e.g. `5.0`), then click **Send**.
3. **Verify computation**:
  - The **Topic Values** panel on the right will update immediately.
  - The graph visualizer will dynamically reflect the new values below the topic node names in brackets (e.g. `A [ 5.0 ]`).
  - If downstream agents are linked (e.g., `PlusAgent` summing `A` and `B` to `C`), their values will be computed asynchronously, and both the graph labels and values table will automatically reload to show the recalculated values.

---

## API Documentation (Javadoc)

The project includes generated HTML API reference documentation under [`doc/api/`](doc/api/). It documents **22 public types** across five packages.

### Viewing the docs

Open [`doc/api/index.html`](doc/api/index.html) in a web browser (file URL or IDE preview). No HTTP server is required; all assets are self-contained under `doc/api/`.

| Package | Description |
|---------|-------------|
| `configs` | Configuration loaders, graph model, and built-in agents (`PlusAgent`, `IncAgent`, `BinOpAgent`, etc.) |
| `graph` | Pub-sub engine: topics, messages, agents, and asynchronous `ParallelAgent` wrapper |
| `server` | Embeddable HTTP server (`HTTPServer`, `MyHTTPServer`) and `RequestParser` |
| `servlets` | Request handler interface and dashboard servlets (`ConfLoader`, `HtmlLoader`, `TopicDisplayer`) |
| `views` | HTML graph visualization builder (`HtmlGraphWriter`) |

### Regenerating the docs

Compile the project first (see **Building the Project** above). Javadoc requires compiled classes on the classpath at `out/production/Advanced_Programming_Project`.

**On Windows (PowerShell):**

```powershell
javadoc -d doc/api `
  -sourcepath "server/src;servlets/src;configs/src;graph/src;views/src" `
  -subpackages server:servlets:configs:graph:views `
  -classpath "out/production/Advanced_Programming_Project"
```

**On Linux / macOS (Bash):**

```bash
javadoc -d doc/api \
  -sourcepath "server/src:servlets/src:configs/src:graph/src:views/src" \
  -subpackages server:servlets:configs:graph:views \
  -classpath "out/production/Advanced_Programming_Project"
```

If `javadoc` is not on your PATH (common on Windows), use the full JDK path, for example:

```powershell
& "C:\Program Files\Java\jdk-24\bin\javadoc.exe" -d doc/api `
  -sourcepath "server/src;servlets/src;configs/src;graph/src;views/src" `
  -subpackages server:servlets:configs:graph:views `
  -classpath "out/production/Advanced_Programming_Project"
```

---

## Submission Deliverables

- `**link.txt`**: Contains the Git URL and registration information for the project.
- `**demo_video.mp4**`: A video demonstration showing:
  - System architecture slides
  - Live demo of file deployment and dynamic calculations
- **`doc/api/`**: Pre-generated Javadoc HTML for the full public API (`configs`, `graph`, `server`, `servlets`, `views`). Entry point: [`doc/api/index.html`](doc/api/index.html). Regenerate using the commands in the **API Documentation (Javadoc)** section above.

