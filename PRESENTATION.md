# Demo video script — Computational Graph project

A slide-by-slide outline for the **5-minute** demo recording required by
Ex 6. Total target runtime: **4:45**, leaving 15&nbsp;s of buffer.

Recommended setup:

- Use **OBS Studio** or **Loom** for screen + face-cam recording.
- Open two windows side-by-side before you start:
  1. Your browser pointed at `http://localhost:8080/app/index.html`
  2. A code editor showing the project tree
- Have the server running and `chain.conf` ready to deploy.
- Practise once with the script before the real take &mdash; this script
  is paced for about **120 words per minute**, which is comfortable.

---

## Slide 1 &mdash; Title (00:00 &rarr; 00:20)

> ### **Computational Graph**
> ### Advanced Programming &middot; Bar-Ilan University
> Course 89-210 / 89-211, Dr. Eliahu Khalastchi
>
> **Submitter:** Liran Marciano
> *liranmar88@gmail.com*
>
> Repository: github.com/Liran-marciano/advanced-programming-project

**What to say** (~15&nbsp;s):

> "Hi, I am Liran Marciano. This is my final project for the Advanced
> Programming course taught by Dr. Khalastchi at Bar-Ilan. The project
> brings together the six exercises we built over the semester into one
> live, browser-driven system for running publish/subscribe computational
> graphs."

---

## Slide 2 &mdash; Background (00:20 &rarr; 01:00)

> ### The problem
>
> - **Publish/subscribe:** topics are channels, agents subscribe to or
>   publish on them.
> - **Computational graph:** chains of agents form pipelines that
>   propagate values.
> - Example: `Result = (A + B + C) + 1` &mdash; two adders feeding an
>   incrementer.
>
> ### Goal of exercise 6
>
> Expose this pipeline through a **browser UI** served by a homemade
> HTTP server.

**What to say** (~35&nbsp;s):

> "The core idea is the observer pattern at scale. Agents subscribe to
> topics; when a message is published, every subscriber gets a callback
> and may publish derived values to its own outputs. The result is a
> directed graph that propagates data automatically.
>
> The first five exercises gave us the message bus, parallel agents,
> the graph representation, a generic configuration loader, and a
> custom HTTP server. Exercise six is the view layer &mdash; the
> browser interface that turns the abstract topology into something
> a user can play with."

---

## Slide 3 &mdash; Design (01:00 &rarr; 01:50)

> ### Architecture
>
> ```
> project_biu
> ├── graph/     Message, Topic, ParallelAgent (Active Object)
> ├── configs/   Graph, GenericConfig, agents
> ├── server/    Homemade HTTP server (Ex 5)
> ├── servlets/  TopicDisplayer, ConfLoader, HtmlLoader, TopicReset
> ├── views/     HtmlGraphWriter (template + injection)
> └── Main.java  Wires four servlets to four URIs
> ```
>
> ### Patterns applied
>
> - **Active Object** &mdash; ParallelAgent
> - **Singleton (Bill Pugh)** &mdash; TopicManager
> - **Strategy via lambdas** &mdash; BinOpAgent
> - **Template-method view** &mdash; HtmlGraphWriter loads a static
>   `graph.html`, injects SVG fragments
> - **Longest-prefix URI routing** in the server

**What to say** (~45&nbsp;s):

> "The code is organised into six packages. The `graph` package holds
> the pub/sub core; the `configs` package describes the runtime
> topology; the `server` package is the homemade HTTP server we built
> in exercise five; the `servlets` package implements four request
> handlers; and the `views` package is responsible for turning a
> graph into HTML.
>
> Two design choices are worth pointing out. First, **the view layer
> never builds HTML inline.** It loads a static `graph.html` template
> and injects SVG fragments &mdash; so a designer can iterate on the
> visuals without recompiling Java. Second, every agent that comes
> through `GenericConfig` is wrapped in a `ParallelAgent`, so each
> agent runs on its own thread and the publish/subscribe layer is
> never blocked by slow work."

---

## Slide 4 &mdash; Live demo (01:50 &rarr; 04:00)

**Switch to the browser, then return to slides for the wrap-up.**

### Demo flow (2 minutes, 10 seconds)

#### Step 1 (~20&nbsp;s) &mdash; **Start fresh, deploy `simple.conf`**

- Show the empty UI: three columns, header with the "localhost:8080"
  badge pulsing, spinner placeholders.
- Click the **`simple.conf` chip** &mdash; the centre fills with a
  4-topic / 2-agent graph; the right panel auto-shows an empty topic
  table.

> "I click the simple.conf chip. The centre panel renders the
> graph: A and B are cyan input rectangles, the gold circle is
> PlusAgent, C is a blue intermediate topic, and D is the pink
> output. The right panel auto-shows the topic table."

#### Step 2 (~25&nbsp;s) &mdash; **Publish and watch values flow**

- Publish `A` = `7`, then `B` = `3` from the form.
- Point out the pulse-in animation on the new values; point out that
  C jumps to 10 and D to 11.

