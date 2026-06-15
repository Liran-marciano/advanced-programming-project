# Computational Graph &mdash; Advanced Programming Project

A browser-driven application that loads a publish/subscribe **computational
graph** from a configuration file, renders it as an interactive SVG
visualization, and lets you publish messages to any topic to watch
values flow through the network in real time.

Built as the final deliverable for the *Advanced Programming* course at
**Bar-Ilan University** (Dr. Eliahu Khalastchi, courses 89-210 / 89-211).
Combines the work of exercises 1&ndash;6 into one runnable system with
zero external dependencies (just the JDK).

![overview](docs/screenshot.png)

---

## 1. Background

The project models the **observer pattern** at scale. *Agents* subscribe
to *topics*; when a `Message` is published to a topic, every subscriber
receives a `callback` and may publish derived values to its own output
topics. The result is a **directed computational graph** that lets
numerical (or textual) data flow through arbitrarily complex pipelines.

The example `chain.conf` shipped with the project, for instance,
computes `Result = ((A + B) + C) + 1` purely by composing two
`PlusAgent`s and one `IncAgent`:

```
A ─┐
   ├─► PlusAgent ──► Sum ─┐
B ─┘                       ├─► PlusAgent ──► Total ──► IncAgent ──► Result
                       C ─┘
```

The six course exercises that culminate in this codebase:

| Ex | Brings to the project |
|---|---|
| 1 | `Message`, `Topic`, `TopicManagerSingleton` &mdash; the pub/sub bus core |
| 2 | `ParallelAgent` &mdash; an *Active Object* decorator giving every agent its own worker thread |
| 3 | `Node`, `Graph`, `Config`, `BinOpAgent` &mdash; runtime topology + cycle detection |
| 4 | `GenericConfig`, `PlusAgent`, `IncAgent` &mdash; reflective loading from a `.conf` file |
| 5 | `RequestParser`, `MyHTTPServer`, `Servlet` &mdash; a homemade HTTP server |
| **6** | **`TopicDisplayer`, `ConfLoader`, `HtmlLoader`, `TopicReset`, `HtmlGraphWriter`, the HTML view, `Main`** &mdash; the browser UI that ties it all together |

This repository represents the state after exercise 6.

---

## 2. Installation

### Prerequisites

- **JDK 17** or newer on the `PATH`. JDK 11 also works (the code uses
  Java 8 syntax with no third-party libraries).
- A modern browser: Chrome, Firefox, Edge or Safari.
- Any OS &mdash; tested on Windows 11 and Linux.

### Clone

```bash
git clone https://github.com/Liran-marciano/advanced-programming-project.git
cd advanced-programming-project
```

That is the only setup step. There is no `pom.xml`, no `build.gradle`,
no `node_modules` &mdash; the project is intentionally dependency-free
to make grading and reading the source easy.

---

## 3. Run commands

### Build

From the repository root:

**Linux / macOS / Git Bash:**

```bash
javac -d out -sourcepath src $(find src -name '*.java')
```

**Windows PowerShell:**

```powershell
javac -d out -sourcepath src (Get-ChildItem -Recurse src -Filter *.java).FullName
```

That single `javac` invocation compiles every class into the `out/`
directory.

### Run

```bash
java -cp out project_biu.Main
```

You should see:

```
Server running on http://localhost:8080/app/index.html
Press Enter to shut down.
```

Open that URL in a browser. To stop the server, switch back to the
terminal and press **Enter**.

### Using the app

The page is split into three columns:

| Column | What it shows |
|---|---|
| **Left** &mdash; `form.html` | Two forms + quick actions: upload a `.conf` file (button *Deploy*), publish a message to a topic, and *Reset values* |
| **Centre** &mdash; the graph view | Topic rectangles (colour-coded by role: input / intermediate / output), agent circles, animated dashed edges, value pills above topics |
| **Right** &mdash; the topic table | Current value of every topic, in tabular form |

**Quick demo (60 seconds):**

