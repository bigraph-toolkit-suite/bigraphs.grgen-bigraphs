---
title: 'BiGGer: A Model Transformation Tool written in Java for Bigraph Rewriting in GrGen.NET'
tags:
  - Java
  - bigraphs
  - model transformation
  - bigraphical reactive systems
  - graph rewriting
authors:
  - name: Dominik Grzelak
    orcid: 0000-0001-6334-2356
    corresponding: true # (This is how to denote the corresponding author)
    affiliation: "1, 2" # (Multiple affiliations must be quoted)
affiliations:
 - name: Chair of Software Technology, Technische Universität Dresden, Germany
   index: 1
 - name: The Centre for Tactile Internet with Human-in-the-Loop (CeTI), Germany
   index: 2
date: 09 February 2024
bibliography: paper.bib
---

# Summary

Graphs are well-studied mathematical structures that have diverse applications in fields such as computer science, chemistry, biology and social sciences.
In this respect, graph rewriting is a powerful technique that allows for the manipulation of graph structures through the application of so-called *graph transformation rules*.[^1]
In other words, graph rewriting techniques elevate static graphs to the concept of time-varying graphs.

[^1]: The terms graph rewriting and graph transformation are used interchangeably.

BiGGer is a Java library that implements a novel approach to graph rewriting for *bigraphs*, as devised by Robin Milner [@milner_SpaceMotionCommunicating_2009], using the graph transformation tool *GrGen.NET* [@geiss_GrGenFastSPOBased_2006].[^2] 
Bigraphs provide a compositional framework to model graph structures with two semantic dimensions that can be reconfigured by rules.