> "Notice the value pills pulse-animate above each topic the moment
> it updates. C is ten, D is eleven. That is two parallel agent
> threads firing in turn &mdash; PlusAgent computes A plus B, then
> IncAgent picks up the new C and publishes C plus one."

#### Step 3 (~25&nbsp;s) &mdash; **Switch to `chain.conf`**

- Click the **`chain.conf` chip**.
- Notice the canvas widens to fit the seven columns.

> "Now a more complex topology &mdash; Result equals A plus B plus
> C, plus one. The canvas resizes automatically to fit seven
> columns. Notice the role-based colours: cyan inputs on the left,
> blue intermediates in the middle, pink output on the right."

#### Step 4 (~25&nbsp;s) &mdash; **Reset and re-publish to prove statelessness**

- Publish a few messages so values fill in.
- Click **Reset values**: every value clears, but the graph stays.
- Publish `A` = `99`. Show that Sum = 99 (not 99 + stale&nbsp;B),
  proving agent internal state was cleared too.

> "I click reset. Every topic value clears. The graph stays
> intact. I publish A equals ninety-nine; Sum is ninety-nine, not
> some number contaminated by old B &mdash; because reset also
> wipes every PlusAgent's internal x and y."

#### Step 5 (~30&nbsp;s) &mdash; **Show the SVG up close**

- Hover over an edge: point out the dashed-flow animation.
- Mention the legend in the corner.
- Open dev tools, show that the graph is real SVG, not a screenshot.

> "Two details I am proud of. The edges animate &mdash; the dashed
> stroke offset is keyframe-animated so the graph looks alive even
> when nothing is happening. And the entire view is just SVG plus
> CSS &mdash; no charting libraries, no canvas tag, no images."

---

## Slide 5 &mdash; Beyond the minimum (04:00 &rarr; 04:25)

> ### Things we added beyond the spec
>
> 1. Animated, role-coloured graph view (gradients, drop shadows,
>    flowing edges, pulse-in value pills)
> 2. A fourth servlet, `TopicReset`, to clear state without re-uploading
> 3. In-page example loader (chips for `simple.conf` &amp;
>    `chain.conf`)
> 4. Custom file picker (locale-proof, always English)
> 5. Dynamic canvas sizing (wider SVG for longer pipelines)
> 6. Two genuine bugs found by writing the view layer:
>    - Agent name collision in `Graph.createFromTopics`
>    - Broken longest-path BFS in the layout, fixed with Kahn's
>      algorithm

**What to say** (~25&nbsp;s):

> "Two of these I want to highlight. First, building the view layer
> exposed a real design bug: every PlusAgent instance was returning
> the same getName, so the graph deduplicated them into a single
> node. Once I fixed getName to include the output topic, two
> distinct PlusAgents finally got two distinct circles. Second, my
> first layout algorithm short-circuited the longest-path
> relaxation; replacing the BFS with Kahn's topological sort fixed
> it. Both bugs were invisible to the unit tests &mdash; only the
> visualisation surfaced them."

---

## Slide 6 &mdash; Lessons learned (04:25 &rarr; 04:45)

> ### What I take from this course
>
> - **SOLID is not a checklist** &mdash; it is the difference between
>   adding a feature in one line and rewriting three classes.
> - **A good visualisation is a debugging tool**, not decoration.
> - **Read the PDF every time.** Subtle one-liners (`reader.ready()`,
>   "delegate to another class") were the difference between 0 and
>   100 points in exercise 5.
> - **Active Object pays off** the moment a single agent slows down.
> - Pub/sub plus reflection plus a homemade HTTP server is enough to
>   build a surprising amount of product.

**What to say** (~20&nbsp;s):

> "To wrap up, three things stay with me. SOLID is not a checklist;
> applying it changes the cost of every future change. A good
> visualisation is a debugging tool. And the PDF is always the
> source of truth &mdash; subtle requirements buried in the text
> cost or saved real points more than once. Thank you for watching."

---

## End-card (04:45 &rarr; 05:00)

Repository URL, name, email, course details, and "Thanks for watching".

---

## Timing summary

| Slide | Topic | Cumulative |
|---|---|---|
| 1 | Title | 0:20 |
| 2 | Background | 1:00 |
| 3 | Design | 1:50 |
| 4 | Live demo | 4:00 |
| 5 | Beyond the minimum | 4:25 |
| 6 | Lessons learned | 4:45 |
| &mdash; | End-card | 5:00 |

## Recording checklist

- [ ] Server is running and `chain.conf` is ready to deploy
- [ ] Browser zoom set so all three iframes are visible at once
- [ ] System notifications muted (Slack, Mail, &hellip;)
- [ ] Mic level checked, no nearby fans / typing
- [ ] One full run-through *off-record* before the real take
- [ ] Final video uploaded to YouTube (unlisted) and the link pasted
      into the README

## Suggested tools

- **Slides:** Google Slides, Keynote, PowerPoint, or **Marp** (markdown
  &rarr; slides) if you prefer to keep everything in code
- **Recording:** Loom (easiest, instantly shareable) or OBS Studio
- **Trimming / overlays:** DaVinci Resolve, Premiere, or iMovie
