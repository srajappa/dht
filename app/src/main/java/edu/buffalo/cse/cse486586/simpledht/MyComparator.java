package edu.buffalo.cse.cse486586.simpledht;

import java.util.Comparator;

/**
 * Created by srajappa on 4/3/15.
 */
class MyComparator implements Comparator<NodeHashList> {
    @Override
    public int compare(NodeHashList o1, NodeHashList o2) {
        if (o1.hashCode.compareTo(o2.hashCode) <0) {
            return -1;
        } else if (o1.hashCode.compareTo(o2.hashCode) < 0) {
            return 1;
        }
        return 0;
    }}
