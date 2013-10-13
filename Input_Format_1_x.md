#Input Format for LinguaView 1.x
##1. Overview
LinguaView accepts inputs in xml format.
For those unfamiliar with xml, first take three minutes to read [this](http://www.w3schools.com/xml/xml_syntax.asp) (you can skip entity reference, since we won't use that).

An overview of the input format is like the following:

    <?xml version="1.0" encoding="utf-8" ?>
    <viewer>
    <sentence id="...">
        <wordlist length="...">
            wordlist goes here
        </wordlist>
        <constree> constituent parse tree goes here </constree>
        <deptree> dependency parse tree goes here </deptree>
        <deepdep> deep dependency graph goes here </deepdep>
        <lfg>
            <cstruct> constituent structure of LFG goes here </cstruct>
            <fstruct id="...">
                feature structure of LFG goes here
            </fstruct>
        </lfg>
        <ccg> CCG parse tree goes here </ccg>
    </sentence>
    <sentence id="...">
        ...
    </sentence>
    ...
    </viewer>

As you can see, the whole input contains several **sentences**.
Each sentence has a corresponding **wordlist**,
which tells LinguaView how this sentence is tokenized and what pos-tagging is attached to each sentence.
The most important part is the following descriptions for the parse trees, namely **constituent tree**, **dependency tree**, **deep dependency graph**, **LFG structure** and **CCG tree**.
These descriptions tell LinguaView how the sentence should be parsed so it can offer a graphical representation of these trees.

The rest of this introduction deals with details of each element.

##2. Wordlist
A wordlist looks like this:

    <wordlist length="...">
        <tok id="(id of token goes here, starting from 0)" head="..." />
        <tok id="..." head="..." />
        ...
    </wordlist>

Each "tok" tag should (at least) has attribute "id" and "head". Any other attributes could be added, but should also follow the format above.

We use the following sentence in Penn Treebank as an example:
> Pierre Vinken, 61 years old, will join the board as a nonexecutive director Nov. 29.

Suppose we take the word "board" as a token, the minimum representation of this token is as follows:

    <tok id="10" head="board" />

If you would like to add other attributes, you can continue before the slash, like this:

    <tok id="10" head="board" pos="NN" cgcat="N" />
    
##3. Constituent Tree
The format of constituent tree is completely in accordance of *Penn Treebank*.
Examples of Penn Treebank format are available [here](http://web.mit.edu/course/6/6.863/OldFiles/share/data/corpora/treebank/), which is a sample about 5% the size of complete Penn Treebank.

##4. Dependency Tree
Each edge of depedency tree is represented by a *tri-tuple*, which looks as follows:

    (head dependency label)

The elements in the tuple are separated by space.
The head and dependency are both represented by the id of tokens (starting from 0), assigned in the **wordlist**.
Taking the previous Penn Treebank sentence (mentioned in the **wordlist** part) as example, if there is an edge labeled "A" between "join" and "board", with "board" as the head, the corresponding representation in the input would be (8, 10, A).

Please be noted that there might be a *root* node in the dependency tree.
The root node will display if there are any edges with either an head or a dependency numbered -1.
Otherwise, it will not appear.

##5. Deep Dependency Graph
Same as **dependency tree**.

##6. LFG
The LFG format is as follows:

    <lfg>
        <cstruct> constituent structure of LFG goes here </cstruct>
        <fstruct id="...">
            feature structure of LFG goes here
        </fstruct>
    </lfg>

Obviously, an LFG is divided into two parts: constituent structure and feature structure.
Each structure is represented by a corresponding xml element.

There are only *one* constituent structure and *one* feature structure in each "LFG" tag.

###i). Constituent Structure
The representation of constituent structure is almost the same as **constituent tree**.
The only difference is that an id of feature structure should be tagged after the POS tagging if *correspondence* exists between the constituent structure and feature structure.
The format is as follows:

    POStag#id

Please note that there are *no space* between POStag, # and (feature structure) id.
For example the following correspondence tag is acceptable:

    ( (S (NP-SBJ#1 (NP Pierre Vinken)
                ,
                (ADJP (NP 61 years)
                      old)
                ...

The `#` tag above means that an correspondence exists between constituent "NP-SBJ" and feature structure numbered 1.

###ii). Feature Structure
The format of a feature structure is as follows:

    <fstruct id="f-structure id goes here">
        <attr name="attribute name goes here" valtype="value type goes here">
            attribute value goes here
        </attr>
    </fstruct>
    
The f-structure id is any *unique* number.
This id does not indicate any order.
It is only used to identify a feature structure.

The content of value type could be any one of the following:

* For simple value, `valtype` should be "atomic"
* For semantic form, `valtype` should be "sem"
* For a feature structure value, `valtype` should be "fstruct".
At this time a nested feature structure should be used as attribute value.
* For a value composed of multiple feature structures, `valtype` should be "set".
At this time several parallel, nested feature structures should be used as attribute value.

**Note:** To bind current attribute with a feature structure, `valtype` should be set as "fstruct".
As for the corresponding attribute value, only the feature structure id may be specified, instead of the complete content of that feature structure.
For example, if we want to bind the attribute with a feature structure numbered 1, the attribute representation is as follows:

    <attr name="SUBJ" valtype="fstruct">
        <fstruct id="1">
        </fstruct>
    </attr>

LinguaView will automatically search for the specified feature structure.
If it is not found, LinguaView presumes that you're trying to insert a void feature struct here and no binding will be done.

##7. CCG
The format of CCG parse tree is designed in accordance with the LDC2005T13 corpus, which is an translation of Penn Treebank into CCG derivations.
In this format, there are two types of CCG nodes: the internal node and the leaf node.
The format of leaf node is as follows:
> ({L CCGcat mod_POS-tag orig_POS-tag word PredArgCat})

which looks like this in the treebank:
> ({L N/N NNP NNP Pierre N_73/N_73})

The format of internal node is a recursive definition, as follows:
> ({T CCGcat head dtrs} child-node1 child-node2)

Note that child nodes could either be leaf nodes or internal nodes.

For more detailed explanation of each field and examples, please look at [the LDC2005T13 readme](http://catalog.ldc.upenn.edu/docs/LDC2005T13/AUTO.readme).
But please be noted that **all the angle brackets in this LDC2005T13 document have been substituted by curly brackets**, since angle brackets are reserved for xml formatting.
