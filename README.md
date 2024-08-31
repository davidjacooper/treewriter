# TreeWriter

A simple Java library to draw a console-based tree, in the manner often used to show file trees.

Say you store your trees in a memory-based data structure like this:

```java
interface MyNode
{
    Collection<MyNode> getChildNodes();
    String toString();
}
```

Then, `TreeWriter` can display a tree as follows:
```java
MyNode root = ...;
var tw = new TreeWriter();
tw.printTree(root, MyNode::getChildNodes, MyNode::toString);
```

However, there need not be a pre-existing data structure. `TreeWriter` can also display data in tree form where:

* Nodes can be obtained in depth-first order; and
* When a node is retrieved, it is immediately known whether it is the last node among its siblings.

```java
var tw = new TreeWriter();
tw.print("root node");

tw.startNode(true);
tw.print("child 1");

tw.startNode(false);
tw.print("child 1-1");
tw.endNode();
tw.endNode();

tw.startNode(false);
tw.print("child 2");
tw.endNode();
```

`TreeWriter` expands on the standard `PrintWriter` class, with additional methods for indicating where nodes start and end. The boolean values indicate whether any more sibling nodes are expected.
