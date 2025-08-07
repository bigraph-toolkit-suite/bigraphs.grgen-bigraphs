# BiGGer: A Model Transformation Tool for Bigraph Rewriting with GrGen.NET

---

**Latest Version:** 1.2.1 <a href='https://docshoster.org/p/bigraph-toolkit-suite/bigraphs.grgen-bigraphs/latest/introduction.html'><img src='https://docshoster.org/pstatic/bigraph-toolkit-suite/bigraphs.grgen-bigraphs/latest/badge.svg'/>
</a>

**Older Version:** 1.2.0 <a href='https://docshoster.org/p/bigraph-toolkit-suite/bigraphs.grgen-bigraphs/1.2.0/introduction.html'><img src='https://docshoster.org/pstatic/bigraph-toolkit-suite/bigraphs.grgen-bigraphs/1.2.0/badge.svg'/>
</a>

---

Cite BiGGer: [![DOI](https://joss.theoj.org/papers/10.21105/joss.06491/status.svg)](https://doi.org/10.21105/joss.06491)

---

> 📌 **Benchmarks:** Refer to the companion repository [BiggerBenchmarkSolution](https://github.com/bigraph-toolkit-suite/BiggerBenchmarkSolution), which accompanies this tool.

---

This project provides a model transformation tool called **BiGGer** that translates bigraphs and bigraphical reactive systems (BRS) to several [GrGen.NET](https://grgen.de/)-compatible model files that can be executed.
Effectively, this transformation enables bigraph rewriting in GrGen.NET.

[GrGen.NET](https://grgen.de/) is a framework for graph-based pattern matching and transformation.
It is primarily used for rule-based graph processing, which is especially useful in various applications related to modeling, code generation, and graph transformation.

The functionality of this project is offered via a command-line interface (CLI) and a Java API.
The tool implements a unidirectional transformation from bigraphs to GrGen.NET models.

**Which GrGen.NET file formats are generated?**

- `*.gm` (graph metamodel)
- `*.grs` (graph model)
- `*.grg` (rules)
- `*.grs` (script that defines a simple rule control strategy)

**What parts of the bigraph specification are translated?**

- Signatures (instance and metamodel)
- Bigraphs (instance and metamodel)
- Rules and Tracking Map

**What is not translated yet?**

- Place-Sorts
- Link-sorts
- Rule control instructions (however, a default one is provided and can be customized)
- Instantiation map of a reaction rule
- User-defined attributes on bigraph nodes/edges

## Getting Started

Before using the tool or library, you have to build the project (refer to [Development](#Development)), or use the shipped JAR, which is available via [GitHub Releases](https://github.com/bigraph-toolkit-suite/bigraphs.grgen-bigraphs/releases).

### Requirements

**For GrGen.NET**
- Mono for execution of GrGen.NET
  - See the following link on how to install Mono on macOS, Linux or Windows: https://www.mono-project.com/download/stable/#download-lin
- Java 11
  - For executing GrGen.NET's visualization (is done via yComp)
  - See also [Troubleshooting](#Troubleshooting)
- GrGen.NET Release 6.7
    - Requires Mono and Java 11
    - Needs to be installed on the host system
    - View the project website: https://grgen.de/
    - View the manual: https://grgen.de/GrGenNET-Manual.pdf

**For BiGGer**
- Java >= 17 and Maven >=3.8.3 
  - For development, building and execution of BiGGer

### Basic Usage via the Command-line:

This projects provides a CLI tool to access the transformation functionality.

> **Note:** An executable JAR of BiGGer can be found in the directory `./bin/` after [building the project](#build-configuration) or can be found on the [Releases](https://github.com/bigraph-toolkit-suite/bigraphs.grgen-bigraphs/releases) page.

**Example:**

```shell
java -jar bigger.jar \ 
  --host=/path/host.xmi \
  --sig=/path/sig.xmi --sigM=/path/signatureBaseModel.ecore \
  --rule=rule1:/path/l.xmi,/path/r.xmi \ 
  --rule=rule2:/path/l2.xmi,/path/r2.xmi \ 
  --rule=rule3:/path/l3.xmi,/path/r3.xmi \ 
  --tracking=/path/map.json
```

Bigraph instance models in the Ecore XMI format `*.xmi` (XML files) have to be provided as arguments, and their metamodels in the Ecore format `*.ecore`.
See [Bigraph Ecore Metamodel](https://bigraphs.org/) or [Bigraph Framework](https://bigraphs.org/products/bigraph-framework/) on how to create bigraphs in this format.
This topic is also briefly covered in section ["Basic usage in Java"](#Basic-usage-in-Java) and ["How to model bigraphs?"](#How-to-model-bigraphs)
The bare Bigraph Ecore Specification is also available [here](https://zenodo.org/records/10043063).

> **Note:** Check out the `./sample` folder that includes ready-to-use bigraph model files (signature, host bigraph, and rules).

#### Basic Arguments

##### **--output**

This option is _optional_.

The output directory, where BiGGer shall write all generated transformation files.
These can be used and executed by GrGen.NET.
Refer to section [Output](#output-of-the-tool).

##### **--host**

This option is _mandatory_.

The host graph for the rewriting in GrGen.NET.
The value of this argument is a file to the `*.xmi` file of the bigraph to be converted.
Only one host graph argument is allowed.

##### **--metamodel**

This option is _optional_.

The bigraphical metamodel in the `*.ecore` format for the host bigraph and rules can be passed explicitly.
The value of this argument is the file to this metamodel.

Example: `--metamodel=<FILE_TO_ECORE>`

The host bigraph and rules are always validated against a specification before the transformation.
This specification is called the _bigraph metamodel_.
When passing a Ecore file directly as argument, it is used instead of querying each XMI file for the corresponding Ecore
file.

If the argument `--metamodel` is not supplied, the default behavior is to use the Ecore file located
at `./bigraphBaseModel.ecore` relative to the current XMI file.
See the following example of an XMI file (excerpt) that is used to specify a host bigraph, redex or reactum of a rule:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bigraphBaseModel:BBigraph xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bigraphBaseModel="http://de.tudresden.inf.st.bigraphs.models" xsi:schemaLocation="http://de.tudresden.inf.st.bigraphs.models ./bigraphBaseModel.ecore">
<!-- content of a bigraph -->
</bigraphBaseModel:BBigraph>
```

> Every provided XMI model file (i.e., a signature, rule or a host bigraph model) has the attribute `xsi:schemaLocation`.
> The schema location attribute (`xsi:schemaLocation`) specifies the location of the Ecore file that describes the
> structure and constraints for validating the XMI document.
> This XML schema is referred to as the _bigraph metamodel_ (in the `*.ecore` format, which is basically an XML file),
> which can also be regarded as an XML Schema Definition (XSD).
> The attribute has two parts separated by a space:
> The first part is the namespace identifier (URI), e.g., http://de.tudresden.inf.st.bigraphs.models.
> The second part is the location of the Ecore model that defines the structure for the elements within that namespace.
> In this case, it's a URL to a specific Ecore file.


##### **--sig**, **--sigM**

These are _mandatory_ options.

Pass the signature for the host bigraph and rules as `*.xmi` via `--sig`.
Pass the signature metamodel for the signature as `*.ecore` via `--sigM`.

These options are similar to **--host** and **--metamodel** but for bigraphical signature (meta-)models.

##### **--rule**

This option is _optional_.

Multiple `--rule` options can be specified.
The format is as follows:

`--rule=<RULENAME>:<REDEX_FILE>,<REACTUM_FILE>`

where `<RULENAME>` is a unique name for the rule (optional).
If omitted, a generic label is created following a convention.
That is, rules can be named explicitly, otherwise they are assigned a generic prefix string "rule" suffixed with an incrementing
number, e.g., "rule1", "rule2", etc.
The order of arguments determine the index of a rule name.

Each rule that is passed requires a redex and reactum specification in `*.xmi`.
That is, after the colon `:` (required) the file path of the rule's redex as `*.xmi` file as to be provided followed by
the rule's reactum file.
Both must be separated by a comma `,` and are required values for the `--rule` argument.

Example: `--rule=rule3:sample/petrinet-simple/r3-lhs.xmi,sample/petrinet-simple/r3-rhs.xmi`

##### **--tracking**

This option is _optional_.
But when **--rule** ist used, this option is _mandatory_.

Additionally, for each rule a "tracking map" has to be supplied.
The format of this map is in JSON and looks like this:

```
{
    "rule1": {"map": [[r1,l1], [r2,l2], ...[rn,ln]], "links": ["outername1"] },
    "rule2": {"map": [[r1,l1], [r2,l2], ...[rm,lm]], "links": ["e0", "e1"] },
    "rule3": {"map": [["v0","v0"], ["v1",""], ["x","x"]], "links": ["x"] }
    ...
}
```

Each array represents the element mappings for a specific rule; thus, it must be assigned to a JSON
object (`"rule1": {"map": [ ... the mapping ... ]}`).
Either use the given name of the rule or use the generic autogenerated label when none is passed.
They follow a simple pattern as depicted above in the example.
Note that for the latter, the order of arguments plays an important role when supplying rules without names.

The array elements indicate the mapping of graph element IDs from the reactum to the redex of a rule (`[R1,L1]` means `R1 -> L1`).
Note that `Li` can also be empty (i.e., node addition).
A graph element can be a node or a link (edge or outer name).
Additionally, each mapped link has to be explicitly specified under the object `"links"` in each rule JSON object.

This file must contain _all_ tracking maps for _all_ rules.

Example: `--tracking=sample/petrinet-simple/map.json`

##### **--basepath**

This option is _optional_.

It specifies the base path for all (meta-)models (tracking map, rules, signature, host graph, ...).
If not specified, an absolute path or relative path for each model has to be specified for the respective argument.

### Output of the Tool

Executing the command creates several files:

- a metamodel graph `*.gm`
- the declaration of the initial host graph `*.grs` conforming to the metamodel graph `*.gm`
- a ruleset file `*.grg` containing all rule specifications
- a script file named `script.grs` for applying the rules (a simple rule execution strategy is used)

To customize the rule strategy, edit the generated script file `script.grs`.

After the transformation, you can execute the rewriting with GrGen.NET:
```shell
GrShell script.grs
```

### Basic usage in Java

This project can also be used as a library to access its functionality.
The following example shows how to instantiate a `BigraphTransformer` that
translates a pure bigraph into GrGen.NET's graph model format (`*.grs`):

```java
// Create a demo bigraph
DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();
DefaultDynamicSignature sig = signatureProvider.petriNet();
DemoBigraphProvider bigraphProvider = DemoBigraphProvider.getInstance();
PureBigraph bigraph = bigraphProvider.petriNet(sig);

// Start the bigraph2GrGen transformation for the host bigraph
PureBigraphTransformer transformer = new PureBigraphTransformer().withOppositeEdges(false);
String grgenGraphModel = transformer.toString(bigraph);
System.out.println(grgenGraphModel);

// Transform a rule
DemoRuleProvider ruleProvider = DemoRuleProvider.getInstance();
ReactionRule<PureBigraph> rr = ruleProvider.petriNetFireRule(sig);
PureParametrizedRuleTransformer t = new PureParametrizedRuleTransformer();
// Tracking map from reactum to redex elements (right to left of a rule)
TrackingMap trackingMap = RuleTransformer.createMap();
trackingMap.put("v0", "v0");
trackingMap.put("v2", "v3");
trackingMap.put("e1", "e1");
trackingMap.addLinkNames("e1");
t.withMap(trackingMap);
String grgenRule = t.toString(rr);

// Export the bigraph as *.xmi: This is part of the Bigraph Framework API
// BigraphFileModelManagement.Store.exportAsInstanceModel(bigraph, System.out);
// Export the metamodel of the bigraph as *.ecore        
// BigraphFileModelManagement.Store.exportAsMetaModel(bigraph, System.out);
```

The following transformers are available:
- `SignatureTransformer`
- `BigraphTransformer`
- `RuleTransformer`
Use the concrete implementations of these Java interfaces.

This project includes several demo providers that show how to create signatures, bigraphs and reaction rules with
Bigraph Framework.
To model bigraphs, the Java framework called [Bigraph Framework](https://bigraphs.org/products/bigraph-framework/) is
used.
See also section ["How to model bigraphs?"](#How-to-model-bigraphs).

### How to model bigraphs?

Visit [www.bigraphs.org](www.bigraphs.org) to learn more about how to model bigraphs using a:

- Java Framework
- DSL
- Converters/Exporters
- Visual Modeling Editor

All approaches allow exporting bigraphs and rules as `*.ecore` and `*.xmi` files.

### Docker Container Setup

This project is shipped also with a Dockerfile.
The container comes with Mono, GrGen.NET, and BiGGer already installed.
To build the Docker image and to run the interactive shell of the Docker container, execute the following commands:

```shell
cd docker
# Build Once:
docker build -t bigger-setup .
# Execute:
docker run -it bigger-setup /bin/bash
# With GUI (X11 forwarding):
xhost +local:root
docker run --rm -it --env DISPLAY --volume /tmp/.X11-unix:/tmp/.X11-unix:rw bigger-setup /bin/bash
```

Building can take some time, also more than 5 min.

To run a demo, issue the following commands inside the running Docker container:
```shell
# Change directory if you are not in /bigraphs.grgen-bigraphs/bin/
cd /bigraphs.grgen-bigraphs/bin/

# Execute BiGGer for the example use case 'concurrent-append'
java -jar bigger.jar --verbose --basepath=../sample/concurrent-append/ --host=host.xmi --output=foo --sig=sig.xmi --sigM=signatureMetaModel.ecore --metamodel=bigraphMetaModel.ecore --rule=nextRule:nextRule-lhs.xmi,nextRule-rhs.xmi --rule=appendRule:appendRule-lhs.xmi,appendRule-rhs.xmi --rule=returnRule:returnRule-lhs.xmi,returnRule-rhs.xmi --tracking=map.json

# Observe the resulting files
cd ../sample/concurrent-append/foo
GrShell script.grs
```

Note that you will not see GrGen.NET's graph visualization window (via yComp). You will probably see an exception.
However, the transformation of BiGGer and the execution of GrGen.NET rewriting can still be tested.

To remove the Docker image:
```shell
docker rmi -f $(docker images --filter=reference="bigger-setup" -q)
```

## Examples

The folder `./sample/` contains several demo scenarios.
Each scenario contains a signature, a bigraph meta- and instance model, and reaction rule models, all in the `*.ecore` and `*.xmi` format. 
Additionally, a tracking map is supplied for each rule in JSON format.
A README is also supplied explaining the execution.

In the following it is assumed that **BiGGer** is executed from the `bin/` directory of this project.

### Petri net Simple

A transition is fired.

```shell
java -jar bigger.jar --verbose \
  --basepath=../sample/petrinet-simple/ \
  --output=foo \
  --sig=sig.xmi --sigM=signatureBaseModel.ecore \
  --metamodel=bigraphBaseModel.ecore --host=host.xmi \
  --rule=rule1:r1-lhs.xmi,r1-rhs.xmi \
  --tracking=map.json
```

It is assumed that the tool is started from the `./bin/` folder from the root of this project.
So all paths inside the options as shown above are relative.

Since `../sample/petrinet-simple/` is set as the base path, the file paths of all other options are shortened and more readable.

> **Note:** The `--basepath` argument affects all other options.

The host graph is located in `./sample/petrinet-simple/host.xmi`, etc.
The output is generated in the folder `./sample/petrinet-simple/foo/`.

### Concurrent Append Problem

This use case models _n_ processes that want to add an integer to a linked list concurrently.
Three rules are involved.

```shell
java -jar bigger.jar --verbose \
  --basepath=../sample/concurrent-append/ \
  --output=foo \
  --sig=sig.xmi --sigM=signatureMetaModel.ecore \
  --metamodel=bigraphMetaModel.ecore --host=host.xmi \
  --rule=nextRule:nextRule-lhs.xmi,nextRule-rhs.xmi \
  --rule=appendRule:appendRule-lhs.xmi,appendRule-rhs.xmi \
  --rule=returnRule:returnRule-lhs.xmi,returnRule-rhs.xmi \
  --tracking=map.json
```

Output after executing the command (without `--verbose`):

```shell
Rules recognized successfully (3): [nextRule, appendRule, returnRule]
Loading now: /grgen-bigraphs/sample/concurrent-append/signatureMetaModel.ecore
Loading now: /grgen-bigraphs/sample/concurrent-append/sig.xmi
Signature loaded successfully.
Loading now: /grgen-bigraphs/sample/concurrent-append/bigraphMetaModel.ecore
Loading now: /grgen-bigraphs/sample/concurrent-append/host.xmi
Bigraph metamodel loaded successfully.
Host bigraph loaded successfully.
Conversion finished successfully. All model files are created in the folder /grgen-bigraphs/sample/concurrent-append/foo
```


## Development

### Folder Structure

- `./bin/`: Contains an executable binary (`*.jar`) of the command-line tool
- `./sample/`: Contains several demo scenarios. Each scenario includes a bigraphical host graph and signature (meta-)model, and some rules.
  - These files were generated using [Bigraph Framework](https://www.bigraphs.org/products/bigraph-framework/).
- `./src/`: The complete source code of this project including unit tests.
- `./libs/`: Additional Java libraries required for the development. Must be installed in the local Maven repository first.

### Executing the Binary

An executable tool in form of a `*.jar` is provided within the `./bin/` directory of this project after [building](#build-configuration).
Another way is to use the Docker image within the `docker/` folder (see section [Docker](#docker-container-setup)).

To start the application, issue the following command in the terminal:

```shell
java -jar bigger.jar -h
```

### Build Configuration

There are three options for building this project from source as described next.

The recommended one is the Fat-JAR approach designed for portability.

#### Create a Fat-JAR / Uber-JAR

All the dependencies are included in the generated JAR.

```shell
# Create the executable JAR
mvn clean package -PfatJar
mvn clean package -PfatJar -DskipTests #without running tests

# Execute the application
java -jar ./target/fatJar-<NAME>-<VERSION>.jar
java -jar ./bin/bigger.jar # the tool is also copied to the `bin/` folder
```

> **Note:** When executing this Maven goal, a ready to use tool is deployed using the Fat-JAR approach inside the `./bin/` folder.

#### Classpath-Approach (1): Relative Libs-Folder

The necessary dependencies are installed in your local Maven repository, and also copied in a local folder next to the
generated JAR and referred to at runtime.
That is, the classpath in the `MANIFEST.MF` is set to `libs/` (relative to the generated JAR).

```shell
# Create the executable JAR
mvn clean install -PlocalLib
# Execute the application
java -jar ./target/localLib-<NAME>-<VERSION>.jar
```

#### Classpath-Approach (2): Local Maven Repository

The necessary dependencies are installed in your local Maven repository, which is where the generated application refers
to.
That is, the classpath in the `MANIFEST.MF` is set to `~/.m2/repository/`.

```shell
# Create the executable JAR
mvn clean install -PlocalM2
# Execute the application
java -jar ./target/localM2-<NAME>-<VERSION>.jar
```

### Building the Documentation

The Java API Documentation for all versions (stable releases, SNAPSHOT releases, ...)
of **BiGGer** is available [here](https://docshoster.org/p/bigraph-toolkit-suite/bigraphs.grgen-bigraphs/latest/introduction.html). 

To manually generate the Java API documentation with javadoc, the `JAVA_HOME` environment variable must be configured.
Under Linux this can be achieved as follows: `export JAVA_HOME=$(readlink -f /usr/bin/javac | sed "s:bin/javac::")`.
Check that Java matches the [Requirements](#requirements).

Finally, run this command to build the API documentation:
```shell
mvn clean package -Pdocs -DskipTests
```

A dedicated profile `docs` is available that can be combined with other profiles and Maven goals
(refer also to [Build Configuration](#build-configuration)).

The generated API documentation can be found at `target/apidocs/` from the root of this project.
The respective javadoc `*.jar` is located in the `target/` folder.

## Misc

### Additional Features

- The GrGen.NET approach for bigraphs allows for tracking rules in bigraphs (enables model synchronization and causal reasoning)
- Outer names of an agent in a BRS can be renamed now (not possible in standard bigraph rewriting)

### Future Work

- Attribute evaluation (not possible in original bigraph rewriting)

### Dependencies

- [Maven](https://maven.apache.org/) is used as build and package management tool
- All external dependencies are automatically fetched via Maven from the Central Repository
- Central dependencies:
  - 'Bigraph Framework' and 'Bigraph Ecore Metamodel (BEM)' (for creating bigraph models that BiGGer understands)
  - See [Bigraph Ecore Metamodel](https://github.com/bigraph-toolkit-suite/bigraphs.bigraph-ecore-metamodel) or [Bigraph Framework](https://bigraphs.org/products/bigraph-framework/) on how to create bigraphs practically
  - See [here](https://zenodo.org/doi/10.5281/zenodo.10043062) for the bare _Bigraph Ecore Specification_ on Zenodo
  - See [[KeTG16]](https://doi.org/10.4204/EPTCS.231.2) for theoretical details of the abstract syntax tree of bigraphs


## Troubleshooting

If you get errors while using **BiGGer**, GrGen.NET or yComp, this section provides some help.

### **Error Description**: JDK Missing Library

This or similar errors occur when the Java installation misses a required library. This might be the case when the headless version of an OpenJDK package under Ubuntu was installed.

Error Message:
```shell
/opt/grgen/bin$ java -jar yComp.jar ./sample/concurrent-append/foo/host_graph.grs
Exception in thread "main" java.lang.UnsatisfiedLinkError: Can't load library: /usr/lib/jvm/java-11-openjdk-amd64/lib/libawt_xawt.so
	at java.base/java.lang.ClassLoader.loadLibrary(ClassLoader.java:2638)
	at java.base/java.lang.Runtime.load0(Runtime.java:768)
	at java.base/java.lang.System.load(System.java:1850)
	at java.base/java.lang.ClassLoader$NativeLibrary.load0(Native Method)
	at java.base/java.lang.ClassLoader$NativeLibrary.load(ClassLoader.java:2450)
	at java.base/java.lang.ClassLoader$NativeLibrary.loadLibrary(ClassLoader.java:2506)
	at java.base/java.lang.ClassLoader.loadLibrary0(ClassLoader.java:2705)
	at java.base/java.lang.ClassLoader.loadLibrary(ClassLoader.java:2656)
	at java.base/java.lang.Runtime.loadLibrary0(Runtime.java:830)
	at java.base/java.lang.System.loadLibrary(System.java:1886)
	at java.desktop/java.awt.Toolkit$3.run(Toolkit.java:1395)
	at java.desktop/java.awt.Toolkit$3.run(Toolkit.java:1393)
	at java.base/java.security.AccessController.doPrivileged(Native Method)
	at java.desktop/java.awt.Toolkit.loadLibraries(Toolkit.java:1392)
	at java.desktop/java.awt.Toolkit.<clinit>(Toolkit.java:1425)
	at java.desktop/java.awt.Component.<clinit>(Component.java:621)
	at de.unika.ipd.ycomp.YComp.main(YComp.java:114) 
```

**Solution**

On Ubuntu 20.04/22.04: `sudo apt install openjdk-11-jdk`

### **Error Description**: yComp's Swing UI cannot be created (wrong JDK)

You are not using Java 11 when running GrGen.NET-tools (e.g., GrShell), or GrGen.NET does not find the path to your JDK 11 installation.

Error Message:
```shell
Exception in thread "main" Exception in thread "main" java.lang.IllegalAccessError: superclass access check failed: class de.unika.ipd.ycomp.tooltip.YCompToolTipManager$Actions (in unnamed module @0x5a6d67c3) cannot access class sun.swing.UIAction (in module java.desktop) because module java.desktop does not export sun.swing to unnamed module @0x5a6d67c3
java.lang.IllegalAccessError: superclass access check failed: class de.unika.ipd.ycomp.tooltip.YCompToolTipManager$Actions (in unnamed module @0x5a6d67c3) cannot access class sun.swing.UIAction (in module java.desktop) because module java.desktop does not export sun.swing to unnamed module @0x5a6d67c3
	at java.base/java.lang.ClassLoader.defineClass1(Native Method)
	at java.base/java.lang.ClassLoader.defineClass1(Native Method)
	at java.base/java.lang.ClassLoader.defineClass(ClassLoader.java:1017)
	at java.base/java.security.SecureClassLoader.defineClass(SecureClassLoader.java:150)
	at java.base/java.lang.ClassLoader.defineClass(ClassLoader.java:1017)
	at java.base/jdk.internal.loader.BuiltinClassLoader.defineClass(BuiltinClassLoader.java:862)
	at java.base/java.security.SecureClassLoader.defineClass(SecureClassLoader.java:150)
	at java.base/jdk.internal.loader.BuiltinClassLoader.findClassOnClassPathOrNull(BuiltinClassLoader.java:760)
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClassOrNull(BuiltinClassLoader.java:681)
	at java.base/jdk.internal.loader.BuiltinClassLoader.defineClass(BuiltinClassLoader.java:862)
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:639)
	at java.base/jdk.internal.loader.BuiltinClassLoader.findClassOnClassPathOrNull(BuiltinClassLoader.java:760)
	at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188)
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClassOrNull(BuiltinClassLoader.java:681)
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:525)
	at de.unika.ipd.ycomp.tooltip.YCompToolTipManager.<init>(YCompToolTipManager.java:107)
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:639)
	at de.unika.ipd.ycomp.tooltip.YCompToolTipManager.<clinit>(YCompToolTipManager.java:76)
	at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188)
	at de.unika.ipd.ycomp.view.YCompView.createGraph2DView(YCompView.java:2022)
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:525)
	at de.unika.ipd.ycomp.view.YCompView.init(YCompView.java:1724)
	at de.unika.ipd.ycomp.tooltip.YCompToolTipManager.<init>(YCompToolTipManager.java:107)
	at de.unika.ipd.ycomp.tooltip.YCompToolTipManager.<clinit>(YCompToolTipManager.java:76)
	at de.unika.ipd.ycomp.view.YCompView.<init>(YCompView.java:1630)
	at de.unika.ipd.ycomp.view.YCompView.createGraph2DView(YCompView.java:2022)
	at de.unika.ipd.ycomp.YComp.main(YComp.java:114)
	at de.unika.ipd.ycomp.view.YCompView.init(YCompView.java:1724)
	at de.unika.ipd.ycomp.view.YCompView.<init>(YCompView.java:1630)
	at de.unika.ipd.ycomp.YComp.main(YComp.java:114)

```

**Solution**

Two solutions exist:

- A: Install Java 11. If multiple installations are present switch to Java 11: `sudo update-alternatives --config java`
- B: Locate the GrGen.NET installation folder (e.g., `/opt/grgen/bin/`) and modify the yComp execution scripts (`ycomp`or `ycomp.bat`) as follows:
  - `java --add-exports=java.desktop/sun.swing=ALL-UNNAMED -Xmx640m -jar "$YCOMP_HOME/yComp.jar" $*` (similar for the `*.bat` file). Since we have control over the Java 11 yComp's startup (GrGen.NET is calling yComp internally), we basically add a JVM option that exports the `sun.swing` module and allow any unnamed module to access this package.

## How to cite this tool

[![DOI](https://joss.theoj.org/papers/10.21105/joss.06491/status.svg)](https://doi.org/10.21105/joss.06491)

BibTeX:

```bibtex
@article{Grzelak_BiGGer_A_Model_2024,
  author = {Grzelak, Dominik},
  doi = {10.21105/joss.06491},
  journal = {Journal of Open Source Software},
  month = jun,
  number = {98},
  pages = {6491},
  title = {{BiGGer: A Model Transformation Tool written in Java for Bigraph Rewriting in GrGen.NET}},
  url = {https://joss.theoj.org/papers/10.21105/joss.06491},
  volume = {9},
  year = {2024}
}
```

## License

**BiGGer** is Open Source software released under the Apache 2.0 license.

```text
   Copyright 2025 Bigraph Toolkit Suite Developers

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
