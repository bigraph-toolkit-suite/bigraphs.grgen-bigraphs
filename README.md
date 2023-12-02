# BiGGer: A Model Transformation Tool for Bigraph Rewriting with GrGen.NET

**Version:** 1.0-SNAPSHOT

----

This project provides a model transformation tool called **BiGGer** that translates bigraphs and bigraphical reactive systems
(BRS) to several [GrGen.NET](https://grgen.de/)-compatible model files that can be executed.
Effectively, this transformation enables bigraph rewriting in GrGen.NET.

[GrGen.NET](https://grgen.de/) is a framework for graph-based pattern matching and transformation.
It is primarily used for rule-based graph processing, which is especially useful in various applications related to
modeling, code generation, and graph transformation.

The functionality of this project is offered via a command-line interface (CLI) and a Java API.
The tool implements a unidirectional transformation from bigraphs to GrGen.NET models.

**Which file formats are generated?**

- `*.gm` (graph metamodel)
- `*.grs` (graph model)
- `*.grg` (rules)
- `*.grs` (script that defines a simple rule control strategy)

**What is translated?**

- Signatures
- Bigraphs
- Rules and Tracking Map

**What is not translated?**

- Place-Sorts
- Link-sorts
- Rule control instructions (however, a default one is provided and can be customized)
- Instantiation map of a rule
- User-defined attributes on bigraph nodes/edges

**Additional Features**

- GrGen.NET approach allows for tracking rules in bigraphs (enables model synchronization and causal reasoning)
- Outer names of an agent in a BRS can be renamed now (not possible in standard bigraph rewriting)

**Future Work**

- Attribute evaluation (not possible in standard bigraph rewriting)

## Getting Started

Before using the tool or library you have to build the project, or use the pre-compiled JAR, which includes all
dependencies (refer also to [Development](#Development)).

**Requirements**

- GrGen.NET Release 6.7
    - Needs to be installed on the host system 
    - View the project website: https://grgen.de/
    - View the manual: https://grgen.de/GrGenNET-Manual.pdf
- Java 17 and Maven >=3.8.3
- Bigraph Framework and Ecore Metamodel (for creating bigraph models that BiGGer understands)
  - Are specified as external dependencies
  - See [Bigraph Ecore Metamodel](https://github.com/bigraph-toolkit-suite/bigraphs.bigraph-ecore-metamodel) or [Bigraph Framework](https://bigraphs.org/products/bigraph-framework/) on how to create bigraphs practically
  - See [here](https://zenodo.org/doi/10.5281/zenodo.10043062) for the bare _Bigraph Ecore Specification_ on Zenodo
  - See [[KeTG16]](https://doi.org/10.4204/EPTCS.231.2) for theoretical details

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

Bigraph instance models in the Ecore XMI format `*.xmi` (XML files) have to be provided as arguments, and
their metamodels in the Ecore format `*.ecore`.
See [Bigraph Ecore Metamodel](https://bigraphs.org/) or [Bigraph Framework](https://bigraphs.org/products/bigraph-framework/) on how
to create bigraphs in this format.
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

```
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

Additionally, for each rule a tracking map has to be supplied.
The format of this map is in JSON and looks like this:

```
{
    "rule1": {"map": [[r1,l1], [r2,l2], ...[rn,ln]], "links": ["outername1"] },
    "rule2": {"map": [[r1,l1], [r2,l2], ...[rm,lm]], "links": ["e0", "e1"] },
    "rule3": {"map": [["v0","v0"], ["v1",""], ["x","x"]], "links": ["x"] }
    ...
}
```

Each array represents the element mappings for a specific rule, thus, it must be assigned to a JSON
object (`"rule1": {"map": [ ... the mapping ... ]}`).
Either use the given name of the rule or use the generic autogenerated label when none is passed.
Note that for the latter the order of arguments play an important role when supplying rules without names.
The array elements indicate the mapping of graph elements from the reactum to the redex of a rule (`[r1,l1]`
means `r1 -> l1`).
Note that `li` can also be empty (i.e., node addition).
A graph element can be a node or a link (edge or outer name).
Additionally, each mapped link has to be explicitly specified under the object `"links"` in each rule JSON object.

This file must contain all tracking maps for all rules.

Example: `--tracking=sample/petrinet-simple/map.json`

##### **--basepath**

This option is _optional_.

It specifies the base path for all (meta-)models (tracking map, rules, signature, host graph, ...).
If not specified, an absolute path or relative path for each model has to be specified.

### Output of the Tool

Executing the command creates several files:

- a metamodel graph `*.gm`
- the declaration of the initial host graph `*.grs` conforming to the metamodel graph `*.gm`
- a ruleset file `*.grg` containing all rule specifications
- a script file named `script.grs` for applying the rules (a simple control strategy is used)

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
DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();
DefaultDynamicSignature sig = signatureProvider.petriNet();
DemoBigraphProvider bigraphProvider = DemoBigraphProvider.getInstance();
PureBigraph bigraph = bigraphProvider.petriNet(sig);
PureBigraphTransformer transformer = new PureBigraphTransformer().withOppositeEdges(false);
String grgenGraphModel = transformer.toString(bigraph);
System.out.println(grgenGraphModel);

// Export the bigraph as *.xmi
// BigraphFileModelManagement.Store.exportAsInstanceModel(bigraph, System.out);
// Export the metamodel of the bigraph as *.ecore        
// BigraphFileModelManagement.Store.exportAsMetaModel(bigraph, System.out);
```

The following transformers are available:
- `SignatureTransformer`
- `BigraphTransformer`
- `RuleTransformer`
Use the concrete implementations of these Java interfaces.

This project includes several demo providers that show how to create signatures, bigraphs and reactules with
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

All approaches allow to export bigraphs and rules as `*.ecore` and `*.xmi` files.

## Examples

The folder `./sample/` contains several demo scenarios.
Each scenario contains a signature and a bigraph (meta-)model, and also reaction rule models in the `*.ecore` and `*.xmi` format. 
Additionally, a tracking map is supplied for each rule.

### Petri net Simple

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

Since `../sample/petrinet-simple/` is set as the base path, the file paths of all other options is shortened and more readable.
The base path argument affects all other options.

The host graph is located in `./sample/petrinet-simple/host.xmi`, etc.
The output is generated in the folder `./sample/petrinet-simple/foo/`.

## Development

### Folder Structure

- `./bin/`: Contains an executable binary (`*.jar`) of the command-line tool
- `./sample/`: Contains several demo scenarios. Each scenario includes a bigraphical host graph and signature (meta-)model, and some rules.
  - These files were generated using [Bigraph Framework](https://www.bigraphs.org/products/bigraph-framework/).
- `./src/`: The complete source code of this project including unit tests.
- `./libs/`: Additional Java libraries required for the development. Must be installed in the local Maven repository first.

### Pre-Build Binary

An executable tool in form of a `*.jar` is provided within the `./bin/` directory of this project after [building](#build-configuration).

To start the application, issue the following command in the terminal:

```shell
java -jar bigger.jar -h
```

### Build Configuration

There are also three other options for building this project from source as described next.

The recommended one is the Fat-JAR approach.

#### Create a Fat-JAR / Uber-JAR

All the dependencies are included in the generated JAR.

```shell
# Create the executable JAR
mvn clean package -PfatJar
# Execute the application
java -jar ./target/fatJar-<NAME>-<VERSION>.jar
java -jar ./bin/bigger.jar # the tool is also copied to the `bin/` folder
```

> **Note:** When executing the Maven goal, a ready to use tool is deployed using the Fat-JAR approach inside the `./bin/` folder.

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

## License

**BiGGer** is Open Source software released under the Apache 2.0 license.

```text
   Copyright 2023 Dominik Grzelak

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
