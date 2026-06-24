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
│       ├── Agent.java                     # Agent functional interface
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
- **`MyHTTPServer`**: A concurrent HTTP server utilizing an `ExecutorService` thread pool to dispatch client handlers asynchronously.
- **`RequestParser`**: Processes HTTP request headers, extracts URL parameters, parses multipart form data (for `.conf` file uploads), and segments URIs.
- **Servlets**:
  - **`HtmlLoader`**: Resolves static HTML file requests under `/app/`.
  - **`ConfLoader`**: Stores uploaded configurations, instantiates reflection-based agents via `GenericConfig`, builds the graph, and outputs visualization HTML.
  - **`TopicDisplayer`**: Publishes user messages, outputs the dynamic HTML table of current topic values, and orchestrates live dashboard reloading.

### 2. The Model Layer (`graph`, `configs`)
- **Pub-Sub Graph Engine**:
  - **`Topic`**: Tracks registered `Agent` subscribers and publishes values.
  - **`TopicManagerSingleton`**: A thread-safe catalog storing all active channels.
  - **`ParallelAgent`**: Wraps any standard `Agent` implementation with an internal thread-safe `BlockingQueue` and a dedicated worker thread, allowing asynchronous execution of callback calculations without blocking HTTP requests.
- **Configuration & Graph Building**:
  - **`GenericConfig`**: Uses dynamic ClassLoader loading and Java reflection to construct agents on-the-fly from file schemas.
  - **`Graph`**: Generates a bipartite representation showing data flow paths (`Topic` -> `Agent` -> `Topic`) and performs DFS cycle detection.

### 3. The View Layer (`views`, `html_files`)
- **`HtmlGraphWriter`**: Dynamically binds the current graph configuration and active topic values into the Vis.js network visualizer template.
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
You can build the project inside your IDE (e.g., IntelliJ IDEA):
1. Import the project root directory.
2. Ensure JDK 17 is configured under Project Settings.
3. Choose **Build** -> **Rebuild Project** to compile all modules.

### 2. Running the Server
Run the `Main` class. The server will boot and begin listening for connections:
```text
[HTTP Server started on port 8080]
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

## Submission Deliverables
- **`link.txt`**: Contains the Git URL and registration information for the project authors.
- **`demo_video.mp4`**: A video demonstration showing:
  - System architecture slides
  - Live demo of file deployment and dynamic calculations
  - SOLID principles recap and key concepts learned.
- **`Javadoc`**: Full API reference documentation (can be generated using `javadoc -d doc -sourcepath ...`).
