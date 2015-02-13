#LinguaView
*Release Note:*

+ (As of Feb. 2015) LinguaView is undergoing major code refactoring in order to transform into a Google Web App
+ (As of Nov. 2013) 1.0.1 revised constituent part, so it can read trees that are written in multiple lines, like those in Penn-Treebank

LinguaView is an light-weight graphical tool aiming to aid manual construction of linguistically-deep corpuses.
It reads in a series of tagged sentences (which is organised by a [defined xml format](https://github.com/shuoyangd/LinguaView/blob/master/Input_Format_1_x.md)) and displays the corresponding syntax representations.
With a simple xml editor attached to the tool, the corpus builder may create or edit the syntax tags, and check his/her tagging work with the viewer at the first place.
Currently, viewers for context-free grammar, dependency structure, deep dependency graph, combinatory categorial grammar and lexical functional grammar have been supported.

LinguaView is developed by [Shuoyang Ding](mailto:shuoyangd@gmail.com), who was an intern research assistant in Language Computing & Web Mining Group, Peking University and is now a PhD student in Center for Speech and Language Processing, Johns Hopkins University.

Acknowledgement goes to Federico Sangati, the original author of the constituent viewer part, and also to Weiwei Sun and Chen Wang, authors of the data structures that constitute an implementation of CCG.

Currently LinguaView does not have a user manual.
But according to the report from the first few users, you may find the rest part very foolproof after you read [this introduction to the input format](https://github.com/shuoyangd/LinguaView/blob/master/Input_Format_1_x.md).

If you have any problems or would like to report bugs, please [contact the author](mailto:dsy100@gmail.com).