[^2]: [https://grgen.de/](https://grgen.de/) 

With regard to BiGGer, bigraphs are specified using a meta-modeling approach [@grzelak_BigraphEcoreMetamodel_2023; @kehrer_EMOFCompliantAbstractSyntax_2016] that is based on the EMOF standard [@ISO19508_2014], which is common in the software engineering sciences, where this library is most useful.
Grounding bigraphs on metamodels facilitate its construction via a universal and platform-agnostic language.

Ultimately, this library transforms EMOF-complaint bigraphs into multigraphs that GrGen.NET can visualize and execute.
Furthermore, BiGGer is also shipped as a command-line tool for using the functionality via the terminal for experimentation.
The most challenging aspect was the accurate translation of bigraphical rules to SPO-based rules, given that bigraphs and GrGen.NET employ distinct approaches regarding graph rewriting.[^3]

[^3]: For an analysis of the interrelated aspects of these approaches, refer to [@milner_EmbeddingsContextsLink_2005; @ehrig_BigraphsMeetDouble_2004].


# Statement of Need

### Primary Purpose

The tool is built upon the formal principles of bigraphs, allowing for the intuitive and expressive representation of dynamic graph structures equipped with two semantic dimensions. In the domain of bigraph rewriting, the Java library and command-line utility BiGGer provide a highly efficient solution by streamlining the intricacies linked to model conversions and system simulation when bigraphs and GrGen.NET are utilized in conjunction. The fundamental objective of BiGGer is to establish an alternative and resilient setting for bigraph matching and rewriting. This will enable the simulation of state transitions in software applications, models, and systems, as well as in any other field where graph-structured data possesses inherent advantages over simple data structures such as scalars, lists, or pointers.

### Efficiency

When considering the manipulation of bigraphs through rules, the central elements that require implementation are bigraph matching and rewriting.
This issue is mathematically known as the *subgraph isomorphism problem*, a general variant of the graph isomorphism problem.
Until now, it is unknown whether the test for graph isomorphism can be solved in polynomial time or whether it is NP-complete.
Subgraph isomorphism is computationally more complex than graph isomorphism and is known to be NP-complete due to the combinatorial nature of finding subpatterns in graphs.
Thus, graph rewriting is NP-complete [@bacci_FindingForestTree_2014].

However, bigraphs require the matching and rewriting of two substructures.
Specifically, one substructure of a bigraph is a *forest*, which is relevant to the problem of locating forests in trees [@bacci_FindingForestTree_2014], while the other substructure is a *hypergraph*, which is associated with the subhypergraph matching problem.
To date, practical bigraph matching primarily exists as a CSP implementation [@miculan_CSPImplementationBigraph_2014], or SAT-based algorithm [@sevegnani_SATBasedAlgorithm_2010] in the literature.

GrGen.NET provides a universal framework written in C# implementing the so-called Single-Pushout (SPO) approach of graph rewriting for multigraphs (see, for example, [@ehrig_FundamentalsAlgebraicGraph_2006; @jakumeit_GrGenNETUser_2023]).
The choice to use GrGen.NET instead of other common graph rewrite systems in the software domain (e.g., AGG, PROGRES, FUJABA) is its execution performance with respect to matching and rewriting (refer to the benchmark described in [@geiss_ImprovementsVarroBenchmark_2007], which was introduced in [@varro_BenchmarkingGraphTransformation_2005] first).
Specifically, GrGen.NET's pattern matching engine employs some techniques that increase the practical execution performance of subgraph matching, which is an NP-complete problem but is at least one order of magnitude faster than comparable candidates [@jakumeit_GrGenNETUser_2023].

# Key Features

The tool implements a unidirectional transformation from bigraph models to GrGen.NET models, and generates several files that GrGen.NET can execute and visualize. 
These include GrGen.NET's graph metamodel (`*.gm`), the graph model conforming to the metamodel (`*.grs`), rules (`*.grg`), and a script that configures the initial graph state and defines a simple rule execution strategy (`*.grs`). 
In turn this means that BiGGer is able to process the signature, bigraph, rules and tracking maps specified in the Eclipse EMF Ecore format [@steinberg_EMFEclipseModeling_2008], which implements the EMOF standard [@ISO19508_2014]. 
Refer to [www.bigraphs.org](www.bigraphs.org) on how to model EMOF-compliant bigraphs in Java, visually or via the dedicated domain-specific language [@grzelak_BigraphicalDomainspecificLanguage_2021]. 

The functionality of BiGGer is offered via different interfaces suited for users of different backgrounds: programmatically in Java via the API, or by using the command-line tool.
Developers and students benefit from the integration of BiGGer with other software systems relying on bigraph rewriting via an object-oriented API.
BiGGer supports the efficient execution of transformation rules on large bigraphs currently available in the literature.

Moreover, BiGGer is shipped with sample bigraphs and rules and detailed instructions, which are essential in facilitating a better understanding of BiGGer's capabilities and usage.

# [State of the Field\label{sec:StateoftheField}]()

There are few practical solutions concerned with the bigraph matching problem for various kinds of bigraphs; a non-exhaustive presentation is the following. 
For *binding bigraphs* (i.e., links have local scopes) by an inductive characterization of matching [@birkedal_MatchingBigraphs_2007; @damgaard_InductiveCharacterizationMatching_2013]; for *directed bigraphs* (which subsume *pure bigraphs*) [@bacci_DBtkToolkitDirected_2009]; for \emph{bigraphs with sharing} (i.e., the place graph is a directed acyclic graph) by using a SAT-based algorithm [@sevegnani_BigraphERRewritingAnalysis_2016]; for the pure case by [@miculan_CSPImplementationBigraph_2014] as a CSP, and further an adapted reduction of the problem for directed bigraphs to a CSP [@chiapperini_ComputingEmbeddingsDirected_2020]; and the work in [@gassara_ExecutingBigraphicalReactive_2019], which proposes a toolchain for bigraph matching that is conceptually most similar to our approach but not actively developed anymore and not as efficient as GrGen.NET.

# Acknowledgements

Funded by the German Research Foundation (DFG, Deutsche  Forschungsgemeinschaft) as part of Germany’s Excellence Strategy – EXC  2050/1 – Project ID 390696704 – Cluster of Excellence "Centre for Tactile Internet with Human-in-the-Loop" (CeTI) of Technische  Universität Dresden.

# References