# LLVM GUI

This tool allows you to run simple C programs in a code editor, select an order of optimizations, and even view
the control-flow-graph, dominator tree, or the post-dominator tree. This is a pedagogical tool that has been
adopted from the [GUI](https://github.com/LouisJenkinsCS/llvm-jvm-gui) used in my [LLVM-JVM project](https://github.com/LouisJenkinsCS/LLVM-JVM).
This has been modified to run C code rather than requiring the LLVM-JVM to compile Java down to Bytecode and then down to LLVM IR.

## Build Instructions

**Dependencies**

* `clang`
* `llvm`

**Build from Release**


'java -jar LLVM-Simple-GUI.jar [filename]' where 'filename' is an optional C file.

**Build from Source**

Advised to use NetBeans, as no Makefile will be provided!

## What does it do?

**Includes a Code Editor with basic [Syntax Highlighting](https://github.com/bobbylight/RSyntaxTextArea)!**

![CodeEditor](screenshots/CodeEditor.png)

**See the CFG, DOM Tree, and POSTDOM Tree!**

![CFG](screenshots/CFG.png)
![DOM](screenshots/DOM.png)
![POSTDOM](screenshots/POSTDOM.png)

**Select from various optimizations**

![OptimizationPanel](screenshots/OptimizationsPanel.png)

**See Before-And-After Transformations!**

![CFG](screenshots/CFG.png)
![After-Optimization](screenshots/After-Optimization.png)

**View Basic Block Headers Only for Larger Functions!**

![CFG Header](screenshots/CFG-Headers.png)
![DOM Header](screenshots/DOM-Headers.png)
![POSTDOM Header](screenshots/POSTDOM-Headers.png)