1. Click the `chain.conf` chip on the left &mdash; the centre fills
   with a 3-agent graph and the right shows an empty topic table.
2. In the "Publish message" form, send `A` = `10`, then `B` = `5`,
   then `C` = `3`.
3. Watch the values cascade: `Sum = 15`, `Total = 18`, `Result = 19`.
   The centre graph refreshes after every publish so the numbers
   above each topic stay live.
4. Click **Reset values** &mdash; every topic value clears and every
   `PlusAgent`'s internal state resets, but the graph topology stays
   intact (no need to re-upload).

A complete walkthrough lives in [PRESENTATION.pptx](PRESENTATION.pptx) &mdash;
the deck used for the demo video below, with speaker notes for every slide.

---

## 4. Demo video

A 5-minute walk-through is included in the repo at [demo.mp4](demo.mp4)
(download to play; GitHub doesn't auto-play MP4s inline).

Sections of the recording:

1. **Title** &mdash; course, project, submitter
2. **Background** &mdash; the publish/subscribe pattern and what we built on top of it
3. **Design** &mdash; the package structure, SOLID highlights, and the
   homemade HTTP server
4. **Live demo** &mdash; `simple.conf` &rarr; `chain.conf` &rarr;
   publishing &rarr; reset &rarr; SVG animation details
5. **Beyond the minimum** &mdash; pieces we added beyond the PDF spec
6. **Lessons learned** &mdash; what the course taught us

Full slide-by-slide content (with the speaker notes used to record the
voice-over) lives in [PRESENTATION.pptx](PRESENTATION.pptx).

---

## 5. Architecture overview

```
project_biu
├── graph/         Message, Topic, TopicManagerSingleton, Agent,
│                  ParallelAgent
├── configs/       Node, Graph, Config, BinOpAgent, GenericConfig,
│                  PlusAgent, IncAgent
├── server/        RequestParser, HTTPServer, MyHTTPServer
├── servlets/      Servlet, TopicDisplayer, ConfLoader, HtmlLoader,
│                  TopicReset
├── views/         HtmlGraphWriter (loads html_files/graph.html as a
│                  template and injects nodes/edges)
└── Main.java      Wires up MyHTTPServer(8080, 5) and registers the
                   four servlets

html_files/        Static view assets (index.html, form.html,
                   graph.html, temp.html)
config_files/      Sample / uploaded configurations
docs/javadoc/      Generated API documentation
```

### Design highlights (SOLID)

- **Single responsibility.** Each servlet does one thing
  (`TopicDisplayer` publishes + renders the table, `ConfLoader` loads
  the topology, `HtmlLoader` serves static assets, `TopicReset`
  clears values). `HtmlGraphWriter` is the *only* place that builds
  graph HTML, and even it delegates the markup to a static template
  so a designer can iterate without touching Java.
- **Open / closed.** Adding a new agent type means writing one class
  with the `(String[], String[])` constructor and listing it in a
  `.conf`. No core code needs to change.
- **Liskov.** Every `ParallelAgent` is interchangeable with the raw
  `Agent` it wraps; the rest of the system never has to know which
  variant it is talking to.
- **Interface segregation.** `Agent`, `Config`, `Servlet`,
  `HTTPServer` are each four-method interfaces; nothing in the
  system depends on a method it does not call.
- **Dependency inversion.** `MyHTTPServer` depends on the `Servlet`
  *interface*, not on any concrete servlet; `Main` is the only file
  that wires concrete classes to URI paths.

Other notable choices:

- **Active Object pattern.** `ParallelAgent` wraps any `Agent` in a
  worker thread + `ArrayBlockingQueue` so publish/subscribe never
  blocks the publisher.
- **Bill Pugh / lazy-inner-class singleton** for `TopicManager`, so
  no `synchronized` is needed at lookup time and a
  `ConcurrentHashMap` carries the actual storage.
- **Longest-prefix URI routing** in `MyHTTPServer` lets a single
  servlet at `/app/` catch every static asset while `/publish`,
  `/upload`, and `/reset` keep their own servlets.

### Beyond the PDF's minimum requirements

The exercise PDF defines a minimum and explicitly encourages going
past it; the items we added:

1. **Animated, role-coloured graph view.** Topics get different
   gradients based on whether they are inputs (cyan), intermediates
   (blue), or outputs (pink). Edges are curved bezier paths with a
   flowing dash animation, so the dataflow direction is visible
   even when no message is moving. Each value label pulse-animates
   on every update.
2. **A fourth servlet, `TopicReset`.** Lets the user clear every
   topic's last value and every agent's internal state without
   re-uploading the config &mdash; useful during the demo.
3. **In-page example loader.** Two clickable chips
   (`simple.conf`, `chain.conf`) deploy a baked-in config in a
   single click, so a demo never has to fumble with the file
   picker.
4. **Custom locale-proof file picker.** The native HTML file input
   renders its prompt in the browser locale (Hebrew on the
   development machine); we replaced it with a custom-styled
   button + filename label so the UI is always English.
5. **Dynamic canvas sizing.** Long pipelines (e.g. `chain.conf`'s
   seven columns) get a wider SVG so edges stay readable.
6. **404 / error pages styled to match the dark theme.**
7. **JSON-safe embedded graph snapshots.** Every `/publish` and
   `/reset` response includes a `<script>` that updates the centre
   iframe in the parent document, keeping the table and graph in
   sync atomically.

---

## 6. Repository layout

```
.
├── src/                      Java source (mirrors the project_biu
│                             package tree)
├── html_files/               index.html, form.html, graph.html,
│                             temp.html
├── config_files/             simple.conf, chain.conf, and any
│                             uploaded configs
├── tools/                    make_submission.{ps1,sh} -- packages a
│                             "test"-package zip for the per-exercise
│                             autograders (exercises 1-5 only)
├── tests/                    Reserved for JUnit tests
├── lib/                      JUnit jar (only build-time dependency)
├── docs/javadoc/             Generated API documentation
├── PRESENTATION.pptx         Demo-video deck (with speaker notes)
└── README.md                 (this file)
```

---

## 7. Javadoc

The homemade HTTP server is designed to be **reusable** &mdash; another
developer could in principle drop `project_biu.server.*` and
`project_biu.servlets.Servlet` into a new project and stand up a custom
HTTP server with a few lines of code. To make that practical, full
Javadoc is checked into `docs/javadoc/` and can be regenerated with:

```bash
javadoc -d docs/javadoc -sourcepath src -subpackages project_biu \
        -windowtitle "Computational Graph -- API reference"
```

Open `docs/javadoc/index.html` in any browser to browse the API.

### Minimal example of reusing the server

```java
import project_biu.server.*;
import project_biu.server.RequestParser.RequestInfo;
import project_biu.servlets.Servlet;
import java.io.OutputStream;

public class Hello {
    public static void main(String[] args) throws Exception {
        HTTPServer server = new MyHTTPServer(9000, 4);
        server.addServlet("GET", "/hello", new Servlet() {
            public void handle(RequestInfo ri, OutputStream out) throws java.io.IOException {
                String body = "Hello, " + ri.getParameters().getOrDefault("name", "world") + "!";
                String resp = "HTTP/1.1 200 OK\r\nContent-Length: "
                            + body.length() + "\r\n\r\n" + body;
                out.write(resp.getBytes());
            }
            public void close() {}
        });
        server.start();
        System.in.read();
        server.close();
    }
}
```

Then `curl http://localhost:9000/hello?name=Yuval` prints
`Hello, Yuval!`.

---

## 8. Submitter

Submitted by **Liran Marciano** (`liranmar88@gmail.com`).

Course: Advanced Programming, Bar-Ilan University &mdash;
Dr. Eliahu Khalastchi (89-210 / 89-211), Spring 2026.

